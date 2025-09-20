package com.google.android.gms.ads;

import android.content.Context;

import java.util.List;

public class RequestConfiguration {

    public static class Builder {

        public Builder() {
        }

        public Builder setTestDeviceIds(List<String> testDevices) {
            return this;
        }

        public RequestConfiguration build() {
            return new RequestConfiguration();
        }
    }
}
