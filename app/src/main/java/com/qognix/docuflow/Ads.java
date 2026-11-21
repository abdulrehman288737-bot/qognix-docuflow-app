package com.qognix.docuflow;

import android.app.Activity;
import android.content.Context;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.startapp.sdk.adsbase.Ad;
import com.startapp.sdk.adsbase.StartAppAd;
import com.startapp.sdk.adsbase.StartAppSDK;
import com.startapp.sdk.adsbase.adlisteners.AdEventListener;

// Start.io (StartApp) full-screen video ads for Qognix DocuFlow.
// App ID belongs to the user's own Start.io account (portal.start.io) — it is
// a public ad identifier, not a password or secret key.
// Formats used: interstitial (serves video/display full-screen ads between
// actions) and rewarded video (user taps the button to watch a video ad).
// Return ads are disabled on purpose: no ads pop up when returning to the app.
public class Ads {
    public static final String APP_ID = "210654162";

    private static boolean inited = false;
    private static StartAppAd interstitial = null;
    private static boolean interstitialReady = false;
    private static StartAppAd rewarded = null;
    private static boolean rewardedReady = false;

    public static void init(Context c) {
        if (inited) return;
        inited = true;
        try {
            StartAppSDK.initParams(c.getApplicationContext(), APP_ID)
                    .setReturnAdsEnabled(false)
                    .setCallback(new Runnable() {
                        @Override
                        public void run() {
                            // SDK ready to request ads.
                        }
                    })
                    .init();
        } catch (Exception e) {
            // Ads must never crash the app.
        }
    }

    public static void preload(Activity a) {
        preloadInterstitial(a);
        preloadRewarded(a);
    }

    private static void preloadInterstitial(final Activity a) {
        try {
            interstitialReady = false;
            interstitial = new StartAppAd(a);
            interstitial.loadAd(new AdEventListener() {
                @Override
                public void onReceiveAd(@NonNull Ad ad) {
                    interstitialReady = true;
                }

                @Override
                public void onFailedToReceiveAd(@Nullable Ad ad) {
                    interstitialReady = false;
                }
            });
        } catch (Exception e) {
            interstitialReady = false;
        }
    }

    // Shows the preloaded full-screen interstitial if one is ready.
    // Safe to call right after startActivity(), the documented Start.io
    // pattern for an ad shown between activities. Never blocks navigation.
    public static void showInterstitial(Activity a) {
        if (interstitial != null && interstitialReady) {
            interstitialReady = false;
            try {
                interstitial.showAd();
            } catch (Exception e) {
                // ignore — the tool screen is already opening
            }
            preloadInterstitial(a);
        }
    }

    private static void preloadRewarded(final Activity a) {
        try {
            rewardedReady = false;
            rewarded = new StartAppAd(a);
            rewarded.loadAd(StartAppAd.AdMode.REWARDED_VIDEO, new AdEventListener() {
                @Override
                public void onReceiveAd(@NonNull Ad ad) {
                    rewardedReady = true;
                }

                @Override
                public void onFailedToReceiveAd(@Nullable Ad ad) {
                    rewardedReady = false;
                }
            });
        } catch (Exception e) {
            rewardedReady = false;
        }
    }

    // Shows the rewarded video ad. Returns false when it is still loading.
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
            } catch (Exception e) {
                preloadRewarded(a);
                return false;
            }
            preloadRewarded(a);
            return true;
        }
        preloadRewarded(a);
        return false;
    }
}
