package com.systemhelper;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends Activity {

    private static final int OVERLAY_PERMISSION_CODE = 1001;
    private TextView statusText;
    private Button startButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statusText = findViewById(R.id.statusText);
        startButton = findViewById(R.id.startButton);

        startButton.setOnClickListener(v -> {
            if (!Settings.canDrawOverlays(this)) {
                requestOverlayPermission();
            } else if (!isRootAvailable()) {
                statusText.setText("Root erişimi bulunamadı!");
                Toast.makeText(this, "Cihaz rootlu değil!", Toast.LENGTH_LONG).show();
            } else {
                startOverlay();
            }
        });

        checkRoot();
    }

    private void checkRoot() {
        if (isRootAvailable()) {
            statusText.setText("Root erişimi: ✓\nOverlay izni: " +
                    (Settings.canDrawOverlays(this) ? "✓" : "✗"));
        } else {
            statusText.setText("Root erişimi: ✗\nCihaz rootlu değil!");
        }
    }

    private boolean isRootAvailable() {
        try {
            Process process = Runtime.getRuntime().exec("su -c id");
            process.waitFor();
            return process.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private void requestOverlayPermission() {
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName()));
        startActivityForResult(intent, OVERLAY_PERMISSION_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == OVERLAY_PERMISSION_CODE) {
            checkRoot();
            if (Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "Overlay izni verildi!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void startOverlay() {
        try {
            injectLibrary();
            Intent serviceIntent = new Intent(this, OverlayService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
            statusText.setText("Servis çalışıyor...");
            startButton.setEnabled(false);
            startButton.setText("Çalışıyor");
        } catch (Exception e) {
            statusText.setText("Hata: " + e.getMessage());
            Toast.makeText(this, "Başlatma başarısız: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void injectLibrary() throws IOException, InterruptedException {
        String libPath = getApplicationInfo().nativeLibraryDir + "/libhelper.so";
        String packageName = "com.innersloth.spacemafia";
        int pid = getPid(packageName);

        if (pid == -1) {
            throw new IOException("Uygulama çalışmıyor! Önce uygulamayı başlatın.");
        }

        Process process = Runtime.getRuntime().exec("su");
        java.io.OutputStream os = process.getOutputStream();
        os.write(("chmod 755 " + libPath + "\n").getBytes());
        os.write(("cat " + libPath + " > /proc/" + pid + "/mem\n").getBytes());
        os.write(("kill -9 " + pid + "\n").getBytes());
        os.flush();
        os.close();
        process.waitFor();
    }

    private int getPid(String packageName) {
        try {
            Process process = Runtime.getRuntime().exec("pidof " + packageName);
            java.io.InputStream is = process.getInputStream();
            byte[] buffer = new byte[1024];
            int len = is.read(buffer);
            process.waitFor();
            if (len > 0) {
                String pidStr = new String(buffer, 0, len).trim();
                return Integer.parseInt(pidStr);
            }
        } catch (Exception ignored) {}
        return -1;
    }
}
