package jp.co.cyberagent.android.gpuimage;

import android.graphics.SurfaceTexture;

/**
 * Render回调接口
 */
public interface IRenderCallback {
    void onSurfaceTextureCreated(SurfaceTexture surfaceTexture);
    void onFrameAvailable(long frameTimeNanos);
}
