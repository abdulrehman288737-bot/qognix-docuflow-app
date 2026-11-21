package com.qognix.docuflow;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;

// History tab in the approved premium design: PDF-tile result cards with
// date and a round chevron, a gradient Clear button and a trash shortcut
// in the header. Tap an item to read, copy, share or delete it.
public class HistoryActivity extends AppCompatActivity {
    LinearLayout listBox;

    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(s);
        Ui.applyTheme(this);
        Ads.init(this);
        Ui.Scaffold sc = Ui.scaffold(this, 2);
        LinearLayout root = sc.content;
        root.addView(Ui.pageHeader(this,
                new int[]{R.drawable.icon_trash},
                new android.view.View.OnClickListener[]{v -> confirmClear()}));
        root.addView(Ui.titleBlock(this, "History", "Results you generate are saved here, privately on this phone."));
        Button clear = Ui.button(this, "Clear all history");
        clear.setBackground(Ui.gradientBg(28));
        clear.setOnClickListener(v -> confirmClear());
        root.addView(clear);
        listBox = new LinearLayout(this);
        listBox.setOrientation(LinearLayout.VERTICAL);
        root.addView(listBox);
        refresh();
        Ads.addBanner(this, sc.bannerBox);
        setContentView(sc.root);
    }

    void confirmClear() {
        if (Store.history(this).isEmpty()) { toast("History is already empty"); return; }
        new AlertDialog.Builder(this)
                .setTitle("Clear all history?")
                .setMessage("All saved results on this phone will be deleted.")
                .setPositiveButton("Clear", (d, w) -> {
                    Store.clearHistory(this);
                    toast("History cleared");
                    refresh();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    void refresh() {
        listBox.removeAllViews();
        List<String[]> hist = Store.history(this);
        if (hist.isEmpty()) {
            Ui.card(this, listBox, "Nothing here yet", "Use any AI tool on Home — summaries, MCQs, answers and quiz results will be saved here automatically.");
            return;
        }
        for (final String[] h : hist) {
            LinearLayout c = new LinearLayout(this);
            c.setOrientation(LinearLayout.HORIZONTAL);
            c.setGravity(Gravity.CENTER_VERTICAL);
            c.setBackground(Ui.bg(Ui.CARD, 28, 1));
            c.setPadding(26, 22, 26, 22);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
            lp.setMargins(0, 12, 0, 12);
            c.setLayoutParams(lp);
            ImageView iv = new ImageView(this);
            iv.setImageResource(R.drawable.icon_pdf);
            c.addView(iv, new LinearLayout.LayoutParams(96, 128));
            LinearLayout mid = new LinearLayout(this);
            mid.setOrientation(LinearLayout.VERTICAL);
            mid.setPadding(22, 0, 8, 0);
            TextView title = new TextView(this);
            title.setText(h[1]);
            title.setTextColor(Ui.TEXT);
            title.setTextSize(16);
            title.setTypeface(Typeface.DEFAULT_BOLD);
            mid.addView(title);
            String when = "";
            try { when = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(new Date(Long.parseLong(h[3]))); } catch (Exception ignored) { }
            TextView sub = new TextView(this);
            sub.setText((h[2].isEmpty() ? "" : h[2] + "  •  ") + when);
            sub.setTextColor(Ui.SUB);
            sub.setTextSize(13);
            mid.addView(sub);
            c.addView(mid, new LinearLayout.LayoutParams(0, -2, 1f));
            TextView chev = new TextView(this);
            chev.setText("›");
            chev.setTextColor(Ui.SUB);
            chev.setTextSize(22);
            chev.setTypeface(Typeface.DEFAULT_BOLD);
            chev.setGravity(Gravity.CENTER);
            chev.setBackground(Ui.bg(Ui.CARD2, 40, 1));
            c.addView(chev, new LinearLayout.LayoutParams(64, 64));
            c.setOnClickListener(v -> { Sfx.pop(); openItem(h); });
            listBox.addView(c);
        }
    }

    void openItem(final String[] h) {
        final String content = Store.loadHistory(this, h[0]);
        TextView tv = new TextView(this);
        tv.setText(content.isEmpty() ? "(empty)" : content);
        tv.setTextColor(Ui.TEXT);
        tv.setTextSize(14);
        tv.setPadding(40, 20, 40, 20);
        ScrollView sv = new ScrollView(this);
        sv.addView(tv);
        new AlertDialog.Builder(this)
                .setTitle(h[1])
                .setView(sv)
                .setPositiveButton("Copy", (d, w) -> {
                    ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    cm.setPrimaryClip(ClipData.newPlainText("Qognix " + h[1], content));
                    toast("Copied");
                })
                .setNeutralButton("Share", (d, w) -> {
                    Intent sh = new Intent(Intent.ACTION_SEND);
                    sh.setType("text/plain");
                    sh.putExtra(Intent.EXTRA_SUBJECT, "Qognix DocuFlow — " + h[1]);
                    sh.putExtra(Intent.EXTRA_TEXT, content);
                    startActivity(Intent.createChooser(sh, "Share result"));
                })
                .setNegativeButton("Delete", (d, w) -> {
                    Store.deleteHistory(this, h[0]);
                    toast("Deleted");
                    refresh();
                })
                .show();
    }

    void toast(String m) { Toast.makeText(this, m, Toast.LENGTH_LONG).show(); }
}
