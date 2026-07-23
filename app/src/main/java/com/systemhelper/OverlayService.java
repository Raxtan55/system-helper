package com.systemhelper;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class OverlayService extends Service {

    private WindowManager wm;
    private View fabView, menuView;
    private boolean menuOpen = false;
    private Handler handler;
    private LinearLayout listContainer;
    private AntiBan antiBan;

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler(Looper.getMainLooper());
        antiBan = new AntiBan(this);
        startForeground(1, buildNotif());
        createFab();
    }

    private Notification buildNotif() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel("voyage", "VoyageLoader", NotificationManager.IMPORTANCE_LOW);
            ch.setDescription("VoyageLoader servisi");
            getSystemService(NotificationManager.class).createNotificationChannel(ch);
            return new Notification.Builder(this, "voyage")
                    .setContentTitle("VoyageLoader")
                    .setContentText("Aktif")
                    .setSmallIcon(android.R.drawable.ic_menu_info_details)
                    .build();
        }
        return new Notification.Builder(this)
                .setContentTitle("VoyageLoader")
                .setContentText("Aktif")
                .setSmallIcon(android.R.drawable.ic_menu_info_details)
                .build();
    }

    private void createFab() {
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        int type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ?
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY :
                WindowManager.LayoutParams.TYPE_PHONE;

        WindowManager.LayoutParams p = new WindowManager.LayoutParams(150, 150, type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT);
        p.gravity = Gravity.TOP | Gravity.END;
        p.x = 32;
        p.y = 280;

        FrameLayout fab = new FrameLayout(this);

        // Outer glow
        View glow = new View(this);
        GradientDrawable glowBg = new GradientDrawable();
        glowBg.setShape(GradientDrawable.OVAL);
        glowBg.setColor(Color.parseColor("#204FC3F7"));
        glow.setBackground(glowBg);
        fab.addView(glow, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        // Inner circle
        LinearLayout inner = new LinearLayout(this);
        inner.setGravity(Gravity.CENTER);
        GradientDrawable innerBg = new GradientDrawable();
        innerBg.setShape(GradientDrawable.OVAL);
        innerBg.setColor(Color.parseColor("#1A1A2E"));
        innerBg.setStroke(3, Color.parseColor("#4FC3F7"));
        inner.setBackground(innerBg);

        TextView icon = new TextView(this);
        icon.setText("V");
        icon.setTextColor(Color.parseColor("#4FC3F7"));
        icon.setTextSize(24);
        icon.setTypeface(Typeface.DEFAULT_BOLD);
        inner.addView(icon);

        FrameLayout.LayoutParams innerLP = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
        innerLP.setMargins(12, 12, 12, 12);
        fab.addView(inner, innerLP);

        fabView = fab;
        wm.addView(fabView, p);

        final float[] tx = new float[1];
        final float[] ty = new float[1];
        final int[] sx = new int[1];
        final int[] sy = new int[1];
        final boolean[] moved = new boolean[1];

        fabView.setOnTouchListener((v, ev) -> {
            switch (ev.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    tx[0] = ev.getRawX(); ty[0] = ev.getRawY();
                    sx[0] = p.x; sy[0] = p.y;
                    moved[0] = false;
                    return true;
                case MotionEvent.ACTION_MOVE:
                    float dx = ev.getRawX() - tx[0];
                    float dy = ev.getRawY() - ty[0];
                    if (Math.abs(dx) > 10 || Math.abs(dy) > 10) moved[0] = true;
                    p.x = sx[0] - (int) dx;
                    p.y = sy[0] + (int) dy;
                    wm.updateViewLayout(fabView, p);
                    return true;
                case MotionEvent.ACTION_UP:
                    if (!moved[0]) toggleMenu();
                    return true;
            }
            return false;
        });
    }

    private void toggleMenu() {
        if (menuOpen) closeMenu();
        else openMenu();
    }

    private void openMenu() {
        if (menuView != null) wm.removeView(menuView);

        int type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ?
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY :
                WindowManager.LayoutParams.TYPE_PHONE;

        WindowManager.LayoutParams p = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                type, WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL, PixelFormat.TRANSLUCENT);
        p.gravity = Gravity.CENTER;

        FrameLayout frame = new FrameLayout(this);

        // Background dim
        View dim = new View(this);
        dim.setBackgroundColor(Color.parseColor("#80000000"));
        frame.addView(dim, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        // Card
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(0, 0, 0, 0);

        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setCornerRadius(32);
        cardBg.setColor(Color.parseColor("#161625"));
        cardBg.setStroke(1, Color.parseColor("#252540"));
        card.setBackground(cardBg);
        card.setElevation(24);

        FrameLayout.LayoutParams cardLP = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        cardLP.setMargins(48, 48, 48, 48);
        cardLP.gravity = Gravity.CENTER;

        // Header
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(48, 40, 48, 32);

        // Header gradient bg
        GradientDrawable headerBg = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{Color.parseColor("#1A1A3E"), Color.parseColor("#161625")});
        headerBg.setCornerRadii(new float[]{32, 32, 32, 32, 0, 0, 0, 0});
        header.setBackground(headerBg);

        LinearLayout headerTexts = new LinearLayout(this);
        headerTexts.setOrientation(LinearLayout.VERTICAL);
        headerTexts.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView title = new TextView(this);
        title.setText("VoyageLoader");
        title.setTextColor(Color.WHITE);
        title.setTextSize(20);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setLetterSpacing(0.03f);

        TextView sub = new TextView(this);
        sub.setText("ESP Panel • v1.0");
        sub.setTextColor(Color.parseColor("#64748B"));
        sub.setTextSize(12);
        sub.setLetterSpacing(0.08f);

        headerTexts.addView(title);
        headerTexts.addView(sub);

        // Close button
        FrameLayout closeWrap = new FrameLayout(this);
        TextView close = new TextView(this);
        close.setText("✕");
        close.setTextColor(Color.parseColor("#64748B"));
        close.setTextSize(18);
        close.setGravity(Gravity.CENTER);
        close.setPadding(20, 20, 20, 20);
        close.setOnClickListener(v -> closeMenu());

        GradientDrawable closeBg = new GradientDrawable();
        closeBg.setShape(GradientDrawable.OVAL);
        closeBg.setColor(Color.parseColor("#1E293B"));
        close.setBackground(closeBg);

        closeWrap.addView(close, new FrameLayout.LayoutParams(72, 72));

        header.addView(headerTexts);
        header.addView(closeWrap);
        card.addView(header);

        // Divider
        View div = new View(this);
        div.setBackgroundColor(Color.parseColor("#1E293B"));
        card.addView(div, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 2));

        // ESP Label
        LinearLayout labelWrap = new LinearLayout(this);
        labelWrap.setPadding(48, 24, 48, 8);

        TextView espLabel = new TextView(this);
        espLabel.setText("OYUNCU LİSTESİ");
        espLabel.setTextColor(Color.parseColor("#4FC3F7"));
        espLabel.setTextSize(11);
        espLabel.setLetterSpacing(0.15f);
        espLabel.setTypeface(Typeface.DEFAULT_BOLD);
        labelWrap.addView(espLabel);
        card.addView(labelWrap);

        // Player list
        ScrollView scroll = new ScrollView(this);
        scroll.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 550));

        listContainer = new LinearLayout(this);
        listContainer.setOrientation(LinearLayout.VERTICAL);
        listContainer.setPadding(32, 16, 32, 32);
        scroll.addView(listContainer);
        card.addView(scroll);

        frame.addView(card, cardLP);
        menuView = frame;
        wm.addView(menuView, p);
        menuOpen = true;

        startUpdating();
    }

    private void closeMenu() {
        if (menuView != null) { wm.removeView(menuView); menuView = null; }
        menuOpen = false;
    }

    private void startUpdating() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (menuOpen && antiBan.isSafe()) {
                    updateList();
                    handler.postDelayed(this, 200);
                }
            }
        }, 200);
    }

    private void updateList() {
        listContainer.removeAllViews();

        if (!NativeHelper.isGameRunning()) {
            addEmpty("Oyun çalışmıyor");
            return;
        }

        NativeHelper.PlayerInfo[] players = NativeHelper.getPlayerList();
        if (players == null || players.length == 0) {
            addEmpty("Oyuncu bulunamadı");
            return;
        }

        for (NativeHelper.PlayerInfo pl : players) addPlayer(pl);
    }

    private void addEmpty(String msg) {
        TextView tv = new TextView(this);
        tv.setText(msg);
        tv.setTextColor(Color.parseColor("#475569"));
        tv.setTextSize(14);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(0, 64, 0, 64);
        listContainer.addView(tv);
    }

    private void addPlayer(NativeHelper.PlayerInfo p) {
        boolean imp = isImp(p.role);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(28, 20, 28, 20);

        GradientDrawable rowBg = new GradientDrawable();
        rowBg.setCornerRadius(16);
        rowBg.setColor(Color.parseColor("#1E1E2E"));
        row.setBackground(rowBg);

        LinearLayout.LayoutParams rowLP = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        rowLP.bottomMargin = 12;

        // Role indicator
        View dot = new View(this);
        GradientDrawable dotBg = new GradientDrawable();
        dotBg.setShape(GradientDrawable.OVAL);
        dotBg.setColor(imp ? Color.parseColor("#EF4444") : Color.parseColor("#10B981"));
        dot.setBackground(dotBg);
        LinearLayout.LayoutParams dotLP = new LinearLayout.LayoutParams(20, 20);
        dotLP.rightMargin = 24;
        row.addView(dot, dotLP);

        // Info
        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        info.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView roleTv = new TextView(this);
        roleTv.setText(getRole(p.role));
        roleTv.setTextSize(10);
        roleTv.setTextColor(imp ? Color.parseColor("#EF4444") : Color.parseColor("#10B981"));
        roleTv.setLetterSpacing(0.12f);
        roleTv.setTypeface(Typeface.DEFAULT_BOLD);

        TextView nameTv = new TextView(this);
        nameTv.setText(p.name);
        nameTv.setTextSize(15);
        nameTv.setTextColor(Color.WHITE);
        nameTv.setTypeface(Typeface.DEFAULT_BOLD);
        nameTv.setPadding(0, 4, 0, 2);

        TextView colorTv = new TextView(this);
        colorTv.setText(p.colorName);
        colorTv.setTextSize(12);
        colorTv.setTextColor(Color.parseColor("#64748B"));

        info.addView(roleTv);
        info.addView(nameTv);
        info.addView(colorTv);
        row.addView(info);

        // Dead badge
        if (p.isDead) {
            TextView dead = new TextView(this);
            dead.setText("ÖLÜ");
            dead.setTextSize(10);
            dead.setTextColor(Color.parseColor("#EF4444"));
            dead.setTypeface(Typeface.DEFAULT_BOLD);
            dead.setLetterSpacing(0.1f);
            row.addView(dead);
        }

        listContainer.addView(row, rowLP);
    }

    private boolean isImp(int r) {
        return r == 1 || r == 5 || r == 7 || r == 9 || r == 18;
    }

    private String getRole(int r) {
        switch (r) {
            case 0: return "CREWMATE";
            case 1: return "IMPOSTOR";
            case 2: return "SCIENTIST";
            case 3: return "ENGINEER";
            case 4: return "GUARDIAN ANGEL";
            case 5: return "SHAPESHIFTER";
            case 6: return "CREWMATE GHOST";
            case 7: return "IMPOSTOR GHOST";
            case 8: return "NOISEMAKER";
            case 9: return "PHANTOM";
            case 10: return "TRACKER";
            case 12: return "DETECTIVE";
            case 18: return "VIPER";
            default: return "UNKNOWN";
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (fabView != null) wm.removeView(fabView);
        if (menuView != null) wm.removeView(menuView);
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}
