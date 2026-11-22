package com.qognix.docuflow;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Helpers for page-aware reading of the extracted PDF text, which carries
// "[Page N]" markers. Used by AI Chat and Ask Your PDF so that a question
// about one page ("page no 86", "safha 86") is answered from that exact
// page instead of only the start of the document.
public class PageText {

    public static int totalPages(String pagedDoc) {
        int max = 0;
        Matcher m = Pattern.compile("\\[Page (\\d+)\\]").matcher(pagedDoc);
        while (m.find()) {
            try { max = Math.max(max, Integer.parseInt(m.group(1))); } catch (Exception ignored) { }
        }
        return max;
    }

    // Detect a page reference in the user's message. Understands English
    // ("page no 86", "page 86", "86 page"), Roman Urdu ("safha 86") and
    // Urdu digits. Returns -1 when there is no page reference.
    public static int pageRef(String msg) {
        if (msg == null) return -1;
        String s = toLatinDigits(msg.toLowerCase());
        Matcher m;
        m = Pattern.compile("page\\s*(?:no\\.?|number|#)?\\s*(\\d{1,4})").matcher(s);
        if (m.find()) return parse(m.group(1));
        m = Pattern.compile("(?:safha|safah|panna)\\s*(?:no\\.?)?\\s*(\\d{1,4})").matcher(s);
        if (m.find()) return parse(m.group(1));
        m = Pattern.compile("(\\d{1,4})\\s*(?:page|safha|safah|panna)").matcher(s);
        if (m.find()) return parse(m.group(1));
        return -1;
    }

    private static int parse(String g) {
        try { return Integer.parseInt(g); } catch (Exception e) { return -1; }
    }

    private static String toLatinDigits(String s) {
        StringBuilder b = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch >= '۰' && ch <= '۹') b.append((char) ('0' + (ch - '۰')));       // Urdu digits
            else if (ch >= '٠' && ch <= '٩') b.append((char) ('0' + (ch - '٠'))); // Arabic digits
            else b.append(ch);
        }
        return b.toString();
    }

    // The exact text of one page (without its marker), or null if absent.
    public static String pageContent(String pagedDoc, int page) {
        String marker = "[Page " + page + "]";
        int start = pagedDoc.indexOf(marker);
        if (start < 0) return null;
        start += marker.length();
        int end = pagedDoc.indexOf("[Page ", start);
        if (end < 0) end = pagedDoc.length();
        return pagedDoc.substring(start, end).trim();
    }

    // Context to send to the AI: if the question names a page, that page's
    // exact text; otherwise as much of the document as fits maxChars.
    // Returns null when a named page is out of range (caller reports it).
    public static String contextFor(String doc, String question, int maxChars) {
        int ref = pageRef(question);
        if (ref > 0) {
            int total = totalPages(doc);
            if (total > 0 && ref > total) return null;
            String pc = pageContent(doc, ref);
            if (pc != null) {
                return "[The user is asking about page " + ref + " of " + total
                        + ". Below is the exact text of page " + ref + ". Answer from this page and cite it as (Page "
                        + ref + ").]\n\n" + pc;
            }
        }
        return doc.length() > maxChars ? doc.substring(0, maxChars) : doc;
    }

    public static String outOfRangeMessage(int ref, int total) {
        return "This PDF has " + total + " pages in total, so there is no page " + ref + " in it.\n\n"
                + "Note: the page numbers printed inside a book are often different from the PDF's own page numbers "
                + "(for example, printed page 95 may be PDF page 98). Try asking for a nearby page number.";
    }
}
