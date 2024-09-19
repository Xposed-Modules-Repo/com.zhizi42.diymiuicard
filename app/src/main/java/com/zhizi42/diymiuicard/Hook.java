package com.zhizi42.diymiuicard;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

import dalvik.system.DexClassLoader;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static android.content.Context.MODE_PRIVATE;

import androidx.annotation.Keep;

@Keep
public class Hook implements IXposedHookLoadPackage {

    public static ArrayList<String> cardUrlList = new ArrayList<>();
    private Context context;
    private boolean isDebug = true;

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
                    XposedBridge.log("zhizi42's diy miui card: attach context is null");
                    return;
                }
                MultiprocessSharedPreferences.setAuthority("com.zhizi42.diymiuicard.provider");
                SharedPreferences sharedPreferencesSettings = MultiprocessSharedPreferences
                        .getSharedPreferences(context, "com.zhizi42.diymiuicard_preferences", MODE_PRIVATE);
                isDebug = sharedPreferencesSettings.getBoolean("debug", false);
                SharedPreferences sharedPreferences = MultiprocessSharedPreferences
                        .getSharedPreferences(context, "settings", MODE_PRIVATE);
                @SuppressLint("SdCardPath") File file = new File("/data/data/com.miui.tsmclient/files/images");
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
                                            try {
                                                hookTarget(targetClassName, targetMethodName, loadPackageParam);
                                            } catch (NoSuchMethodError error)  {
                                                XposedBridge.log(
                                                        "zhizi42's diy miui card: hook load image method error: no such method, after positioning target method. message:" + error.toString());
                                            }
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
        SharedPreferences sharedPreferences = MultiprocessSharedPreferences
                .getSharedPreferences(context, "settings", MODE_PRIVATE);
        SharedPreferences sharedPreferencesSettings = MultiprocessSharedPreferences
                .getSharedPreferences(context, "com.zhizi42.diymiuicard_preferences", MODE_PRIVATE);
        String className = sharedPreferencesSettings.getString("class", "");
        String methodName = sharedPreferencesSettings.getString("method", "");
        if (!className.isEmpty()) {
            targetClassName = className;
        }
        if (!methodName.isEmpty()) {
            targetMethodName = methodName;
        }


        XposedHelpers.findAndHookMethod(targetClassName, loadPackageParam.classLoader, targetMethodName, String.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
                String url = (String) param.args[0];
                debugLog("zhizi42's diy miui card: load image's url:" + url);
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
                debugLog("zhizi42's diy miui card: load image's diy image name:" + imageName);
                if (!imageName.isEmpty()) {
                    String imageUrl;
                    if (imageName.startsWith("https://") || imageName.startsWith("http://")) {
                        imageUrl = imageName;
                    } else {
                        @SuppressLint("SdCardPath") String imagePath = "/data/data/com.miui.tsmclient/files/images/" + imageName;
                        imageUrl = "file://" + imagePath;
                        if (isDebug) {
                            File file = new File(imagePath);
                            if (file.exists()) {
                                debugLog("zhizi42's diy miui card: diy image file exist");
                            } else {
                                debugLog("zhizi42's diy miui card: diy image file not exist");
                            }
                        }
                    }
                    param.setResult(imageUrl);
                } else {
                    debugLog("zhizi42's diy miui card: load image's not have diy image");
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

    public void debugLog(String s) {
        if (isDebug) {
            XposedBridge.log(s);
        }
    }
}