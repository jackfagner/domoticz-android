package com.google.android.gms.ads;

import android.content.Context;

import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdOptions;

public class AdLoader {
    public void loadAd(AdRequest adRequest) {
    }

    public static class Builder {

        public Builder(Context mContext, String string) {
        }

        public Builder withAdListener(AdListener adListener) {
            return this;
        }

        public Builder withNativeAdOptions(NativeAdOptions build) {
            return this;
        }

        public AdLoader build() {
            return new AdLoader();
        }

        public Builder forNativeAd(NativeAd.OnNativeAdLoadedListener listener) {
            return this;
        }
    }
}
