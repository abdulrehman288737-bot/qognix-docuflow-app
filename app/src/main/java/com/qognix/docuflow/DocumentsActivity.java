package com.qognix.docuflow;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;

// Documents tab: the private on-device library. Open, rename, remove and
// re-open saved PDFs. One clearly-labelled Start.io native ad card may
// appear in the list. Documents never leave the phone from this screen.
public class DocumentsActivity extends AppCompatActivity {
    LinearLayout listBox;
    ActivityResultLauncher<String[]> picker;

    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(s);
        Ui.applyTheme(this);
        Ads.init(this);
        PDFBoxResourceLoader.init(this);
        picker = registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> { if (uri != null) loadPdf(uri); });
        Ui.Scaffold sc = Ui.scaffold(this, 1);
        LinearLayout root = sc.content;
        root.addView(Ui.text(this, "Documents", 24, Ui.TEXT, true));
        root.addView(Ui.text(this, "Saved privately on this phone. Tap a document to make it active, then use the tools on Home.", 13, Ui.SUB, false));
        Button add = Ui.button(this, "Add PDF");
        add.setOnClickListener(v -> picker.launch(new String[]{"application/pdf"}));
        root.addView(add);
        listBox = new LinearLayout(this);
        listBox.setOrientation(LinearLayout.VERTICAL);
        root.addView(listBox);
        refresh();
        Ads.addBanner(this, sc.bannerBox);
        setContentView(sc.root);
    }

    void refresh() {
        listBox.removeAllViews();
        List<String[]> docs = Store.docs(this);
        if (docs.isEmpty()) {
            Ui.card(this, listBox, "No documents yet", "Upload your first PDF from Home or with the Add PDF button above. It will appear here and stay on your phone.");
            return;
        }
        boolean adInserted = false;
        for (final String[] d : docs) {
            LinearLayout c = new LinearLayout(this);
            c.setOrientation(LinearLayout.VERTICAL);
            c.setBackground(Ui.bg(Ui.CARD, 28, 1));
            c.setPadding(30, 24, 30, 24);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
            lp.setMargins(0, 14, 0, 14);
            c.setLayoutParams(lp);
            boolean active = d[0].equals(Store.currentId(this));
            c.addView(Ui.text(this, d[1] + (active ? "  •  ACTIVE" : ""), 16, active ? Ui.LIGHT : Ui.TEXT, true));
            String when = "";
            try { when = " • " + DateFormat.getDateInstance(DateFormat.MEDIUM).format(new Date(Long.parseLong(d[4]))); } catch (Exception ignored) { }
            c.addView(Ui.text(this, d[2] + " pages • " + d[3] + " KB" + when, 13, Ui.SUB, false));
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            Button open = Ui.ghostButton(this, "Open");
            open.setOnClickListener(v -> {
                Store.setCurrent(this, d[0]);
                toast("Active document: " + d[1]);
                Intent i = new Intent(this, MainActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(i);
                finish();
            });
            Button rename = Ui.ghostButton(this, "Rename");
            rename.setOnClickListener(v -> renameDialog(d));
            Button remove = Ui.ghostButton(this, "Remove");
            remove.setOnClickListener(v -> {
                Store.deleteDoc(this, d[0]);
                toast("Removed");
                refresh();
            });
            row.addView(open, Ui.third());
            row.addView(rename, Ui.third());
            row.addView(remove, Ui.third());
            c.addView(row);
            listBox.addView(c);
            if (!adInserted) {
                // One native ad card, clearly labelled, after the first document.
                Ads.loadNativeInto(this, listBox);
                adInserted = true;
            }
        }
    }

    void renameDialog(final String[] d) {
        final EditText input = new EditText(this);
        input.setText(d[1]);
        input.setTextColor(Ui.TEXT);
        new AlertDialog.Builder(this)
                .setTitle("Rename document")
                .setView(input)
                .setPositiveButton("Save", (dlg, w) -> {
                    String n = input.getText().toString().trim();
                    if (!n.isEmpty()) { Store.renameDoc(this, d[0], n); refresh(); }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    void loadPdf(Uri uri) {
        toast("Reading PDF…");
        PdfHelper.load(this, uri, new PdfHelper.Cb() {
            @Override
            public void done(String name, int pages, long sizeBytes, String text) {
                runOnUiThread(() -> {
                    if (text.trim().length() < 20) { toast("This PDF has very little readable text."); return; }
                    Store.saveDoc(DocumentsActivity.this, name, text, pages, sizeBytes);
                    toast("Added: " + name);
                    refresh();
                });
            }

            @Override
            public void fail(String message) {
                runOnUiThread(() -> toast("Could not read this PDF: " + message));
            }
        });
    }

    void toast(String m) { Toast.makeText(this, m, Toast.LENGTH_LONG).show(); }
}
