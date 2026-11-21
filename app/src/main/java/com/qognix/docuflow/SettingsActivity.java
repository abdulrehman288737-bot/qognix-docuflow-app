package com.qognix.docuflow;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

// Settings tab: theme, language, Gemini key management with connection
// status and test, storage, privacy and about. Nothing unnecessary.
public class SettingsActivity extends AppCompatActivity {
    EditText keyInput;
    TextView aiStatus;
    String[] langs = {"English", "Urdu", "Arabic", "Hindi", "Spanish", "French", "Turkish", "Bengali", "Punjabi", "Pashto"};

    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(s);
        Ui.applyTheme(this);
        Ads.init(this);
        Ui.Scaffold sc = Ui.scaffold(this, 3);
        LinearLayout root = sc.content;
        root.addView(Ui.text(this, "Settings", 24, Ui.TEXT, true));

        // ---- Appearance ----
        root.addView(Ui.text(this, "Appearance", 17, Ui.TEXT, true));
        LinearLayout themeRow = new LinearLayout(this);
        themeRow.setOrientation(LinearLayout.HORIZONTAL);
        Button darkB = Ui.ghostButton(this, "Dark theme");
        Button lightB = Ui.ghostButton(this, "Light theme");
        darkB.setOnClickListener(v -> { Store.setTheme(this, "dark"); recreate(); });
        lightB.setOnClickListener(v -> { Store.setTheme(this, "light"); recreate(); });
        themeRow.addView(darkB, Ui.third());
        themeRow.addView(lightB, Ui.third());
        root.addView(themeRow);
        root.addView(Ui.text(this, "Current: " + ("light".equals(Store.theme(this)) ? "Light" : "Dark (primary)"), 13, Ui.SUB, false));

        // ---- Language ----
        root.addView(Ui.text(this, "Preferred language", 17, Ui.TEXT, true));
        root.addView(Ui.text(this, "Used as the default target language in the PDF Translator.", 13, Ui.SUB, false));
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
        sp.setBackground(Ui.bg(Ui.CARD, 24, 1));
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
        root.addView(sp);

        // ---- Gemini AI ----
        root.addView(Ui.text(this, "Gemini AI", 17, Ui.TEXT, true));
        aiStatus = Ui.text(this, aiStatusText(), 14, Ui.SUB, false);
        root.addView(aiStatus);
        keyInput = Ui.input(this, "Paste Gemini API key (free, from aistudio.google.com)", false);
        if (!Store.key(this).isEmpty()) keyInput.setText(Store.key(this));
        root.addView(keyInput);
        Button save = Ui.button(this, "Save, Test & Use Key");
        save.setOnClickListener(v -> {
            String k = keyInput.getText().toString().trim();
            if (k.isEmpty()) { toast("Paste a key first"); return; }
            Store.setKey(this, k);
            aiStatus.setText("Testing Gemini key…");
            GeminiClient.generate(k, "Reply with exactly: OK", new GeminiClient.Cb() {
                @Override
                public void ok(String t, String m) {
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
        root.addView(save);
        Button removeKey = Ui.ghostButton(this, "Remove saved key");
        removeKey.setOnClickListener(v -> {
            Store.setKey(this, "");
            keyInput.setText("");
            aiStatus.setText(aiStatusText());
            toast("Key removed");
        });
        root.addView(removeKey);
        root.addView(Ui.text(this, "Your key is stored only in this app's private storage on this phone. It is never in the app code and never sent anywhere except to Google Gemini when you use an AI tool.", 12, Ui.SUB, false));

        // ---- Storage ----
        root.addView(Ui.text(this, "Document storage", 17, Ui.TEXT, true));
        root.addView(Ui.text(this, Store.storageInfo(this), 14, Ui.SUB, false));
        Button clear = Ui.ghostButton(this, "Clear history");
        clear.setOnClickListener(v -> { Store.clearHistory(this); toast("History cleared"); });
        root.addView(clear);

        // ---- Privacy ----
        Ui.card(this, root, "Privacy",
                "Your PDFs and their text stay in this app's private storage on your phone. Nothing is uploaded by Qognix. "
                        + "If you save a Gemini key and use an AI tool, the relevant document text is sent to Google Gemini to produce the answer. "
                        + "Ads are served by Start.io (App ID 210654162) and need internet; the app works fully without ads when offline or when ads fail to load.");

        // ---- About ----
        Ui.card(this, root, "About Qognix DocuFlow",
                "Version 3.0 — a native Android app (no website inside). Turn PDFs into summaries, MCQs, quizzes, answers, translations and simple explanations.\n\nYour Documents. Smarter with AI.");

        Ads.addBanner(this, sc.bannerBox);
        setContentView(sc.root);
    }

    String aiStatusText() {
        return Store.key(this).isEmpty()
                ? "Status: Not connected — on-device mode. Add a free key to enable full AI."
                : "Status: Key saved on this phone. Tap Save, Test & Use Key to verify the connection.";
    }

    void toast(String m) { Toast.makeText(this, m, Toast.LENGTH_LONG).show(); }
}
