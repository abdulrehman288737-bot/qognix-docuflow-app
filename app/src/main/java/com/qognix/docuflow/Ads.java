package com.qognix.docuflow;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.startapp.sdk.ads.banner.Banner;
import com.startapp.sdk.ads.nativead.NativeAdDetails;
import com.startapp.sdk.ads.nativead.NativeAdPreferences;
import com.startapp.sdk.ads.nativead.StartAppNativeAd;
import com.startapp.sdk.adsbase.Ad;
import com.startapp.sdk.adsbase.StartAppAd;
import com.startapp.sdk.adsbase.StartAppSDK;
import com.startapp.sdk.adsbase.adlisteners.AdDisplayListener;
import com.startapp.sdk.adsbase.adlisteners.AdEventListener;

import java.util.ArrayList;

// Start.io (StartApp) ads for Qognix DocuFlow — App ID 210654162 belongs to
// the user's own Start.io account (a public ad identifier, not a secret).
//
// Placement rules (user spec):
// - Banner: only in the designated slot at the bottom of tab screens.
// - Interstitial: only at natural transitions (leaving a finished result
//   screen, finishing a quiz), never on every click, with a time cap.
// - Native: one clearly-labelled card inside the Documents list.
// - Rewarded: only when the user taps the "Watch a video ad" button.
// - Return ads are disabled. One launch interstitial is shown when the
//   app opens (added in v3.3 at the user's explicit request).
// - Test ads are enabled automatically in DEBUG builds only (the APK the
//   user tests with); release builds serve real ads.
// - Every call is wrapped so an ad failure can never crash or block the app.
public class Ads {
    public static final String APP_ID = "210654162";
    private static final long INTERSTITIAL_CAP_MS = 150000; // at most one per 2.5 minutes

    private static boolean inited = false;
    private static boolean sdkReady = false;
    private static StartAppAd interstitial = null;
    private static boolean interstitialReady = false;
    private static long lastInterstitialShown = 0;
    private static StartAppAd rewarded = null;
    private static boolean rewardedReady = false;
    private static int rewardedRetries = 0;
    private static boolean rewardedRetryScheduled = false;

    public static void init(Context c) {
        if (inited) return;
        inited = true;
        try {
            StartAppSDK.initParams(c.getApplicationContext(), APP_ID)
                    .setReturnAdsEnabled(false)
                    .setCallback(new Runnable() {
                        @Override
                        public void run() {
                            // SDK finished initializing — now ad loads are
                            // reliable. Fire the queued launch ad, if any.
                            sdkReady = true;
                            if (launchRequested && launchHost != null) doLaunch(launchHost);
                        }
                    })
                    .init();
            boolean debuggable = (c.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
            if (debuggable) StartAppSDK.setTestAdsEnabled(true);
        } catch (Throwable e) {
            // Ads must never crash the app.
        }
    }

    // ---- Banner (designated bottom slot on tab screens) ----
    public static void addBanner(Activity a, ViewGroup box) {
        try {
            Banner banner = new Banner(a);
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, 170);
            p.setMargins(0, 6, 0, 6);
            box.addView(banner, p);
        } catch (Throwable e) {
            // No banner if the SDK cannot create one — app stays usable.
        }
    }

    // ---- Interstitial (natural transitions only, frequency-capped) ----
    public static void preloadInterstitial(Activity a) {
        try {
            interstitialReady = false;
            interstitial = new StartAppAd(a);
            interstitial.loadAd(new AdEventListener() {
                @Override
                public void onReceiveAd(@NonNull Ad ad) { interstitialReady = true; }

                @Override
                public void onFailedToReceiveAd(@Nullable Ad ad) { interstitialReady = false; }
            });
        } catch (Throwable e) {
            interstitialReady = false;
        }
    }

    // Shows an interstitial if one is ready and the cap allows it, then runs
    // the follow-up action (e.g. closing the screen). If no ad is available
    // the action runs immediately — navigation is never blocked by ads.
    public static void showTransition(final Activity a, final Runnable after) {
        long now = System.currentTimeMillis();
        if (interstitial == null || !interstitialReady || now - lastInterstitialShown < INTERSTITIAL_CAP_MS) {
            after.run();
            return;
        }
        interstitialReady = false;
        lastInterstitialShown = now;
        final boolean[] done = {false};
        final Runnable once = new Runnable() {
            @Override
            public void run() {
                if (!done[0]) { done[0] = true; after.run(); }
            }
        };
        try {
            interstitial.showAd(new AdDisplayListener() {
                @Override
                public void adDisplayed(Ad ad) { }

                @Override
                public void adHidden(Ad ad) { once.run(); }

                @Override
                public void adClicked(Ad ad) { }

                @Override
                public void adNotDisplayed(Ad ad) { once.run(); }
            });
            // Safety net: never trap the user on a screen because of an ad.
            new Handler(Looper.getMainLooper()).postDelayed(once, 6000);
        } catch (Throwable e) {
            try { interstitial.showAd(); } catch (Throwable ignored) { }
            once.run();
        }
        preloadInterstitial(a);
    }

    // ---- Launch ad (one interstitial when the app opens; user request) ----
    // v3.5: the load waits for the SDK-ready callback (loading earlier can
    // fail silently), with a timed fallback and one retry, so the launch
    // ad actually appears whenever Start.io has one to show.
    private static boolean launchAdShown = false;
    private static boolean launchRequested = false;
    private static boolean launchStarted = false;
    private static boolean launchRetried = false;
    private static Activity launchHost = null;
    private static StartAppAd launchAd = null;

