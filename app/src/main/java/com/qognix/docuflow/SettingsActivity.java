package com.qognix.docuflow;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

// Settings tab in the approved premium design: section cards for
// Appearance, Preferred language, Gemini AI, Document storage and Privacy.
// All v3 functionality is preserved (theme, language, key save/test/remove,
// storage info, clear history).
public class SettingsActivity extends AppCompatActivity {
    EditText keyInput;
    TextView aiStatus;
    String[] langs = {"English", "Urdu", "Arabic", "Hindi", "Spanish", "French", "Turkish", "Bengali", "Punjabi", "Pashto"};
    boolean keyVisible = false;

    static final String PRIVACY_TEXT =
            "Your PDFs and their text stay in this app's private storage on your phone. Nothing is uploaded by Qognix. "
                    + "If you save a Gemini key and use an AI tool, the relevant document text is sent to Google Gemini to produce the answer. "
                    + "Ads are served by Start.io (App ID 210654162) and need internet; the app works fully without ads when offline or when ads fail to load.";

    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(s);
        Ui.applyTheme(this);
        Ads.init(this);
        Ui.Scaffold sc = Ui.scaffold(this, 3);
        LinearLayout root = sc.content;
        root.addView(Ui.pageHeader(this,
                new int[]{R.drawable.img_bell, R.drawable.img_gear},
                new View.OnClickListener[]{
                        v -> new AlertDialog.Builder(SettingsActivity.this)
                                .setTitle("Notifications")
                                .setMessage("You are all caught up.\n\nQognix DocuFlow v3.2 — premium design on every screen, with safe spacing from the system bars.")
                                .setPositiveButton("OK", null)
                                .show(),
                        v -> {
                            Intent in = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            in.setData(android.net.Uri.parse("package:" + getPackageName()));
                            startActivity(in);
                        }
                }));
        root.addView(Ui.titleBlock(this, "Settings", "Customize your app experience"));

        buildAppearance(root);
        buildLanguage(root);
        buildGemini(root);
        buildStorage(root);
        buildPrivacy(root);

        Ui.card(this, root, "About Qognix DocuFlow",
                "Version 3.2 — a native Android app (no website inside). Turn PDFs into summaries, MCQs, quizzes, answers, translations and simple explanations.\n\nYour Documents. Smarter with AI.");

