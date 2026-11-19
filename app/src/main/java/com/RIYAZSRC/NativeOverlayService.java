package com.RIYAZSRC;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.opengl.GLSurfaceView;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/** Opt-in manual Android overlay. Requires draw-over-apps permission only. */
public final class NativeOverlayService extends Service {
    public static final String ACTION_STOP = "com.RIYAZSRC.STOP_NATIVE_OVERLAY";
    private static final String CHANNEL = "nova_prime_session";
    private static final int NOTIFICATION_ID = 82;
    private static final String PREFS = "nova_prime";
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable watch = new Runnable() {
        @Override public void run() {
            if (!Settings.canDrawOverlays(NativeOverlayService.this)) {
                setGameActive(false);
                stopSelf();
                return;
            }
            if (gameActive) {
                try {
                    syncNativeFlags();
                } catch (RuntimeException failure) {
                    setGameActive(false);
                    stopSelf();
                    return;
                }
            }
            handler.postDelayed(this, 1100L);
        }
    };
    private WindowManager windows;
    private TextView bubble;
    private WindowManager.LayoutParams bubbleParams;
    private GLSurfaceView panel;
    private View reticle;
    private boolean panelShown;
    private boolean gameActive;
    private volatile boolean rendererReady;
    private int flags;

    private int dp(float n) {
        return Math.round(n * getResources().getDisplayMetrics().density);
    }

