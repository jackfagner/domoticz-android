package com.google.android.gms.ads.nativead;

import android.graphics.drawable.Drawable;

public class NativeAd {
    public String getStore() {
        return "";
    }

    public String getAdvertiser() {
        return "";
    }

    public String getHeadline() {
        return "";
    }

    public String getBody() {
        return "";
    }

    public String getCallToAction() {
        return "";
    }

    public Double getStarRating() {
        return 0.0;
    }

    public NativeAd.Image getIcon() {
        return null;
    }

    public void destroy() {
    }

    public interface OnNativeAdLoadedListener {
        abstract void onNativeAdLoaded(NativeAd ad);
    }

    public class Image {
        public Drawable getDrawable() {
            return null;
        }
    }
}