        Ads.addBanner(this, sc.bannerBox);
        setContentView(sc.root);
    }

    LinearLayout sectionCard(LinearLayout root, int iconRes, String title, String subtitle) {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setBackground(Ui.bg(Ui.CARD, 28, 1));
        c.setPadding(28, 24, 28, 24);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.setMargins(0, 12, 0, 12);
        c.setLayoutParams(p);
        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        ImageView iv = new ImageView(this);
        iv.setImageResource(iconRes);
        head.addView(iv, new LinearLayout.LayoutParams(96, 96));
        LinearLayout tt = new LinearLayout(this);
        tt.setOrientation(LinearLayout.VERTICAL);
        tt.setPadding(20, 0, 0, 0);
        TextView t = new TextView(this);
        t.setText(title);
        t.setTextColor(Ui.TEXT);
        t.setTextSize(17);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        tt.addView(t);
        if (subtitle != null) {
            TextView st = new TextView(this);
            st.setText(subtitle);
            st.setTextColor(Ui.SUB);
            st.setTextSize(13);
            tt.addView(st);
        }
        head.addView(tt, new LinearLayout.LayoutParams(0, -2, 1f));
        c.addView(head);
        root.addView(c);
        return c;
    }

    void buildAppearance(LinearLayout root) {
        LinearLayout c = sectionCard(root, R.drawable.set_palette, "Appearance", "Choose your theme");
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        boolean darkNow = !"light".equals(Store.theme(this));
        row.addView(themePill("☾  Dark theme", darkNow, v -> { Store.setTheme(this, "dark"); recreate(); }), Ui.third());
        row.addView(themePill("☀  Light theme", !darkNow, v -> { Store.setTheme(this, "light"); recreate(); }), Ui.third());
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(-1, -2);
        rp.setMargins(0, 16, 0, 0);
        row.setLayoutParams(rp);
        c.addView(row);
    }

    TextView themePill(String label, boolean selected, View.OnClickListener click) {
        TextView t = new TextView(this);
        t.setText(label);
        t.setTextSize(14.5f);
        t.setGravity(Gravity.CENTER);
        t.setPadding(10, 22, 10, 22);
        if (selected) {
            t.setTextColor(Color.WHITE);
            t.setTypeface(Typeface.DEFAULT_BOLD);
            t.setBackground(Ui.gradientBg(40));
        } else {
            t.setTextColor(Ui.SUB);
            t.setBackground(Ui.bg(Ui.CARD2, 40, 1));
        }
        t.setOnClickListener(click);
        return t;
    }

    void buildLanguage(LinearLayout root) {
        LinearLayout c = sectionCard(root, R.drawable.set_globe, "Preferred language",
                "Used as the default target language in the PDF Translator.");
        Spinner sp = new Spinner(this);
        ArrayAdapter<String> ad = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, langs) {
            @Override
            public View getView(int pos, View cv, ViewGroup par) {
                TextView t = (TextView) super.getView(pos, cv, par);
                t.setTextColor(Ui.TEXT);
                t.setPadding(24, 20, 24, 20);
                return t;
            }
        };
        sp.setAdapter(ad);
        sp.setBackground(Ui.bg(Ui.CARD2, 24, 1));
        String cur = Store.uiLang(this);
        for (int i = 0; i < langs.length; i++) if (langs[i].equals(cur)) sp.setSelection(i);
        sp.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> p, View v, int pos, long id) {
                Store.setUiLang(SettingsActivity.this, langs[pos]);
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> p) { }
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, 16, 0, 0);
        sp.setLayoutParams(lp);
        c.addView(sp);
    }

    void buildGemini(LinearLayout root) {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setBackground(Ui.bg(Ui.CARD, 28, 1));
        c.setPadding(28, 24, 28, 24);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.setMargins(0, 12, 0, 12);
        c.setLayoutParams(p);
        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        ImageView iv = new ImageView(this);
        iv.setImageResource(R.drawable.set_sparkle);
        head.addView(iv, new LinearLayout.LayoutParams(96, 96));
        TextView t = new TextView(this);
        t.setText("Gemini AI");
        t.setTextColor(Ui.TEXT);
        t.setTextSize(17);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setPadding(20, 0, 0, 0);
        head.addView(t, new LinearLayout.LayoutParams(0, -2, 1f));
        TextView check = new TextView(this);
        check.setText("✓");
        check.setTextColor(Color.WHITE);
        check.setTextSize(15);
        check.setTypeface(Typeface.DEFAULT_BOLD);
        check.setGravity(Gravity.CENTER);
        check.setBackground(Ui.bg(Color.rgb(34, 197, 94), 40, 0));
        check.setVisibility(Store.key(this).isEmpty() ? View.GONE : View.VISIBLE);
        head.addView(check, new LinearLayout.LayoutParams(56, 56));
        c.addView(head);

        aiStatus = Ui.text(this, aiStatusText(), 14, Ui.SUB, false);
        c.addView(aiStatus);

        LinearLayout keyRow = new LinearLayout(this);
        keyRow.setOrientation(LinearLayout.HORIZONTAL);
        keyRow.setGravity(Gravity.CENTER_VERTICAL);
        keyRow.setBackground(Ui.bg(Ui.CARD2, 24, 1));
        keyRow.setPadding(24, 4, 12, 4);
        keyInput = new EditText(this);
        keyInput.setHint("Paste Gemini API key (free, from aistudio.google.com)");
        keyInput.setHintTextColor(Ui.SUB);
        keyInput.setTextColor(Ui.TEXT);
        keyInput.setTextSize(14);
        keyInput.setSingleLine(true);
        keyInput.setTransformationMethod(PasswordTransformationMethod.getInstance());
        if (!Store.key(this).isEmpty()) keyInput.setText(Store.key(this));
        keyRow.addView(keyInput, new LinearLayout.LayoutParams(0, -2, 1f));
        TextView eye = new TextView(this);
        eye.setText("👁");
        eye.setTextSize(17);
        eye.setPadding(12, 12, 12, 12);
        eye.setOnClickListener(v -> {
            keyVisible = !keyVisible;
            keyInput.setTransformationMethod(keyVisible
                    ? HideReturnsTransformationMethod.getInstance()
                    : PasswordTransformationMethod.getInstance());
            keyInput.setSelection(keyInput.getText().length());
        });
        keyRow.addView(eye);
        LinearLayout.LayoutParams kp = new LinearLayout.LayoutParams(-1, -2);
        kp.setMargins(0, 10, 0, 4);
        keyRow.setLayoutParams(kp);
        c.addView(keyRow);

        Button save = Ui.button(this, "Save, Test & Use Key");
        save.setBackground(Ui.gradientBg(28));
        save.setOnClickListener(v -> {
            String k = keyInput.getText().toString().trim();
            if (k.isEmpty()) { toast("Paste a key first"); return; }
            Store.setKey(this, k);
            aiStatus.setText("Testing Gemini key…");
            GeminiClient.generate(k, "Reply with exactly: OK", new GeminiClient.Cb() {
                @Override
                public void ok(String t2, String m) {
                    runOnUiThread(() -> {
                        aiStatus.setText("Gemini ON — connected. Working model: " + m);
                        toast("Gemini key works: " + m);
                    });
                }

                @Override
                public void fail(String msg) {
                    runOnUiThread(() -> aiStatus.setText("Gemini test failed: " + msg + "\nOn-device mode will be used until a working key is saved."));
                }
            });
        });
        c.addView(save);
        Button removeKey = Ui.ghostButton(this, "Remove saved key");
        removeKey.setOnClickListener(v -> {
            Store.setKey(this, "");
            keyInput.setText("");
            aiStatus.setText(aiStatusText());
            toast("Key removed");
            recreate();
        });
        c.addView(removeKey);
        c.addView(Ui.text(this, "Your key is stored only in this app's private storage on this phone. It is never in the app code and never sent anywhere except to Google Gemini when you use an AI tool.", 12, Ui.SUB, false));
        root.addView(c);
    }

    void buildStorage(LinearLayout root) {
        LinearLayout c = sectionCard(root, R.drawable.set_db, "Document storage", null);
        c.addView(Ui.text(this, Store.storageInfo(this), 14, Ui.SUB, false));
        Button clear = Ui.ghostButton(this, "Clear history");
        clear.setOnClickListener(v -> { Store.clearHistory(this); toast("History cleared"); });
        c.addView(clear);
    }

    void buildPrivacy(LinearLayout root) {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.HORIZONTAL);
        c.setGravity(Gravity.CENTER_VERTICAL);
        c.setBackground(Ui.bg(Ui.CARD, 28, 1));
        c.setPadding(28, 24, 28, 24);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.setMargins(0, 12, 0, 12);
        c.setLayoutParams(p);
        ImageView iv = new ImageView(this);
        iv.setImageResource(R.drawable.set_shield);
        c.addView(iv, new LinearLayout.LayoutParams(96, 96));
        LinearLayout tt = new LinearLayout(this);
        tt.setOrientation(LinearLayout.VERTICAL);
        tt.setPadding(20, 0, 0, 0);
        TextView t = new TextView(this);
        t.setText("Privacy");
        t.setTextColor(Ui.TEXT);
        t.setTextSize(17);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        tt.addView(t);
        TextView st = new TextView(this);
        st.setText("Your PDFs and their text stay in this app's private storage.");
        st.setTextColor(Ui.SUB);
        st.setTextSize(13);
        tt.addView(st);
        c.addView(tt, new LinearLayout.LayoutParams(0, -2, 1f));
        TextView chev = new TextView(this);
        chev.setText("›");
        chev.setTextColor(Ui.SUB);
        chev.setTextSize(26);
        c.addView(chev);
        c.setOnClickListener(v -> new AlertDialog.Builder(SettingsActivity.this)
                .setTitle("Privacy")
                .setMessage(PRIVACY_TEXT)
                .setPositiveButton("OK", null)
                .show());
        root.addView(c);
    }

    String aiStatusText() {
        return Store.key(this).isEmpty()
                ? "Status: Not connected — on-device mode. Add a free key to enable full AI."
                : "Status: Key saved on this phone.";
    }

    void toast(String m) { Toast.makeText(this, m, Toast.LENGTH_LONG).show(); }
}
