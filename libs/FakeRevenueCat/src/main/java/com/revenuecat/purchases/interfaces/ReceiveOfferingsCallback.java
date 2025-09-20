package com.revenuecat.purchases.interfaces;

import androidx.annotation.NonNull;

import com.revenuecat.purchases.Offerings;
import com.revenuecat.purchases.PurchasesError;

public interface ReceiveOfferingsCallback {
    public void onReceived(@NonNull Offerings offerings);
    public void onError(@NonNull PurchasesError purchasesError);
}
