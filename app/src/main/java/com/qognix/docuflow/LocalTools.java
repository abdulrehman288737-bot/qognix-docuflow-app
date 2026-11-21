package com.qognix.docuflow;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

// On-device (offline) document tools. These never invent content: they
// extract and reorganize sentences that are actually in the PDF. AI-quality
// results come from Gemini when the user adds their free key.
public class LocalTools {

    public static List<String> sentences(String t) {
        List<String> out = new ArrayList<>();
        String clean = t.replaceAll("\\[Page \\d+\\]", " ").replaceAll("\\s+", " ").trim();
        for (String s : clean.split("(?<=[.!?])\\s+")) {
            s = s.trim();
            if (s.length() > 35) out.add(s);
        }
        return out;
    }

    static List<String> topSentences(String t, int n) {
        List<String> s = sentences(t);
        Map<String, Integer> freq = new HashMap<>();
        for (String sent : s) for (String w : sent.toLowerCase().split("\\W+")) if (w.length() > 4) freq.put(w, freq.getOrDefault(w, 0) + 1);
        List<String> sorted = new ArrayList<>(s);
        sorted.sort((a, b) -> score(b, freq) - score(a, freq));
        List<String> out = new ArrayList<>();
        for (String x : sorted) { out.add(x); if (out.size() >= n) break; }
        // restore document order for readability
        out.sort((a, b) -> s.indexOf(a) - s.indexOf(b));
        return out;
    }

    static int score(String sent, Map<String, Integer> freq) {
        int sc = 0;
        for (String w : sent.toLowerCase().split("\\W+")) if (w.length() > 4) sc += freq.getOrDefault(w, 0);
        return sc;
    }

    public static String summary(String t) { return summaryLen(t, 1); }

    // level: 0 short, 1 medium, 2 detailed
    public static String summaryLen(String t, int level) {
        List<String> top = topSentences(t, level == 0 ? 5 : level == 2 ? 18 : 10);
        if (top.isEmpty()) return "No readable text found in this PDF.";
        StringBuilder b = new StringBuilder("On-device summary (add a Gemini key for a full AI summary):\n\n");
        for (String s : top) b.append("• ").append(s).append("\n\n");
        return b.toString();
    }

    public static String points(String t) {
        List<String> top = topSentences(t, 12);
        if (top.isEmpty()) return "No readable text found in this PDF.";
        StringBuilder b = new StringBuilder("Important points from your document (on-device extraction):\n\n");
        int i = 1;
        for (String s : top) b.append(i++).append(". ").append(s).append("\n\n");
        return b.toString();
    }

    public static String terms(String t) {
        List<String> s = sentences(t);
        if (s.isEmpty()) return "No readable text found in this PDF.";
        Map<String, Integer> freq = new HashMap<>();
        Map<String, String> firstSeen = new HashMap<>();
        for (String sent : s) {
            for (String w : sent.split("\\s+")) {
                String c = w.replaceAll("[^A-Za-z-]", "");
                if (c.length() >= 7 && Character.isLowerCase(c.charAt(0))) {
                    String low = c.toLowerCase();
                    freq.put(low, freq.getOrDefault(low, 0) + 1);
                    if (!firstSeen.containsKey(low)) firstSeen.put(low, sent);
                }
            }
        }
        List<Map.Entry<String, Integer>> list = new ArrayList<>(freq.entrySet());
        list.sort((a, b) -> b.getValue() - a.getValue());
        StringBuilder b = new StringBuilder("Key terms from your document (on-device extraction — the line under each term is where it appears in your PDF):\n\n");
        int n = 0;
        for (Map.Entry<String, Integer> e : list) {
            if (e.getValue() < 2) break;
            b.append("• ").append(e.getKey()).append(" (used ").append(e.getValue()).append(" times)\n");
            b.append("  ").append(firstSeen.get(e.getKey())).append("\n\n");
            if (++n >= 12) break;
        }
        if (n == 0) return "No repeated key terms found. Try a longer document, or add a Gemini key for AI-extracted terms with definitions.";
        return b.toString();
    }

