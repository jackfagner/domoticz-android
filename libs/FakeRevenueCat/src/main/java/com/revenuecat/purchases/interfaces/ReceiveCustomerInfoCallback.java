package com.revenuecat.purchases.interfaces;

import androidx.annotation.NonNull;

import com.revenuecat.purchases.CustomerInfo;
import com.revenuecat.purchases.PurchasesError;

public interface ReceiveCustomerInfoCallback {
    public void onReceived(@NonNull CustomerInfo customerInfo);
    public void onError(@NonNull PurchasesError purchasesError);
}
