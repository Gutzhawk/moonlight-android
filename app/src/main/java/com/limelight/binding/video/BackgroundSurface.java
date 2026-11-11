package com.limelight.binding.video;

import android.graphics.SurfaceTexture;
import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLSurface;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.os.Handler;
import android.os.HandlerThread;
import android.view.Surface;

import com.limelight.LimeLog;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

/** An off-screen decoder output that survives destruction of the activity's window. */
public final class BackgroundSurface {
    private final HandlerThread thread = new HandlerThread("BackgroundVideoSurface");
    private final Handler handler;
    private EGLDisplay display = EGL14.EGL_NO_DISPLAY;
    private EGLContext context = EGL14.EGL_NO_CONTEXT;
    private EGLSurface eglSurface = EGL14.EGL_NO_SURFACE;
    private SurfaceTexture texture;
    private Surface surface;
    private int textureId;

    public BackgroundSurface() {
        thread.start();
        handler = new Handler(thread.getLooper());
        FutureTask<Void> init = new FutureTask<>(() -> { initialize(); return null; });
        handler.post(init);
        try {
            init.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            release();
            throw new IllegalStateException("Interrupted creating background surface", e);
        } catch (ExecutionException e) {
            release();
            throw new IllegalStateException("Cannot create background surface", e.getCause());
        }
    }

    private void initialize() {
        display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
        int[] version = new int[2];
        if (!EGL14.eglInitialize(display, version, 0, version, 1)) {
            throw new IllegalStateException("eglInitialize failed");
        }
        EGLConfig[] configs = new EGLConfig[1];
        int[] count = new int[1];
        int[] attrs = {EGL14.EGL_RED_SIZE, 8, EGL14.EGL_GREEN_SIZE, 8,
                EGL14.EGL_BLUE_SIZE, 8, EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                EGL14.EGL_SURFACE_TYPE, EGL14.EGL_PBUFFER_BIT, EGL14.EGL_NONE};
        if (!EGL14.eglChooseConfig(display, attrs, 0, configs, 0, 1, count, 0) || count[0] == 0) {
            throw new IllegalStateException("No background EGL config");
        }
        context = EGL14.eglCreateContext(display, configs[0], EGL14.EGL_NO_CONTEXT,
                new int[]{EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE}, 0);
        eglSurface = EGL14.eglCreatePbufferSurface(display, configs[0],
                new int[]{EGL14.EGL_WIDTH, 1, EGL14.EGL_HEIGHT, 1, EGL14.EGL_NONE}, 0);
        if (!EGL14.eglMakeCurrent(display, eglSurface, eglSurface, context)) {
            throw new IllegalStateException("eglMakeCurrent failed");
        }
        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);
        textureId = textures[0];
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId);
        texture = new SurfaceTexture(textureId);
        texture.setOnFrameAvailableListener(t -> {
            try {
                t.updateTexImage();
            } catch (RuntimeException e) {
                LimeLog.warning("Background frame drain failed: " + e);
            }
        }, handler);
        surface = new Surface(texture);
    }

    public Surface getSurface() {
        return surface;
    }

    /** Call only after the decoder has stopped using this surface. */
    public void release() {
        handler.post(() -> {
            if (surface != null) surface.release();
            if (texture != null) texture.release();
            if (display != EGL14.EGL_NO_DISPLAY) {
                if (textureId != 0) GLES20.glDeleteTextures(1, new int[]{textureId}, 0);
                EGL14.eglMakeCurrent(display, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT);
                if (eglSurface != EGL14.EGL_NO_SURFACE) EGL14.eglDestroySurface(display, eglSurface);
                if (context != EGL14.EGL_NO_CONTEXT) EGL14.eglDestroyContext(display, context);
                EGL14.eglReleaseThread();
                EGL14.eglTerminate(display);
            }
            thread.quitSafely();
        });
    }
}
