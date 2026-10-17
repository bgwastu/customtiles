package net.wastu.customtiles;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

public class DisplayHardware {
    private static final String TAG = "DisplayHardware";
    public static final String PREFS_NAME = "customtiles_prefs";
    public static final String KEY_LIGHT_COLOR_LEVEL = "light_color_level";
    public static final String KEY_PREV_BRIGHTNESS = "prev_brightness";
    public static final String KEY_PREV_BACKLIGHT = "prev_backlight";
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

    private static final String XIAOMI_NODE = "/sys/devices/virtual/mi_display/disp_feature/disp-DSI-0/disp_param";
    private static final String XIAOMI_BACKLIGHT_NODE = "/sys/devices/virtual/mi_display/disp_feature/disp-DSI-0/backlight";
    private static final String XIAOMI_MAX_NODE = "/sys/devices/virtual/mi_display/disp_feature/disp-DSI-0/factory_max_brightness";

    private static HbmType sDetectedType = null;
    private static String sDetectedNode = null;

    public static boolean isXiaomiDevice() {
        String m = Build.MANUFACTURER != null ? Build.MANUFACTURER.toLowerCase() : "";
        String b = Build.BRAND != null ? Build.BRAND.toLowerCase() : "";
        return m.contains("xiaomi") || b.contains("redmi") || b.contains("poco") || b.contains("xiaomi");
    }

    public static HbmType detectHbmType() {
        if (sDetectedType != null) {
            return sDetectedType;
        }

        if (isXiaomiDevice()) {
            sDetectedType = HbmType.XIAOMI_DISP_PARAM;
            sDetectedNode = XIAOMI_NODE;
            return sDetectedType;
        }

        // Test sysfs nodes via root shell to bypass untrusted_app SELinux restrictions
        String res = RootShell.exec(
            "if [ -e " + XIAOMI_NODE + " ]; then echo 'xiaomi';" +
            "elif [ -e /sys/class/drm/card0-DSI-1/hbm ]; then echo 'qcom_drm';" +
            "elif [ -e /sys/class/graphics/fb0/hbm ]; then echo 'qcom_fb0';" +
            "elif [ -e /sys/class/backlight/panel0-backlight/hbm_mode ]; then echo 'pixel';" +
            "elif [ -e /sys/kernel/oppo_display/hbm ]; then echo 'oplus';" +
            "else echo 'fallback'; fi"
        );

        if (res.contains("xiaomi")) {
            sDetectedType = HbmType.XIAOMI_DISP_PARAM;
            sDetectedNode = XIAOMI_NODE;
        } else if (res.contains("qcom_drm")) {
            sDetectedType = HbmType.QUALCOMM_DRM;
            sDetectedNode = "/sys/class/drm/card0-DSI-1/hbm";
        } else if (res.contains("qcom_fb0")) {
            sDetectedType = HbmType.QUALCOMM_FB0;
            sDetectedNode = "/sys/class/graphics/fb0/hbm";
        } else if (res.contains("pixel")) {
            sDetectedType = HbmType.GOOGLE_PIXEL;
            sDetectedNode = "/sys/class/backlight/panel0-backlight/hbm_mode";
        } else if (res.contains("oplus")) {
            sDetectedType = HbmType.OPPO_ONEPLUS;
            sDetectedNode = "/sys/kernel/oppo_display/hbm";
        } else {
            sDetectedType = HbmType.SYSTEM_FALLBACK;
            sDetectedNode = null;
        }

        return sDetectedType;
    }

