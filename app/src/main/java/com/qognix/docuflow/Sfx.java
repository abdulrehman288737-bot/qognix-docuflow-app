package com.qognix.docuflow;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioAttributes;
import android.media.SoundPool;

// Small UI sound effects (user request): a soft whoosh when switching
// pages and a gentle pop on taps. Sounds are tiny synthesized WAV files
// in assets. Every call is guarded — a sound must never crash the app.
public class Sfx {
    private static SoundPool pool;
    private static int whooshId = 0, popId = 0;

    public static void init(Context c) {
        if (pool != null) return;
        try {
            AudioAttributes attrs = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();
            pool = new SoundPool.Builder().setMaxStreams(3).setAudioAttributes(attrs).build();
            AssetFileDescriptor a = c.getAssets().openFd("whoosh.wav");
            whooshId = pool.load(a, 1);
            a.close();
            AssetFileDescriptor b = c.getAssets().openFd("pop.wav");
            popId = pool.load(b, 1);
            b.close();
        } catch (Throwable e) {
            pool = null;
        }
    }

    public static void whoosh() { play(whooshId, 0.45f); }

    public static void pop() { play(popId, 0.55f); }

    private static void play(int id, float vol) {
        if (pool == null || id == 0) return;
        try { pool.play(id, vol, vol, 1, 0, 1f); } catch (Throwable ignored) { }
    }
}
