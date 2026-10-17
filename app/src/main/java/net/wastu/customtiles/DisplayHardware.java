package net.wastu.customtiles;

import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

public class DisplayHardware {
    private static final String TAG = "DisplayHardware";
    public static final String PREFS_NAME = "customtiles_prefs";
    public static final String KEY_LIGHT_COLOR_LEVEL = "light_color_level";
    public static final String KEY_PREV_BRIGHTNESS = "prev_brightness";
    public static final String KEY_PREV_AUTO = "prev_auto";
    public static final String KEY_HBM_ACTIVE = "hbm_active";
    public static final String KEY_LIGHT_COLOR_ACTIVE = "light_color_active";

    public static final int DEFAULT_LIGHT_COLOR_LEVEL = 50;

    public enum HbmType {
        XIAOMI_DISP_PARAM,
        QUALCOMM_DRM,
        QUALCOMM_FB0,
        GOOGLE_PIXEL,
        OPPO_ONEPLUS,
        SYSTEM_FALLBACK
    }

    private static final String[] XIAOMI_NODES = {
        "/sys/devices/virtual/mi_display/disp_feature/disp-DSI-0/disp_param",
        "/sys/devices/virtual/mi_display/disp_feature/disp-DSI-1/disp_param"
    };

    private static final String[] QUALCOMM_DRM_NODES = {
        "/sys/class/drm/card0-DSI-1/hbm",
        "/sys/devices/platform/soc/soc:qcom,dsi-display-primary/hbm"
    };

    private static final String[] QUALCOMM_FB0_NODES = {
        "/sys/class/graphics/fb0/hbm"
    };

    private static final String[] PIXEL_NODES = {
        "/sys/class/backlight/panel0-backlight/hbm_mode"
    };

    private static final String[] OPPO_NODES = {
        "/sys/kernel/oppo_display/hbm"
    };

    public static HbmType detectHbmType() {
        for (String node : XIAOMI_NODES) {
            if (new File(node).exists()) return HbmType.XIAOMI_DISP_PARAM;
        }
        for (String node : QUALCOMM_DRM_NODES) {
            if (new File(node).exists()) return HbmType.QUALCOMM_DRM;
        }
        for (String node : QUALCOMM_FB0_NODES) {
            if (new File(node).exists()) return HbmType.QUALCOMM_FB0;
        }
        for (String node : PIXEL_NODES) {
            if (new File(node).exists()) return HbmType.GOOGLE_PIXEL;
        }
        for (String node : OPPO_NODES) {
            if (new File(node).exists()) return HbmType.OPPO_ONEPLUS;
        }
        return HbmType.SYSTEM_FALLBACK;
    }

    public static String getDetectedNodePath() {
        HbmType type = detectHbmType();
        switch (type) {
            case XIAOMI_DISP_PARAM:
                for (String node : XIAOMI_NODES) if (new File(node).exists()) return node;
                break;
            case QUALCOMM_DRM:
                for (String node : QUALCOMM_DRM_NODES) if (new File(node).exists()) return node;
                break;
            case QUALCOMM_FB0:
                for (String node : QUALCOMM_FB0_NODES) if (new File(node).exists()) return node;
                break;
            case GOOGLE_PIXEL:
                for (String node : PIXEL_NODES) if (new File(node).exists()) return node;
                break;
            case OPPO_ONEPLUS:
                for (String node : OPPO_NODES) if (new File(node).exists()) return node;
                break;
            default:
                break;
        }
        return null;
    }

    public static String getHardwareDescription() {
        HbmType type = detectHbmType();
        switch (type) {
            case XIAOMI_DISP_PARAM:
                return "Xiaomi Display Feature (disp_param)";
            case QUALCOMM_DRM:
                return "Qualcomm DRM Driver (card0-DSI)";
            case QUALCOMM_FB0:
                return "Qualcomm Framebuffer (fb0/hbm)";
            case GOOGLE_PIXEL:
                return "Google Pixel Display (panel0-backlight)";
            case OPPO_ONEPLUS:
                return "OPlus Display Driver (oppo_display/hbm)";
            default:
                return "Universal System Max Brightness";
        }
    }

    // --- HBM Methods ---