    public static String chapters(String t) {
        // Split on [Page N] markers and group pages into sections of ~4 pages.
        String[] parts = t.split("\\[Page (\\d+)\\]");
        // parts[0] is preamble; after each marker the following text alternates.
        List<String> sections = new ArrayList<>();
        List<Integer> starts = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        int curStart = 1, page = 0;
        for (int i = 1; i < parts.length; i += 2) {
            // With a capturing group, split keeps the number between segments.
            String seg = i + 1 < parts.length ? parts[i + 1] : "";
            int pg;
            try { pg = Integer.parseInt(parts[i].trim()); } catch (Exception e) { pg = ++page; }
            if (cur.length() == 0) curStart = pg;
            cur.append(seg).append("\n");
            if (pg - curStart >= 3 || i + 2 >= parts.length) {
                sections.add(cur.toString());
                starts.add(curStart);
                cur = new StringBuilder();
            }
        }
        if (sections.isEmpty()) {
            // No page markers (older documents): one section from plain text.
            sections.add(t);
            starts.add(1);
        }
        StringBuilder b = new StringBuilder("Chapter-wise summary (on-device mode — sections are page groups; add a Gemini key for true chapter detection):\n\n");
        for (int i = 0; i < sections.size(); i++) {
            List<String> top = topSentences(sections.get(i), 3);
            b.append("Section ").append(i + 1).append(" (from page ").append(starts.get(i)).append(")\n");
            if (top.isEmpty()) b.append("No readable text in this section.\n\n");
            else for (String s : top) b.append("• ").append(s).append("\n");
            b.append("\n");
        }
        return b.toString();
    }

    public static String explain(String t) {
        List<String> top = topSentences(t, 6);
        if (top.isEmpty()) return "No readable text found in this PDF.";
        StringBuilder b = new StringBuilder("Simple explanation (on-device basic mode — add a Gemini key for a full simple-language explanation):\n\n");
        b.append("This document mainly talks about the following ideas, in its own words:\n\n");
        for (String s : top) b.append("• ").append(s).append("\n\n");
        return b.toString();
    }

    public static String mcqs(String t, boolean quiz) {
        List<QItem> items = quizItems(t, 8);
        if (items.isEmpty()) return "Not enough text in this PDF to make questions. Try a longer PDF.";
        StringBuilder b = new StringBuilder(quiz ? "Quiz (on-device mode):\n\n" : "MCQs (on-device mode — add a Gemini key for AI MCQs with explanations):\n\n");
        int i = 1;
        for (QItem q : items) {
            if (quiz) {
                b.append("Q").append(i++).append(": ").append(q.question.replace(q.options[q.answer], "_____")).append("\nAnswer: ").append(q.options[q.answer]).append("\n\n");
            } else {
                b.append("Q").append(i++).append(": ").append(q.question).append("\n");
                char ch = 'A';
                for (String o : q.options) b.append(ch++).append(") ").append(o).append("\n");
                b.append("Correct: ").append((char) ('A' + q.answer)).append(") ").append(q.options[q.answer]).append("\n");
                if (q.explanation != null && !q.explanation.isEmpty()) b.append("Why: ").append(q.explanation).append("\n");
                b.append("\n");
            }
        }
        return b.toString();
    }

    public static class QItem {
        public String question;
        public String[] options = new String[4];
        public int answer;
        public String explanation = "";
    }

