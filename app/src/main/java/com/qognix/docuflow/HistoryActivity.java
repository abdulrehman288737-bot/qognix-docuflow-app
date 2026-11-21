package com.qognix.docuflow;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;

// History tab: every generated result is saved here automatically.
// Tap an item to read it, copy it, share it or delete it.
public class HistoryActivity extends AppCompatActivity {
    LinearLayout listBox;

    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(s);
        Ui.applyTheme(this);
        Ads.init(this);
        Ui.Scaffold sc = Ui.scaffold(this, 2);
        LinearLayout root = sc.content;
        root.addView(Ui.text(this, "History", 24, Ui.TEXT, true));
        root.addView(Ui.text(this, "Results you generate are saved here, privately on this phone.", 13, Ui.SUB, false));
        Button clear = Ui.ghostButton(this, "Clear all history");
        clear.setOnClickListener(v -> {
            Store.clearHistory(this);
            toast("History cleared");
            refresh();
        });
        root.addView(clear);
        listBox = new LinearLayout(this);
        listBox.setOrientation(LinearLayout.VERTICAL);
        root.addView(listBox);
        refresh();
        Ads.addBanner(this, sc.bannerBox);
        setContentView(sc.root);
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
            c.setOrientation(LinearLayout.VERTICAL);
            c.setBackground(Ui.bg(Ui.CARD, 28, 1));
            c.setPadding(30, 24, 30, 24);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
            lp.setMargins(0, 14, 0, 14);
            c.setLayoutParams(lp);
            c.addView(Ui.text(this, h[1], 16, Ui.TEXT, true));
            String when = "";
            try { when = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(new Date(Long.parseLong(h[3]))); } catch (Exception ignored) { }
            c.addView(Ui.text(this, (h[2].isEmpty() ? "" : h[2] + "  •  ") + when, 13, Ui.SUB, false));
            c.setOnClickListener(v -> openItem(h));
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
