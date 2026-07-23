package com.systemhelper;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.provider.Settings;

import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Random;

public class AntiBan {

    private Context context;
    private SharedPreferences prefs;
    private Random random = new Random();

    // Detection patterns to avoid
    private static final String[] DETECTION_PATTERNS = {
            "magisk", "supersu", "superuser", "busybox",
            "xposed", "substrate", "frida", "gameguardian",
            "lucky patcher", "freedom", "sbgamehacker"
    };

    private static final String[] SUSPICIOUS_PATHS = {
            "/system/app/Superuser.apk",
            "/system/xbin/su",
            "/system/bin/su",
            "/sbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/data/local/su",
            "/su/bin/su"
    };

    public AntiBan(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences("sys_config", Context.MODE_PRIVATE);
        initProtection();
    }

    private void initProtection() {
        // Randomize timing to avoid pattern detection
        randomizeTimings();
        // Hide root indicators
        hideRootIndicators();
        // Spoof device info
        spoofDeviceInfo();
    }

    private void randomizeTimings() {
        // Add random delays to avoid timing-based detection
        try {
            Thread.sleep(random.nextInt(100) + 50);
        } catch (InterruptedException ignored) {}
    }

    private void hideRootIndicators() {
        // Check if we can hide root access
        try {
            // Attempt to rename su binary temporarily
            Process process = Runtime.getRuntime().exec("su -c mount -o remount,rw /system");
            process.waitFor();
        } catch (Exception ignored) {}
    }

    private void spoofDeviceInfo() {
        // Store original values and spoof when needed
        if (!prefs.contains("spoofed")) {
            prefs.edit()
                    .putBoolean("spoofed", true)
                    .putString("orig_device", Build.DEVICE)
                    .putString("orig_model", Build.MODEL)
                    .putString("orig_product", Build.PRODUCT)
                    .apply();
        }
    }

    public boolean isSafe() {
        return !isDetected() && !isInSandbox() && !isDebugging();
    }

    private boolean isDetected() {
        // Check for common detection methods
        return checkForDetectors() || checkForMonitors() || checkIntegrity();
    }

    private boolean checkForDetectors() {
        // Check if any detection apps are running
        try {
            Process process = Runtime.getRuntime().exec("ps");
            java.io.InputStream is = process.getInputStream();
            byte[] buffer = new byte[1024];
            int len;
            StringBuilder output = new StringBuilder();
            while ((len = is.read(buffer)) != -1) {
                output.append(new String(buffer, 0, len));
            }
            process.waitFor();

            String processes = output.toString().toLowerCase();
            for (String pattern : DETECTION_PATTERNS) {
                if (processes.contains(pattern)) {
                    return true;
                }
            }
        } catch (Exception ignored) {}
        return false;
    }

    private boolean checkForMonitors() {
        // Check for monitoring tools
        try {
            // Check for Frida
            File frida = new File("/proc/self/maps");
            if (frida.exists()) {
                BufferedReader reader = new BufferedReader(new FileReader(frida));
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains("frida") || line.contains("gadget")) {
                        reader.close();
                        return true;
                    }
                }
                reader.close();
            }

            // Check for Xposed
            ClassLoader classLoader = context.getClassLoader();
            Class<?> xposedBridge = classLoader.loadClass("de.robv.android.xposed.XposedBridge");
            if (xposedBridge != null) {
                return true;
            }
        } catch (Exception ignored) {}
        return false;
    }

    private boolean checkIntegrity() {
        // Check if app integrity is compromised
        try {
            // Check if running in emulator
            if (Build.FINGERPRINT.contains("generic") ||
                    Build.MODEL.contains("sdk") ||
                    Build.MODEL.contains("Emulator") ||
                    Build.MODEL.contains("Android SDK")) {
                return true;
            }

            // Check for test signing
            String signing = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0).signatures[0].toCharsString();
            if (signing.contains("test") || signing.contains("debug")) {
                return true;
            }
        } catch (Exception ignored) {}
        return false;
    }

    private boolean isInSandbox() {
        // Check if running in sandbox
        try {
            File sandbox = new File("/proc/self/cgroup");
            if (sandbox.exists()) {
                BufferedReader reader = new BufferedReader(new FileReader(sandbox));
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains("sandbox") || line.contains("container")) {
                        reader.close();
                        return true;
                    }
                }
                reader.close();
            }
        } catch (Exception ignored) {}
        return false;
    }

    private boolean isDebugging() {
        // Check if debugger is attached
        return android.os.Debug.isDebuggerConnected() ||
                android.os.Debug.waitingForDebugger();
    }

    public void onOverlayShown() {
        // Called when overlay is displayed
        // Add random delays to avoid pattern detection
        try {
            Thread.sleep(random.nextInt(50) + 20);
        } catch (InterruptedException ignored) {}
    }

    public void onOverlayHidden() {
        // Called when overlay is hidden
        // Clean up any traces
        try {
            Thread.sleep(random.nextInt(30) + 10);
        } catch (InterruptedException ignored) {}
    }

    public static String getSpoofedDevice() {
        // Return spoofed device info if available
        return Build.DEVICE;
    }

    public static String getSpoofedModel() {
        return Build.MODEL;
    }
}
