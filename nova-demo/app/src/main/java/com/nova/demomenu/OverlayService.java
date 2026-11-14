package com.nova.demomenu;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * User-consented Android overlay. Does not inject into, inspect, hook or modify games.
 * All controls operate only on this app and its own overlay windows.
 */
public class OverlayService extends Service {
    public static final String ACTION_STOP = "com.nova.demomenu.STOP_OVERLAY";
    private static final String CHANNEL_ID = "nova_overlay";
    private static final int NOTIFICATION_ID = 49;
    private static final int DARK = Color.rgb(12, 18, 34);
    private static final int WHITE = Color.rgb(238, 244, 255);
    private static final int GRAY = Color.rgb(174, 187, 207);
    private static final int[] COLORS = {
        Color.rgb(130, 105, 255),
        Color.rgb(22, 194, 220),
        Color.rgb(39, 203, 155),
        Color.rgb(255, 167, 77)
    };

    private WindowManager manager;
    private SharedPreferences prefs;
    private WindowManager.LayoutParams bubbleParams;
    private View bubble;
    private View panel;
    private TextView timeLabel;
    private TextView timerLabel;
    private TextView bubbleText;
    private boolean panelShown;
    private boolean clockEnabled;
    private int themeIndex;
    private int opacityPercent;
    private long endAt;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private final Runnable ticker = new Runnable() {
        @Override public void run() {
            if (timeLabel != null) {
                String time = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
                timeLabel.setText(clockEnabled ? "الساعة: " + time : "الساعة: مخفية");
            }
            long remaining = Math.max(0L, (endAt - System.currentTimeMillis() + 999L) / 1000L);
            if (timerLabel != null) {
                timerLabel.setText("مؤقت التركيز  " + String.format(Locale.US, "%02d:%02d", remaining / 60L, remaining % 60L));
            }
            if (bubbleText != null) {
                bubbleText.setText(remaining > 0 ? "N\n" + (remaining / 60L) + "m" : "N");
            }
            if (endAt > 0 && remaining == 0) {
                endAt = 0;
                Toast.makeText(OverlayService.this, "انتهى مؤقت التركيز", Toast.LENGTH_SHORT).show();
            }
            handler.postDelayed(this, 1000L);
        }
    };

