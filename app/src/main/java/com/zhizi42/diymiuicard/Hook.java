package com.zhizi42.diymiuicard;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.annotations.AfterInvocation;
import io.github.libxposed.api.annotations.BeforeInvocation;
import io.github.libxposed.api.annotations.XposedHooker;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;

import com.crossbowffs.remotepreferences.RemotePreferences;

import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.exceptions.NoResultException;
import org.luckypray.dexkit.query.FindClass;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.enums.StringMatchType;
import org.luckypray.dexkit.query.matchers.ClassMatcher;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.query.matchers.base.StringMatcher;
import org.luckypray.dexkit.result.MethodData;
import org.luckypray.dexkit.result.MethodDataList;
import org.luckypray.dexkit.wrap.DexMethod;

@Keep
public class Hook extends XposedModule {

    private static SharedPreferences sharedPreferences;
    private static SharedPreferences writeSharedPreference;
    private static Hook self;
    private int OSType;

    static {
        System.loadLibrary("dexkit");
    }

    public Hook(@NonNull XposedInterface base, @NonNull ModuleLoadedParam param) {
        super(base, param);
        self = this;
    }

    @Keep
    @XposedHooker
    public static class HookHyper implements Hooker {
        @AfterInvocation
        public static void after(AfterHookCallback callback) {
            //获取替换后的图片路径，不为空就设置hook的函数返回值为路径
            String url = replaceUrl((String) callback.getArgs()[0], 0);
            if (! url.isEmpty()) {
                callback.setResult(url);
            }
        }
    }

    @Keep
    @XposedHooker
    public static class HookHyperSuperLand implements Hooker {
        @AfterInvocation
        public static void after(AfterHookCallback callback) {
            //获取替换后的图片路径，不为空就设置hook的函数返回值为路径
            String url = replaceUrl((String) callback.getResult(), 0);
            if (! url.isEmpty()) {
                callback.setResult(url);
            }
        }
    }

    @Keep
    @XposedHooker
    public static class HookColor implements Hooker {
        @BeforeInvocation
        public static void before(BeforeHookCallback callback) {
            //获取替换后的图片路径，不为空就设置hook的函数参数为路径
            String url = replaceUrl((String) callback.getArgs()[0], 1);
            if (! url.isEmpty()) {
                callback.getArgs()[0] = url;
            }
        }
    }

    @Keep
    @XposedHooker
    public static class HookContext implements Hooker {
        @BeforeInvocation
        public static void before(BeforeHookCallback callback) {
            //获取可写入的prefs
            try {
                Context context = (Context) callback.getArgs()[0];
                writeSharedPreference = new RemotePreferences(
                        context, "com.zhizi42.diymiuicard.preference", "settings");
            } catch (Exception e) {
                self.log("hook context error:");
                self.log(e.toString());
            }
        }
    }

    private String[] readMethodFile(String name) {
        //读取缓存的类名和方法名
        String path;
        if (OSType == 0) {
            path = "/data/data/com.miui.tsmclient/files/";
        } else if (OSType == 1) {
            path = "/data/data/com.finshell.wallet/files/";
        } else {
            return new String[0];
        }
        try {
            File methodFile = new File(path, name);
            FileInputStream inputStream = new FileInputStream(methodFile);
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            BufferedReader reader = new BufferedReader(inputStreamReader);
            String targetClassName = reader.readLine();
            String targetMethodName = reader.readLine();
            if (targetClassName == null || targetMethodName == null) {
                return new String[0];
            }
            inputStream.close();
            inputStreamReader.close();
            reader.close();
            return new String[]{targetClassName, targetMethodName};
        } catch (IOException e) {
            return new String[0];
        }
    }

