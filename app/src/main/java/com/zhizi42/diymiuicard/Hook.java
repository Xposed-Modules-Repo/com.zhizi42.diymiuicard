package com.zhizi42.diymiuicard;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.ParcelFileDescriptor;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

import dalvik.system.DexClassLoader;
import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.utils.DexParser;

import static android.content.Context.MODE_PRIVATE;

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

    static {
        System.loadLibrary("dexkit");
    }

    public Hook(@NonNull XposedInterface base, @NonNull ModuleLoadedParam param) {
        super(base, param);
        self = this;
    }

    public static class HookHyper implements Hooker {
        @Keep
        public static void after(AfterHookCallback callback) {
            //获取替换后的图片路径，不为空就设置hook的函数返回值为路径
            String url = replaceUrl((String) callback.getArgs()[0], 0);
            if (! url.isEmpty()) {
                callback.setResult(url);
            }
        }
    }

    public static class HookColor implements Hooker {
        @Keep
        public static void before(BeforeHookCallback callback) {
            //获取替换后的图片路径，不为空就设置hook的函数参数为路径
            String url = replaceUrl((String) callback.getArgs()[0], 1);
            if (! url.isEmpty()) {
                callback.getArgs()[0] = url;
            }
        }
    }

    public static class HookContext implements Hooker {
        @Keep
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

    @Override
    public void onPackageLoaded(@NonNull PackageLoadedParam param) {
        super.onPackageLoaded(param);
        switch (param.getPackageName()) {
            case "com.miui.tsmclient":
                hookStart(param, 0);
                break;
            case "com.finshell.wallet":
                hookStart(param, 1);
                break;
        }
    }

    public void hookStart(PackageLoadedParam param, int OSType) {
        sharedPreferences = getRemotePreferences("settings");

        String targetClassName;
        String targetMethodName;
        try {
            String methodFilePath;
            if (OSType == 0) {
                methodFilePath = "/data/data/com.miui.tsmclient/files/";
            } else if (OSType == 1) {
                methodFilePath = "/data/data/com.finshell.wallet/files/";
            } else {
                return;
            }
            File methodFile = new File(methodFilePath, "zhizi42.diycard.method.txt");
            FileInputStream inputStream = new FileInputStream(methodFile);
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            BufferedReader reader = new BufferedReader(inputStreamReader);
            targetClassName = reader.readLine();
            targetMethodName = reader.readLine();
            if (targetClassName == null || targetMethodName == null) {
                positionMethod(param, OSType);
                return;
            }
            inputStream.close();
            inputStreamReader.close();
            reader.close();
        } catch (Exception e) {
            positionMethod(param, OSType);
            return;
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


        try {
            Method attachMethod = Application.class.getDeclaredMethod("attach", Context.class);
            hook(attachMethod, HookContext.class);

            Class<?> targetClass = param.getClassLoader().loadClass(targetClassName);
            Method targetMethod = targetClass.getDeclaredMethod(targetMethodName, String.class);

            if (OSType == 0) {
                hook(targetMethod, HookHyper.class);
            } else if (OSType == 1) {
                hook(targetMethod, HookColor.class);
            }
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            positionMethod(param, OSType);
        }
    }

    public void positionMethod(PackageLoadedParam param, int OSType) {
            Utils.debugLog(this, "first hook error no such method, try positioning method.");

            ApplicationInfo appInfo = param.getApplicationInfo();
            String apkPath = appInfo.sourceDir;
            try (DexKitBridge dexKitBridge = DexKitBridge.create(apkPath)) {
                MethodData methodData;
                if (OSType == 0) {
                    methodData = dexKitBridge.findClass(FindClass.create()
                            .searchPackages("com.miui.tsmclient.util")
                    ).findMethod(FindMethod.create()
                            .matcher(MethodMatcher.create()
                                    .paramCount(1)
                                    .paramTypes(String.class)
                                    .modifiers(Modifier.PUBLIC | Modifier.STATIC)
                                    .returnType(Object.class)
                            )
                    ).single();
                } else if (OSType == 1) {
                    methodData = dexKitBridge.findClass(FindClass.create().matcher(ClassMatcher.create()
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
                            ).single();
                } else {
                    return;
                }
                Method method = methodData.getMethodInstance(param.getClassLoader());
                DexMethod dexMethod = methodData.toDexMethod();
                updateTargetHookName(dexMethod.getClassName(), dexMethod.getName(), OSType);
                if (OSType == 0) {
                    hook(method, HookHyper.class);
                } else if (OSType == 1) {
                    hook(method, HookColor.class);
                }
            } catch (NoSuchMethodException | NoResultException ex) {
                log("hook load image method error: no such method, after positioning target method. message:" + ex);
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

    public void updateTargetHookName(String targetClassName, String targetMethodName, int OSType) {
        //获取文件，写入类名-换行-写入方法名
        String methodFilePath;
        if (OSType == 0) {
            methodFilePath = "/data/data/com.miui.tsmclient/files/";
        } else if (OSType == 1) {
            methodFilePath = "/data/data/com.finshell.wallet/files/";
        } else {
            return;
        }
        File methodFile = new File(methodFilePath, "zhizi42.diycard.method.txt");
        try {
            FileOutputStream outputStream = new FileOutputStream(methodFile);
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream);
            BufferedWriter bufferedWriter = new BufferedWriter(outputStreamWriter);
            bufferedWriter.write(targetClassName);
            bufferedWriter.newLine();
            bufferedWriter.write(targetMethodName);
            bufferedWriter.flush();
            outputStream.close();
            outputStreamWriter.close();
            bufferedWriter.close();
        } catch (IOException e) {
            log(e.toString());
        }
    }

    public static void updateCardUrlList(String newCardUrl) {
        SharedPreferences.Editor editor = writeSharedPreference.edit();
        Set<String> cardUrlSet = new HashSet<>(writeSharedPreference.getStringSet("all_card_url_set", new HashSet<>()));
        cardUrlSet.add(newCardUrl);
        editor.putStringSet("all_card_url_set", cardUrlSet);
        editor.apply();
    }
}