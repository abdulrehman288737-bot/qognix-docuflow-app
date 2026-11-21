package com.qognix.docuflow;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
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

// Documents tab in the approved premium design: gradient Add PDF button,
// PDF-tile document cards (open / rename / remove preserved), an empty
// state with the design's folder illustration, and one clearly-labelled
// Start.io native ad card in the list. Documents never leave the phone.
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
        root.addView(Ui.pageHeader(this,
                new int[]{R.drawable.icon_addfolder},
                new View.OnClickListener[]{v -> picker.launch(new String[]{"application/pdf"})}));
        root.addView(Ui.titleBlock(this, "Documents", "Saved privately on this phone. Tap a document to make it active, then use the tools on Home."));
        Button add = Ui.button(this, "+  Add PDF");
        add.setBackground(Ui.gradientBg(28));
        add.setOnClickListener(v -> { Sfx.pop(); picker.launch(new String[]{"application/pdf"}); });
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
            LinearLayout c = new LinearLayout(this);
            c.setOrientation(LinearLayout.HORIZONTAL);
            c.setGravity(Gravity.CENTER_VERTICAL);
            c.setBackground(Ui.bg(Ui.CARD, 28, 1));
            c.setPadding(28, 24, 28, 24);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
            lp.setMargins(0, 12, 0, 12);
            c.setLayoutParams(lp);
            ImageView iv = new ImageView(this);
            iv.setImageResource(R.drawable.icon_lockshield);
            c.addView(iv, new LinearLayout.LayoutParams(110, 110));
            LinearLayout tt = new LinearLayout(this);
            tt.setOrientation(LinearLayout.VERTICAL);
            tt.setPadding(20, 0, 0, 0);
            TextView t = new TextView(this);
            t.setText("No documents yet");
            t.setTextColor(Ui.TEXT);
            t.setTextSize(17);
            t.setTypeface(Typeface.DEFAULT_BOLD);
            tt.addView(t);
            TextView st = new TextView(this);
            st.setText("Upload your first PDF from Home or with the Add PDF button above. It will appear here and stay on your phone.");
            st.setTextColor(Ui.SUB);
            st.setTextSize(13);
            tt.addView(st);
            c.addView(tt, new LinearLayout.LayoutParams(0, -2, 1f));
            listBox.addView(c);
            ImageView art = new ImageView(this);
            art.setImageResource(R.drawable.img_folder);
            art.setScaleType(ImageView.ScaleType.FIT_CENTER);
            LinearLayout.LayoutParams ap = new LinearLayout.LayoutParams(560, 560);
            ap.gravity = Gravity.CENTER_HORIZONTAL;
            ap.setMargins(0, 26, 0, 0);
            art.setLayoutParams(ap);
            listBox.addView(art);
            return;
        }
        boolean adInserted = false;
        for (final String[] d : docs) {
            LinearLayout c = new LinearLayout(this);
            c.setOrientation(LinearLayout.VERTICAL);
            c.setBackground(Ui.bg(Ui.CARD, 28, 1));
            c.setPadding(26, 22, 26, 22);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
            lp.setMargins(0, 12, 0, 12);
            c.setLayoutParams(lp);
            LinearLayout top = new LinearLayout(this);
            top.setOrientation(LinearLayout.HORIZONTAL);
            top.setGravity(Gravity.CENTER_VERTICAL);
            ImageView iv = new ImageView(this);
            iv.setImageResource(R.drawable.icon_pdf);
            top.addView(iv, new LinearLayout.LayoutParams(88, 118));
            LinearLayout mid = new LinearLayout(this);
            mid.setOrientation(LinearLayout.VERTICAL);
            mid.setPadding(20, 0, 0, 0);
            boolean active = d[0].equals(Store.currentId(this));
            TextView name = new TextView(this);
            name.setText(d[1] + (active ? "  •  ACTIVE" : ""));
            name.setTextColor(active ? Ui.LIGHT : Ui.TEXT);
            name.setTextSize(16);
            name.setTypeface(Typeface.DEFAULT_BOLD);
            mid.addView(name);
            String when = "";
            try { when = " • " + DateFormat.getDateInstance(DateFormat.MEDIUM).format(new Date(Long.parseLong(d[4]))); } catch (Exception ignored) { }
            TextView meta = new TextView(this);
            meta.setText(d[2] + " pages • " + d[3] + " KB" + when);
            meta.setTextColor(Ui.SUB);
            meta.setTextSize(13);
            mid.addView(meta);
            top.addView(mid, new LinearLayout.LayoutParams(0, -2, 1f));
            c.addView(top);
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            Button open = Ui.ghostButton(this, "Open");
            open.setOnClickListener(v -> {
                Sfx.pop();
                Store.setCurrent(this, d[0]);
                toast("Active document: " + d[1]);
                Intent i = new Intent(this, MainActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(i);
                Ui.backAnim(this);
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
