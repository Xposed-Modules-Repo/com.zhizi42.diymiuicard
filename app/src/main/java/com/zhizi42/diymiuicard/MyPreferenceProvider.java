package com.zhizi42.diymiuicard;

import com.crossbowffs.remotepreferences.RemotePreferenceProvider;

public class MyPreferenceProvider extends RemotePreferenceProvider {
    public MyPreferenceProvider() {
        super("com.zhizi42.diymiuicard.preference", new String[] {"settings"});
    }
}
