package com.zhizi42.diymiuicard;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

import dalvik.system.DexClassLoader;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static android.content.Context.MODE_PRIVATE;

import androidx.annotation.Keep;
import androidx.preference.PreferenceManager;

@Keep
public class Hook implements IXposedHookLoadPackage {

    public static ArrayList<String> cardUrlList = new ArrayList<>();
    public Context context;

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam loadPackageParam) {
        if (loadPackageParam.packageName.equals("com.zhizi42.diymiuicard")) {
            XposedHelpers.findAndHookMethod("com.zhizi42.diymiuicard.MainActivity", loadPackageParam.classLoader, "isOn", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    super.afterHookedMethod(param);
                    param.setResult(true);
                }
            });
        } else if (loadPackageParam.packageName.equals("com.miui.tsmclient")) {
            hook(loadPackageParam);
        }
    }

    public void hook(XC_LoadPackage.LoadPackageParam loadPackageParam) {
        XposedHelpers.findAndHookMethod(Application.class, "attach", Context.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
                context = (Context) param.args[0];
                if (context == null) {
                    XposedBridge.log("attach context is null");
                    return;
                }
                MultiprocessSharedPreferences.setAuthority("com.zhizi42.diymiuicard.provider");
                SharedPreferences sharedPreferences = MultiprocessSharedPreferences.getSharedPreferences(context, "settings", MODE_PRIVATE);
                File file = new File("/data/data/com.zhizi42.tsmclient/files/images");
                if (! file.exists()) {
                    file.mkdir();
                }

                String targetClassName = sharedPreferences.getString("target_class_name", "com.miui.tsmclient.util.z");
                String targetMethodName = sharedPreferences.getString("target_method_name", "i");
                try {
                    hookTarget(targetClassName, targetMethodName, loadPackageParam);
                } catch (NoSuchMethodError e) {
                    PackageManager packageManager = context.getPackageManager();
                    try {
                        ApplicationInfo appInfo = packageManager.getApplicationInfo("com.miui.tsmclient", 0);
                        String apkPath = appInfo.sourceDir;
                        ClassLoader classLoader = new DexClassLoader(apkPath, null, null, null);
                        Object dexPathList = XposedHelpers.getObjectField(classLoader, "pathList");
                        Object[] dexElements = (Object[]) XposedHelpers.getObjectField(dexPathList, "dexElements");
                        ArrayList<String> utilClassList = new ArrayList<>();
                        for (Object dexElement : dexElements) {
                            Object dexFile = XposedHelpers.getObjectField(dexElement, "dexFile");
                            Enumeration<String> classNames = (Enumeration<String>) XposedHelpers.callMethod(dexFile, "entries");
                            while (classNames.hasMoreElements()) {
                                String className = classNames.nextElement();
                                if (className.startsWith("com.miui.tsmclient.util")) {
                                    utilClassList.add(className);
                                }
                            }
                        }
                        for (String className : utilClassList) {
                            Class<?> clazz = XposedHelpers.findClass(className, loadPackageParam.classLoader);
                            Method[] methods = clazz.getMethods();

                            for (Method method : methods) {
                                if (method.getParameterCount() == 1) {
                                    if (method.getParameterTypes()[0] == String.class) {
                                        if (method.getReturnType() == Object.class) {
                                            targetClassName = className;
                                            targetMethodName = method.getName();
                                            updateTargetHookName(sharedPreferences, targetClassName, targetMethodName);
                                            hookTarget(targetClassName, targetMethodName, loadPackageParam);
                                        }
                                    }
                                }
                            }
                        }
                    } catch (Exception e0) {
                        XposedBridge.log("hook error:" + e0);
                    }
                }



            }
        });
    }

    public void hookTarget(String targetClassName, String targetMethodName,
                           XC_LoadPackage.LoadPackageParam loadPackageParam) {
        MultiprocessSharedPreferences.setAuthority("com.zhizi42.diymiuicard.provider");
        SharedPreferences sharedPreferences = MultiprocessSharedPreferences.getSharedPreferences(context, "settings", MODE_PRIVATE);
        SharedPreferences sharedPreferencesSettings = MultiprocessSharedPreferences
                .getSharedPreferences(context, "com.zhizi42.diymiuicard_preferences", MODE_PRIVATE);
        String className = sharedPreferencesSettings.getString("class", "");
        String methodName = sharedPreferencesSettings.getString("method", "");
        if (! className.equals("")) {
            targetClassName = className;
        }
        if (! methodName.equals("")) {
            targetMethodName = methodName;
        }


        XposedHelpers.findAndHookMethod(targetClassName, loadPackageParam.classLoader, targetMethodName, String.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
                if (context == null) {
                    XposedBridge.log("hook target context is null");
                }
                String url = (String) param.args[0];
                boolean showAllCard = sharedPreferencesSettings.getBoolean("show_all_cards", false);
                if (showAllCard) {
                    cardUrlList.add(url);
                } else {
                    if (! (url.contains("w270h480") || url.contains("/door-card-img/logo/"))) {
                        cardUrlList.add(url);
                    }
                }
                updateCardUrlList(sharedPreferences);
                String imageName = sharedPreferences.getString(url, "");
                if (! imageName.equals("")) {
                    String urlNew;
                    if (!imageName.startsWith("https://") || imageName.startsWith("http://")) {
                        urlNew = "file:///data/data/com.miui.tsmclient/files/images/" + imageName;
                    } else {
                        urlNew = imageName;
                    }
                    param.setResult(urlNew);
                }
            }
        });
    }

    public void updateTargetHookName(SharedPreferences sharedPreferences, String targetClassName, String targetMethodName) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("target_class_name", targetClassName);
        editor.putString("target_method_name", targetMethodName);
        editor.apply();
    }

    public void updateCardUrlList(SharedPreferences sharedPreferences) {
        if (cardUrlList != null) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            Set<String> cardUrlSet = new HashSet<>(cardUrlList);
            cardUrlSet.addAll(sharedPreferences.getStringSet("all_card_url_set", new HashSet<>()));
            editor.putStringSet("all_card_url_set", cardUrlSet);
            editor.apply();
        }
    }
}
