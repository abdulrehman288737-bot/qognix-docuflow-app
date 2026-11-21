package com.qognix.docuflow;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

// Interactive quiz: questions are generated from the loaded PDF (Gemini when
// a key is saved, on-device cloze questions otherwise). The user answers one
// question at a time, sees the correct answer and explanation, and gets a
// final score with a retry option.
public class QuizActivity extends AppCompatActivity {
    LinearLayout root, quizBox;
    TextView statusLine;
    List<LocalTools.QItem> items;
    int idx = 0, score = 0, difficulty = 1; // 0 easy, 1 medium, 2 hard
    Button[] diffBtns = new Button[3];
    boolean answered = false, finished = false;

    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(s);
        Ui.applyTheme(this);
        Ads.init(this);
        Ads.preloadInterstitial(this);
        root = Ui.root(this);
        root.addView(Ui.text(this, "Quiz Generator", 24, Ui.TEXT, true));
        root.addView(Ui.text(this, "Document: " + Store.docName(this), 13, Ui.SUB, false));
        root.addView(Ui.text(this, "Difficulty", 14, Ui.SUB, true));
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        String[] names = {"Easy", "Medium", "Hard"};
        for (int i = 0; i < 3; i++) {
            final int d = i;
            Button b = Ui.ghostButton(this, names[i]);
            b.setOnClickListener(v -> { difficulty = d; paintDiff(); });
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, -2, 1f);
            p.setMargins(4, 4, 4, 4);
            row.addView(b, p);
            diffBtns[i] = b;
        }
        root.addView(row);
        paintDiff();
        Button start = Ui.button(this, "Start Quiz");
        start.setOnClickListener(v -> startQuiz());
        root.addView(start);
        statusLine = Ui.text(this, "", 13, Ui.SUB, false);
        root.addView(statusLine);
        quizBox = new LinearLayout(this);
        quizBox.setOrientation(LinearLayout.VERTICAL);
        root.addView(quizBox);
        setContentView(Ui.scroll(this, root));
    }

    void paintDiff() {
        for (int i = 0; i < 3; i++) {
            diffBtns[i].setBackground(Ui.bg(i == difficulty ? Ui.VIOLET : Ui.CARD, 28, 1));
            diffBtns[i].setTextColor(i == difficulty ? Color.WHITE : Ui.TEXT);
        }
    }

    int countFor() { return difficulty == 0 ? 6 : difficulty == 2 ? 12 : 8; }

    void startQuiz() {
        String doc = Store.loadDoc(this);
        if (doc.isEmpty()) { toast("Upload a PDF first"); return; }
        statusLine.setText("Preparing questions…");
        quizBox.removeAllViews();
        String key = Store.key(this);
        if (key.isEmpty()) {
            begin(LocalTools.quizItems(doc, countFor()), "On-device mode");
            return;
        }
        String snippet = doc.length() > 12000 ? doc.substring(0, 12000) : doc;
        String diff = difficulty == 0 ? "easy" : difficulty == 2 ? "hard" : "medium";
        String prompt = "Create a " + diff + " quiz with " + countFor() + " multiple-choice questions from this document. Use EXACTLY this format for every question, nothing else:\n"
                + "Q: <question>\nA: <option>\nB: <option>\nC: <option>\nD: <option>\nANS: <A, B, C or D>\nEXP: <one line why the answer is correct>\n\nDocument:\n" + snippet;
        GeminiClient.generate(key, prompt, new GeminiClient.Cb() {
            @Override
            public void ok(String t, String m) {
                List<LocalTools.QItem> parsed = LocalTools.parseQuiz(t);
                runOnUiThread(() -> {
                    if (parsed.size() >= 3) begin(parsed, "Gemini (" + m + ")");
                    else begin(LocalTools.quizItems(doc, countFor()), "On-device mode (AI format not usable)");
                });
            }

            @Override
            public void fail(String msg) {
                runOnUiThread(() -> begin(LocalTools.quizItems(doc, countFor()), "On-device mode (Gemini failed)"));
            }
        });
    }

    void begin(List<LocalTools.QItem> qs, String how) {
        if (qs == null || qs.isEmpty()) {
            statusLine.setText("Not enough readable text in this PDF to build a quiz. Try a longer PDF.");
            return;
        }
        items = qs;
        idx = 0;
        score = 0;
        finished = false;
        statusLine.setText(how + " • " + qs.size() + " questions");
        showQuestion();
    }

    void showQuestion() {
        quizBox.removeAllViews();
        answered = false;
        LocalTools.QItem q = items.get(idx);
        quizBox.addView(Ui.text(this, "Question " + (idx + 1) + " of " + items.size() + "  •  Score: " + score, 13, Ui.LIGHT, true));
        TextView qt = Ui.text(this, q.question, 18, Ui.TEXT, true);
        quizBox.addView(qt);
        final TextView exp = Ui.text(this, "", 14, Ui.SUB, false);
        final Button next = Ui.button(this, idx + 1 == items.size() ? "See Result" : "Next Question");
        next.setVisibility(View.GONE);
        next.setOnClickListener(v -> {
            idx++;
            if (idx >= items.size()) showResult();
            else showQuestion();
        });
        for (int i = 0; i < 4; i++) {
            final int oi = i;
            Button ob = Ui.ghostButton(this, (char) ('A' + i) + ") " + q.options[i]);
            ob.setOnClickListener(v -> {
                if (answered) return;
                answered = true;
                if (oi == q.answer) {
                    score++;
                    ob.setBackground(Ui.bg(Color.rgb(34, 197, 94), 28, 0));
                    ob.setTextColor(Color.WHITE);
                } else {
                    ob.setBackground(Ui.bg(Color.rgb(239, 68, 68), 28, 0));
                    ob.setTextColor(Color.WHITE);
                }
                if (q.explanation != null && !q.explanation.isEmpty())
                    exp.setText((oi == q.answer ? "Correct. " : "Correct answer: " + (char) ('A' + q.answer) + ") " + q.options[q.answer] + ". ") + q.explanation);
                else
                    exp.setText(oi == q.answer ? "Correct." : "Correct answer: " + (char) ('A' + q.answer) + ") " + q.options[q.answer]);
                next.setVisibility(View.VISIBLE);
            });
            quizBox.addView(ob);
        }
        quizBox.addView(exp);
        quizBox.addView(next);
    }

    void showResult() {
        finished = true;
        quizBox.removeAllViews();
        quizBox.addView(Ui.text(this, "Quiz Complete", 22, Ui.TEXT, true));
        quizBox.addView(Ui.text(this, "Your score: " + score + " / " + items.size(), 18, Ui.LIGHT, true));
        quizBox.addView(Ui.text(this, score * 2 >= items.size() ? "Good work — you know this document well." : "Keep studying — try the Summary and Important Points tools, then retry.", 14, Ui.SUB, false));
        Store.addHistory(this, "Quiz Result", Store.docName(this), "Quiz score: " + score + " / " + items.size() + " (difficulty " + (difficulty == 0 ? "Easy" : difficulty == 2 ? "Hard" : "Medium") + ")");
        Button retry = Ui.button(this, "Retry Quiz");
        retry.setOnClickListener(v -> startQuiz());
        quizBox.addView(retry);
        Button finishBtn = Ui.ghostButton(this, "Finish");
        finishBtn.setOnClickListener(v -> Ads.showTransition(this, () -> finish()));
        quizBox.addView(finishBtn);
    }

    void toast(String m) { Toast.makeText(this, m, Toast.LENGTH_LONG).show(); }
}
