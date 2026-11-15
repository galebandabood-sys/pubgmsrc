package com.RIYAZSRC;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.opengl.GLSurfaceView;
import android.os.IBinder;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * User-consented SYSTEM_ALERT_WINDOW overlay with the repository's real ImGui renderer.
 * Displays on top of apps that permit overlays. Does not load libItsMyHubS.so, read
 * another app's memory, alter game files, or bypass a game's anti-cheat controls.
 */
public final class NativeOverlayService extends Service {
    public static final String ACTION_STOP = "com.RIYAZSRC.STOP_NATIVE_OVERLAY";
    private static final String CHANNEL = "nova_native_overlay";
    private static final int NOTIFICATION_ID = 82;
    private WindowManager windows;
    private TextView bubble;
    private WindowManager.LayoutParams bubbleParams;
    private GLSurfaceView panel;
    private boolean panelShown;

    private int dp(int n) {
        return Math.round(n * getResources().getDisplayMetrics().density);
    }

    private GradientDrawable bubbleBackground() {
        GradientDrawable d = new GradientDrawable();
        d.setColor(Color.rgb(102, 84, 236));
        d.setStroke(dp(2), Color.WHITE);
        d.setCornerRadius(dp(34));
        return d;
    }

    private Notification notification() {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.createNotificationChannel(new NotificationChannel(CHANNEL,
                "NOVA native overlay", NotificationManager.IMPORTANCE_LOW));
        int flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        PendingIntent open = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class), flags);
        PendingIntent stop = PendingIntent.getService(this, 1,
                new Intent(this, NativeOverlayService.class).setAction(ACTION_STOP), flags);
        return new Notification.Builder(this, CHANNEL)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("NOVA / Native ImGui")
                .setContentText("المنيو العائم يعمل — اضغط فقاعة N")
                .setContentIntent(open)
                .setOngoing(true)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "إيقاف", stop)
                .build();
    }

    @Override public void onCreate() {
        super.onCreate();
        startForeground(NOTIFICATION_ID, notification());
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "فعّل إذن الظهور فوق التطبيقات", Toast.LENGTH_LONG).show();
            stopSelf();
            return;
        }
        if (!NativePreviewActivity.isLibraryAvailable()) {
            Toast.makeText(this, "مكتبة ImGui غير متوفرة", Toast.LENGTH_LONG).show();
            stopSelf();
            return;
        }
        windows = (WindowManager) getSystemService(WINDOW_SERVICE);
        try {
            createBubble();
        } catch (RuntimeException failure) {
            Toast.makeText(this, "لم يتمكن أندرويد من إظهار الفقاعة", Toast.LENGTH_LONG).show();
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

    @Override public IBinder onBind(Intent intent) {
        return null;
    }

    private void createBubble() {
        bubble = new TextView(this);
        bubble.setText("N");
        bubble.setTextSize(22);
        bubble.setTextColor(Color.WHITE);
        bubble.setGravity(Gravity.CENTER);
        bubble.setBackground(bubbleBackground());

        bubbleParams = new WindowManager.LayoutParams(dp(58), dp(58),
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT);
        bubbleParams.gravity = Gravity.LEFT | Gravity.TOP;
        bubbleParams.x = Math.max(0, getResources().getDisplayMetrics().widthPixels - dp(78));
        bubbleParams.y = dp(110);
        windows.addView(bubble, bubbleParams);

        bubble.setOnTouchListener(new View.OnTouchListener() {
            private float downX, downY;
            private int startX, startY;
            private boolean dragged;
            @Override public boolean onTouch(View v, MotionEvent e) {
                int action = e.getActionMasked();
                if (action == MotionEvent.ACTION_DOWN) {
                    downX = e.getRawX();
                    downY = e.getRawY();
                    startX = bubbleParams.x;
                    startY = bubbleParams.y;
                    dragged = false;
                    return true;
                }
                if (action == MotionEvent.ACTION_MOVE) {
                    int dx = Math.round(e.getRawX() - downX);
                    int dy = Math.round(e.getRawY() - downY);
                    if (Math.abs(dx) > dp(6) || Math.abs(dy) > dp(6)) dragged = true;
                    if (dragged) {
                        bubbleParams.x = Math.max(0, Math.min(
                                getResources().getDisplayMetrics().widthPixels - dp(58), startX + dx));
                        bubbleParams.y = Math.max(0, Math.min(
                                getResources().getDisplayMetrics().heightPixels - dp(58), startY + dy));
                        try { windows.updateViewLayout(bubble, bubbleParams); }
                        catch (RuntimeException ignored) { }
                    }
                    return true;
                }
                if (action == MotionEvent.ACTION_UP) {
                    if (!dragged) {
                        if (panelShown) hidePanel();
                        else showPanel();
                    }
                    return true;
                }
                return action == MotionEvent.ACTION_CANCEL;
            }
        });
    }

    private void showPanel() {
        if (panelShown || windows == null || !Settings.canDrawOverlays(this)) return;
        GLSurfaceView view = new GLSurfaceView(this);
        view.setEGLContextClientVersion(3);
        view.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        view.setZOrderOnTop(true);
        view.getHolder().setFormat(PixelFormat.TRANSLUCENT);
        final float density = getResources().getDisplayMetrics().density;
        view.setRenderer(new GLSurfaceView.Renderer() {
            @Override public void onSurfaceCreated(GL10 gl, EGLConfig config) {
                NativePreviewActivity.nativeInit(density);
                NativePreviewActivity.nativeSetOverlayMode(true);
            }
            @Override public void onSurfaceChanged(GL10 gl, int w, int h) {
                NativePreviewActivity.nativeResize(w, h);
            }
            @Override public void onDrawFrame(GL10 gl) {
                NativePreviewActivity.nativeRender();
            }
        });
        view.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        view.setOnTouchListener((v, event) -> {
            int action = event.getActionMasked();
            boolean pressed = action == MotionEvent.ACTION_DOWN ||
                    action == MotionEvent.ACTION_MOVE ||
                    action == MotionEvent.ACTION_POINTER_DOWN;
            float x = event.getX(), y = event.getY();
            view.queueEvent(() -> NativePreviewActivity.nativeTouch(x, y, pressed));
            return true;
        });

        int width = Math.max(dp(220), Math.min(dp(410),
                getResources().getDisplayMetrics().widthPixels - dp(24)));
        int height = Math.max(dp(220), Math.min(dp(540),
                getResources().getDisplayMetrics().heightPixels - dp(28)));
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                width, height,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.CENTER;
        try {
            windows.addView(view, params);
            panel = view;
            panelShown = true;
        } catch (RuntimeException failure) {
            view.onPause();
            Toast.makeText(this, "تعذّر فتح منيو ImGui فوق التطبيق", Toast.LENGTH_SHORT).show();
        }
    }

    private void hidePanel() {
        GLSurfaceView view = panel;
        panel = null;
        panelShown = false;
        if (view == null) return;
        // Finish GL work on the GL thread before its surface is detached.
        try {
            view.queueEvent(NativePreviewActivity::nativeShutdown);
            view.onPause();
        } catch (RuntimeException ignored) { }
        if (windows != null) {
            try { windows.removeView(view); } catch (RuntimeException ignored) { }
        }
    }

    @Override public void onDestroy() {
        hidePanel();
        if (bubble != null && windows != null) {
            try { windows.removeView(bubble); } catch (RuntimeException ignored) { }
        }
        bubble = null;
        stopForeground(true);
        super.onDestroy();
    }
}
