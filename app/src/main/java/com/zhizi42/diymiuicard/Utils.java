package com.zhizi42.diymiuicard;

import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.PreferenceManager;

import com.bumptech.glide.Glide;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class Utils {

    public static boolean debug = BuildConfig.DEBUG;

    public static void setDebug(boolean debug0) {
        if (BuildConfig.DEBUG) {
            debug = true;
        } else {
            debug = debug0;
        }
    }

    public static boolean executeShell(List<String> cmd) {
        try {
            Process process = Runtime.getRuntime().exec("su --mount-master");//加上--mount-master才能读写/data/data下其它应用
            BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(
                            process.getOutputStream(), StandardCharsets.UTF_8));
            if (debug) {
                writer.write("echo execute command start\n");
                String commandAll = "";
                for (String s:cmd) {
                    commandAll += s + "\n";
                }
                Log.e("execute command info", "echo will execute command:" + commandAll);
            }
            for (String s:cmd) {
                writer.write(s + "\n");
            }
            if (debug) {
                writer.write("echo end\n");
            }
            writer.write("exit\n");
            writer.flush();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            //log记录输出
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                Log.i("RootCommandOutput", line);
            }
            BufferedReader stdError = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            while ((line = stdError.readLine()) != null) {
                Log.e("RootCommandError", line);
            }

            int code = process.waitFor();
            bufferedReader.close();
            stdError.close();
            return code == 0;
        } catch (IOException | InterruptedException e) {
            Log.e("execute error", e.toString());
            return false;
        }
    }

    public static void clearCache(Context context) {
        new AlertDialog.Builder(context)
                .setTitle(R.string.settings_clear_cache_dialog_title)
                .setMessage(R.string.settings_clear_cache_dialog_text)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.confirm, (dialog, which) -> {
                    Glide.get(context).clearMemory();
                    Glide.get(context).clearDiskCache();
                    Toast.makeText(context,
                                    R.string.clear_card_cache_succ,
                                    Toast.LENGTH_SHORT)
                            .show();
                })
                .show();
    }
    public static void clearHookCache(Context context) {
        new AlertDialog.Builder(context)
                .setTitle(R.string.settings_clear_hook_cache_dialog_title)
                .setMessage(R.string.settings_clear_hook_cache_dialog_text)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.confirm, (dialog, which) -> {
                    int OSType = checkOSType(context);
                    List<String> commandList = new ArrayList<>();
                    if (OSType == 0) {
                        commandList.add("am force-stop com.miui.tsmclient");
                        commandList.add("rm -rf /data/data/com.miui.tsmclient/files/zhizi42.diycard.method.txt");
                        commandList.add("rm -rf /data/data/com.miui.tsmclient/files/zhizi42.diycard.method.1.txt");
                        if (PreferenceManager
                                .getDefaultSharedPreferences(context)
                                .getBoolean("super_land", false)) {
                            commandList.add("rm -rf /data/data/com.miui.tsmclient/files/zhizi42.diycard.HyperSuperLand.method.txt");
                        }
                    } else if (OSType == 1) {
                        commandList.add("am force-stop com.finshell.wallet");
                        commandList.add("rm -rf /data/data/com.finshell.wallet/files/zhizi42.diycard.method.txt");
                    }
                    boolean succ = Utils.executeShell(commandList);
                    if (succ) {
                        Toast.makeText(context,
                                        R.string.clear_card_cache_succ,
                                        Toast.LENGTH_SHORT)
                                .show();
                    }
                })
                .show();
    }

    public static void utilsLog(Hook hook, boolean error, String s) {
        if (error || debug) {
            hook.log(error ? Log.ERROR : Log.DEBUG, "DIY NFC Card", s);
        }
    }

    public static void utilsLog(Hook hook, String s) {
        utilsLog(hook, false, s);
    }

    public static int checkOSType(Context context) {
        try {
            context.getPackageManager().getPackageInfo("com.miui.tsmclient", PackageManager.GET_META_DATA);
            return 0;
        } catch (PackageManager.NameNotFoundException e) {
            try {
                context.getPackageManager().getPackageInfo("com.finshell.wallet", PackageManager.GET_META_DATA);
                return 1;
            } catch (PackageManager.NameNotFoundException e1) {
                return -1;
            }
        }
    }

    public static String getWalletPath(int OSType) {
        if (OSType == 0) {
            return "/data/data/com.miui.tsmclient/";
        } else if (OSType == 1) {
            return "/data/data/com.finshell.wallet/";
        } else if (OSType == 2) {
            return "/data/data/com.meizu.mznfcpay/";
        } else {
            return "";
        }
    }

    public static void showNoCardApp(Context context) {
        new AlertDialog.Builder(context)
                .setTitle(R.string.no_card_app_installed_title)
                .setMessage(R.string.no_card_app_installed_content)
                .setPositiveButton(R.string.confirm, null)
                .show();
    }
}