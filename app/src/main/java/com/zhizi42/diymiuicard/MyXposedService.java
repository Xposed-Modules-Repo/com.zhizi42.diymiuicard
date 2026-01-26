package com.zhizi42.diymiuicard;

import android.content.SharedPreferences;

import io.github.libxposed.service.XposedService;

public class MyXposedService {
    public static XposedService xposedService;

    public static void setService(XposedService service) {
        xposedService = service;
    }

    public static XposedService getService() {
        return xposedService;
    }
}
