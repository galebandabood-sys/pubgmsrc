package com.RIYAZSRC;

import android.app.Activity;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/** Standalone native ImGui rendering preview. Never loads the game's native hook module. */
public final class NativePreviewActivity extends Activity {
    private GLSurfaceView surface;
    private static boolean libraryAvailable;
    static {
        try {
            System.loadLibrary("nova_preview");
            libraryAvailable = true;
        } catch (UnsatisfiedLinkError error) {
            libraryAvailable = false;
        }
    }

    static native void nativeInit(float density);
    static native void nativeResize(int width, int height);
    static native void nativeTouch(float x, float y, boolean pressed);
    static native void nativeRender();
    static native void nativeShutdown();
    static native void nativeSetOverlayMode(boolean enabled);

    static boolean isLibraryAvailable() {
        return libraryAvailable;
    }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        if (!libraryAvailable) {
            Toast.makeText(this, "مكتبة المعاينة الأصلية غير موجودة", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        getWindow().setStatusBarColor(0xff071022);
        getWindow().setNavigationBarColor(0xff071022);

        surface = new GLSurfaceView(this);
        surface.setEGLContextClientVersion(3);
        final float screenDensity = getResources().getDisplayMetrics().density;
        surface.setRenderer(new GLSurfaceView.Renderer() {
            @Override public void onSurfaceCreated(GL10 gl, EGLConfig config) {
                nativeInit(screenDensity);
            }
            @Override public void onSurfaceChanged(GL10 gl, int width, int height) {
                nativeResize(width, height);
            }
            @Override public void onDrawFrame(GL10 gl) {
                nativeRender();
            }
        });
        surface.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        surface.setOnTouchListener(new View.OnTouchListener() {
            @Override public boolean onTouch(View view, MotionEvent event) {
                final int action = event.getActionMasked();
                final boolean down = action == MotionEvent.ACTION_DOWN
                    || action == MotionEvent.ACTION_MOVE
                    || action == MotionEvent.ACTION_POINTER_DOWN;
                final float x = event.getX();
                final float y = event.getY();
                surface.queueEvent(() -> nativeTouch(x, y, down));
                return true;
            }
        });
        setContentView(surface);
    }

    @Override protected void onResume() {
        super.onResume();
        if (surface != null) surface.onResume();
    }

    @Override protected void onPause() {
        if (surface != null) surface.onPause();
        super.onPause();
    }

    @Override protected void onDestroy() {
        // GLSurfaceView's EGL context lifecycle is owned by Android.
        if (surface != null) {
            surface.queueEvent(() -> nativeShutdown());
        }
        super.onDestroy();
    }
}
