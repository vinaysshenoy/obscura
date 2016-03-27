package com.vinaysshenoy.obscura.camera.controllers;

import android.Manifest;
import android.content.Context;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresPermission;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;

import com.vinaysshenoy.obscura.AutoFitTextureView;
import com.vinaysshenoy.obscura.Obscura;
import com.vinaysshenoy.obscura.ObscuraUtils;
import com.vinaysshenoy.obscura.camera.Size;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by vinaysshenoy on 20/03/16.
 */
public abstract class BaseController {

    /**
     * Conversion from screen rotation to JPEG orientation.
     */
    protected static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    private static final String TAG = "BaseController";

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    protected Obscura.CameraDesc cameraToUse;
    /**
     * Size fo the camera preview
     */
    protected Size previewSize;
    /**
     * Size of the still capture
     */
    protected Size pictureSize;
    protected AutoFitTextureView textureView;
    private HandlerThread backgroundThread;
    private Handler backgroundHandler;
    @NonNull
    private Handler mainHandler;
    protected Context context;

    protected boolean isFlashSupported;

    private final TextureView.SurfaceTextureListener surfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            openCamera(width, height);
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            configureTransform(width, height);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {

        }
    };
    private boolean isPreviewHappening;

    protected BaseController(@NonNull AutoFitTextureView textureView) {
        this.textureView = textureView;
        this.context = textureView.getContext();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    /**
     * Given {@code choices} of {@code Size}s supported by a camera, choose the smallest one that
     * is at least as large as the respective texture view size, and that is at most as large as the
     * respective max size, and whose aspect ratio matches with the specified value. If such size
     * doesn't exist, choose the largest one that is at most as large as the respective max size,
     * and whose aspect ratio matches with the specified value.
     *
     * @param choices           The list of sizes that the camera supports for the intended output
     *                          class
     * @param textureViewWidth  The width of the texture view relative to sensor coordinate
     * @param textureViewHeight The height of the texture view relative to sensor coordinate
     * @param maxWidth          The maximum width that can be chosen
     * @param maxHeight         The maximum height that can be chosen
     * @param aspectRatio       The aspect ratio
     * @return The optimal {@code Size}, or an arbitrary one if none were big enough
     */
    protected static Size chooseOptimalSize(Size[] choices, int textureViewWidth,
                                            int textureViewHeight, int maxWidth, int maxHeight, Size aspectRatio) {

        // Collect the supported resolutions that are at least as big as the preview Surface
        List<Size> bigEnough = new ArrayList<>();
        // Collect the supported resolutions that are smaller than the preview Surface
        List<Size> notBigEnough = new ArrayList<>();
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();
        for (Size option : choices) {
            if (option.getWidth() <= maxWidth && option.getHeight() <= maxHeight &&
                    option.getHeight() == option.getWidth() * h / w) {
                if (option.getWidth() >= textureViewWidth &&
                        option.getHeight() >= textureViewHeight) {
                    bigEnough.add(option);
                } else {
                    notBigEnough.add(option);
                }
            }
        }

        // Pick the smallest of those big enough. If there is no one big enough, pick the
        // largest of those not big enough.
        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CompareSizesByArea());
        } else if (notBigEnough.size() > 0) {
            return Collections.max(notBigEnough, new CompareSizesByArea());
        } else {
            Log.e(TAG, "Couldn't find any suitable preview size");
            return choices[0];
        }
    }

    @Nullable
    protected Handler backgroundHandler() {
        return backgroundHandler;
    }

    @NonNull
    protected Handler mainHandler() {
        return mainHandler;
    }

    @RequiresPermission(Manifest.permission.CAMERA)
    public void stopPreview() {

        closeCamera();
        stopBackgroundThread();
    }

    protected abstract void closeCamera();

    @RequiresPermission(Manifest.permission.CAMERA)
    public void startPreview() {

        startBackgroundThread();

        // When the screen is turned off and turned back on, the SurfaceTexture is already
        // available, and "onSurfaceTextureAvailable" will not be called. In that case, we can open
        // a camera and start preview from here (otherwise, we wait until the surface is ready in
        // the SurfaceTextureListener).
        if (textureView.isAvailable()) {
            openCamera(textureView.getWidth(), textureView.getHeight());
        } else {
            textureView.setSurfaceTextureListener(surfaceTextureListener);
        }
    }

    /**
     * Configures the necessary {@link android.graphics.Matrix} transformation to `textureView`.
     * This method should be called after the camera preview size is determined in
     * setUpCameraOutputs and also the size of `textureView` is fixed.
     *
     * @param viewWidth  The width of `textureView`
     * @param viewHeight The height of `textureView`
     */
    protected void configureTransform(int viewWidth, int viewHeight) {
        if (textureView == null || previewSize == null) {
            return;
        }
        int rotation = ObscuraUtils.windowManagerFromContext(context).getDefaultDisplay().getRotation();
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        RectF bufferRect = new RectF(0, 0, previewSize.getHeight(), previewSize.getWidth());
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max(
                    (float) viewHeight / previewSize.getHeight(),
                    (float) viewWidth / previewSize.getWidth());
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        } else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180, centerX, centerY);
        }
        textureView.setTransform(matrix);
    }

    private void openCamera(int width, int height) {

        if (ObscuraUtils.isCameraPermissionGranted(context)) {
            cameraToUse = getCameraToUse();
            if (cameraToUse != null) {
                setupCamera(cameraToUse, width, height);
            } else {
                //TODO: Error message
            }
        }
    }

    protected abstract void setupCamera(@NonNull Obscura.CameraDesc cameraDesc, int width, int height);

    @Nullable
    private Obscura.CameraDesc getCameraToUse() {

        if (cameraToUse != null) {
            return cameraToUse;
        }

        //Don't care about permission because it will never reach this stage if we don't have it
        final List<Obscura.CameraDesc> cameraDescs = Obscura.get().camerasInfo(context);
        if (cameraDescs != null) {
            //Find the first back facing camera
            Obscura.CameraDesc cameraDesc;
            for (int i = 0; i < cameraDescs.size(); i++) {
                cameraDesc = cameraDescs.get(i);
                if (cameraDesc.facing == Obscura.CameraDesc.FACING_BACK) {
                    return cameraDesc;
                }
            }
        }
        return null;
    }

    public void setCameraToUse(@Nullable Obscura.CameraDesc cameraToUse) {
        this.cameraToUse = cameraToUse;
        //TODO: Restart preview if its already happening
    }

    private void startBackgroundThread() {
        backgroundThread = new HandlerThread("CameraBackground");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }

    private void stopBackgroundThread() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            backgroundThread.quitSafely();
        } else {
            backgroundThread.quit();
        }
        try {
            backgroundThread.join();
            backgroundThread = null;
            backgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public abstract boolean isModernCameraApi();

    /**
     * Compares two {@code Size}s based on their areas.
     */
    public static class CompareSizesByArea implements Comparator<Size> {

        @Override
        public int compare(Size lhs, Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }

    }
}
