package com.systemhelper;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private static final int OVERLAY_PERMISSION_CODE = 1001;
    private TextView statusText, rootStatus, overlayStatus, gameStatus;
    private Button startButton;
    private LinearLayout logContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        createPremiumUI();
        checkAll();
    }

    private void createPremiumUI() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#FAFAFA"));
        root.setPadding(0, 0, 0, 0);

        // Header
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setBackgroundColor(Color.WHITE);
        header.setPadding(48, 40, 48, 40);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setElevation(8);

        LinearLayout titleLayout = new LinearLayout(this);
        titleLayout.setOrientation(LinearLayout.VERTICAL);

        TextView title = new TextView(this);
        title.setText("VoyageLoader");
        title.setTextColor(Color.parseColor("#1A1A2E"));
        title.setTextSize(28);
        title.setTypeface(null, 1);

        TextView version = new TextView(this);
        version.setText("Version 1.0");
        version.setTextColor(Color.parseColor("#9E9E9E"));
        version.setTextSize(14);

        titleLayout.addView(title);
        titleLayout.addView(version);
        header.addView(titleLayout);

        root.addView(header);

        // Content
        ScrollView scrollView = new ScrollView(this);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(48, 48, 48, 48);

        // Status Cards
        rootStatus = createStatusCard("Root Erişimi", "Kontrol ediliyor...");
        overlayStatus = createStatusCard("Overlay İzin", "Kontrol ediliyor...");
        gameStatus = createStatusCard("Oyun Durumu", "Kontrol ediliyor...");

        content.addView(rootStatus);
        content.addView(createSpacer(16));
        content.addView(overlayStatus);
        content.addView(createSpacer(16));
        content.addView(gameStatus);
        content.addView(createSpacer(32));

        // Status text
        statusText = new TextView(this);
        statusText.setText("Hazır");
        statusText.setTextColor(Color.parseColor("#4CAF50"));
        statusText.setTextSize(16);
        statusText.setGravity(Gravity.CENTER);
        statusText.setPadding(0, 16, 0, 16);
        content.addView(statusText);
        content.addView(createSpacer(16));

        // Start Button
        startButton = new Button(this);
        startButton.setText("Servisi Başlat");
        startButton.setBackgroundColor(Color.parseColor("#1A1A2E"));
        startButton.setTextColor(Color.WHITE);
        startButton.setTextSize(16);
        startButton.setPadding(48, 32, 48, 32);
        startButton.setOnClickListener(v -> handleStart());
        content.addView(startButton);
        content.addView(createSpacer(32));

        // Log area
        TextView logTitle = new TextView(this);
        logTitle.setText("Log");
        logTitle.setTextColor(Color.parseColor("#757575"));
        logTitle.setTextSize(12);
        content.addView(logTitle);
        content.addView(createSpacer(8));

        logContainer = new LinearLayout(this);
        logContainer.setOrientation(LinearLayout.VERTICAL);
        logContainer.setBackgroundColor(Color.WHITE);
        logContainer.setPadding(24, 24, 24, 24);
        content.addView(logContainer);

        scrollView.addView(content);
        root.addView(scrollView);

        setContentView(root);
    }

    private LinearLayout createStatusCard(String label, String value) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundColor(Color.WHITE);
        card.setPadding(32, 24, 32, 24);
        card.setElevation(4);

        TextView labelText = new TextView(this);
        labelText.setText(label);
        labelText.setTextColor(Color.parseColor("#9E9E9E"));
        labelText.setTextSize(12);

        TextView valueText = new TextView(this);
        valueText.setText(value);
        valueText.setTextColor(Color.parseColor("#1A1A2E"));
        valueText.setTextSize(16);
        valueText.setTypeface(null, 1);

        card.addView(labelText);
        card.addView(valueText);
        return card;
    }

    private View createSpacer(int height) {
        View spacer = new View(this);
        spacer.setLayoutParams(new LinearLayout.LayoutParams(1, height));
        return spacer;
    }

    private void addLog(String msg) {
        TextView log = new TextView(this);
        log.setText("• " + msg);
        log.setTextColor(Color.parseColor("#616161"));
        log.setTextSize(13);
        log.setPadding(0, 8, 0, 8);
        logContainer.addView(log);
    }

    private void checkAll() {
        boolean root = isRootAvailable();
        boolean overlay = Settings.canDrawOverlays(this);
        boolean game = isGameRunning();

        updateStatusCard(rootStatus, root ? "Mevcut ✓" : "Yok ✗", root);
        updateStatusCard(overlayStatus, overlay ? "Verilmiş ✓" : "Verilmemiş ✗", overlay);
        updateStatusCard(gameStatus, game ? "Çalışıyor ✓" : "Kapalı ✗", game);

        if (root && overlay && game) {
            statusText.setText("Hazır - Başlatılabilir");
            statusText.setTextColor(Color.parseColor("#4CAF50"));
            startButton.setEnabled(true);
        } else if (!root) {
            statusText.setText("Root gerekli");
            statusText.setTextColor(Color.parseColor("#F44336"));
            startButton.setEnabled(false);
        } else if (!overlay) {
            statusText.setText("Overlay izni gerekli");
            statusText.setTextColor(Color.parseColor("#FF9800"));
            startButton.setEnabled(false);
        } else {
            statusText.setText("Oyun bekleniyor...");
            statusText.setTextColor(Color.parseColor("#FF9800"));
            startButton.setEnabled(false);
        }
    }

    private void updateStatusCard(LinearLayout card, String value, boolean ok) {
        TextView tv = (TextView) card.getChildAt(1);
        tv.setText(value);
        tv.setTextColor(ok ? Color.parseColor("#4CAF50") : Color.parseColor("#F44336"));
    }

    private boolean isRootAvailable() {
        try {
            Process p = Runtime.getRuntime().exec("su -c id");
            p.waitFor();
            return p.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isGameRunning() {
        try {
            Process p = Runtime.getRuntime().exec("pidof com.innersloth.spacemafia");
            java.io.InputStream is = p.getInputStream();
            byte[] buf = new byte[128];
            int len = is.read(buf);
            p.waitFor();
            return len > 0 && new String(buf, 0, len).trim().length() > 0;
        } catch (Exception e) {
            return false;
        }
    }

    private int getGamePid() {
        try {
            Process p = Runtime.getRuntime().exec("pidof com.innersloth.spacemafia");
            java.io.InputStream is = p.getInputStream();
            byte[] buf = new byte[128];
            int len = is.read(buf);
            p.waitFor();
            if (len > 0) {
                String s = new String(buf, 0, len).trim();
                if (!s.isEmpty()) return Integer.parseInt(s.split("\\s+")[0]);
            }
        } catch (Exception ignored) {}
        return -1;
    }

    private void handleStart() {
        if (!Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, OVERLAY_PERMISSION_CODE);
            return;
        }

        if (!isRootAvailable()) {
            Toast.makeText(this, "Root gerekli!", Toast.LENGTH_LONG).show();
            return;
        }

        if (!isGameRunning()) {
            Toast.makeText(this, "Önce Among Us'u açın!", Toast.LENGTH_LONG).show();
            addLog("Oyun açık değil, Among Us'u başlatın");
            // Periyodik kontrol başlat
            startGameCheck();
            return;
        }

        startService();
    }

    private void startGameCheck() {
        new Thread(() -> {
            for (int i = 0; i < 30; i++) {
                try { Thread.sleep(2000); } catch (Exception ignored) {}
                if (isGameRunning()) {
                    runOnUiThread(() -> {
                        addLog("Oyun algılandı!");
                        checkAll();
                        startService();
                    });
                    return;
                }
            }
            runOnUiThread(() -> addLog("Oyun zaman aşımı - tekrar deneyin"));
        }).start();
    }

    private void startService() {
        try {
            int pid = getGamePid();
            if (pid == -1) {
                addLog("PID alınamadı");
                return;
            }

            addLog("Oyun PID: " + pid);
            injectLibrary(pid);

            Intent serviceIntent = new Intent(this, OverlayService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }

            addLog("Servis başlatıldı");
            statusText.setText("Çalışıyor");
            statusText.setTextColor(Color.parseColor("#4CAF50"));
            startButton.setText("Çalışıyor");
            startButton.setEnabled(false);
            startButton.setBackgroundColor(Color.parseColor("#9E9E9E"));

        } catch (Exception e) {
            addLog("Hata: " + e.getMessage());
            Toast.makeText(this, "Hata: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void injectLibrary(int pid) throws Exception {
        String libPath = getApplicationInfo().nativeLibraryDir + "/libhelper.so";
        addLog("Kütüphane: " + libPath);

        Process p = Runtime.getRuntime().exec("su");
        java.io.OutputStream os = p.getOutputStream();

        // Kütüphaneyi hedef process'in maps'ine enjekte et
        os.write(("echo 'Enjeksiyon basliyor...'\n").getBytes());
        os.write(("ls -la " + libPath + "\n").getBytes());
        os.write(("cp " + libPath + " /data/local/tmp/libhelper.so\n").getBytes());
        os.write(("chmod 755 /data/local/tmp/libhelper.so\n").getBytes());

        // LD_PRELOAD ile enjeksiyon
        os.write(("echo '/data/local/tmp/libhelper.so' >> /proc/" + pid + "/maps\n").getBytes());

        os.flush();
        os.close();

        int exit = p.waitFor();
        addLog("Enjeksiyon exit: " + exit);
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkAll();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == OVERLAY_PERMISSION_CODE) {
            checkAll();
        }
    }
}
