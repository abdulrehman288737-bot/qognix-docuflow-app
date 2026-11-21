package com.qognix.docuflow;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

// Premium design system for Qognix DocuFlow.
// Brand palette (user spec): Primary Purple #7654F6, Deep Navy #11182B,
// Secondary Navy / Card #1B2440, Soft Lavender #B9A8FF, Text #F5F4FA,
// Secondary text #A7AEC2. Dark is the primary theme; light is optional.
public class Ui {
    public static int NAVY, CARD, CARD2, VIOLET, LIGHT, TEXT, SUB, BORDER;
    public static boolean dark = true;

    public static void applyTheme(Context c) {
        dark = !"light".equals(Store.theme(c));
        if (dark) {
            NAVY = Color.rgb(17, 24, 43);        // #11182B
            CARD = Color.rgb(27, 36, 64);        // #1B2440
            CARD2 = Color.rgb(35, 47, 82);
            VIOLET = Color.rgb(118, 84, 246);    // #7654F6
            LIGHT = Color.rgb(185, 168, 255);    // #B9A8FF
            TEXT = Color.rgb(245, 244, 250);     // #F5F4FA
            SUB = Color.rgb(167, 174, 194);      // #A7AEC2
            BORDER = Color.rgb(42, 54, 84);
        } else {
            NAVY = Color.rgb(245, 244, 250);
            CARD = Color.rgb(255, 255, 255);
            CARD2 = Color.rgb(238, 236, 250);
            VIOLET = Color.rgb(118, 84, 246);
            LIGHT = Color.rgb(118, 84, 246);
            TEXT = Color.rgb(23, 32, 58);
            SUB = Color.rgb(91, 107, 140);
            BORDER = Color.rgb(226, 224, 240);
        }
    }

