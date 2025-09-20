package com.revenuecat.purchases.interfaces;

import androidx.annotation.NonNull;

import com.revenuecat.purchases.CustomerInfo;
import com.revenuecat.purchases.PurchasesError;
import com.revenuecat.purchases.models.StoreTransaction;

public interface PurchaseCallback {
    public void onError(@NonNull PurchasesError purchasesError, boolean b);
    public void onCompleted(@NonNull StoreTransaction storeTransaction, @NonNull CustomerInfo customerInfo);
}
