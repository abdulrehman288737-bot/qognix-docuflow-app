package com.qognix.docuflow;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;

import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.text.PDFTextStripper;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// Reads a PDF on a background thread, page by page, so Ask-the-PDF can cite
// page numbers. Extracted text carries "[Page N]" markers. Two clean-ups
// happen here (v3.4): the size cap is generous so later pages of big books
// are not cut off, and repeated website watermarks/headers printed on many
// pages of downloaded PDFs are stripped out. Nothing leaves the device.
public class PdfHelper {
    public interface Cb {
        void done(String name, int pages, long sizeBytes, String text);
        void fail(String message);
    }

    private static final int MAX_CHARS = 1500000;

    public static void load(final Context c, final Uri uri, final Cb cb) {
        new Thread(() -> {
            try {
                String name = "document.pdf";
                long size = 0;
                try {
                    Cursor cur = c.getContentResolver().query(uri, null, null, null, null);
                    if (cur != null && cur.moveToFirst()) {
                        int ni = cur.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                        int si = cur.getColumnIndex(OpenableColumns.SIZE);
                        if (ni >= 0) name = cur.getString(ni);
                        if (si >= 0) size = cur.getLong(si);
                        cur.close();
                    }
                } catch (Exception ignored) {}
                InputStream in = c.getContentResolver().openInputStream(uri);
                PDDocument doc = PDDocument.load(in);
                int pages = doc.getNumberOfPages();
                PDFTextStripper stripper = new PDFTextStripper();
                List<String> pageTexts = new ArrayList<>();
                for (int p = 1; p <= pages; p++) {
                    stripper.setStartPage(p);
                    stripper.setEndPage(p);
                    String pageText = stripper.getText(doc);
                    pageTexts.add(pageText == null ? "" : pageText);
                }
                doc.close();
                if (in != null) in.close();

                Set<String> watermarks = findWatermarks(pageTexts);
                StringBuilder b = new StringBuilder();
                for (int p = 1; p <= pages; p++) {
                    b.append("\n[Page ").append(p).append("]\n");
                    b.append(cleanPage(pageTexts.get(p - 1), watermarks));
                    if (b.length() > MAX_CHARS) break;
                }
                String text = b.toString();
                if (text.length() > MAX_CHARS) text = text.substring(0, MAX_CHARS);
                final String fn = name, ft = text;
                final long fs = size;
                cb.done(fn, pages, fs, ft);
            } catch (Exception e) {
                cb.fail(e.getMessage() == null ? "could not read this PDF" : e.getMessage());
            }
        }).start();
    }

    // A line counts as a watermark/header when it is short, looks like a
    // website stamp (domain, URL or copyright line) and repeats on many
    // pages of the PDF — typical of books downloaded from websites.
    private static Set<String> findWatermarks(List<String> pageTexts) {
        Map<String, Integer> counts = new HashMap<>();
        for (String page : pageTexts) {
            Set<String> seenOnPage = new HashSet<>();
            for (String line : page.split("\n")) {
                String t = line.trim();
                if (t.length() >= 4 && t.length() <= 90 && looksLikeStamp(t) && seenOnPage.add(t)) {
                    Integer n = counts.get(t);
                    counts.put(t, n == null ? 1 : n + 1);
                }
            }
        }
        int threshold = Math.max(4, (int) (pageTexts.size() * 0.35));
        Set<String> out = new HashSet<>();
        for (Map.Entry<String, Integer> e : counts.entrySet()) {
            if (e.getValue() >= threshold) out.add(e.getKey());
        }
        return out;
    }

    private static boolean looksLikeStamp(String t) {
        String low = t.toLowerCase();
        return low.contains("www.") || low.contains("http") || low.contains(".com")
                || low.contains(".pk") || low.contains(".net") || low.contains(".org")
                || low.contains("©") || low.contains("copyright")
                || low.matches(".*\\bdownload(ed)?\\s+from\\b.*");
    }

    private static boolean isStandaloneDomain(String t) {
        return t.length() <= 60 && t.matches("(?i)(www\\.)?[a-z0-9-]+\\.(com|pk|net|org|info|io)(/\\S*)?");
    }

    private static String cleanPage(String pageText, Set<String> watermarks) {
        if (watermarks.isEmpty() && pageText.length() < 4000) {
            // Still drop standalone domain lines even in small PDFs.
            StringBuilder sb = new StringBuilder();
            for (String line : pageText.split("\n")) {
                if (isStandaloneDomain(line.trim())) continue;
                sb.append(line).append("\n");
            }
            return sb.toString();
        }
        StringBuilder sb = new StringBuilder();
        for (String line : pageText.split("\n")) {
            String t = line.trim();
            if (watermarks.contains(t) || isStandaloneDomain(t)) continue;
            sb.append(line).append("\n");
        }
        return sb.toString();
    }
}
