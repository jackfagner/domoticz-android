package com.google.android.gms.analytics;

public class GoogleAnalytics {
    public static GoogleAnalytics getInstance(Object source) {
        return new GoogleAnalytics();
    }

    public Tracker newTracker(String string) {
        return new Tracker();
    }
}
