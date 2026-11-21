package com.qognix.docuflow;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;

import java.text.DateFormat;
import java.util.Date;

// Home tab: header, document upload card, AI tools grid, Gemini status,
// rewarded video button, banner slot and bottom navigation (via Ui.Scaffold).
public class MainActivity extends AppCompatActivity {
    TextView docStatus;
    ProgressBar progress;
    Button removeBtn, replaceBtn;
    ActivityResultLauncher<String[]> picker;

    static final String[][] TOOLS = {
            {"S", "AI Summary", "Short, medium or detailed summary", "summary"},
            {"M", "MCQ Generator", "Questions with answers & reasons", "mcq"},
            {"Q", "Quiz Generator", "Interactive quiz with score", "quiz"},
            {"A", "Ask Your PDF", "Answers with page references", "ask"},
            {"T", "PDF Translator", "Translate into many languages", "translate"},
            {"C", "AI Chat", "Chat about your document", "chat"},
            {"P", "Important Points", "The key points, extracted", "points"},
            {"K", "Key Terms", "Terms & where they are explained", "terms"},
            {"H", "Chapter Summary", "Section-by-section summary", "chapter"},
            {"E", "Explain Simply", "Difficult text in simple words", "explain"},
    };

    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(s);
        Ui.applyTheme(this);
        Ads.init(this);
        PDFBoxResourceLoader.init(this);
        picker = registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> { if (uri != null) loadPdf(uri); });

        Ui.Scaffold sc = Ui.scaffold(this, 0);
        LinearLayout root = sc.content;
        root.addView(Ui.header(this, true));

        // ---- Document upload card ----
        LinearLayout upload = new LinearLayout(this);
        upload.setOrientation(LinearLayout.VERTICAL);
        upload.setBackground(Ui.bg(Ui.CARD, 28, 1));
        upload.setPadding(30, 26, 30, 26);
        LinearLayout.LayoutParams uplp = new LinearLayout.LayoutParams(-1, -2);
        uplp.setMargins(0, 6, 0, 16);
        upload.setLayoutParams(uplp);
        upload.addView(Ui.text(this, "Your Document", 17, Ui.TEXT, true));
        docStatus = Ui.text(this, "", 14, Ui.SUB, false);
        upload.addView(docStatus);
        progress = new ProgressBar(this);
        progress.setVisibility(View.GONE);
        upload.addView(progress);
        Button pick = Ui.button(this, "Upload PDF");
        pick.setOnClickListener(v -> picker.launch(new String[]{"application/pdf"}));
        upload.addView(pick);
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        replaceBtn = Ui.ghostButton(this, "Replace");
        replaceBtn.setOnClickListener(v -> picker.launch(new String[]{"application/pdf"}));
        removeBtn = Ui.ghostButton(this, "Remove");
        removeBtn.setOnClickListener(v -> {
            String id = Store.currentId(this);
            if (!id.isEmpty()) Store.deleteDoc(this, id);
            refreshDocCard();
            toast("Document removed");
        });
        LinearLayout.LayoutParams half = new LinearLayout.LayoutParams(0, -2, 1f);
        half.setMargins(0, 4, 8, 4);
        row.addView(replaceBtn, half);
        LinearLayout.LayoutParams half2 = new LinearLayout.LayoutParams(0, -2, 1f);
        half2.setMargins(8, 4, 0, 4);
        row.addView(removeBtn, half2);
        upload.addView(row);
        root.addView(upload);
        refreshDocCard();

        // ---- Gemini status ----
        String key = Store.key(this);
        Ui.card(this, root, "Gemini AI — " + (key.isEmpty() ? "On-device mode" : "Key saved"),
                key.isEmpty()
                        ? "Tools are running on-device. Add your free Gemini key in Settings for full AI results."
                        : "Your key is saved on this phone only. Manage or test it in Settings.");

        // ---- AI tools grid ----
        TextView toolsHead = Ui.text(this, "AI Tools", 19, Ui.TEXT, true);
        toolsHead.setPadding(0, 18, 0, 4);
        root.addView(toolsHead);
        for (int i = 0; i < TOOLS.length; i += 2) {
            View left = toolCard(TOOLS[i]);
            View right = i + 1 < TOOLS.length ? toolCard(TOOLS[i + 1]) : null;
            root.addView(Ui.gridRow(this, left, right));
        }

        // ---- Rewarded video (explicit user choice) ----
        Button watch = Ui.ghostButton(this, "Watch a video ad (optional, supports Qognix)");
        watch.setOnClickListener(v -> { if (!Ads.showRewarded(this)) toast("Video ad is still loading — try again in a few seconds"); });
        root.addView(watch);
        Ads.preloadRewarded(this);

        root.addView(Ui.text(this, "Ads by Start.io: a banner below, video ads only at natural breaks, never during your document work.", 12, Ui.SUB, false));

        Ads.addBanner(this, sc.bannerBox);
        setContentView(sc.root);
    }

    View toolCard(final String[] t) {
        return Ui.toolCard(this, t[0], t[1], t[2], v -> openTool(t[3]));
    }

    void openTool(String mode) {
        if (Store.loadDoc(this).isEmpty()) { toast("Upload a PDF first"); return; }
        Intent i;
        if (mode.equals("chat")) i = new Intent(this, ChatActivity.class);
        else if (mode.equals("quiz")) i = new Intent(this, QuizActivity.class);
        else i = new Intent(this, FeatureActivity.class).putExtra("mode", mode);
        startActivity(i);
    }

    void refreshDocCard() {
        String[] meta = Store.currentMeta(this);
        if (meta == null || Store.docName(this).isEmpty()) {
            docStatus.setText("No PDF loaded yet. Upload one to start using the AI tools.");
            removeBtn.setVisibility(View.GONE);
            replaceBtn.setVisibility(View.GONE);
        } else {
            String when = "";
            try { when = " • " + DateFormat.getDateInstance(DateFormat.MEDIUM).format(new Date(Long.parseLong(meta[4]))); } catch (Exception ignored) { }
            docStatus.setText(meta[1] + "\n" + meta[2] + " pages • " + meta[3] + " KB" + when);
            removeBtn.setVisibility(View.VISIBLE);
            replaceBtn.setVisibility(View.VISIBLE);
        }
    }

    void loadPdf(Uri uri) {
        progress.setVisibility(View.VISIBLE);
        docStatus.setText("Reading PDF…");
        PdfHelper.load(this, uri, new PdfHelper.Cb() {
            @Override
            public void done(String name, int pages, long sizeBytes, String text) {
                runOnUiThread(() -> {
                    progress.setVisibility(View.GONE);
                    if (text.trim().length() < 20) {
                        docStatus.setText("This PDF has very little readable text (it may be scanned images). Try another PDF.");
                        return;
                    }
                    Store.saveDoc(MainActivity.this, name, text, pages, sizeBytes);
                    refreshDocCard();
                    toast("PDF loaded: " + pages + " pages");
                });
            }

            @Override
            public void fail(String message) {
                runOnUiThread(() -> {
                    progress.setVisibility(View.GONE);
                    docStatus.setText("Could not read this PDF: " + message);
                });
            }
        });
    }

    void toast(String m) { Toast.makeText(this, m, Toast.LENGTH_LONG).show(); }
}
