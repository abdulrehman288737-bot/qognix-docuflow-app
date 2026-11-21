package com.qognix.docuflow;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

// Local storage for Qognix DocuFlow.
// - Gemini key: app-private SharedPreferences only, never in code.
// - Documents: a small library in the app's private files folder. Each PDF's
//   extracted text is a private file; an index line keeps name/pages/size.
//   Nothing is uploaded anywhere by this class.
// - History: generated results saved privately, with their own index.
// Index line format uses '|' separators; names are sanitized on save.
public class Store {
    public static SharedPreferences prefs(Context c) { return c.getSharedPreferences("qognix", Context.MODE_PRIVATE); }

    // ---- Gemini key ----
    public static String key(Context c) { return prefs(c).getString("gemini_key", ""); }
    public static void setKey(Context c, String k) { prefs(c).edit().putString("gemini_key", k.trim()).apply(); }

    // ---- Preferences ----
    public static String theme(Context c) { return prefs(c).getString("theme", "dark"); }
    public static void setTheme(Context c, String t) { prefs(c).edit().putString("theme", t).apply(); }
    public static String uiLang(Context c) { return prefs(c).getString("ui_lang", "English"); }
    public static void setUiLang(Context c, String l) { prefs(c).edit().putString("ui_lang", l).apply(); }

    static String clean(String s) { return s == null ? "" : s.replace("|", "-").replace("\n", " ").trim(); }

    // ---- Document library ----
    static File docsDir(Context c) { File d = new File(c.getFilesDir(), "docs"); if (!d.exists()) d.mkdirs(); return d; }
    static File histDir(Context c) { File d = new File(c.getFilesDir(), "history"); if (!d.exists()) d.mkdirs(); return d; }

    static List<String[]> readIndex(Context c, String prefKey) {
        List<String[]> out = new ArrayList<>();
        String raw = prefs(c).getString(prefKey, "");
        if (raw == null || raw.isEmpty()) return out;
        for (String line : raw.split("\n")) {
            if (line.trim().isEmpty()) continue;
            out.add(line.split("\\|", -1));
        }
        return out;
    }

    static void writeIndex(Context c, String prefKey, List<String[]> rows, int cols) {
        StringBuilder b = new StringBuilder();
        for (String[] r : rows) {
            for (int i = 0; i < cols; i++) {
                if (i > 0) b.append('|');
                b.append(i < r.length ? clean(r[i]) : "");
            }
            b.append('\n');
        }
        prefs(c).edit().putString(prefKey, b.toString()).apply();
    }

    // Returns list of {id, name, pages, sizeKb, timestamp}
    public static List<String[]> docs(Context c) { return readIndex(c, "doc_index"); }

    public static String saveDoc(Context c, String name, String text, int pages, long sizeBytes) {
        String id = String.valueOf(System.currentTimeMillis());
        try {
            FileOutputStream o = new FileOutputStream(new File(docsDir(c), id + ".txt"));
            o.write(text.getBytes("UTF-8"));
            o.close();
        } catch (Exception e) { return ""; }
        List<String[]> rows = docs(c);
        rows.add(0, new String[]{id, clean(name), String.valueOf(pages), String.valueOf(Math.max(1, sizeBytes / 1024)), id});
        writeIndex(c, "doc_index", rows, 5);
        prefs(c).edit().putString("current_doc", id).putString("doc_name", clean(name)).apply();
        return id;
    }

    // Backward-compatible save (used by older call sites).
    public static void saveDoc(Context c, String name, String text) throws Exception {
        saveDoc(c, name, text, 0, text.getBytes("UTF-8").length);
    }

    public static String currentId(Context c) { return prefs(c).getString("current_doc", ""); }

    public static void setCurrent(Context c, String id) {
        for (String[] r : docs(c)) {
            if (r[0].equals(id)) {
                prefs(c).edit().putString("current_doc", id).putString("doc_name", r[1]).apply();
                return;
            }
        }
    }

