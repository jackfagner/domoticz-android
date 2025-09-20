package com.google.android.gms.ads;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

public class AdView extends View {
    public AdView(Context context) {
        super(context);
    }

    public AdView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public AdView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public AdView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public void loadAd(AdRequest adRequest) {
    }

    public void setAdUnitID(String adUnitID) {

    }

    public void setAdSize(String adUnitID) {

    }
}
