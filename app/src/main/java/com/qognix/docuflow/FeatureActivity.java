package com.qognix.docuflow;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.OutputStream;

// One screen for the text-based tools: Summary, MCQs, Ask, Translate,
// Important Points, Key Terms, Chapter Summary and Explain Simply.
// Every result can be copied, shared, downloaded and is saved to History.
public class FeatureActivity extends AppCompatActivity {
    String mode, result = "";
    TextView out, statusLine;
    EditText extra;
    Spinner lang;
    Button go;
    String[] langs = {"Urdu", "English", "Arabic", "Hindi", "Spanish", "French", "Turkish", "Bengali", "Punjabi", "Pashto"};
    int summaryLevel = 1; // 0 short, 1 medium, 2 detailed
    Button[] levelBtns = new Button[3];
    boolean generated = false, busy = false;

    static String titleOf(String mode) {
        switch (mode) {
            case "mcq": return "MCQ Generator";
            case "ask": return "Ask Your PDF";
            case "translate": return "PDF Translator";
            case "points": return "Important Points";
            case "terms": return "Key Terms & Definitions";
            case "chapter": return "Chapter-wise Summary";
            case "explain": return "Document Explanation";
            default: return "AI Summary";
        }
    }

    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(s);
        Ui.applyTheme(this);
        Ads.init(this);
        Ads.preloadInterstitial(this);
        mode = getIntent().getStringExtra("mode");
        if (mode == null) mode = "summary";
        LinearLayout root = Ui.root(this);
        root.addView(Ui.text(this, titleOf(mode), 24, Ui.TEXT, true));
        root.addView(Ui.text(this, "Document: " + Store.docName(this), 13, Ui.SUB, false));

