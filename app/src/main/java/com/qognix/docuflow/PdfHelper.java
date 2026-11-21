package com.qognix.docuflow;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;

import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.text.PDFTextStripper;

import java.io.InputStream;

// Reads a PDF on a background thread, page by page, so Ask-the-PDF can cite
// page numbers. Extracted text carries "[Page N]" markers. Text is capped to
// keep memory safe on large files. Nothing leaves the device here.
public class PdfHelper {
    public interface Cb {
        void done(String name, int pages, long sizeBytes, String text);
        void fail(String message);
    }

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
                StringBuilder b = new StringBuilder();
                for (int p = 1; p <= pages; p++) {
                    stripper.setStartPage(p);
                    stripper.setEndPage(p);
                    String pageText = stripper.getText(doc);
                    b.append("\n[Page ").append(p).append("]\n");
                    if (pageText != null) b.append(pageText);
                    if (b.length() > 200000) break;
                }
                doc.close();
                if (in != null) in.close();
                String text = b.toString();
                if (text.length() > 200000) text = text.substring(0, 200000);
                final String fn = name, ft = text;
                final long fs = size;
                cb.done(fn, pages, fs, ft);
            } catch (Exception e) {
                cb.fail(e.getMessage() == null ? "could not read this PDF" : e.getMessage());
            }
        }).start();
    }
}