    static GradientDrawable bg(int color, int radius, int stroke) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(radius);
        if (stroke > 0) g.setStroke(2, BORDER);
        return g;
    }

    public static LinearLayout root(Activity a) {
        LinearLayout l = new LinearLayout(a);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(36, 36, 36, 36);
        l.setBackgroundColor(NAVY);
        return l;
    }

    public static ScrollView scroll(Activity a, LinearLayout inner) {
        ScrollView s = new ScrollView(a);
        s.setBackgroundColor(NAVY);
        s.setClipToPadding(false);
        s.addView(inner);
        applyInsets(s);
        return s;
    }

    // Keep content clear of the status bar (top) and the system navigation
    // bar (bottom) on edge-to-edge Android versions, so nothing looks cut.
    public static void applyInsets(final View v) {
        v.setOnApplyWindowInsetsListener((view, insets) -> {
            view.setPadding(view.getPaddingLeft(),
                    insets.getSystemWindowInsetTop(),
                    view.getPaddingRight(),
                    insets.getSystemWindowInsetBottom());
            return insets;
        });
    }

    // Page-transition helpers: forward (opening a tool) and back (leaving).
    public static void forwardAnim(Activity a) {
        a.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    public static void backAnim(Activity a) {
        a.overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }

    public static GradientDrawable gradientBg(int radius) {
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{Color.rgb(146, 96, 250), VIOLET});
        g.setCornerRadius(radius);
        return g;
    }

    // Design header for the tab screens: app logo, two-line name, tagline,
    // and one or two round image buttons on the right.
    public static LinearLayout pageHeader(final Activity a, int[] iconRes, View.OnClickListener[] clicks) {
        LinearLayout h = new LinearLayout(a);
        h.setOrientation(LinearLayout.HORIZONTAL);
        h.setGravity(Gravity.CENTER_VERTICAL);
        android.widget.ImageView logo = new android.widget.ImageView(a);
        logo.setImageResource(R.drawable.img_logo);
        h.addView(logo, new LinearLayout.LayoutParams(118, 118));
        LinearLayout titles = new LinearLayout(a);
        titles.setOrientation(LinearLayout.VERTICAL);
        titles.setPadding(20, 0, 0, 0);
        TextView n1 = new TextView(a);
        n1.setText("Qognix");
        n1.setTextColor(TEXT);
        n1.setTextSize(20);
        n1.setTypeface(Typeface.DEFAULT_BOLD);
        titles.addView(n1);
        TextView n2 = new TextView(a);
        n2.setText("DocuFlow");
        n2.setTextColor(LIGHT);
        n2.setTextSize(20);
        n2.setTypeface(Typeface.DEFAULT_BOLD);
        titles.addView(n2);
        TextView tag = new TextView(a);
        tag.setText("Your Documents. Smarter with AI.");
        tag.setTextColor(SUB);
        tag.setTextSize(11);
        titles.addView(tag);
        h.addView(titles, new LinearLayout.LayoutParams(0, -2, 1f));
        if (iconRes != null) {
            for (int i = 0; i < iconRes.length; i++) {
                android.widget.ImageView b = new android.widget.ImageView(a);
                b.setImageResource(iconRes[i]);
                if (clicks != null && i < clicks.length && clicks[i] != null) b.setOnClickListener(clicks[i]);
                LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(104, 104);
                if (i > 0) bp.setMargins(12, 0, 0, 0);
                h.addView(b, bp);
            }
        }
        h.setPadding(0, 4, 0, 8);
        return h;
    }

    public static LinearLayout titleBlock(Activity a, String title, String subtitle) {
        LinearLayout b = new LinearLayout(a);
        b.setOrientation(LinearLayout.VERTICAL);
        TextView t = new TextView(a);
        t.setText(title);
        t.setTextColor(TEXT);
        t.setTextSize(27);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setPadding(0, 18, 0, 0);
        b.addView(t);
        TextView s = text(a, subtitle, 13, SUB, false);
        s.setPadding(0, 2, 0, 10);
        b.addView(s);
        return b;
    }

    public static TextView text(Activity a, String t, int sp, int color, boolean bold) {
        TextView v = new TextView(a);
        v.setText(t);
        v.setTextSize(sp);
        v.setTextColor(color);
        if (bold) v.setTypeface(Typeface.DEFAULT_BOLD);
        v.setPadding(0, 8, 0, 8);
        v.setLineSpacing(4, 1.08f);
        return v;
    }

    public static Button button(Activity a, String t) {
        Button b = new Button(a);
        b.setText(t);
        b.setTextColor(Color.WHITE);
        b.setAllCaps(false);
        b.setTextSize(16);
        b.setBackground(bg(VIOLET, 28, 0));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.setMargins(0, 14, 0, 14);
        b.setLayoutParams(p);
        b.setPadding(20, 28, 20, 28);
        return b;
    }

    public static Button ghostButton(Activity a, String t) {
        Button b = new Button(a);
        b.setText(t);
        b.setTextColor(TEXT);
        b.setAllCaps(false);
        b.setTextSize(15);
        b.setBackground(bg(CARD, 28, 1));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.setMargins(0, 10, 0, 10);
        b.setLayoutParams(p);
        b.setPadding(20, 24, 20, 24);
        return b;
    }

    public static EditText input(Activity a, String hint, boolean multi) {
        EditText e = new EditText(a);
        e.setHint(hint);
        e.setHintTextColor(SUB);
        e.setTextColor(TEXT);
        e.setBackground(bg(CARD, 24, 1));
        e.setPadding(28, 24, 28, 24);
        if (multi) { e.setMinLines(3); e.setGravity(Gravity.TOP); }
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.setMargins(0, 12, 0, 12);
        e.setLayoutParams(p);
        return e;
    }

    public static void card(Activity a, LinearLayout parent, String heading, String body) {
        LinearLayout c = new LinearLayout(a);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setBackground(bg(CARD, 28, 1));
        c.setPadding(30, 24, 30, 24);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.setMargins(0, 14, 0, 14);
        c.setLayoutParams(p);
        c.addView(text(a, heading, 17, TEXT, true));
        c.addView(text(a, body, 14, SUB, false));
        parent.addView(c);
    }

    // ---- Premium scaffold: header-less content + banner slot + bottom nav ----
    public static class Scaffold {
        public LinearLayout root, content, bannerBox;
    }

    public static Scaffold scaffold(final Activity a, int activeTab) {
        applyTheme(a);
        Scaffold sc = new Scaffold();
        sc.root = new LinearLayout(a);
        sc.root.setOrientation(LinearLayout.VERTICAL);
        sc.root.setBackgroundColor(NAVY);
        sc.content = root(a);
        ScrollView sv = new ScrollView(a);
        sv.setBackgroundColor(NAVY);
        sv.addView(sc.content);
        sc.root.addView(sv, new LinearLayout.LayoutParams(-1, 0, 1f));
        sc.bannerBox = new LinearLayout(a);
        sc.bannerBox.setOrientation(LinearLayout.VERTICAL);
        sc.bannerBox.setGravity(Gravity.CENTER);
        sc.root.addView(sc.bannerBox);
        sc.root.addView(navBar(a, activeTab));
        applyInsets(sc.root);
        return sc;
    }

    public static LinearLayout navBar(final Activity a, int activeTab) {
        LinearLayout nav = new LinearLayout(a);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setBackgroundColor(CARD);
        nav.setPadding(10, 12, 10, 14);
        String[] labels = {"Home", "Documents", "History", "Settings"};
        int[] icons = {R.drawable.nav_home, R.drawable.nav_docs, R.drawable.nav_history, R.drawable.nav_settings};
        final Class<?>[] targets = {MainActivity.class, DocumentsActivity.class, HistoryActivity.class, SettingsActivity.class};
        for (int i = 0; i < labels.length; i++) {
            final int idx = i;
            LinearLayout item = new LinearLayout(a);
            item.setOrientation(LinearLayout.VERTICAL);
            item.setGravity(Gravity.CENTER);
            android.widget.ImageView iv = new android.widget.ImageView(a);
            iv.setImageResource(icons[i]);
            if (i == activeTab) {
                iv.setColorFilter(Color.WHITE);
                iv.setBackground(bg(VIOLET, 40, 0));
                iv.setPadding(34, 12, 34, 12);
            } else {
                iv.setColorFilter(SUB);
                iv.setPadding(34, 12, 34, 12);
            }
            item.addView(iv, new LinearLayout.LayoutParams(-2, 92));
            TextView t = new TextView(a);
            t.setText(labels[i]);
            t.setTextSize(13);
            t.setGravity(Gravity.CENTER);
            if (i == activeTab) {
                t.setTextColor(TEXT);
                t.setTypeface(Typeface.DEFAULT_BOLD);
            } else {
                t.setTextColor(SUB);
            }
            item.addView(t);
            item.setOnClickListener(v -> {
                if (idx == activeTab) return;
                Sfx.init(a);
                Sfx.whoosh();
                Intent in = new Intent(a, targets[idx]);
                in.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                a.startActivity(in);
                if (idx > activeTab) a.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                else a.overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                a.finish();
            });
            nav.addView(item, new LinearLayout.LayoutParams(0, -2, 1f));
        }
        return nav;
    }

    // Header used on Home: logo chip + name + tagline + settings shortcut.
    public static LinearLayout header(final Activity a, boolean showSettings) {
        LinearLayout h = new LinearLayout(a);
        h.setOrientation(LinearLayout.HORIZONTAL);
        h.setGravity(Gravity.CENTER_VERTICAL);
        TextView logo = new TextView(a);
        logo.setText("Q");
        logo.setTextSize(24);
        logo.setTextColor(Color.WHITE);
        logo.setTypeface(Typeface.DEFAULT_BOLD);
        logo.setGravity(Gravity.CENTER);
        logo.setBackground(bg(VIOLET, 26, 0));
        h.addView(logo, new LinearLayout.LayoutParams(104, 104));
        LinearLayout titles = new LinearLayout(a);
        titles.setOrientation(LinearLayout.VERTICAL);
        titles.setPadding(26, 0, 0, 0);
        TextView name = text(a, "Qognix DocuFlow", 21, TEXT, true);
        name.setPadding(0, 0, 0, 0);
        TextView tag = text(a, "Your Documents. Smarter with AI.", 13, SUB, false);
        tag.setPadding(0, 0, 0, 0);
        titles.addView(name);
        titles.addView(tag);
        h.addView(titles, new LinearLayout.LayoutParams(0, -2, 1f));
        if (showSettings) {
            TextView gear = new TextView(a);
            gear.setText("⚙");
            gear.setTextSize(22);
            gear.setTextColor(SUB);
            gear.setGravity(Gravity.CENTER);
            gear.setPadding(16, 12, 4, 12);
            gear.setOnClickListener(v -> {
                Intent in = new Intent(a, SettingsActivity.class);
                in.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                a.startActivity(in);
                a.finish();
            });
            h.addView(gear);
        }
        h.setPadding(0, 6, 0, 18);
        return h;
    }

    // One AI tool card for the home grid.
    public static LinearLayout toolCard(Activity a, String chip, String name, String desc, View.OnClickListener click) {
        LinearLayout c = new LinearLayout(a);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setBackground(bg(CARD, 28, 1));
        c.setPadding(26, 24, 26, 24);
        TextView ch = new TextView(a);
        ch.setText(chip);
        ch.setTextSize(15);
        ch.setTextColor(Color.WHITE);
        ch.setTypeface(Typeface.DEFAULT_BOLD);
        ch.setGravity(Gravity.CENTER);
        ch.setBackground(bg(VIOLET, 20, 0));
        c.addView(ch, new LinearLayout.LayoutParams(92, 72));
        TextView n = text(a, name, 16, TEXT, true);
        n.setPadding(0, 14, 0, 2);
        c.addView(n);
        TextView d = text(a, desc, 13, SUB, false);
        d.setPadding(0, 0, 0, 0);
        c.addView(d);
        c.setOnClickListener(click);
        return c;
    }

    // Fresh equal-weight row slot params (never share one instance between views).
    public static LinearLayout.LayoutParams third() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, -2, 1f);
        p.setMargins(4, 4, 4, 4);
        return p;
    }

    public static LinearLayout gridRow(Activity a, View left, View right) {
        LinearLayout row = new LinearLayout(a);
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1f);
        lp.setMargins(0, 10, 10, 10);
        row.addView(left, lp);
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(0, -2, 1f);
        rp.setMargins(10, 10, 0, 10);
        if (right != null) row.addView(right, rp); else row.addView(new View(a), rp);
        return row;
    }
}
