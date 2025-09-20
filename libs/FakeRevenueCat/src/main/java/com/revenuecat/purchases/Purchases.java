package com.revenuecat.purchases;

import android.app.Activity;

import com.revenuecat.purchases.interfaces.PurchaseCallback;
import com.revenuecat.purchases.interfaces.ReceiveCustomerInfoCallback;
import com.revenuecat.purchases.interfaces.ReceiveOfferingsCallback;
import com.revenuecat.purchases.models.StoreTransaction;

public class Purchases {
    public static Purchases getSharedInstance() {
        return new Purchases();
    }

    public static void setDebugLogsEnabled(boolean debug) {
    }

    public static void configure(PurchasesConfiguration build) {
    }

    public void restorePurchases(ReceiveCustomerInfoCallback callback) {
        callback.onReceived(new CustomerInfo());
    }

    public void purchasePackage(Activity activity, Package packageToPurchase, PurchaseCallback listener) {
        listener.onCompleted(new StoreTransaction(), new CustomerInfo());
    }

    public void getOfferings(ReceiveOfferingsCallback receiveOfferingsCallback) {
        receiveOfferingsCallback.onReceived(new Offerings());
    }

    public void getCustomerInfo(ReceiveCustomerInfoCallback receiveCustomerInfoCallback) {
        receiveCustomerInfoCallback.onReceived(new CustomerInfo());
    }
}