    public static String docName(Context c) { return prefs(c).getString("doc_name", ""); }

    public static String[] currentMeta(Context c) {
        String id = currentId(c);
        for (String[] r : docs(c)) if (r[0].equals(id)) return r;
        return null;
    }

    static String readFile(File f) {
        try {
            if (!f.exists()) return "";
            byte[] b = new byte[(int) f.length()];
            FileInputStream i = new FileInputStream(f);
            int off = 0, r;
            while (off < b.length && (r = i.read(b, off, b.length - off)) > 0) off += r;
            i.close();
            return new String(b, "UTF-8");
        } catch (Exception e) { return ""; }
    }

    public static String loadDoc(Context c) {
        String id = currentId(c);
        if (id == null || id.isEmpty()) {
            // Migration from v2.x single-doc storage.
            return readFile(new File(c.getFilesDir(), "doc.txt"));
        }
        return readFile(new File(docsDir(c), id + ".txt"));
    }

    public static String loadDocById(Context c, String id) { return readFile(new File(docsDir(c), id + ".txt")); }

    public static void renameDoc(Context c, String id, String newName) {
        List<String[]> rows = docs(c);
        for (String[] r : rows) if (r[0].equals(id)) r[1] = clean(newName);
        writeIndex(c, "doc_index", rows, 5);
        if (id.equals(currentId(c))) prefs(c).edit().putString("doc_name", clean(newName)).apply();
    }

    public static void deleteDoc(Context c, String id) {
        List<String[]> rows = docs(c);
        List<String[]> keep = new ArrayList<>();
        for (String[] r : rows) if (!r[0].equals(id)) keep.add(r);
        writeIndex(c, "doc_index", keep, 5);
        new File(docsDir(c), id + ".txt").delete();
        if (id.equals(currentId(c))) {
            prefs(c).edit().remove("current_doc").putString("doc_name", keep.isEmpty() ? "" : keep.get(0)[1]).apply();
            if (!keep.isEmpty()) prefs(c).edit().putString("current_doc", keep.get(0)[0]).apply();
        }
    }

    // ---- History of generated results ----
    // Returns list of {id, type, docName, timestamp}
    public static List<String[]> history(Context c) { return readIndex(c, "hist_index"); }

    public static void addHistory(Context c, String type, String docName, String content) {
        String id = String.valueOf(System.currentTimeMillis());
        try {
            FileOutputStream o = new FileOutputStream(new File(histDir(c), id + ".txt"));
            o.write(content.getBytes("UTF-8"));
            o.close();
        } catch (Exception e) { return; }
        List<String[]> rows = history(c);
        rows.add(0, new String[]{id, clean(type), clean(docName), id});
        writeIndex(c, "hist_index", rows, 4);
    }

    public static String loadHistory(Context c, String id) { return readFile(new File(histDir(c), id + ".txt")); }

    public static void deleteHistory(Context c, String id) {
        List<String[]> rows = history(c);
        List<String[]> keep = new ArrayList<>();
        for (String[] r : rows) if (!r[0].equals(id)) keep.add(r);
        writeIndex(c, "hist_index", keep, 4);
        new File(histDir(c), id + ".txt").delete();
    }

    public static void clearHistory(Context c) {
        for (String[] r : history(c)) new File(histDir(c), r[0] + ".txt").delete();
        prefs(c).edit().putString("hist_index", "").apply();
    }

    public static String storageInfo(Context c) {
        long total = 0;
        File[] fs = docsDir(c).listFiles();
        if (fs != null) for (File f : fs) total += f.length();
        File[] hs = histDir(c).listFiles();
        if (hs != null) for (File f : hs) total += f.length();
        return docs(c).size() + " documents, " + history(c).size() + " saved results, " + Math.max(1, total / 1024) + " KB used (private app storage)";
    }
}
