package com.systemhelper;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

public class MainActivity extends Activity {

    private TextView statusText, rootVal, overlayVal, gameVal, titleText, subtitleText;
    private LinearLayout rootCard, overlayCard, gameCard;
    private Button startBtn;
    private LinearLayout logBox;
    private ProgressBar progress;
    private View pulseView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(Color.parseColor("#0D0D1A"));
            getWindow().setNavigationBarColor(Color.parseColor("#0D0D1A"));
        }
        buildUI();
        autoRequestOverlay();
    }

    private void autoRequestOverlay() {
        if (!Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, 1001);
        }
        new Handler().postDelayed(this::checkAll, 500);
    }

    private void buildUI() {
        FrameLayout root = new FrameLayout(this);

        // Gradient background
        View bg = new View(this);
        GradientDrawable gradBg = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{Color.parseColor("#0D0D1A"), Color.parseColor("#1A1A3E"), Color.parseColor("#0D0D1A")});
        bg.setBackground(gradBg);
        root.addView(bg, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        // Animated accent circles
        pulseView = new View(this);
        GradientDrawable pulse = new GradientDrawable();
        pulse.setShape(GradientDrawable.OVAL);
        pulse.setColor(Color.parseColor("#154FC3F7"));
        pulseView.setBackground(pulse);
        FrameLayout.LayoutParams pulseP = new FrameLayout.LayoutParams(600, 600);
        pulseP.gravity = Gravity.CENTER;
        pulseP.topMargin = -200;
        root.addView(pulseView, pulseP);

        animatePulse();

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);

        LinearLayout main = new LinearLayout(this);
        main.setOrientation(LinearLayout.VERTICAL);
        main.setGravity(Gravity.CENTER_HORIZONTAL);

        // === HEADER ===
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setGravity(Gravity.CENTER_HORIZONTAL);
        header.setPadding(0, 120, 0, 60);

        // App icon circle
        FrameLayout iconWrap = new FrameLayout(this);
        LinearLayout iconCircle = new LinearLayout(this);
        iconCircle.setGravity(Gravity.CENTER);
        GradientDrawable iconBg = new GradientDrawable();
        iconBg.setShape(GradientDrawable.OVAL);
        iconBg.setColor(Color.parseColor("#1A4FC3F7"));
        iconBg.setStroke(3, Color.parseColor("#4FC3F7"));
        iconCircle.setBackground(iconBg);
        iconCircle.setLayoutParams(new FrameLayout.LayoutParams(120, 120));

        TextView iconText = new TextView(this);
        iconText.setText("V");
        iconText.setTextColor(Color.parseColor("#4FC3F7"));
        iconText.setTextSize(36);
        iconText.setTypeface(Typeface.DEFAULT_BOLD);
        iconCircle.addView(iconText);
        iconWrap.addView(iconCircle);

        LinearLayout.LayoutParams iconLP = new LinearLayout.LayoutParams(120, 120);
        iconLP.bottomMargin = 32;
        header.addView(iconWrap, iconLP);

        titleText = new TextView(this);
        titleText.setText("VoyageLoader");
        titleText.setTextColor(Color.WHITE);
        titleText.setTextSize(32);
        titleText.setTypeface(Typeface.DEFAULT_BOLD);
        titleText.setLetterSpacing(0.05f);
        header.addView(titleText);

        subtitleText = new TextView(this);
        subtitleText.setText("Version 1.0 • Premium Edition");
        subtitleText.setTextColor(Color.parseColor("#64748B"));
        subtitleText.setTextSize(13);
        subtitleText.setLetterSpacing(0.1f);
        subtitleText.setPadding(0, 8, 0, 0);
        header.addView(subtitleText);

        main.addView(header);

        // === STATUS CARDS ===
        LinearLayout cardsWrap = new LinearLayout(this);
        cardsWrap.setOrientation(LinearLayout.VERTICAL);
        cardsWrap.setPadding(48, 0, 48, 32);

        rootCard = makeCard("Root Erişimi", "Kontrol ediliyor...", "#10B981", "#94A3B8");
        overlayCard = makeCard("Overlay İzin", "Kontrol ediliyor...", "#3B82F6", "#94A3B8");
        gameCard = makeCard("Oyun Durumu", "Kontrol ediliyor...", "#8B5CF6", "#94A3B8");

        cardsWrap.addView(rootCard);
        cardsWrap.addView(spacer(16));
        cardsWrap.addView(overlayCard);
        cardsWrap.addView(spacer(16));
        cardsWrap.addView(gameCard);

        main.addView(cardsWrap);

        // === STATUS TEXT ===
        statusText = new TextView(this);
        statusText.setText("Kontrol ediliyor...");
        statusText.setTextColor(Color.parseColor("#94A3B8"));
        statusText.setTextSize(15);
        statusText.setGravity(Gravity.CENTER);
        statusText.setPadding(0, 32, 0, 8);
        main.addView(statusText);

        // === PROGRESS ===
        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setIndeterminate(true);
        LinearLayout.LayoutParams progLP = new LinearLayout.LayoutParams(400, 8);
        progLP.gravity = Gravity.CENTER;
        progLP.topMargin = 16;
        main.addView(progress, progLP);

        // === START BUTTON ===
        FrameLayout btnWrap = new FrameLayout(this);
        btnWrap.setPadding(48, 48, 48, 24);

        startBtn = new Button(this);
        startBtn.setText("  Servisi Başlat");
        startBtn.setTextColor(Color.WHITE);
        startBtn.setTextSize(16);
        startBtn.setTypeface(Typeface.DEFAULT_BOLD);
        startBtn.setAllCaps(false);
        startBtn.setLetterSpacing(0.05f);
        startBtn.setPadding(48, 36, 48, 36);

        GradientDrawable btnBg = new GradientDrawable();
        btnBg.setCornerRadius(20);
        btnBg.setColor(Color.parseColor("#4FC3F7"));
        btnBg.setStroke(0, Color.TRANSPARENT);
        startBtn.setBackground(btnBg);
        startBtn.setElevation(12);
        startBtn.setEnabled(false);
        startBtn.setAlpha(0.5f);
        startBtn.setOnClickListener(v -> handleStart());

        btnWrap.addView(startBtn);
        main.addView(btnWrap);

        // === LOG ===
        LinearLayout logWrap = new LinearLayout(this);
        logWrap.setOrientation(LinearLayout.VERTICAL);
        logWrap.setPadding(48, 24, 48, 48);

        TextView logLabel = new TextView(this);
        logLabel.setText("LOG");
        logLabel.setTextColor(Color.parseColor("#475569"));
        logLabel.setTextSize(11);
        logLabel.setLetterSpacing(0.15f);
        logLabel.setTypeface(Typeface.DEFAULT_BOLD);
        logWrap.addView(logLabel);
        logWrap.addView(spacer(12));

        logBox = new LinearLayout(this);
        logBox.setOrientation(LinearLayout.VERTICAL);
        logBox.setPadding(28, 24, 28, 24);

        GradientDrawable logBg = new GradientDrawable();
        logBg.setCornerRadius(16);
        logBg.setColor(Color.parseColor("#1E1E2E"));
        logBox.setBackground(logBg);

        logWrap.addView(logBox);
        main.addView(logWrap);

        scroll.addView(main);
        root.addView(scroll);

        setContentView(root);

        // Entry animation
        startBtn.setScaleX(0.8f);
        startBtn.setScaleY(0.8f);
        startBtn.animate().scaleX(1f).scaleY(1f).setDuration(600).setStartDelay(800).setInterpolator(new DecelerateInterpolator()).start();
    }

    private LinearLayout makeCard(String label, String value, String accent, String labelColor) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(36, 28, 36, 28);

        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setCornerRadius(20);
        cardBg.setColor(Color.parseColor("#161625"));
        cardBg.setStroke(1, Color.parseColor("#252540"));
        card.setBackground(cardBg);
        card.setElevation(4);

        // Accent dot
        View dot = new View(this);
        GradientDrawable dotBg = new GradientDrawable();
        dotBg.setShape(GradientDrawable.OVAL);
        dotBg.setColor(Color.parseColor(accent));
        dot.setBackground(dotBg);
        LinearLayout.LayoutParams dotLP = new LinearLayout.LayoutParams(16, 16);
        dotLP.rightMargin = 24;
        card.addView(dot, dotLP);

        // Texts
        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        texts.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView labelTv = new TextView(this);
        labelTv.setText(label);
        labelTv.setTextColor(Color.parseColor(labelColor));
        labelTv.setTextSize(12);
        labelTv.setLetterSpacing(0.08f);

        TextView valueTv = new TextView(this);
        valueTv.setText(value);
        valueTv.setTextColor(Color.WHITE);
        valueTv.setTextSize(15);
        valueTv.setTypeface(Typeface.DEFAULT_BOLD);
        valueTv.setPadding(0, 4, 0, 0);

        texts.addView(labelTv);
        texts.addView(valueTv);
        card.addView(texts);

        // Arrow
        TextView arrow = new TextView(this);
        arrow.setText("›");
        arrow.setTextColor(Color.parseColor("#334155"));
        arrow.setTextSize(24);
        card.addView(arrow);

        return card;
    }

    private void updateCard(LinearLayout card, String value, boolean ok) {
        LinearLayout texts = (LinearLayout) card.getChildAt(1);
        TextView valTv = (TextView) texts.getChildAt(1);
        valTv.setText(value);
        valTv.setTextColor(ok ? Color.parseColor("#10B981") : Color.parseColor("#EF4444"));

        View dot = card.getChildAt(0);
        GradientDrawable dotBg = (GradientDrawable) dot.getBackground();
        dotBg.setColor(ok ? Color.parseColor("#10B981") : Color.parseColor("#EF4444"));
    }

    private void animatePulse() {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(pulseView, "scaleX", 1f, 1.5f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(pulseView, "scaleY", 1f, 1.5f, 1f);
        scaleX.setRepeatCount(ValueAnimator.INFINITE);
        scaleY.setRepeatCount(ValueAnimator.INFINITE);
        scaleX.setDuration(4000);
        scaleY.setDuration(4000);
        scaleX.start();
        scaleY.start();
    }

    private void addLog(String msg) {
        TextView tv = new TextView(this);
        tv.setText("› " + msg);
        tv.setTextColor(Color.parseColor("#94A3B8"));
        tv.setTextSize(13);
        tv.setPadding(0, 8, 0, 8);
        logBox.addView(tv);
    }

    private View spacer(int h) {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(1, h));
        return v;
    }

    private void checkAll() {
        boolean root = hasRoot();
        boolean overlay = Settings.canDrawOverlays(this);
        boolean game = isGameRunning();

        updateCard(rootCard, root ? "Erişilebilir" : "Bulunamadı", root);
        updateCard(overlayCard, overlay ? "Verilmiş" : "Verilmedi", overlay);
        updateCard(gameCard, game ? "Çalışıyor" : "Kapalı", game);

        progress.setVisibility(View.GONE);

        if (root && overlay) {
            statusText.setText("Hazır — Başlatılabilir");
            statusText.setTextColor(Color.parseColor("#10B981"));
            startBtn.setEnabled(true);
            startBtn.setAlpha(1f);
        } else if (!root) {
            statusText.setText("Root erişimi gerekli");
            statusText.setTextColor(Color.parseColor("#EF4444"));
        } else {
            statusText.setText("Overlay izni gerekli");
            statusText.setTextColor(Color.parseColor("#F59E0B"));
        }
    }

    private void waitForGame() {
        new Thread(() -> {
            for (int i = 0; i < 60; i++) {
                try { Thread.sleep(2000); } catch (Exception ignored) {}
                if (isGameRunning()) {
                    runOnUiThread(() -> {
                        addLog("Oyun algılandı");
                        checkAll();
                    });
                    return;
                }
            }
            runOnUiThread(() -> addLog("Zaman aşımı — tekrar deneyin"));
        }).start();
    }

    private boolean hasRoot() {
        try {
            Process p = Runtime.getRuntime().exec("su -c id");
            p.waitFor();
            return p.exitValue() == 0;
        } catch (Exception e) { return false; }
    }

    private boolean isGameRunning() {
        try {
            Process p = Runtime.getRuntime().exec("su -c pidof com.innersloth.spacemafia");
            byte[] buf = new byte[128];
            int len = p.getInputStream().read(buf);
            p.waitFor();
            return len > 0 && new String(buf, 0, len).trim().length() > 0;
        } catch (Exception e) { return false; }
    }

    private int getPid() {
        try {
            Process p = Runtime.getRuntime().exec("su -c pidof com.innersloth.spacemafia");
            byte[] buf = new byte[128];
            int len = p.getInputStream().read(buf);
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
            autoRequestOverlay();
            return;
        }
        if (!hasRoot()) {
            addLog("Root erişimi yok");
            return;
        }
        launch();
    }

    private void launch() {
        try {
            addLog("Servis başlatılıyor...");

            Intent svc = new Intent(this, OverlayService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(svc);
            } else {
                startService(svc);
            }

            addLog("Servis başlatıldı");
            statusText.setText("Aktif");
            statusText.setTextColor(Color.parseColor("#10B981"));
            startBtn.setText("  Aktif");
            startBtn.setEnabled(false);
            startBtn.setAlpha(0.5f);

        } catch (Exception e) {
            addLog("Hata: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        new Handler().postDelayed(this::checkAll, 300);
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        new Handler().postDelayed(this::checkAll, 300);
    }
}
