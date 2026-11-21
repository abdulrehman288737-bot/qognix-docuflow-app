package com.qognix.docuflow;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;

import java.text.DateFormat;
import java.util.Date;

// Home tab in the premium design the user approved: logo header with bell +
// settings, gradient hero with Upload PDF, category chips that filter the
// AI tools grid (with the design's own icon artwork), banner slot, bottom nav.
public class MainActivity extends AppCompatActivity {
    TextView docStatus;
    ProgressBar progress;
    Button removeBtn, replaceBtn;
    ActivityResultLauncher<String[]> picker;
    LinearLayout toolsBox, chipsRow;
    ScrollView scroller;
    int filterMask = 1;

    // name, desc, mode, icon, categories (1=AI Tools, 2=PDF Tools, 4=Productivity, 8=Fun & More)
    static final Object[][] TOOLS = {
            {"AI Summary", "Short, medium or detailed summary", "summary", R.drawable.icon_summary, 1 | 2},
            {"MCQ Generator", "Questions with answers & reasons", "mcq", R.drawable.icon_mcq, 1 | 4 | 8},
            {"Quiz Generator", "Interactive quiz with score", "quiz", R.drawable.icon_quiz, 1 | 8},
            {"Ask Your PDF", "Answers with page references", "ask", R.drawable.icon_ask, 1 | 2},
            {"PDF Translator", "Translate into many languages", "translate", R.drawable.icon_translate, 1 | 2},
            {"AI Chat", "Chat about your document", "chat", R.drawable.icon_chat, 1 | 2 | 8},
            {"Important Points", "The key points, extracted", "points", R.drawable.icon_points, 1 | 4},
            {"Key Terms", "Terms & where they are explained", "terms", R.drawable.icon_terms, 1 | 4},
            {"Chapter Summary", "Section-by-section summary", "chapter", R.drawable.icon_chapter, 1 | 4},
            {"Explain Simply", "Difficult text in simple words", "explain", R.drawable.icon_explain, 1 | 2},
    };

    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(s);
        Ui.applyTheme(this);
        Ads.init(this);
        PDFBoxResourceLoader.init(this);
        picker = registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> { if (uri != null) loadPdf(uri); });

        Ui.Scaffold sc = Ui.scaffold(this, 0);
        scroller = (ScrollView) sc.root.getChildAt(0);
        LinearLayout root = sc.content;
        buildHeader(root);
        buildHero(root);
        buildDots(root);
        buildCategories(root);

        TextView toolsHead = new TextView(this);
        SpannableString th = new SpannableString("✦  AI Tools");
        th.setSpan(new ForegroundColorSpan(Ui.LIGHT), 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        toolsHead.setText(th);
        toolsHead.setTextColor(Ui.TEXT);
        toolsHead.setTextSize(20);
        toolsHead.setTypeface(Typeface.DEFAULT_BOLD);
        toolsHead.setPadding(0, 44, 0, 0);
        root.addView(toolsHead);
        TextView toolsSub = Ui.text(this, "Powerful tools at your fingertips", 14, Ui.SUB, false);
        toolsSub.setPadding(0, 2, 0, 10);
        root.addView(toolsSub);
        toolsBox = new LinearLayout(this);
        toolsBox.setOrientation(LinearLayout.VERTICAL);
        root.addView(toolsBox);
        buildTools();

        // Gemini status (kept from v3, below the tools like the design's lower area)
        String key = Store.key(this);
        Ui.card(this, root, "Gemini AI — " + (key.isEmpty() ? "On-device mode" : "Key saved"),
                key.isEmpty()
                        ? "Tools are running on-device. Add your free Gemini key in Settings for full AI results."
                        : "Your key is saved on this phone only. Manage or test it in Settings.");

        Button watch = Ui.ghostButton(this, "Watch a video ad (optional, supports Qognix)");
        watch.setOnClickListener(v -> { if (!Ads.showRewarded(this)) toast("Video ad is still loading — try again in a few seconds"); });
        root.addView(watch);
        Ads.preloadRewarded(this);

        Ads.addBanner(this, sc.bannerBox);
        setContentView(sc.root);
    }

    void buildHeader(LinearLayout root) {
        LinearLayout h = new LinearLayout(this);
        h.setOrientation(LinearLayout.HORIZONTAL);
        h.setGravity(Gravity.CENTER_VERTICAL);
        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.img_logo);
        h.addView(logo, new LinearLayout.LayoutParams(150, 150));
        LinearLayout titles = new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);
        titles.setPadding(24, 0, 0, 0);
        SpannableString name = new SpannableString("Qognix DocuFlow");
        name.setSpan(new ForegroundColorSpan(Ui.LIGHT), 7, 15, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        TextView n = new TextView(this);
        n.setText(name);
        n.setTextColor(Ui.TEXT);
        n.setTextSize(23);
        n.setTypeface(Typeface.DEFAULT_BOLD);
        titles.addView(n);
        TextView tag = new TextView(this);
        tag.setText("Your Documents. Smarter with AI.");
        tag.setTextColor(Ui.SUB);
        tag.setTextSize(13);
        titles.addView(tag);
        h.addView(titles, new LinearLayout.LayoutParams(0, -2, 1f));
        ImageView bell = new ImageView(this);
        bell.setImageResource(R.drawable.img_bell);
        bell.setOnClickListener(v -> new AlertDialog.Builder(MainActivity.this)
                .setTitle("Notifications")
                .setMessage("You are all caught up.\n\nQognix DocuFlow v3.2 — premium design, 10 AI tools, interactive quiz and history.")
                .setPositiveButton("OK", null)
                .show());
        h.addView(bell, new LinearLayout.LayoutParams(118, 118));
        ImageView gear = new ImageView(this);
        gear.setImageResource(R.drawable.img_gear);
        LinearLayout.LayoutParams gp = new LinearLayout.LayoutParams(118, 118);
        gp.setMargins(14, 0, 0, 0);
        gear.setOnClickListener(v -> {
            Intent in = new Intent(MainActivity.this, SettingsActivity.class);
            in.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(in);
            finish();
        });
        h.addView(gear, gp);
        h.setPadding(0, 4, 0, 10);
        root.addView(h);
    }

    void buildHero(LinearLayout root) {
        LinearLayout hero = new LinearLayout(this);
        hero.setOrientation(LinearLayout.HORIZONTAL);
        hero.setGravity(Gravity.CENTER_VERTICAL);
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{Color.rgb(43, 20, 92), Color.rgb(94, 47, 190), Color.rgb(56, 24, 116)});
        g.setCornerRadius(44);
        hero.setBackground(g);
        hero.setPadding(44, 40, 16, 40);
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(-1, -2);
        hp.setMargins(0, 14, 0, 0);
        hero.setLayoutParams(hp);

        LinearLayout left = new LinearLayout(this);
        left.setOrientation(LinearLayout.VERTICAL);
        SpannableString title = new SpannableString("Transform Your\nDocuments\nwith AI");
        title.setSpan(new ForegroundColorSpan(Color.rgb(203, 186, 255)), 15, 24, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        TextView tt = new TextView(this);
        tt.setText(title);
        tt.setTextColor(Color.WHITE);
        tt.setTextSize(27);
        tt.setTypeface(Typeface.DEFAULT_BOLD);
        tt.setLineSpacing(2, 1.1f);
        left.addView(tt);
        TextView st = new TextView(this);
        st.setText("Summarize, translate, quiz and more with the power of AI.");
        st.setTextColor(Color.rgb(219, 211, 247));
        st.setTextSize(13.5f);
        st.setPadding(0, 14, 10, 0);
        left.addView(st);
        TextView up = new TextView(this);
        up.setText("⬆   Upload PDF   ›");
        up.setTextColor(Color.WHITE);
        up.setTextSize(15.5f);
        up.setTypeface(Typeface.DEFAULT_BOLD);
        up.setGravity(Gravity.CENTER);
        up.setBackground(Ui.bg(Color.rgb(146, 96, 250), 44, 0));
        up.setPadding(34, 24, 34, 24);
        LinearLayout.LayoutParams upl = new LinearLayout.LayoutParams(-2, -2);
        upl.setMargins(0, 26, 0, 0);
        up.setLayoutParams(upl);
        up.setOnClickListener(v -> picker.launch(new String[]{"application/pdf"}));
        left.addView(up);
        hero.addView(left, new LinearLayout.LayoutParams(0, -2, 1.2f));

        ImageView art = new ImageView(this);
        art.setImageResource(R.drawable.img_hero);
        art.setScaleType(ImageView.ScaleType.FIT_CENTER);
        hero.addView(art, new LinearLayout.LayoutParams(0, 430, 1f));
        root.addView(hero);

        // Document status + progress + replace/remove (functionality kept, slim line under hero)
        progress = new ProgressBar(this);
        progress.setVisibility(View.GONE);
        root.addView(progress);
        docStatus = Ui.text(this, "", 13, Ui.SUB, false);
        docStatus.setPadding(4, 12, 4, 0);
        root.addView(docStatus);
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
        half.setMargins(0, 2, 8, 2);
        row.addView(replaceBtn, half);
        LinearLayout.LayoutParams half2 = new LinearLayout.LayoutParams(0, -2, 1f);
        half2.setMargins(8, 2, 0, 2);
        row.addView(removeBtn, half2);
        root.addView(row);
        refreshDocCard();
    }

    void buildDots(LinearLayout root) {
        LinearLayout dots = new LinearLayout(this);
        dots.setOrientation(LinearLayout.HORIZONTAL);
        dots.setGravity(Gravity.CENTER);
        View d1 = new View(this);
        d1.setBackground(Ui.bg(Ui.VIOLET, 8, 0));
        dots.addView(d1, new LinearLayout.LayoutParams(56, 16));
        for (int i = 0; i < 2; i++) {
            View d = new View(this);
            d.setBackground(Ui.bg(Color.rgb(92, 94, 122), 8, 0));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(16, 16);
            lp.setMargins(12, 0, 0, 0);
            dots.addView(d, lp);
        }
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, 26, 0, 0);
        dots.setLayoutParams(lp);
        root.addView(dots);
    }

    void buildCategories(LinearLayout root) {
        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        TextView grid = new TextView(this);
        grid.setText("▦");
        grid.setTextColor(Ui.LIGHT);
        grid.setTextSize(21);
        head.addView(grid);
        TextView t = Ui.text(this, "  Categories", 19, Ui.TEXT, true);
        t.setPadding(0, 0, 0, 0);
        head.addView(t, new LinearLayout.LayoutParams(0, -2, 1f));
        TextView all = new TextView(this);
        all.setText("Explore All  →");
        all.setTextColor(Ui.LIGHT);
        all.setTextSize(14.5f);
        all.setOnClickListener(v -> {
            selectChip(1);
            if (scroller != null && toolsBox != null) scroller.post(() -> scroller.smoothScrollTo(0, toolsBox.getTop()));
        });
        head.addView(all);
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(-1, -2);
        hp.setMargins(0, 36, 0, 0);
        head.setLayoutParams(hp);
        root.addView(head);

        HorizontalScrollView hsv = new HorizontalScrollView(this);
        hsv.setHorizontalScrollBarEnabled(false);
        chipsRow = new LinearLayout(this);
        chipsRow.setOrientation(LinearLayout.HORIZONTAL);
        hsv.addView(chipsRow);
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, -2);
        cp.setMargins(0, 16, 0, 0);
        hsv.setLayoutParams(cp);
        root.addView(hsv);
        selectChip(1);
    }

    void selectChip(int mask) {
        filterMask = mask;
        if (chipsRow == null) return;
        chipsRow.removeAllViews();
        Object[][] chips = {
                {1, "✦  AI Tools"},
                {2, "▤  PDF Tools"},
                {4, "⚡  Productivity"},
                {8, "☺  Fun & More"},
        };
        for (Object[] c : chips) {
            int m = (Integer) c[0];
            TextView chip = new TextView(this);
            chip.setText((String) c[1]);
            chip.setTextSize(14.5f);
            chip.setGravity(Gravity.CENTER);
            chip.setPadding(30, 18, 30, 18);
            if (m == filterMask) {
                chip.setTextColor(Color.WHITE);
                chip.setTypeface(Typeface.DEFAULT_BOLD);
                chip.setBackground(Ui.bg(Ui.VIOLET, 40, 0));
            } else {
                chip.setTextColor(Ui.TEXT);
                chip.setBackground(Ui.bg(Ui.CARD, 40, 1));
            }
            chip.setOnClickListener(v -> {
                selectChip(m);
                buildTools();
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
            lp.setMargins(0, 0, 16, 0);
            chipsRow.addView(chip, lp);
        }
        if (toolsBox != null) buildTools();
    }

    void buildTools() {
        if (toolsBox == null) return;
        toolsBox.removeAllViews();
        java.util.List<Object[]> visible = new java.util.ArrayList<>();
        for (Object[] t : TOOLS) if (((Integer) t[4] & filterMask) != 0) visible.add(t);
        for (int i = 0; i < visible.size(); i += 2) {
            View left = toolCard(visible.get(i));
            View right = i + 1 < visible.size() ? toolCard(visible.get(i + 1)) : null;
            toolsBox.addView(Ui.gridRow(this, left, right));
        }
    }

    View toolCard(final Object[] t) {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setBackground(Ui.bg(Ui.CARD, 30, 1));
        c.setPadding(26, 26, 26, 22);
        ImageView iv = new ImageView(this);
        iv.setImageResource((Integer) t[3]);
        c.addView(iv, new LinearLayout.LayoutParams(150, 150));
        TextView n = Ui.text(this, (String) t[0], 17, Ui.TEXT, true);
        n.setPadding(0, 16, 0, 2);
        c.addView(n);
        LinearLayout bottom = new LinearLayout(this);
        bottom.setOrientation(LinearLayout.HORIZONTAL);
        bottom.setGravity(Gravity.BOTTOM);
        TextView d = Ui.text(this, (String) t[1], 13, Ui.SUB, false);
        d.setPadding(0, 0, 6, 0);
        bottom.addView(d, new LinearLayout.LayoutParams(0, -2, 1f));
        TextView arrow = new TextView(this);
        arrow.setText("›");
        arrow.setTextColor(Color.WHITE);
        arrow.setTextSize(21);
        arrow.setTypeface(Typeface.DEFAULT_BOLD);
        arrow.setGravity(Gravity.CENTER);
        arrow.setBackground(Ui.bg(Ui.VIOLET, 40, 0));
        bottom.addView(arrow, new LinearLayout.LayoutParams(62, 62));
        c.addView(bottom);
        c.setOnClickListener(v -> openTool((String) t[2]));
        return c;
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
            docStatus.setText("No document loaded yet — upload a PDF from the banner above.");
            removeBtn.setVisibility(View.GONE);
            replaceBtn.setVisibility(View.GONE);
        } else {
            String when = "";
            try { when = " • " + DateFormat.getDateInstance(DateFormat.MEDIUM).format(new Date(Long.parseLong(meta[4]))); } catch (Exception ignored) { }
            docStatus.setText("Loaded: " + meta[1] + "  •  " + meta[2] + " pages • " + meta[3] + " KB" + when);
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