    private void writeMethodFile(String name, String className, String methodName) {
        //写入类名-换行-写入方法名到缓存文件
        String path;
        if (OSType == 0) {
            path = "/data/data/com.miui.tsmclient/files/";
        } else if (OSType == 1) {
            path = "/data/data/com.finshell.wallet/files/";
        } else {
            return;
        }
        File methodFile = new File(path, name);
        try {
            FileOutputStream outputStream = new FileOutputStream(methodFile);
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream);
            BufferedWriter bufferedWriter = new BufferedWriter(outputStreamWriter);
            bufferedWriter.write(className);
            bufferedWriter.newLine();
            bufferedWriter.write(methodName);
            bufferedWriter.flush();
            outputStream.close();
            outputStreamWriter.close();
            bufferedWriter.close();
        } catch (IOException e) {
            log(e.toString());
        }
    }

    @Override
    public void onPackageLoaded(@NonNull PackageLoadedParam param) {
        super.onPackageLoaded(param);
        switch (param.getPackageName()) {
            case "com.miui.tsmclient":
                OSType = 0;
                hookStart(param);
                break;
            case "com.finshell.wallet":
                OSType = 1;
                hookStart(param);
                break;
        }
    }

    public void hookStart(PackageLoadedParam param) {
        sharedPreferences = getRemotePreferences("settings");
        Utils.setDebug(sharedPreferences.getBoolean("debug", false));

        try {
            Method attachMethod = Application.class.getDeclaredMethod("attach", Context.class);
            hook(attachMethod, HookContext.class);
            Utils.debugLog(this, "hook context succ");
        } catch (NoSuchMethodException e) {
            Utils.debugLog(this, String.format("hook context error:%s", e));
        }

        String imagesDirPath;
        if (OSType == 0) {
            imagesDirPath = "/data/data/com.miui.tsmclient/files/images";
        } else if (OSType == 1) {
            imagesDirPath = "/data/data/com.finshell.wallet/files/images";
        } else {
            return;
        }
        @SuppressLint("SdCardPath") File file = new File(imagesDirPath);
        if (!file.exists()) {
            file.mkdir();
        }

        String targetClassName;
        String targetMethodName;
        Class<?> targetMethodArgClass = String.class;

        String[] methodArray = readMethodFile("zhizi42.diycard.method.txt");
        if (methodArray.length != 2) {
            Utils.debugLog(self, "read method error, pos method");
            positionMethod(param);
            return;
        }
        targetClassName = methodArray[0];
        targetMethodName = methodArray[1];

        String settingClassName = sharedPreferences.getString("class", "");
        String settingMethodName = sharedPreferences.getString("method", "");
        String settingMethodArgName = sharedPreferences.getString("method_arg", "");
        if ((! settingClassName.isEmpty()) & (! settingMethodName.isEmpty())) {
            targetClassName = settingClassName;
            targetMethodName = settingMethodName;
            if (! settingMethodArgName.equals("String")) {
                if (settingMethodArgName.isEmpty()) {
                    targetMethodArgClass = null;
                } else {
                    try {
                        targetMethodArgClass = param.getClassLoader().loadClass(settingMethodArgName);
                    } catch (ClassNotFoundException e) {
                        log(String.format("setting method arg class not found:%s", settingMethodArgName));
                    }
                }
            }
        }

        try {
            Class<?> targetClass = param.getClassLoader().loadClass(targetClassName);
            Method targetMethod;
            if (targetMethodArgClass == null) {
                targetMethod = targetClass.getDeclaredMethod(targetMethodName);
            } else {
                targetMethod = targetClass.getDeclaredMethod(targetMethodName, targetMethodArgClass);
            }

            if (OSType == 0) {
                hook(targetMethod, HookHyper.class);
            } else if (OSType == 1) {
                hook(targetMethod, HookColor.class);
            }
            Utils.debugLog(this, "hook main method succ");
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            Utils.debugLog(this, String.format(
                    "first hook error no such method, error:%s, class:%s, method:%s, method arg class:%s, try positioning method.",
                    e, targetClassName, targetMethodName, (targetMethodArgClass == null) ? "null" : targetMethodArgClass.toString()));
            positionMethod(param);
        }

        if (OSType == 0) {
            String[] hyperSuperLandMethodArray = readMethodFile("zhizi42.diycard.HyperSuperLand.method.txt");
            if (hyperSuperLandMethodArray.length != 2) {
                Utils.debugLog(self, "read super land method error, pos method");
                positionMethod(param, true);
                return;
            }

            String targetSuperLandClassName = hyperSuperLandMethodArray[0];
            String targetSuperLandMethodName = hyperSuperLandMethodArray[1];

            try {
                Class<?> cardInfo = param.getClassLoader().loadClass("com.miui.tsmclient.entity.CardInfo");
                Class<?> targetSuperLandClass = param.getClassLoader().loadClass(targetSuperLandClassName);
                Method targetSuperLandMethod = targetSuperLandClass.getDeclaredMethod(targetSuperLandMethodName, cardInfo);
                hook(targetSuperLandMethod, HookHyperSuperLand.class);
                Utils.debugLog(this, "hook hyper os super land succ");
            } catch (ClassNotFoundException | NoSuchMethodException e) {
                Utils.debugLog(this, String.format(
                        "first hook error no such method, error:%s, class:%s, method:%s, try positioning method.",
                        e, targetSuperLandClassName, targetSuperLandMethodName
                        ));
                positionMethod(param, true);
            }
        }
    }

    public void positionMethod(PackageLoadedParam param) {
        positionMethod(param, false);
    }

    public void positionMethod(PackageLoadedParam param, boolean isSuperLand) {
            ApplicationInfo appInfo = param.getApplicationInfo();
            String apkPath = appInfo.sourceDir;
            try (DexKitBridge dexKitBridge = DexKitBridge.create(apkPath)) {
                MethodDataList methodDataList;
                if (OSType == 0) {
                    if (! isSuperLand) {
                        methodDataList = dexKitBridge.findClass(FindClass.create()
                                .searchPackages("com.miui.tsmclient.util")
                        ).findMethod(FindMethod.create()
                                .matcher(MethodMatcher.create()
                                        .paramCount(1)
                                        .paramTypes(String.class)
                                        .modifiers(Modifier.PUBLIC | Modifier.STATIC)
                                        .returnType(Object.class)
                                )
                        );
                    } else {
                        methodDataList = dexKitBridge.findClass(FindClass.create()
                                .searchPackages("com.miui.tsmclient.util")
                        ).findMethod(FindMethod.create()
                                .matcher(MethodMatcher.create()
                                        .paramCount(1)
                                        .paramTypes("com.miui.tsmclient.entity.CardInfo")
                                        .modifiers(Modifier.PRIVATE | Modifier.STATIC)
                                        .returnType(String.class)
                                )
                        );
                    }
                } else if (OSType == 1) {
                    methodDataList = dexKitBridge.findClass(FindClass.create().matcher(
                            ClassMatcher.create()
                                    .className("com.finshell.finui.widget.imageview.CircleNetworkImageView")))
                            .findMethod(FindMethod.create().matcher(MethodMatcher.create()
                                            .paramCount(1)
                                            .paramTypes(String.class)
                                            .modifiers(Modifier.PRIVATE)
                                            .returnType(ClassMatcher.create().className(
                                                    StringMatcher.create(
                                                            "com.bumptech.glide.integration.okhttp3",
                                                            StringMatchType.StartsWith)
                                            ))
                                    )
                            );
                } else {
                    return;
                }
                if (methodDataList.getSize() != 1) {
                    log(String.format("pos target method result num not 1, OS Type:%s, is Super Land:%s, positioning target method num is:%s", OSType, isSuperLand, methodDataList.getSize()));
                    for (MethodData method:methodDataList) {
                        log(String.format("class name:%s, method name:%s", method.getClassName(), method.getMethodName()));
                    }
                    return;
                }
                MethodData methodData = methodDataList.single();
                Method method = methodData.getMethodInstance(param.getClassLoader());
                DexMethod dexMethod = methodData.toDexMethod();
                updateTargetHookName(dexMethod.getClassName(), dexMethod.getName(), isSuperLand);
                if (OSType == 0) {
                    if (! isSuperLand) {
                        hook(method, HookHyper.class);
                    } else {
                        hook(method, HookHyperSuperLand.class);
                    }
                } else if (OSType == 1) {
                    hook(method, HookColor.class);
                }
            } catch (NoSuchMethodException | NoResultException e) {
                log("positioning target method and hook still error, message:" + e);
            }
    }

    public static String replaceUrl(String url, int OSType) {
        Utils.debugLog(self, "load image's url:" + url);//记录加载的图片链接到log

        boolean showAllCard = sharedPreferences.getBoolean("show_all_cards", false);
        if (showAllCard) {
            updateCardUrlList(url);//如果是显示所有卡片就直接添加到应用数据
        } else {
            if (OSType == 0) {
                if (!(url.contains("w270h480") || url.contains("/door-card-img/logo/"))) {
                    updateCardUrlList(url);//如果不是显示所有卡片，不是小图标才添加到数据
                }
            } else if (OSType == 1) {
                updateCardUrlList(url);
            } else {
                return "";
            }
        }

        String imageName = sharedPreferences.getString(url, "");//获取原卡面图片对应的diy卡面
        if (!imageName.isEmpty()) {//如果diy卡面不为空
            Utils.debugLog(self, "load image's diy image name:" + imageName);
            String imageUrl;
            if (imageName.startsWith("https://") || imageName.startsWith("http://")) {
                imageUrl = imageName;//如果是链接就直接设置为结果
            } else {
                String imagePath;
                if (OSType == 0) {
                    imagePath = "/data/data/com.miui.tsmclient/files/images/" + imageName;
                } else if (OSType == 1) {
                    imagePath = "/data/data/com.finshell.wallet/files/images/" + imageName;
                } else {
                    return "";
                }
                imageUrl = "file://" + imagePath;//如果是文件名字就加上file协议头和图片文件夹路径再设置为结果
                //如果开启debug，判断本地diy图片文件是否存在
                if (sharedPreferences.getBoolean("debug", false)) {
                    File file = new File(imagePath);
                    if (file.exists()) {
                        Utils.debugLog(self, "diy image file exist");
                    } else {
                        Utils.debugLog(self, "diy image file not exist");
                    }
                }
            }
            return imageUrl;
        } else {
            Utils.debugLog(self, "load image's not have diy image");
            return "";
        }
    }

    public void updateTargetHookName(String targetClassName, String targetMethodName, boolean isSuperLand) {
        String name = "zhizi42.diycard.method.txt";
        if (isSuperLand) {
            name = "zhizi42.diycard.HyperSuperLand.method.txt";
        }
        writeMethodFile(name, targetClassName, targetMethodName);
    }

    public static void updateCardUrlList(String newCardUrl) {
        if (writeSharedPreference != null) {
            SharedPreferences.Editor editor = writeSharedPreference.edit();
            Set<String> cardUrlSet = new HashSet<>(writeSharedPreference.getStringSet("all_card_url_set", new HashSet<>()));
            cardUrlSet.add(newCardUrl);
            editor.putStringSet("all_card_url_set", cardUrlSet);
            editor.apply();
        } else {
            Utils.debugLog(self, "when update card url, write shared pref is null");
        }
    }
}