    public static String getHardwareDescription() {
        HbmType type = detectHbmType();
        switch (type) {
            case XIAOMI_DISP_PARAM:
                return "Xiaomi Display Feature (disp_param & 10926 peak)";
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
        SharedPreferences sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return sp.getBoolean(KEY_HBM_ACTIVE, false);
    }

    public static void setHbmEnabled(Context context, boolean enable) {
        SharedPreferences sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        HbmType type = detectHbmType();

        if (enable) {
            // Save current brightness and auto brightness state
            try {
                int curBright = Settings.System.getInt(context.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, 200);
                int curAuto = Settings.System.getInt(context.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE, 0);
                sp.edit().putInt(KEY_PREV_BRIGHTNESS, curBright).putInt(KEY_PREV_AUTO, curAuto).apply();
            } catch (Throwable ignored) {}

            if (type == HbmType.XIAOMI_DISP_PARAM) {
                // Save current physical backlight before boosting
                String curBlStr = RootShell.exec("cat " + XIAOMI_BACKLIGHT_NODE + " 2>/dev/null");
                int curBl = 4000;
                try {
                    curBl = Integer.parseInt(curBlStr.trim());
                } catch (Throwable ignored) {}
                sp.edit().putInt(KEY_PREV_BACKLIGHT, curBl).putBoolean(KEY_HBM_ACTIVE, true).apply();

                StringBuilder sb = new StringBuilder();
                sb.append("echo '01 1' > ").append(XIAOMI_NODE).append("; ");
                sb.append("MAX=$(cat ").append(XIAOMI_MAX_NODE).append(" 2>/dev/null || echo 10926); ");
                sb.append("echo $MAX > ").append(XIAOMI_BACKLIGHT_NODE).append("; ");
                sb.append("settings put system screen_brightness_mode 0; ");
                sb.append("settings put system screen_brightness 255; ");
                sb.append("cmd display set-brightness 1.0; ");
                RootShell.exec(sb.toString());
            } else if (sDetectedNode != null) {
                sp.edit().putBoolean(KEY_HBM_ACTIVE, true).apply();
                StringBuilder sb = new StringBuilder();
                sb.append("echo 1 > ").append(sDetectedNode).append("; ");
                sb.append("settings put system screen_brightness_mode 0; ");
                sb.append("settings put system screen_brightness 255; ");
                sb.append("cmd display set-brightness 1.0; ");
                RootShell.exec(sb.toString());
            } else {
                sp.edit().putBoolean(KEY_HBM_ACTIVE, true).apply();
                RootShell.exec("settings put system screen_brightness_mode 0; settings put system screen_brightness 255; cmd display set-brightness 1.0;");
            }
        } else {
            int prevBright = sp.getInt(KEY_PREV_BRIGHTNESS, 128);
            int prevAuto = sp.getInt(KEY_PREV_AUTO, 0);
            int prevBl = sp.getInt(KEY_PREV_BACKLIGHT, 4000);
            sp.edit().putBoolean(KEY_HBM_ACTIVE, false).apply();
            float prevRatio = Math.max(0.01f, Math.min(1.0f, prevBright > 255 ? prevBright / 4095.0f : prevBright / 255.0f));

            if (type == HbmType.XIAOMI_DISP_PARAM) {
                StringBuilder sb = new StringBuilder();
                sb.append("echo '01 0' > ").append(XIAOMI_NODE).append("; ");
                sb.append("echo ").append(prevBl).append(" > ").append(XIAOMI_BACKLIGHT_NODE).append(" 2>/dev/null; ");
                sb.append("settings put system screen_brightness ").append(prevBright).append("; ");
                sb.append("settings put system screen_brightness_mode ").append(prevAuto).append("; ");
                sb.append("cmd display set-brightness ").append(prevRatio).append("; ");
                RootShell.exec(sb.toString());
            } else if (sDetectedNode != null) {
                StringBuilder sb = new StringBuilder();
                sb.append("echo 0 > ").append(sDetectedNode).append("; ");
                sb.append("settings put system screen_brightness ").append(prevBright).append("; ");
                sb.append("settings put system screen_brightness_mode ").append(prevAuto).append("; ");
                sb.append("cmd display set-brightness ").append(prevRatio).append("; ");
                RootShell.exec(sb.toString());
            } else {
                RootShell.exec("settings put system screen_brightness " + prevBright + "; settings put system screen_brightness_mode " + prevAuto + "; cmd display set-brightness " + prevRatio + ";");
            }
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
            applyDisplaySaturation(context, percent, true);
        }
    }

    public static void setLightColorEnabled(Context context, boolean enable) {
        SharedPreferences sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        sp.edit().putBoolean(KEY_LIGHT_COLOR_ACTIVE, enable).apply();

        if (enable) {
            int targetLevel = getTargetLightColorLevel(context);
            applyDisplaySaturation(context, targetLevel, true);
        } else {
            applyDisplaySaturation(context, 100, false);
        }
    }

    private static void applyDisplaySaturation(Context context, int levelPercent, boolean enable) {
        if (isXiaomiDevice()) {
            // Xiaomi HyperOS / MIUI Paper Mode (Light Color with 0% yellow warmth)
            StringBuilder sb = new StringBuilder();
            int colorType = (levelPercent == 0) ? 2 : 1; // 1: Light Color (淡彩), 2: Black & White (黑白)
            if (enable && levelPercent < 100) {
                sb.append("settings put system screen_mode_type 2; ");
                sb.append("settings put system screen_paper_texture_level 0; ");
                sb.append("settings put system screen_texture_color_type ").append(colorType).append("; ");
                sb.append("settings put system screen_texture_eyecare_level 0; ");
                sb.append("settings put system screen_paper_mode_enabled 1; ");
                sb.append("am broadcast -a com.miui.securitycore.action.EYEPROTECTION_MODE_CHANGE --user 0; ");
            } else {
                sb.append("settings put system screen_paper_mode_enabled 0; ");
                sb.append("am broadcast -a com.miui.securitycore.action.EYEPROTECTION_MODE_CHANGE --user 0; ");
            }
            RootShell.exec(sb.toString());
            Log.i(TAG, "Applied Xiaomi Light Color mode: enable=" + enable + " colorType=" + colorType);
        } else {
            // AOSP / Pixel / Generic SurfaceFlinger call
            String cmd = "service call color_display 24 i32 " + levelPercent;
            RootShell.exec(cmd);
            Log.i(TAG, "Applied AOSP display saturation: " + levelPercent + "% via " + cmd);
        }
    }
}
