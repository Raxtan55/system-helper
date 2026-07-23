package com.systemhelper;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
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
            NotificationChannel ch = new NotificationChannel("vh", "Voyage", NotificationManager.IMPORTANCE_LOW);
            getSystemService(NotificationManager.class).createNotificationChannel(ch);
            return new Notification.Builder(this, "vh")
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

        WindowManager.LayoutParams p = new WindowManager.LayoutParams(140, 140, type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT);
        p.gravity = Gravity.TOP | Gravity.END;
        p.x = 24;
        p.y = 200;

        // Premium FAB
        LinearLayout fab = new LinearLayout(this);
        fab.setOrientation(LinearLayout.VERTICAL);
        fab.setGravity(Gravity.CENTER);

        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        bg.setColor(Color.parseColor("#1A1A2E"));
        bg.setStroke(3, Color.parseColor("#4FC3F7"));
        fab.setBackground(bg);
        fab.setElevation(12);

        TextView icon = new TextView(this);
        icon.setText("V");
        icon.setTextColor(Color.parseColor("#4FC3F7"));
        icon.setTextSize(22);
        icon.setTypeface(null, 1);
        icon.setGravity(Gravity.CENTER);
        fab.addView(icon);

        fabView = fab;
        wm.addView(fabView, p);

        // Drag + Click
        final float[] touchX = new float[1];
        final float[] touchY = new float[1];
        final int[] startX = new int[1];
        final int[] startY = new int[1];
        final boolean[] moved = new boolean[1];

        fabView.setOnTouchListener((v, ev) -> {
            switch (ev.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    touchX[0] = ev.getRawX();
                    touchY[0] = ev.getRawY();
                    startX[0] = p.x;
                    startY[0] = p.y;
                    moved[0] = false;
                    return true;
                case MotionEvent.ACTION_MOVE:
                    float dx = ev.getRawX() - touchX[0];
                    float dy = ev.getRawY() - touchY[0];
                    if (Math.abs(dx) > 10 || Math.abs(dy) > 10) moved[0] = true;
                    p.x = startX[0] - (int) dx;
                    p.y = startY[0] + (int) dy;
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

        // Premium Menu
        LinearLayout menu = new LinearLayout(this);
        menu.setOrientation(LinearLayout.VERTICAL);
        menu.setPadding(48, 40, 48, 40);

        GradientDrawable menuBg = new GradientDrawable();
        menuBg.setCornerRadius(24);
        menuBg.setColor(Color.parseColor("#FFFFFF"));
        menu.setBackground(menuBg);
        menu.setElevation(16);

        // Header
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = new TextView(this);
        title.setText("VoyageLoader");
        title.setTextColor(Color.parseColor("#1A1A2E"));
        title.setTextSize(20);
        title.setTypeface(null, 1);

        TextView ver = new TextView(this);
        ver.setText("v1.0");
        ver.setTextColor(Color.parseColor("#9E9E9E"));
        ver.setTextSize(12);
        ver.setPadding(16, 0, 0, 0);

        TextView close = new TextView(this);
        close.setText("✕");
        close.setTextColor(Color.parseColor("#F44336"));
        close.setTextSize(20);
        close.setPadding(32, 16, 16, 16);
        close.setOnClickListener(v -> closeMenu());

        header.addView(title);
        header.addView(ver);
        header.addView(close, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        menu.addView(header);

        // ESP Label
        TextView espLabel = new TextView(this);
        espLabel.setText("ESP - Oyuncu Listesi");
        espLabel.setTextColor(Color.parseColor("#4FC3F7"));
        espLabel.setTextSize(14);
        espLabel.setPadding(0, 24, 0, 16);
        menu.addView(espLabel);

        // Divider
        View divider = new View(this);
        divider.setBackgroundColor(Color.parseColor("#E0E0E0"));
        menu.addView(divider, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 2));

        // Player list
        ScrollView scroll = new ScrollView(this);
        scroll.setMaxHeight(600);

        listContainer = new LinearLayout(this);
        listContainer.setOrientation(LinearLayout.VERTICAL);
        listContainer.setPadding(0, 16, 0, 16);

        scroll.addView(listContainer);
        menu.addView(scroll);

        menuView = menu;
        wm.addView(menuView, p);
        menuOpen = true;

        startUpdating();
    }

    private void closeMenu() {
        if (menuView != null) {
            wm.removeView(menuView);
            menuView = null;
        }
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
            addEmptyState("Oyun çalışmıyor");
            return;
        }

        NativeHelper.PlayerInfo[] players = NativeHelper.getPlayerList();
        if (players == null || players.length == 0) {
            addEmptyState("Oyuncu bulunamadı");
            return;
        }

        for (NativeHelper.PlayerInfo p : players) {
            addPlayer(p);
        }
    }

    private void addEmptyState(String msg) {
        TextView tv = new TextView(this);
        tv.setText(msg);
        tv.setTextColor(Color.parseColor("#9E9E9E"));
        tv.setTextSize(14);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(0, 48, 0, 48);
        listContainer.addView(tv);
    }

    private void addPlayer(NativeHelper.PlayerInfo p) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, 16, 0, 16);

        // Role indicator
        LinearLayout indicator = new LinearLayout(this);
        GradientDrawable indBg = new GradientDrawable();
        indBg.setShape(GradientDrawable.OVAL);
        indBg.setColor(isImpostor(p.role) ? Color.parseColor("#F44336") : Color.parseColor("#4CAF50"));
        indicator.setBackground(indBg);
        indicator.setLayoutParams(new LinearLayout.LayoutParams(24, 24));

        // Info
        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        info.setPadding(24, 0, 0, 0);

        TextView role = new TextView(this);
        role.setText(getRoleName(p.role));
        role.setTextSize(11);
        role.setTextColor(isImpostor(p.role) ? Color.parseColor("#F44336") : Color.parseColor("#4CAF50"));

        TextView name = new TextView(this);
        name.setText(p.name);
        name.setTextSize(15);
        name.setTextColor(Color.parseColor("#1A1A2E"));
        name.setTypeface(null, 1);

        TextView color = new TextView(this);
        color.setText(p.colorName);
        color.setTextSize(12);
        color.setTextColor(Color.parseColor("#757575"));

        info.addView(role);
        info.addView(name);
        info.addView(color);

        row.addView(indicator);
        row.addView(info);

        listContainer.addView(row);

        // Divider
        View div = new View(this);
        div.setBackgroundColor(Color.parseColor("#F5F5F5"));
        listContainer.addView(div, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1));
    }

    private boolean isImpostor(int role) {
        return role == 1 || role == 5 || role == 7 || role == 9 || role == 18;
    }

    private String getRoleName(int r) {
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
    public IBinder onBind(Intent intent) {
        return null;
    }
}