    public static void showLaunchAd(final Activity a) {
        if (launchAdShown) return;
        launchAdShown = true;
        launchRequested = true;
        launchHost = a;
        if (sdkReady) {
            doLaunch(a);
        } else {
            // Fallback: if the init callback is slow, try anyway after 4s.
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() { if (!launchStarted && !a.isFinishing()) doLaunch(a); }
            }, 4000);
        }
    }

    private static void doLaunch(final Activity a) {
        if (launchStarted) return;
        launchStarted = true;
        try {
            launchAd = new StartAppAd(a);
            launchAd.loadAd(new AdEventListener() {
                @Override
                public void onReceiveAd(@NonNull Ad ad) {
                    try {
                        if (!a.isFinishing() && !a.isDestroyed()) {
                            launchAd.showAd();
                            lastInterstitialShown = System.currentTimeMillis();
                        }
                    } catch (Throwable ignored) { }
                }

                @Override
                public void onFailedToReceiveAd(@Nullable Ad ad) {
                    if (!launchRetried) {
                        launchRetried = true;
                        launchStarted = false;
                        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                            @Override
                            public void run() { if (!a.isFinishing() && !a.isDestroyed()) doLaunch(a); }
                        }, 8000);
                    }
                }
            });
        } catch (Throwable ignored) { }
    }

    // ---- Rewarded video (explicit user choice only) ----
    public static void preloadRewarded(final Activity a) {
        try {
            rewardedReady = false;
            rewarded = new StartAppAd(a);
            rewarded.loadAd(StartAppAd.AdMode.REWARDED_VIDEO, new AdEventListener() {
                @Override
                public void onReceiveAd(@NonNull Ad ad) {
                    rewardedReady = true;
                    rewardedRetries = 0;
                }

                @Override
                public void onFailedToReceiveAd(@Nullable Ad ad) {
                    rewardedReady = false;
                    // Retry a few times with growing delays — a single failed
                    // load (SDK warming up / momentary no-fill) should not
                    // leave the button dead for the whole session.
                    if (rewardedRetries < 3 && !rewardedRetryScheduled) {
                        rewardedRetryScheduled = true;
                        long delay = rewardedRetries == 0 ? 5000 : rewardedRetries == 1 ? 15000 : 30000;
                        rewardedRetries++;
                        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                rewardedRetryScheduled = false;
                                if (!a.isFinishing()) preloadRewarded(a);
                            }
                        }, delay);
                    }
                }
            });
        } catch (Throwable e) {
            rewardedReady = false;
        }
    }

    public static boolean showRewarded(final Activity a) {
        if (rewarded != null && rewardedReady) {
            rewardedReady = false;
            try {
                rewarded.setVideoListener(new com.startapp.sdk.adsbase.VideoListener() {
                    @Override
                    public void onVideoCompleted() {
                        Toast.makeText(a, "Thanks for watching the ad and supporting Qognix!", Toast.LENGTH_LONG).show();
                    }
                });
                rewarded.showAd();
            } catch (Throwable e) {
                preloadRewarded(a);
                return false;
            }
            preloadRewarded(a);
            return true;
        }
        preloadRewarded(a);
        return false;
    }

    // ---- Native ad (one labelled card, used in the Documents list) ----
    public static void loadNativeInto(final Activity a, final LinearLayout container) {
        try {
            NativeAdPreferences prefs = new NativeAdPreferences();
            prefs.setAutoBitmapDownload(true);
            final StartAppNativeAd nativeAd = new StartAppNativeAd(a.getApplicationContext());
            nativeAd.setPreferences(prefs);
            nativeAd.loadAd(new AdEventListener() {
                @Override
                public void onReceiveAd(@NonNull Ad ad) {
                    try {
                        ArrayList<NativeAdDetails> ads = nativeAd.getNativeAds();
                        if (ads == null || ads.isEmpty()) return;
                        NativeAdDetails d = ads.get(0);
                        LinearLayout card = new LinearLayout(a);
                        card.setOrientation(LinearLayout.VERTICAL);
                        card.setBackground(Ui.bg(Ui.CARD, 28, 1));
                        card.setPadding(30, 24, 30, 24);
                        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
                        lp.setMargins(0, 14, 0, 14);
                        card.setLayoutParams(lp);
                        card.addView(Ui.text(a, "Advertisement", 11, Ui.LIGHT, true));
                        // SDK 5.x exposes the ad image as a URL (there is no
                        // getBitmap()); download it in the background.
                        final ImageView iv = new ImageView(a);
                        iv.setAdjustViewBounds(true);
                        card.addView(iv);
                        final String imgUrl = d.getImageUrl();
                        if (imgUrl != null && !imgUrl.isEmpty()) {
                            new Thread(() -> {
                                try {
                                    java.io.InputStream in = new java.net.URL(imgUrl).openStream();
                                    final Bitmap bmp = android.graphics.BitmapFactory.decodeStream(in);
                                    if (bmp != null) a.runOnUiThread(() -> iv.setImageBitmap(bmp));
                                } catch (Throwable ignored) { }
                            }).start();
                        }
                        if (d.getTitle() != null) card.addView(Ui.text(a, d.getTitle(), 16, Ui.TEXT, true));
                        if (d.getDescription() != null) card.addView(Ui.text(a, d.getDescription(), 13, Ui.SUB, false));
                        d.registerViewForInteraction(card);
                        container.addView(card);
                    } catch (Throwable ignored) { }
                }

                @Override
                public void onFailedToReceiveAd(@Nullable Ad ad) { }
            });
        } catch (Throwable ignored) { }
    }
}