        if (mode.equals("summary")) {
            root.addView(Ui.text(this, "Summary length", 14, Ui.SUB, true));
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            String[] names = {"Short", "Medium", "Detailed"};
            for (int i = 0; i < 3; i++) {
                final int lvl = i;
                Button b = Ui.ghostButton(this, names[i]);
                b.setOnClickListener(v -> { summaryLevel = lvl; paintLevels(); });
                LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, -2, 1f);
                p.setMargins(4, 4, 4, 4);
                row.addView(b, p);
                levelBtns[i] = b;
            }
            root.addView(row);
            paintLevels();
        }
        if (mode.equals("ask")) {
            extra = Ui.input(this, "Type your question about the PDF", true);
            root.addView(extra);
        }
        if (mode.equals("explain")) {
            extra = Ui.input(this, "Optional: paste a difficult passage, or leave empty to explain the whole document", true);
            root.addView(extra);
        }
        if (mode.equals("translate")) {
            root.addView(Ui.text(this, "Translate into", 14, Ui.SUB, true));
            lang = new Spinner(this);
            ArrayAdapter<String> ad = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, langs) {
                @Override
                public View getView(int pos, View cv, ViewGroup par) {
                    TextView t = (TextView) super.getView(pos, cv, par);
                    t.setTextColor(Ui.TEXT);
                    t.setPadding(24, 20, 24, 20);
                    return t;
                }
            };
            lang.setAdapter(ad);
            lang.setBackground(Ui.bg(Ui.CARD, 24, 1));
            String def = Store.uiLang(this);
            for (int i = 0; i < langs.length; i++) if (langs[i].equals(def)) lang.setSelection(i);
            root.addView(lang);
        }

        go = Ui.button(this, "Generate");
        root.addView(go);
        go.setOnClickListener(v -> generate());
        statusLine = Ui.text(this, "", 13, Ui.SUB, false);
        root.addView(statusLine);
        out = Ui.text(this, "Press Generate.", 15, Ui.TEXT, false);
        out.setTextIsSelectable(true);
        root.addView(out);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        Button copy = Ui.ghostButton(this, "Copy");
        copy.setOnClickListener(v -> {
            if (result.isEmpty()) { toast("Generate first"); return; }
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText("Qognix " + titleOf(mode), result));
            toast("Copied");
        });
        Button share = Ui.ghostButton(this, "Share");
        share.setOnClickListener(v -> {
            if (result.isEmpty()) { toast("Generate first"); return; }
            Intent sh = new Intent(Intent.ACTION_SEND);
            sh.setType("text/plain");
            sh.putExtra(Intent.EXTRA_SUBJECT, "Qognix DocuFlow — " + titleOf(mode));
            sh.putExtra(Intent.EXTRA_TEXT, result);
            startActivity(Intent.createChooser(sh, "Share result"));
        });
        Button dl = Ui.ghostButton(this, "Download .txt");
        dl.setOnClickListener(v -> {
            if (result.isEmpty()) { toast("Generate first"); return; }
            Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            i.addCategory(Intent.CATEGORY_OPENABLE);
            i.setType("text/plain");
            i.putExtra(Intent.EXTRA_TITLE, "qognix-" + mode + ".txt");
            startActivityForResult(i, 7);
        });
        actions.addView(copy, Ui.third());
        actions.addView(share, Ui.third());
        actions.addView(dl, Ui.third());
        root.addView(actions);
        setContentView(Ui.scroll(this, root));
    }

    void paintLevels() {
        for (int i = 0; i < 3; i++) {
            if (levelBtns[i] == null) continue;
            levelBtns[i].setBackground(Ui.bg(i == summaryLevel ? Ui.VIOLET : Ui.CARD, 28, 1));
            levelBtns[i].setTextColor(i == summaryLevel ? 0xFFFFFFFF : Ui.TEXT);
        }
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == 7 && res == RESULT_OK && data != null && data.getData() != null) {
            try {
                OutputStream o = getContentResolver().openOutputStream(data.getData());
                o.write(result.getBytes("UTF-8"));
                o.close();
                toast("Saved");
            } catch (Exception e) { toast("Save failed: " + e.getMessage()); }
        }
    }

    @Override
    public void onBackPressed() {
        if (generated) {
            Ads.showTransition(this, () -> finish());
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void finish() {
        super.finish();
        Ui.backAnim(this);
    }

    void generate() {
        if (busy) return;
        String doc = Store.loadDoc(this);
        if (doc.isEmpty()) { out.setText("No PDF loaded. Go back and upload a PDF first."); return; }
        busy = true;
        go.setEnabled(false);
        statusLine.setText("Working…");
        out.setText("");
        String key = Store.key(this);
        String q = extra != null ? extra.getText().toString() : "";
        String target = lang != null ? langs[lang.getSelectedItemPosition()] : "";
        if (key.isEmpty()) {
            finishWith(local(doc, q), "On-device mode");
            return;
        }
        String snippet = doc.length() > 12000 ? doc.substring(0, 12000) : doc;
        String prompt = promptFor(snippet, q, target);
        GeminiClient.generate(key, prompt, new GeminiClient.Cb() {
            @Override
            public void ok(String t, String m) {
                runOnUiThread(() -> finishWith(t + "\n\n— Generated with Gemini (" + m + ")", "Gemini"));
            }

            @Override
            public void fail(String msg) {
                runOnUiThread(() -> finishWith(local(doc, q) + "\n\n(Gemini failed, on-device mode used: " + msg + ")", "On-device mode"));
            }
        });
    }

    void finishWith(String text, String how) {
        busy = false;
        go.setEnabled(true);
        result = text;
        generated = true;
        statusLine.setText(how + " • Saved to History");
        out.setText(result);
        Store.addHistory(this, titleOf(mode), Store.docName(this), result);
    }

    String promptFor(String snippet, String q, String target) {
        switch (mode) {
            case "summary": {
                String len = summaryLevel == 0 ? "short (about 120 words)" : summaryLevel == 2 ? "detailed (about 500 words, with headings for each main section)" : "medium (about 280 words)";
                return "Write a " + len + " summary of this document for a student. Keep the important facts and headings. Do not add information that is not in the document.\n\nDocument:\n" + snippet;
            }
            case "mcq":
                return "Make 10 multiple-choice questions from this document. For each: the question, 4 options labelled A) B) C) D), the correct answer, and one line explaining why it is correct. Base everything only on the document.\n\nDocument:\n" + snippet;
            case "ask":
                return "Answer this question using ONLY this document. The document has [Page N] markers — cite the page like (Page 3) for each fact you use. If the document does not contain the answer, say so clearly instead of guessing.\n\nQuestion: " + q + "\n\nDocument:\n" + snippet;
            case "translate":
                return "Translate this document text into " + target + ". Keep the headings and paragraph structure. Translate faithfully, do not summarize.\n\nDocument:\n" + snippet;
            case "points":
                return "Extract the 12 most important points from this document as a numbered list. Use only information that is in the document.\n\nDocument:\n" + snippet;
            case "terms":
                return "Extract the key terms from this document. For each term give: the term, and a clear one-or-two line definition based on how the document uses it. Only include terms that actually appear in the document.\n\nDocument:\n" + snippet;
            case "chapter":
                return "Give a chapter-wise (section-by-section) summary of this document. Detect the document's own sections or chapters where possible and use their names as headings; otherwise group by topic. Summarize each section in 2-4 lines. Use only the document's content.\n\nDocument:\n" + snippet;
            case "explain": {
                String src = q != null && !q.trim().isEmpty() ? q : snippet;
                return "Explain the following text in very simple, easy language for a student. Use short sentences and a simple example if it helps. Stay faithful to the original meaning.\n\nText:\n" + src;
            }
            default:
                return "Summarize this document:\n\n" + snippet;
        }
    }

    String local(String doc, String q) {
        switch (mode) {
            case "summary": return LocalTools.summaryLen(doc, summaryLevel);
            case "mcq": return LocalTools.mcqs(doc, false);
            case "ask": return LocalTools.ask(doc, q);
            case "points": return LocalTools.points(doc);
            case "terms": return LocalTools.terms(doc);
            case "chapter": return LocalTools.chapters(doc);
            case "explain": {
                String src = q != null && !q.trim().isEmpty() ? q : doc;
                return LocalTools.explain(src);
            }
            case "translate":
                return "Translation needs a Gemini key in this app. Add your free key in Settings, then try again.\n\nOriginal text preview:\n\n" + (doc.length() > 2000 ? doc.substring(0, 2000) : doc);
            default: return LocalTools.summary(doc);
        }
    }

    void toast(String m) { Toast.makeText(this, m, Toast.LENGTH_LONG).show(); }
}