    @Override public void onCreate() {
        super.onCreate();
        prefs = getSharedPreferences("nova_ui", MODE_PRIVATE);
        manager = (WindowManager) getSystemService(WINDOW_SERVICE);
        themeIndex = prefs.getInt("accent", 0);
        themeIndex = Math.max(0, Math.min(COLORS.length - 1, themeIndex));
        opacityPercent = Math.max(55, Math.min(100, prefs.getInt("overlay_opacity", 95)));
        clockEnabled = prefs.getBoolean("overlay_clock", true);
        startForeground(NOTIFICATION_ID, notification());
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "فعّل إذن الظهور فوق التطبيقات من إعدادات NOVA", Toast.LENGTH_LONG).show();
            stopSelf();
            return;
        }
        try {
            createBubble();
            handler.post(ticker);
        } catch (RuntimeException error) {
            Toast.makeText(this, "تعذّر فتح الفقاعة؛ راجع إذن الظهور فوق التطبيقات", Toast.LENGTH_LONG).show();
            stopSelf();
        }
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            stopSelf();
            return START_NOT_STICKY;
        }
        return START_STICKY;
    }

    @Override public IBinder onBind(Intent intent) { return null; }

    private Notification notification() {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(new NotificationChannel(CHANNEL_ID,
                    "NOVA floating menu", NotificationManager.IMPORTANCE_LOW));
        }
        int piFlags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) piFlags |= PendingIntent.FLAG_IMMUTABLE;
        PendingIntent open = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class), piFlags);
        PendingIntent close = PendingIntent.getService(this, 1,
                new Intent(this, OverlayService.class).setAction(ACTION_STOP), piFlags);
        Notification.Builder b = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, CHANNEL_ID)
                : new Notification.Builder(this);
        return b.setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("NOVA")
                .setContentText("المنيو العائم يعمل — اضغط لفتح الإعدادات")
                .setOngoing(true)
                .setContentIntent(open)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "إيقاف", close)
                .build();
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + .5f);
    }

    private GradientDrawable shape(int color, int stroke, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radius));
        if (stroke != 0) d.setStroke(dp(1), stroke);
        return d;
    }

    private TextView text(String title, int fontSize, int color, boolean bold) {
        TextView v = new TextView(this);
        v.setText(title);
        v.setTextColor(color);
        v.setTextSize(fontSize);
        v.setGravity(Gravity.CENTER_VERTICAL | Gravity.RIGHT);
        v.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        if (bold) v.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return v;
    }

    private LinearLayout column() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        return l;
    }

    private Button button(String title, Runnable onClick, int color) {
        Button b = new Button(this);
        b.setAllCaps(false);
        b.setText(title);
        b.setTextSize(12);
        b.setTextColor(WHITE);
        b.setBackground(shape(color, 0, 10));
        b.setOnClickListener(v -> onClick.run());
        return b;
    }

    private void addRow(LinearLayout parent, String name, Runnable action) {
        Button b = button(name, action, Color.rgb(41, 50, 75));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(42));
        lp.setMargins(0, dp(4), 0, dp(4));
        parent.addView(b, lp);
    }

    private int overlayType() {
        return Build.VERSION.SDK_INT >= 26
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;
    }

    private void createBubble() {
        bubbleText = text("N", 21, WHITE, true);
        bubbleText.setGravity(Gravity.CENTER);
        bubbleText.setBackground(shape(COLORS[themeIndex], WHITE, 24));
        bubble = bubbleText;
        bubbleParams = new WindowManager.LayoutParams(dp(58), dp(58),
                overlayType(),
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT);
        bubbleParams.gravity = Gravity.TOP | Gravity.LEFT;
        int width = getResources().getDisplayMetrics().widthPixels;
        bubbleParams.x = prefs.getInt("bubble_x", Math.max(0, width - dp(80)));
        bubbleParams.y = prefs.getInt("bubble_y", dp(160));
        manager.addView(bubble, bubbleParams);

        bubble.setOnTouchListener(new View.OnTouchListener() {
            private float downX, downY;
            private int initialX, initialY;
            private boolean dragged;
            @Override public boolean onTouch(View view, MotionEvent event) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        downX = event.getRawX();
                        downY = event.getRawY();
                        initialX = bubbleParams.x;
                        initialY = bubbleParams.y;
                        dragged = false;
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        int dx = Math.round(event.getRawX() - downX);
                        int dy = Math.round(event.getRawY() - downY);
                        if (Math.abs(dx) > dp(6) || Math.abs(dy) > dp(6)) dragged = true;
                        if (dragged) {
                            bubbleParams.x = Math.max(0, Math.min(
                                getResources().getDisplayMetrics().widthPixels - dp(58), initialX + dx));
                            bubbleParams.y = Math.max(0, Math.min(
                                getResources().getDisplayMetrics().heightPixels - dp(58), initialY + dy));
                            manager.updateViewLayout(bubble, bubbleParams);
                        }
                        return true;
                    case MotionEvent.ACTION_UP:
                        if (dragged) {
                            prefs.edit().putInt("bubble_x", bubbleParams.x)
                                .putInt("bubble_y", bubbleParams.y).apply();
                        } else {
                            togglePanel();
                        }
                        return true;
                    default: return true;
                }
            }
        });
    }

    private void togglePanel() {
        if (panelShown) hidePanel();
        else showPanel();
    }

    private void showPanel() {
        if (panelShown || !Settings.canDrawOverlays(this)) return;
        LinearLayout base = column();
        base.setPadding(dp(15), dp(13), dp(15), dp(13));
        base.setBackground(shape(DARK, COLORS[themeIndex], 18));
        base.setAlpha(opacityPercent / 100f);

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        TextView brand = text("◆  NOVA  |  القائمة العائمة", 16, WHITE, true);
        top.addView(brand, new LinearLayout.LayoutParams(0, dp(42), 1));
        Button close = button("×", this::hidePanel, COLORS[themeIndex]);
        top.addView(close, new LinearLayout.LayoutParams(dp(38), dp(38)));
        base.addView(top);

        TextView subtitle = text("تحكم بالمنيو فقط • دون تعديل ملفات اللعبة", 11, GRAY, false);
        base.addView(subtitle);

        ScrollView scrolling = new ScrollView(this);
        LinearLayout menu = column();
        menu.setPadding(0, dp(7), 0, dp(7));

        timeLabel = text("", 15, WHITE, true);
        menu.addView(timeLabel);
        addRow(menu, "إظهار / إخفاء الساعة", () -> {
            clockEnabled = !clockEnabled;
            prefs.edit().putBoolean("overlay_clock", clockEnabled).apply();
        });

        timerLabel = text("مؤقت التركيز 00:00", 15, WHITE, true);
        menu.addView(timerLabel);
        addRow(menu, "بدء مؤقت 5 دقائق", () -> endAt = System.currentTimeMillis() + 5 * 60 * 1000L);
        addRow(menu, "إضافة دقيقة للمؤقت", () -> endAt = Math.max(System.currentTimeMillis(), endAt) + 60 * 1000L);
        addRow(menu, "إيقاف المؤقت", () -> endAt = 0L);

        addRow(menu, "تغيير لون المنيو", () -> {
            themeIndex = (themeIndex + 1) % COLORS.length;
            prefs.edit().putInt("accent", themeIndex).apply();
            if (bubbleText != null) bubbleText.setBackground(shape(COLORS[themeIndex], WHITE, 24));
            hidePanel();
            showPanel();
        });
        addRow(menu, "شفافية أكثر / أقل", () -> {
            opacityPercent = opacityPercent <= 60 ? 100 : opacityPercent - 10;
            prefs.edit().putInt("overlay_opacity", opacityPercent).apply();
            if (panel != null) panel.setAlpha(opacityPercent / 100f);
            else base.setAlpha(opacityPercent / 100f);
        });
        addRow(menu, "تصغير المنيو", this::hidePanel);
        addRow(menu, "إيقاف المنيو العائم", this::stopSelf);

        TextView notice = text("ملاحظة: نظام أندرويد أو اللعبة ممكن يمنع إظهار الطبقات العائمة.", 11, GRAY, false);
        notice.setPadding(0, dp(10), 0, 0);
        menu.addView(notice);
        scrolling.addView(menu);
        base.addView(scrolling, new LinearLayout.LayoutParams(-1, dp(380)));

        int width = getResources().getDisplayMetrics().widthPixels;
        WindowManager.LayoutParams p = new WindowManager.LayoutParams(
                Math.min(dp(340), width - dp(24)), -2,
                overlayType(),
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT);
        p.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        p.y = dp(86);
        try {
            manager.addView(base, p);
            panel = base;
            panelShown = true;
        } catch (RuntimeException e) {
            panel = null;
            panelShown = false;
            Toast.makeText(this, "تعذّر فتح المنيو العائم", Toast.LENGTH_SHORT).show();
        }
    }

    private void hidePanel() {
        if (panel != null) {
            try { manager.removeView(panel); } catch (RuntimeException ignored) { }
        }
        panel = null;
        panelShown = false;
        timeLabel = null;
        timerLabel = null;
    }

    @Override public void onDestroy() {
        handler.removeCallbacks(ticker);
        hidePanel();
        if (bubble != null) {
            try { manager.removeView(bubble); } catch (RuntimeException ignored) { }
            bubble = null;
        }
        super.onDestroy();
    }
}