    // Cloze-style questions built from real sentences: blank out a key word,
    // distractors are key words taken from other sentences.
    public static List<QItem> quizItems(String t, int count) {
        List<String> s = sentences(t);
        List<QItem> out = new ArrayList<>();
        if (s.size() < 3) return out;
        Random rnd = new Random(42);
        List<String> pool = new ArrayList<>();
        for (String sent : s) for (String wd : sent.split(" ")) {
            String c = wd.replaceAll("[^A-Za-z0-9-]", "");
            if (c.length() >= 6) pool.add(c);
        }
        for (int i = 0; i < s.size() && out.size() < count; i++) {
            String sent = s.get(i);
            String best = "";
            for (String wd : sent.split(" ")) {
                String c = wd.replaceAll("[^A-Za-z0-9-]", "");
                if (c.length() > best.length() && c.length() >= 6) best = c;
            }
            if (best.isEmpty()) continue;
            QItem q = new QItem();
            q.question = sent.replace(best, "_____");
            q.explanation = sent;
            List<String> opts = new ArrayList<>();
            opts.add(best);
            Set<String> used = new HashSet<>();
            used.add(best.toLowerCase());
            int guard = 0;
            while (opts.size() < 4 && guard++ < 300 && !pool.isEmpty()) {
                String wd = pool.get(rnd.nextInt(pool.size()));
                if (used.add(wd.toLowerCase())) opts.add(wd);
            }
            if (opts.size() < 4) continue;
            Collections.shuffle(opts, rnd);
            q.options = opts.toArray(new String[0]);
            q.answer = opts.indexOf(best);
            out.add(q);
        }
        return out;
    }

    // Parses the structured quiz format requested from Gemini:
    // Q: ... / A: ... / B: ... / C: ... / D: ... / ANS: A / EXP: ...
    public static List<QItem> parseQuiz(String text) {
        List<QItem> out = new ArrayList<>();
        QItem cur = null;
        for (String raw : text.split("\n")) {
            String line = raw.trim();
            if (line.startsWith("Q:")) {
                if (cur != null && cur.question != null) out.add(cur);
                cur = new QItem();
                cur.question = line.substring(2).trim();
            } else if (cur == null) {
                continue;
            } else if (line.startsWith("A:")) cur.options[0] = line.substring(2).trim();
            else if (line.startsWith("B:")) cur.options[1] = line.substring(2).trim();
            else if (line.startsWith("C:")) cur.options[2] = line.substring(2).trim();
            else if (line.startsWith("D:")) cur.options[3] = line.substring(2).trim();
            else if (line.startsWith("ANS:")) {
                String a = line.substring(4).trim().toUpperCase();
                cur.answer = a.startsWith("B") ? 1 : a.startsWith("C") ? 2 : a.startsWith("D") ? 3 : 0;
            } else if (line.startsWith("EXP:")) cur.explanation = line.substring(4).trim();
        }
        if (cur != null && cur.question != null) out.add(cur);
        List<QItem> valid = new ArrayList<>();
        for (QItem q : out) {
            boolean ok = q.question != null && !q.question.isEmpty();
            for (String o : q.options) if (o == null || o.isEmpty()) ok = false;
            if (ok) valid.add(q);
        }
        return valid;
    }

    public static String ask(String t, String q) {
        if (q == null || q.trim().isEmpty()) return "Type a question first.";
        String[] qw = q.toLowerCase().split("\\W+");
        // Search page by page so we can cite the page number.
        String[] sections = t.split("\\[Page ");
        StringBuilder b = new StringBuilder();
        int found = 0;
        for (int i = 1; i < sections.length && found < 5; i++) {
            int close = sections[i].indexOf(']');
            if (close < 0) continue;
            String pageNo = sections[i].substring(0, close).trim();
            String body = sections[i].substring(close + 1);
            for (String sent : sentences(body)) {
                String low = sent.toLowerCase();
                int score = 0;
                for (String w : qw) if (w.length() > 2 && low.contains(w)) score++;
                if (score >= 1) {
                    if (found == 0) b.append("From your PDF:\n\n");
                    b.append("• (Page ").append(pageNo).append(") ").append(sent).append("\n\n");
                    found++;
                    break;
                }
            }
        }
        if (found == 0) {
            // Fallback: whole-text search without page markers.
            List<String> s = sentences(t);
            for (String sent : s) {
                String low = sent.toLowerCase();
                int score = 0;
                for (String w : qw) if (w.length() > 2 && low.contains(w)) score++;
                if (score >= 1) {
                    if (found == 0) b.append("From your PDF:\n\n");
                    b.append("• ").append(sent).append("\n\n");
                    if (++found >= 5) break;
                }
            }
        }
        if (found == 0) return "I could not find that in this PDF (on-device search). The document may not cover it — I will not invent an answer. Add a Gemini key for a fuller AI answer.";
        return b.toString();
    }
}