    private GradientDrawable roundBackground(int fill, int radius, int stroke) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(fill);
        shape.setCornerRadius(dp(radius));
        if (stroke != 0) shape.setStroke(dp(2), stroke);
        return shape;
    }

    private Notification notification() {
        NotificationManager manager =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        manager.createNotificationChannel(new NotificationChannel(CHANNEL,
                "NOVA - manual overlay", NotificationManager.IMPORTANCE_LOW));
        int bits = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        PendingIntent open = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class), bits);
        PendingIntent stop = PendingIntent.getService(this, 1,
                new Intent(this, NativeOverlayService.class).setAction(ACTION_STOP), bits);
        return new Notification.Builder(this, CHANNEL)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("NOVA PRIME / Manual HUD")
                .setContentText("المنيو اليدوي شغال؛ اضغط إيقاف حتى تخفيه")
                .setContentIntent(open)
                .setOngoing(true)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "إيقاف", stop)
                .build();
    }

    @Override public void onCreate() {
        super.onCreate();
        startForeground(NOTIFICATION_ID, notification());
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "فعّل إذن الظهور فوق التطبيقات من NOVA",
                    Toast.LENGTH_LONG).show();
            stopSelf();
            return;
        }
        if (!NativePreviewActivity.isLibraryAvailable()) {
            Toast.makeText(this, "ملف ImGui الأصلي غير موجود", Toast.LENGTH_LONG).show();
            stopSelf();
            return;
        }
        windows = (WindowManager) getSystemService(WINDOW_SERVICE);
        flags = getSharedPreferences(PREFS, MODE_PRIVATE).getInt("hud_flags", 0) & 35;
        // Manual opt-in: show the bubble immediately, without reading usage events.
        // The bubble remains visible until the user stops it in NOVA/notification.
        setGameActive(true);
        handler.post(watch);
    }

    @Override public int onStartCommand(Intent intent, int startupFlags, int id) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            stopSelf();
            return START_NOT_STICKY;
        }
        return START_NOT_STICKY;
    }

    @Override public IBinder onBind(Intent intent) {
        return null;
    }

    private void setGameActive(boolean foreground) {
        if (foreground == gameActive) return;
        gameActive = foreground;
        if (foreground) {
            try {
                createBubble();
                applyHudFlags();
                // The floating control panel opens only when the user taps N.
            } catch (RuntimeException failed) {
                setGameActive(false);
            }
        } else {
            hidePanel();
            removeReticle();
            removeBubble();
        }
    }

    private void createBubble() {
        if (bubble != null || windows == null) return;
        TextView view = new TextView(this);
        view.setText("N");
        view.setTextSize(22);
        view.setTypeface(null, 1);
        view.setGravity(Gravity.CENTER);
        view.setTextColor(Color.WHITE);
        view.setBackground(roundBackground(0xff8856ed, 20, 0xffb99dff));

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                dp(54), dp(54), WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.LEFT | Gravity.TOP;
        params.x = Math.max(0, getResources().getDisplayMetrics().widthPixels - dp(70));
        params.y = dp(120);
        windows.addView(view, params);
        bubble = view;
        bubbleParams = params;

        view.setOnTouchListener(new View.OnTouchListener() {
            float startX, startY;
            int originalX, originalY;
            boolean moved;
            @Override public boolean onTouch(View unused, MotionEvent event) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        startX = event.getRawX();
                        startY = event.getRawY();
                        originalX = bubbleParams.x;
                        originalY = bubbleParams.y;
                        moved = false;
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        int dx = Math.round(event.getRawX() - startX);
                        int dy = Math.round(event.getRawY() - startY);
                        if (Math.abs(dx) + Math.abs(dy) > dp(8)) moved = true;
                        if (moved) {
                            bubbleParams.x = Math.max(0, Math.min(
                                    getResources().getDisplayMetrics().widthPixels - dp(54),
                                    originalX + dx));
                            bubbleParams.y = Math.max(0, Math.min(
                                    getResources().getDisplayMetrics().heightPixels - dp(54),
                                    originalY + dy));
                            try { windows.updateViewLayout(bubble, bubbleParams); }
                            catch (RuntimeException ignored) { }
                        }
                        return true;
                    case MotionEvent.ACTION_UP:
                        if (!moved) {
                            if (panelShown) hidePanel();
                            else showPanel();
                        }
                        return true;
                    default:
                        return true;
                }
            }
        });
    }

    private void removeBubble() {
        if (bubble != null && windows != null) {
            try { windows.removeView(bubble); } catch (RuntimeException ignored) { }
        }
        bubble = null;
        bubbleParams = null;
    }

    private void showPanel() {
        if (!gameActive || panelShown || windows == null || !Settings.canDrawOverlays(this))
            return;
        GLSurfaceView view = new GLSurfaceView(this);
        view.setEGLContextClientVersion(3);
        view.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        view.setZOrderOnTop(true);
        view.getHolder().setFormat(PixelFormat.TRANSLUCENT);
        final float density = getResources().getDisplayMetrics().density;
        final int savedFlags = flags;
        rendererReady = false;
        view.setRenderer(new GLSurfaceView.Renderer() {
            @Override public void onSurfaceCreated(GL10 gl, EGLConfig config) {
                NativePreviewActivity.nativeInit(density);
                NativePreviewActivity.nativeSetOverlayMode(true);
                NativePreviewActivity.nativeSetOverlayFlags(savedFlags);
                rendererReady = true;
            }
            @Override public void onSurfaceChanged(GL10 gl, int width, int height) {
                NativePreviewActivity.nativeResize(width, height);
            }
            @Override public void onDrawFrame(GL10 gl) {
                NativePreviewActivity.nativeRender();
            }
        });
        view.setOnTouchListener((unused, event) -> {
            int type = event.getActionMasked();
            boolean pressed = type == MotionEvent.ACTION_DOWN ||
                    type == MotionEvent.ACTION_MOVE ||
                    type == MotionEvent.ACTION_POINTER_DOWN;
            float x = event.getX(), y = event.getY();
            view.queueEvent(() -> NativePreviewActivity.nativeTouch(x, y, pressed));
            return true;
        });
        view.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        int width = Math.min(dp(460),
                getResources().getDisplayMetrics().widthPixels - dp(18));
        int height = Math.min(dp(620),
                getResources().getDisplayMetrics().heightPixels - dp(42));
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                Math.max(dp(220), width), Math.max(dp(240), height),
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.CENTER;
        try {
            windows.addView(view, params);
            panel = view;
            panelShown = true;
        } catch (RuntimeException failed) {
            rendererReady = false;
            view.onPause();
            Toast.makeText(this, "ما ممكن فتح المنيو على هذا الجهاز",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void syncNativeFlags() {
        if (!panelShown || !rendererReady) return;
        int updated = NativePreviewActivity.nativeGetOverlayFlags() & 35;
        if (updated == flags) return;
        flags = updated;
        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                .putInt("hud_flags", flags).apply();
        applyHudFlags();
    }

    private void hidePanel() {
        if (panel == null) return;
        syncNativeFlags();
        GLSurfaceView view = panel;
        panel = null;
        panelShown = false;
        rendererReady = false;
        try {
            view.queueEvent(NativePreviewActivity::nativeShutdown);
            view.onPause();
        } catch (RuntimeException ignored) { }
        if (windows != null) {
            try { windows.removeView(view); } catch (RuntimeException ignored) { }
        }
    }

    private final class Reticle extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        Reticle() { super(NativeOverlayService.this); setBackgroundColor(Color.TRANSPARENT); }
        @Override protected void onDraw(Canvas canvas) {
            float cx = getWidth() * .5f, cy = getHeight() * .5f;
            paint.setColor(0xffa790ff);
            paint.setStrokeWidth(dp(2));
            paint.setStyle(Paint.Style.STROKE);
            if ((flags & 2) != 0) {
                canvas.drawCircle(cx, cy, dp(68), paint);
            }
            if ((flags & 1) != 0) {
                canvas.drawLine(cx - dp(19), cy, cx - dp(5), cy, paint);
                canvas.drawLine(cx + dp(5), cy, cx + dp(19), cy, paint);
                canvas.drawLine(cx, cy - dp(19), cx, cy - dp(5), paint);
                canvas.drawLine(cx, cy + dp(5), cx, cy + dp(19), paint);
                paint.setStyle(Paint.Style.FILL);
                canvas.drawCircle(cx, cy, dp(2), paint);
            }
        }
    }

    private void applyHudFlags() {
        if (!gameActive || windows == null) return;
        boolean showReticle = (flags & 3) != 0;
        if (showReticle && reticle == null) {
            View view = new Reticle();
            WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                    dp(170), dp(170),
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT);
            params.gravity = Gravity.CENTER;
            params.alpha = .75f;
            try {
                windows.addView(view, params);
                reticle = view;
            } catch (RuntimeException ignored) { }
        } else if (!showReticle) {
            removeReticle();
        }
        if (reticle != null) reticle.invalidate();

    }

    private void removeReticle() {
        if (reticle != null && windows != null) {
            try { windows.removeView(reticle); } catch (RuntimeException ignored) { }
        }
        reticle = null;
    }

    @Override public void onDestroy() {
        handler.removeCallbacks(watch);
        setGameActive(false);
        hidePanel();
        removeReticle();
        removeBubble();
        stopForeground(true);
        super.onDestroy();
    }
}
