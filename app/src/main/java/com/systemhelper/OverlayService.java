package com.systemhelper;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

public class OverlayService extends Service {

    private WindowManager windowManager;
    private View floatingView;
    private View menuView;
    private boolean isMenuVisible = false;
    private Handler handler;
    private LinearLayout playerListContainer;
    private AntiBan antiBan;

    private static final String CHANNEL_ID = "SystemService";

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler(Looper.getMainLooper());
        antiBan = new AntiBan(this);
        createNotificationChannel();
        startForeground(1, createNotification());
        createFloatingButton();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "System Service", NotificationManager.IMPORTANCE_LOW);
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    private Notification createNotification() {
        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(this);
        }
        return builder
                .setContentTitle("System Helper")
                .setContentText("Sistem servisi çalışıyor")
                .setSmallIcon(android.R.drawable.ic_menu_manage)
                .build();
    }

    private void createFloatingButton() {
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        int layoutType;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            layoutType = WindowManager.LayoutParams.TYPE_PHONE;
        }

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 0;
        params.y = 100;

        floatingView = LayoutInflater.from(this).inflate(R.layout.floating_button, null);
        windowManager.addView(floatingView, params);

        floatingView.setOnTouchListener(new View.OnTouchListener() {
            private int initialX, initialY;
            private float initialTouchX, initialTouchY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = params.x;
                        initialY = params.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        params.x = initialX + (int) (event.getRawX() - initialTouchX);
                        params.y = initialY + (int) (event.getRawY() - initialTouchY);
                        windowManager.updateViewLayout(floatingView, params);
                        return true;
                    case MotionEvent.ACTION_UP:
                        if (Math.abs(event.getRawX() - initialTouchX) < 10 &&
                                Math.abs(event.getRawY() - initialTouchY) < 10) {
                            toggleMenu();
                        }
                        return true;
                }
                return false;
            }
        });
    }

    private void toggleMenu() {
        if (isMenuVisible) {
            hideMenu();
        } else {
            showMenu();
        }
    }

    private void showMenu() {
        if (menuView != null) {
            windowManager.removeView(menuView);
        }

        int layoutType;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            layoutType = WindowManager.LayoutParams.TYPE_PHONE;
        }

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.CENTER;

        menuView = LayoutInflater.from(this).inflate(R.layout.overlay_menu, null);
        playerListContainer = menuView.findViewById(R.id.playerListContainer);

        menuView.findViewById(R.id.closeButton).setOnClickListener(v -> hideMenu());

        windowManager.addView(menuView, params);
        isMenuVisible = true;

        startUpdating();
    }

    private void hideMenu() {
        if (menuView != null) {
            windowManager.removeView(menuView);
            menuView = null;
        }
        isMenuVisible = false;
    }

    private void startUpdating() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isMenuVisible) {
                    if (antiBan.isSafe()) {
                        updatePlayerList();
                    }
                    handler.postDelayed(this, 100);
                }
            }
        }, 100);
    }

    private void updatePlayerList() {
        if (!NativeHelper.isGameRunning()) {
            playerListContainer.removeAllViews();
            TextView noGame = new TextView(this);
            noGame.setText("Uygulama çalışmıyor");
            noGame.setTextColor(Color.WHITE);
            noGame.setTextSize(16);
            noGame.setGravity(Gravity.CENTER);
            playerListContainer.addView(noGame);
            return;
        }

        playerListContainer.removeAllViews();
        NativeHelper.PlayerInfo[] players = NativeHelper.getPlayerList();

        if (players == null || players.length == 0) {
            TextView noPlayers = new TextView(this);
            noPlayers.setText("Veri bulunamadı");
            noPlayers.setTextColor(Color.WHITE);
            noPlayers.setTextSize(14);
            playerListContainer.addView(noPlayers);
            return;
        }

        for (NativeHelper.PlayerInfo player : players) {
            addPlayerToMenu(player);
        }
    }

    private void addPlayerToMenu(NativeHelper.PlayerInfo player) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(16, 8, 16, 8);

        TextView roleText = new TextView(this);
        roleText.setText(getRoleName(player.role));
        roleText.setTextSize(12);
        roleText.setTextColor(getRoleColor(player.role));
        roleText.setGravity(Gravity.CENTER);

        TextView nameText = new TextView(this);
        nameText.setText(player.name);
        nameText.setTextSize(16);
        nameText.setTextColor(Color.WHITE);
        nameText.setGravity(Gravity.CENTER);

        TextView colorText = new TextView(this);
        colorText.setText("Renk: " + player.colorName);
        colorText.setTextSize(12);
        colorText.setTextColor(Color.GRAY);
        colorText.setGravity(Gravity.CENTER);

        row.addView(roleText);
        row.addView(nameText);
        row.addView(colorText);

        playerListContainer.addView(row);

        View divider = new View(this);
        divider.setBackgroundColor(Color.parseColor("#33FFFFFF"));
        LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1);
        dividerParams.setMargins(0, 4, 0, 4);
        playerListContainer.addView(divider, dividerParams);
    }

    private String getRoleName(int role) {
        switch (role) {
            case 0: return "Crewmate";
            case 1: return "Impostor";
            case 2: return "Scientist";
            case 3: return "Engineer";
            case 4: return "Guardian Angel";
            case 5: return "Shapeshifter";
            case 6: return "Crewmate Ghost";
            case 7: return "Impostor Ghost";
            case 8: return "Noisemaker";
            case 9: return "Phantom";
            case 10: return "Tracker";
            case 12: return "Detective";
            case 18: return "Viper";
            default: return "Bilinmeyen";
        }
    }

    private int getRoleColor(int role) {
        switch (role) {
            case 1:
            case 5:
            case 7:
            case 9:
            case 18:
                return Color.RED;
            default:
                return Color.parseColor("#4FC3F7");
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (floatingView != null) windowManager.removeView(floatingView);
        if (menuView != null) windowManager.removeView(menuView);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