    public static boolean isHbmActive(Context context) {
        HbmType type = detectHbmType();
        String node = getDetectedNodePath();

        if (type == HbmType.XIAOMI_DISP_PARAM && node != null) {
            try (BufferedReader reader = new BufferedReader(new FileReader(node))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains("hbm[01]:")) {
                        String[] parts = line.split(":");
                        if (parts.length > 1) {
                            return "1".equals(parts[1].trim());
                        }
                    }
                }
            } catch (Throwable ignored) {}
        } else if (node != null) {
            try (BufferedReader reader = new BufferedReader(new FileReader(node))) {
                String line = reader.readLine();
                if (line != null) {
                    return "1".equals(line.trim());
                }
            } catch (Throwable ignored) {}
        }

        SharedPreferences sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return sp.getBoolean(KEY_HBM_ACTIVE, false);
    }

    public static void setHbmEnabled(Context context, boolean enable) {
        SharedPreferences sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        HbmType type = detectHbmType();
        String node = getDetectedNodePath();

        if (enable) {
            // Save current brightness and auto brightness state
            try {
                int curBright = Settings.System.getInt(context.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, 200);
                int curAuto = Settings.System.getInt(context.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE, 0);
                sp.edit().putInt(KEY_PREV_BRIGHTNESS, curBright).putInt(KEY_PREV_AUTO, curAuto).putBoolean(KEY_HBM_ACTIVE, true).apply();
            } catch (Throwable ignored) {}

            StringBuilder sb = new StringBuilder();
            sb.append("settings put system screen_brightness_mode 0; ");
            sb.append("settings put system screen_brightness 4095; ");

            if (type == HbmType.XIAOMI_DISP_PARAM && node != null) {
                sb.append("echo '01 1' > ").append(node).append("; ");
                sb.append("echo '23 4095' > ").append(node).append(" 2>/dev/null; ");
                sb.append("echo '24 4095' > ").append(node).append(" 2>/dev/null; ");
            } else if (node != null) {
                sb.append("echo 1 > ").append(node).append("; ");
            }

            RootShell.exec(sb.toString());
        } else {
            int prevBright = sp.getInt(KEY_PREV_BRIGHTNESS, 200);
            int prevAuto = sp.getInt(KEY_PREV_AUTO, 0);
            sp.edit().putBoolean(KEY_HBM_ACTIVE, false).apply();

            StringBuilder sb = new StringBuilder();
            if (type == HbmType.XIAOMI_DISP_PARAM && node != null) {
                sb.append("echo '01 0' > ").append(node).append("; ");
            } else if (node != null) {
                sb.append("echo 0 > ").append(node).append("; ");
            }
            sb.append("settings put system screen_brightness ").append(prevBright).append("; ");
            sb.append("settings put system screen_brightness_mode ").append(prevAuto).append("; ");

            RootShell.exec(sb.toString());
        }
    }

    // --- Light Color (Saturation) Methods ---

    public static boolean isLightColorActive(Context context) {
        SharedPreferences sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return sp.getBoolean(KEY_LIGHT_COLOR_ACTIVE, false);
    }

    public static int getTargetLightColorLevel(Context context) {
        SharedPreferences sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return sp.getInt(KEY_LIGHT_COLOR_LEVEL, DEFAULT_LIGHT_COLOR_LEVEL);
    }

    public static void setTargetLightColorLevel(Context context, int percent) {
        SharedPreferences sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        sp.edit().putInt(KEY_LIGHT_COLOR_LEVEL, percent).apply();
        if (isLightColorActive(context)) {
            applyDisplaySaturation(percent);
        }
    }

    public static void setLightColorEnabled(Context context, boolean enable) {
        SharedPreferences sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        sp.edit().putBoolean(KEY_LIGHT_COLOR_ACTIVE, enable).apply();

        if (enable) {
            int targetLevel = getTargetLightColorLevel(context);
            applyDisplaySaturation(targetLevel);
        } else {
            applyDisplaySaturation(100);
        }
    }

    private static void applyDisplaySaturation(int levelPercent) {
        // Universal Android SurfaceFlinger/ColorDisplayService call:
        // IColorDisplayManager TRANSACTION_setSaturationLevel = 24
        String cmd = "service call color_display 24 i32 " + levelPercent;
        RootShell.exec(cmd);
        Log.i(TAG, "Applied display saturation: " + levelPercent + "% via " + cmd);
    }
}
