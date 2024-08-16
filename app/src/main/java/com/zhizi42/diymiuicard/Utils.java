package com.zhizi42.diymiuicard;

import android.util.Log;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

public class Utils {
    static boolean executeShell(List<String> cmd) {
        try {
            Process process = Runtime.getRuntime().exec("su --mount-master");//加上--mount-master才能读写/data/data下其它应用
            DataOutputStream os = new DataOutputStream(process.getOutputStream());
            os.writeBytes("echo start\n");
            for (String s:cmd) {
                os.writeBytes(s + "\n");
            }
            os.writeBytes("echo end\n");
            os.writeBytes("exit\n");
            os.flush();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            //log记录输出
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                Log.e("RootCommandOutput", line);
            }
            BufferedReader stdError = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            while ((line = stdError.readLine()) != null) {
                Log.e("RootCommandError", line);
            }

            int code = process.waitFor();
            bufferedReader.close();
            return code == 0;
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
