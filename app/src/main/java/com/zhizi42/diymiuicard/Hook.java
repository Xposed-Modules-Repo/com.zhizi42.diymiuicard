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

import io.github.libxposed.api.XposedModule;

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
    private int OSType = -1;
    private static boolean showAllCard;
    private DexKitBridge dexKitBridge = null;

    static {
        System.loadLibrary("dexkit");
    }

    @Keep
    public class MyHooker implements Hooker {
        boolean loadUrlFromArg;
        boolean replaceUrlToArg;

        public MyHooker(boolean loadUrlFromArg, boolean replaceUrlToArg) {
            //this.OSType = OSType;
            this.loadUrlFromArg = loadUrlFromArg;//图片url是否在函数参数
            this.replaceUrlToArg = replaceUrlToArg;//返回url是否在函数参数
        }

        @Override
        public Object intercept(@NonNull Chain chain) throws Throwable {
            String originalUrl;
            if (loadUrlFromArg) {//如果图片url在函数参数
                originalUrl = (String) chain.getArg(0);//从参数获取url
                String newUrl = replaceUrl(originalUrl);//得到替换url
                if (newUrl.isEmpty()) {//如果不用替换
                    return chain.proceed();//让函数继续执行
                } else {//如果要替换
                    if (replaceUrlToArg) {//如果要替换到参数
                        Object[] args = new Object[]{newUrl};
                        return chain.proceed(args);
                    } else {//如果要替换到返回值
                        return newUrl;
                    }
                }
            } else {
                originalUrl = (String) chain.proceed();//从函数返回值获取图片url
                String newUrl = replaceUrl(originalUrl);//获取替换url
                if (newUrl.isEmpty()) {//如果不用替换
                    return originalUrl;//返回原url
                } else {
                    return newUrl;//返回替换url
                }
            }
        }
    }

    private String[] readMethodFile(String name) {
        //读取缓存的类名和方法名
        String path = Utils.getWalletPath(OSType) + "files/";
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
        String path = Utils.getWalletPath(OSType) + "files/";
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
            Utils.utilsLog(this, true, "write method file error:" + e);
        }
    }

    @Override
    public void onPackageReady(@NonNull PackageReadyParam param) {
        super.onPackageReady(param);
        switch (param.getPackageName()) {
            case "com.miui.tsmclient":
                OSType = 0;
                break;
            case "com.finshell.wallet":
                OSType = 1;
                break;
            default:
                Utils.utilsLog(this, "not support card package name:" + param.getPackageName());
                return;
        }
        prepareHook(param);
    }

    public void startHook(Method targetMethod, int methodNum) {
        boolean loadUrlFromArg;
        boolean replaceUrlToArg;
        if (OSType == 0) {
            if (methodNum == 0) {
                loadUrlFromArg = true;
                replaceUrlToArg = false;
            } else if (methodNum == 1) {
                loadUrlFromArg = true;
                replaceUrlToArg = true;
            } else if (methodNum == 10) {//如果是hook超级岛方法
                loadUrlFromArg = false;
                replaceUrlToArg = false;
            } else {
                return;
            }
        } else if (OSType == 1) {
            loadUrlFromArg = true;
            replaceUrlToArg = true;
        } else if (OSType == 2) {
            loadUrlFromArg = false;
            replaceUrlToArg = false;
        } else {
            return;
        }

        if (methodNum == -1) {
            loadUrlFromArg = sharedPreferences.getBoolean("load_url_from_arg", false);
            replaceUrlToArg = sharedPreferences.getBoolean("replace_url_to_arg", false);
        }
        Utils.utilsLog(this, String.format("start hook method, class name:%s, method name:%s, loadUrlFromArg:%s, replaceUrlToArg:%s, method num:%s", targetMethod.getDeclaringClass().getName(), targetMethod.getName(), loadUrlFromArg, replaceUrlToArg, methodNum));
        hook(targetMethod).intercept(new MyHooker(loadUrlFromArg, replaceUrlToArg));
        Utils.utilsLog(this, String.format("hook method num %s succ", methodNum));
    }

    public void prepareMethodToHook(String targetClassName, String targetMethodName, String targetMethodArgName, PackageReadyParam param, int methodNum) {
        Class<?> targetMethodArgClass = String.class;
        if (! targetMethodArgName.equals("String")) {
            if (targetMethodArgName.isEmpty()) {
                targetMethodArgClass = null;
            } else {
                try {
                    targetMethodArgClass = param.getClassLoader().loadClass(targetMethodArgName);
                } catch (ClassNotFoundException e) {
                    Utils.utilsLog(this, true, "method arg class not found:" + targetMethodArgName);
                    return;
                }
            }
        }

        Method targetMethod;
        try {
            Class<?> targetClass = Class.forName(targetClassName, true, param.getClassLoader());
            if (targetMethodArgClass == null) {
                targetMethod = targetClass.getDeclaredMethod(targetMethodName);
            } else {
                targetMethod = targetClass.getDeclaredMethod(targetMethodName, targetMethodArgClass);
            }
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            Utils.utilsLog(this, true, String.format("no such target method, error:%s, class:%s, method:%s, method arg class:%s", e, targetClassName, targetMethodName, (targetMethodArgClass == null) ? "null" : targetMethodArgClass.toString()));
            if (methodNum != -1) {
                positionMethod(param, methodNum);
            }
            return;
        }

        startHook(targetMethod, methodNum);
    }

    public void prepareHook(PackageReadyParam param) {
        sharedPreferences = getRemotePreferences("settings");
        Utils.setDebug(sharedPreferences.getBoolean("debug", false));
        showAllCard = sharedPreferences.getBoolean("show_all_cards", false);
        boolean superLandEnabled = sharedPreferences.getBoolean("super_land", false);

        try {
            Method attachMethod = Application.class.getDeclaredMethod("attach", Context.class);
            hook(attachMethod).intercept(chain -> {
                //获取可写入的prefs
                try {
                    Context context = (Context) chain.getArg(0);
                    writeSharedPreference = new RemotePreferences(
                            context, "com.zhizi42.diymiuicard.preference", "settings");
                } catch (Exception e) {
                    Utils.utilsLog(this, true, "hook context error:" + e);
                }
                return chain.proceed();
            });
            Utils.utilsLog(this, "hook context succ");
        } catch (NoSuchMethodException e) {
            Utils.utilsLog(this, String.format("hook context error:%s", e));
        }

        String imagesDirPath = Utils.getWalletPath(OSType) + "files/images";
        @SuppressLint("SdCardPath") File file = new File(imagesDirPath);
        if (!file.exists()) {
            file.mkdir();
        }


        String customClassName = sharedPreferences.getString("class", "");
        String customMethodName = sharedPreferences.getString("method", "");
        if ((! customClassName.isEmpty()) && (! customMethodName.isEmpty())) {
            String customMethodArgName = sharedPreferences.getString("method_arg", "");
            Utils.utilsLog(this, String.format("have custom hook point, class:%s, method:%s, method_arg:%s", customClassName, customMethodName, customMethodArgName));
            prepareMethodToHook(customClassName, customMethodName, customMethodArgName, param, -1);
        } else {
            if (OSType == 0 || OSType == 1) {
                String[] methodArray = readMethodFile("zhizi42.diycard.method.txt");
                if (methodArray.length == 2) {
                    prepareMethodToHook(methodArray[0], methodArray[1], "String", param, 0);
                } else {
                    Utils.utilsLog(this, true, "read method 0 error, pos method");
                    positionMethod(param, 0);
                }

                if (OSType == 0) {
                    methodArray = readMethodFile("zhizi42.diycard.method.1.txt");
                    if (methodArray.length == 2) {
                        prepareMethodToHook(methodArray[0], methodArray[1], "String", param, 1);
                    } else {
                        Utils.utilsLog(this, true, "read method 1 error, pos method");
                        positionMethod(param, 1);
                    }

                    if (superLandEnabled) {
                        String[] hyperSuperLandMethodArray = readMethodFile("zhizi42.diycard.HyperSuperLand.method.txt");
                        if (hyperSuperLandMethodArray.length == 2) {
                            prepareMethodToHook(hyperSuperLandMethodArray[0], hyperSuperLandMethodArray[1], "com.miui.tsmclient.entity.CardInfo", param, 10);
                        } else {
                            Utils.utilsLog(this, true, "read super land method error, pos method");
                            positionMethod(param, 10);
                        }
                    }
                }
            } else if (OSType == 2) {
                prepareMethodToHook("com.meizu.mznfcpay.model.BaseCardItem", "largeIconUrl", "", param, 0);
                prepareMethodToHook("com.meizu.mznfcpay.model.BaseCardItem", "smallIconUrl", "", param, 1);
            }
        }
    }

    public void positionMethod(PackageReadyParam param, int methodNum) {
        if (dexKitBridge == null) {
            ApplicationInfo appInfo = param.getApplicationInfo();
            String apkPath = appInfo.sourceDir;
            dexKitBridge = DexKitBridge.create(apkPath);
        }
        try {
            MethodDataList methodDataList;
            if (OSType == 0) {
                if (methodNum == 0) {
                    methodDataList = dexKitBridge.findClass(FindClass
                            .create()
                            .searchPackages("com.miui.tsmclient.util")
                            .matcher(
                                    ClassMatcher
                                            .create()
                                            .source("CustomGlideUrl.java")
                            )
                    ).findMethod(FindMethod
                            .create()
                            .matcher(MethodMatcher
                                    .create()
                                    .paramCount(1)
                                    .paramTypes(String.class)
                                    .modifiers(Modifier.PUBLIC | Modifier.STATIC)
                                    .returnType(Object.class)
                            )
                    );
                } else if (methodNum == 1) {
                    methodDataList = dexKitBridge.findClass(FindClass
                            .create()
                            .searchPackages("com.bumptech.glide")
                            .matcher(
                                    ClassMatcher.create()
                                            .source("RequestManager.java")
                            )
                    ).findMethod(FindMethod
                            .create()
                            .matcher(MethodMatcher
                                    .create()
                                    .paramCount(1)
                                    .paramTypes(String.class)
                                    .modifiers(Modifier.PUBLIC)
                                    .returnType(
                                            "com.bumptech.glide",
                                            StringMatchType.StartsWith
                                    )
                            )
                    );
                } else if (methodNum == 10) {//定位小米超级岛
                    methodDataList = dexKitBridge.findClass(FindClass
                            .create()
                            .searchPackages("com.miui.tsmclient.util")
                    ).findMethod(FindMethod.create()
                            .matcher(MethodMatcher.create()
                                    .paramCount(1)
                                    .paramTypes("com.miui.tsmclient.entity.CardInfo")
                                    .modifiers(Modifier.PRIVATE | Modifier.STATIC)
                                    .returnType(String.class)
                            )
                    );
                } else {
                    return;
                }
            } else if (OSType == 1) {
                methodDataList = dexKitBridge.findClass(FindClass
                                .create()
                                .matcher(
                        ClassMatcher
                                .create()
                                .className("com.finshell.finui.widget.imageview.CircleNetworkImageView")))
                        .findMethod(FindMethod
                                .create()
                                .matcher(MethodMatcher
                                        .create()
                                        .paramCount(1)
                                        .paramTypes(String.class)
                                        .modifiers(Modifier.PUBLIC)
                                        .returnType(ClassMatcher
                                                .create()
                                                .className(
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
                Utils.utilsLog(this, true, String.format("pos target method result num not 1, OS Type:%s, method num:%s, positioning target method num is:%s", OSType, methodNum, methodDataList.getSize()));
                for (MethodData method:methodDataList) {
                    Utils.utilsLog(this, true, String.format("class name:%s, method name:%s", method.getClassName(), method.getMethodName()));
                }
                return;
            }
            MethodData methodData = methodDataList.single();
            Method method = methodData.getMethodInstance(param.getClassLoader());
            DexMethod dexMethod = methodData.toDexMethod();
            Utils.utilsLog(this, String.format("position target method num %s succ, class name:%s, method name:%s", methodNum, dexMethod.getClassName(), dexMethod.getName()));
            updateTargetHookName(dexMethod.getClassName(), dexMethod.getName(), methodNum);
            startHook(method, methodNum);
        } catch (NoSuchMethodException | NoResultException e) {
            Utils.utilsLog(this, true, "positioning target method and hook still error, message:" + e);
        }
    }

    public String replaceUrl(String url) {
        updateCardUrlList(url);

        if (sharedPreferences.getBoolean("test_mode", false)) {
            Utils.utilsLog(this, "test mode enabled, replace url to test image");
            return "https://home.zhizi42.top:555/test_card.png";
        }

        String imageName = sharedPreferences.getString(url, "");//获取原卡面图片对应的diy卡面
        if (!imageName.isEmpty()) {//如果diy卡面不为空
            Utils.utilsLog(this, "load image's diy image name:" + imageName);
            String imageUrl;
            if (imageName.startsWith("https://") || imageName.startsWith("http://")) {
                imageUrl = imageName;//如果是链接就直接设置为结果
            } else {
                String imagePath = Utils.getWalletPath(OSType) + "files/images/" + imageName;//如果是文件名字就加上图片文件夹路径再设置为结果
                imageUrl = "file://" + imagePath;//如果是文件名字就加上file协议头和图片文件夹路径再设置为结果
                //如果开启debug，判断本地diy图片文件是否存在
                if (Utils.debug) {
                    File file = new File(imagePath);
                    if (file.exists()) {
                        Utils.utilsLog(this, "diy image file exist");
                    } else {
                        Utils.utilsLog(this, "diy image file not exist");
                    }
                }
            }
            return imageUrl;
        } else {
            Utils.utilsLog(this, "load image's not have diy image");
            return "";
        }
    }

    public void updateTargetHookName(String targetClassName, String targetMethodName, int methodNum) {
        String name;
        if (methodNum == 0) {
            name = "zhizi42.diycard.method.txt";
        } else if (methodNum == 1) {
            name = "zhizi42.diycard.method.1.txt";
        } else if (methodNum == 10) {
            name = "zhizi42.diycard.HyperSuperLand.method.txt";
        } else {
            return;
        }
        writeMethodFile(name, targetClassName, targetMethodName);
    }

    public void updateCardUrlList(String newCardUrl) {
        Utils.utilsLog(this, "load image's url:" + newCardUrl);//记录加载的图片链接到log

        if (OSType == 0) {
            if (! showAllCard) {
                if (newCardUrl.contains("w270h480") || newCardUrl.contains("/door-card-img/logo/") || newCardUrl.contains("/Mibi/")) {
                    Utils.utilsLog(this, "this card url is small icon url, not add to card url list");
                    return;//如果是小图标就不添加到数据
                }
            }
        }

        Set<String> blackSet = sharedPreferences.getStringSet("black_card_url_set", new HashSet<>());//获取黑名单列表
        if (! blackSet.contains(newCardUrl)) {//如果不在黑名单里就添加到所有卡片列表
            if (writeSharedPreference != null) {
                Set<String> cardUrlSet = new HashSet<>(writeSharedPreference.getStringSet("all_card_url_set", new HashSet<>()));
                if (cardUrlSet.contains(newCardUrl)) {
                    Utils.utilsLog(this, "this card url is already in card url list");
                    return;
                }
                cardUrlSet.add(newCardUrl);
                SharedPreferences.Editor editor = writeSharedPreference.edit();
                editor.putStringSet("all_card_url_set", cardUrlSet);
                editor.apply();
                Utils.utilsLog(this, "add card url to card url list succ");
            } else {
                Utils.utilsLog(this, "when update card url, write shared pref is null");
            }
        } else {
            Utils.utilsLog(this, "this card url is in black list, not add to card url list");
        }
    }
}