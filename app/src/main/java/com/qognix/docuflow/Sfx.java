package com.qognix.docuflow;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioAttributes;
import android.media.SoundPool;

// UI sound effects (user request): soft, calm, layered tones — a gentle
// pop on every tap, a light whoosh on page changes, a soothing two-note
// chime for success and a soft low tone for errors. Nothing harsh or
// loud. Sounds are tiny synthesized WAV files in assets. Every call is
// guarded — a sound must never crash the app.
public class Sfx {
    private static SoundPool pool;
    private static int whooshId = 0, popId = 0, successId = 0, errorId = 0;

    public static void init(Context c) {
        if (pool != null) return;
        try {
            AudioAttributes attrs = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();
            pool = new SoundPool.Builder().setMaxStreams(4).setAudioAttributes(attrs).build();
            whooshId = load(c, "whoosh.wav");
            popId = load(c, "pop.wav");
            successId = load(c, "success.wav");
            errorId = load(c, "error.wav");
        } catch (Throwable e) {
            pool = null;
        }
    }

    private static int load(Context c, String name) {
        try {
            AssetFileDescriptor f = c.getAssets().openFd(name);
            int id = pool.load(f, 1);
            f.close();
            return id;
        } catch (Throwable e) {
            return 0;
        }
    }

    public static void whoosh() { play(whooshId, 0.4f); }

    public static void pop() { play(popId, 0.5f); }

    public static void success() { play(successId, 0.5f); }

    public static void error() { play(errorId, 0.45f); }

    private static void play(int id, float vol) {
        if (pool == null || id == 0) return;
        try { pool.play(id, vol, vol, 1, 0, 1f); } catch (Throwable ignored) { }
    }
}
