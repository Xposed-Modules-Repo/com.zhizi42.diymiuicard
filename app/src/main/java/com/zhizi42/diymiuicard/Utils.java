package com.zhizi42.diymiuicard;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

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

    public static void clearCache(Activity activity) {
        new AlertDialog.Builder(activity)
                .setTitle(R.string.settings_clear_cache_dialog_title)
                .setMessage(R.string.settings_clear_cache_dialog_text)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.confirm, (dialog, which) -> {
                    Glide.get(activity).clearMemory();
                    new Thread(() -> {
                        int OSType = checkOSType(activity);
                        Glide.get(activity).clearDiskCache();
                        List<String> commandList = new ArrayList<>();
                        commandList.add("rm -rf /data/data/com.zhizi42.diymiuicard/cache/*");
                        if (OSType == 0) {
                            commandList.add("am force-stop com.miui.tsmclient");
                            commandList.add("rm -rf /data/data/com.miui.tsmclient/cache/image_manager_disk_cache/*");
                        } else if (OSType == 1) {
                            commandList.add("am force-stop com.finshell.wallet");
                        }
                        boolean succ = Utils.executeShell(commandList);
                        activity.runOnUiThread(() -> {
                            if (succ) {
                                Toast.makeText(activity,
                                                R.string.clear_card_cache_succ,
                                                Toast.LENGTH_SHORT)
                                        .show();
                            } else {
                                Toast.makeText(activity,
                                                R.string.clear_card_cache_fail,
                                                Toast.LENGTH_LONG)
                                        .show();
                            }
                        });
                    }).start();
                })
                .show();
    }

    public static void debugLog(Hook hook, String s) {
        if (debug) {
            hook.log("[DIY NFC Card] " + s);
        }
    }

    public static int checkOSType(Context context) {
        try {
            context.getPackageManager().getPackageInfo("com.miui.tsmclient", PackageManager.GET_META_DATA);
            return 0;
        } catch (PackageManager.NameNotFoundException e) {
            try {
                context.getPackageManager().getPackageInfo("com.finshell.wallet", PackageManager.GET_META_DATA);
                return 1;
            } catch (PackageManager.NameNotFoundException ex) {
                return -1;
            }
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