package com.zhizi42.diymiuicard;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.bumptech.glide.Glide;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class Utils {

    public static SharedPreferences sharedPreferences;

    public static void initSetting(SharedPreferences sharedPreferences0) {
        sharedPreferences = sharedPreferences0;
    }

    public static boolean executeShell(List<String> cmd) {
        boolean debug = sharedPreferences.getBoolean("debug", false);
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
                        Glide.get(activity).clearDiskCache();
                        List<String> commandList = new ArrayList<>();
                        commandList.add("rm -rf /data/data/com.zhizi42.diymiuicard/cache/*");
                        commandList.add("am force-stop com.miui.tsmclient");
                        commandList.add("rm -rf /data/data/com.miui.tsmclient/cache/image_manager_disk_cache/*");
                        boolean succ = Utils.executeShell(commandList);
                        activity.runOnUiThread(() -> {
                            if (succ) {
                                Toast.makeText(
                                                activity,
                                                "清除缓存成功",
                                                Toast.LENGTH_SHORT)
                                        .show();
                            } else {
                                Toast.makeText(
                                                activity,
                                                "清除缓存失败，请手动清除",
                                                Toast.LENGTH_LONG)
                                        .show();
                            }
                        });
                    }).start();
                })
                .show();
    }
}