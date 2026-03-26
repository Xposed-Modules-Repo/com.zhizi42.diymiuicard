package com.zhizi42.diymiuicard;

import android.content.SharedPreferences;

import io.github.libxposed.service.XposedService;

public class MyXposedService {
    public static XposedService xposedService;
    public static SharedPreferences sharedPreferences;

    public static void setService(XposedService service) {
        xposedService = service;
        sharedPreferences = service.getRemotePreferences("settings");
    }

    public static XposedService getService() {
        return xposedService;
    }

    public static SharedPreferences getSharedPreferences() {
        return sharedPreferences;
    }
}