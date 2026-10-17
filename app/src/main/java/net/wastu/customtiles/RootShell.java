package net.wastu.customtiles;

import android.util.Log;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;

public class RootShell {
    private static final String TAG = "RootShell";
    private static Boolean sIsRooted = null;

    public static boolean isRootAvailable() {
        if (sIsRooted != null) {
            return sIsRooted;
        }
        try {
            Process p = Runtime.getRuntime().exec(new String[]{"su", "-c", "id"});
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line = reader.readLine();
                sIsRooted = (line != null && line.contains("uid=0"));
                p.waitFor();
                return sIsRooted;
            }
        } catch (Throwable t) {
            sIsRooted = false;
            return false;
        }
    }

    public static String exec(String command) {
        StringBuilder output = new StringBuilder();
        try {
            Process p = Runtime.getRuntime().exec("su");
            try (DataOutputStream os = new DataOutputStream(p.getOutputStream());
                 BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                os.writeBytes(command + "\n");
                os.writeBytes("exit\n");
                os.flush();

                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
                p.waitFor();
            }
        } catch (Throwable t) {
            Log.e(TAG, "exec error: " + t.getMessage(), t);
        }
        return output.toString().trim();
    }

    public static boolean execBool(String command) {
        try {
            Process p = Runtime.getRuntime().exec("su");
            try (DataOutputStream os = new DataOutputStream(p.getOutputStream())) {
                os.writeBytes(command + "\n");
                os.writeBytes("exit\n");
                os.flush();
                return p.waitFor() == 0;
            }
        } catch (Throwable t) {
            return false;
        }
    }
}
