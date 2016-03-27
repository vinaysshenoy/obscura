package com.vinaysshenoy.obscura.camera.controllers.vintage;

import android.content.res.Configuration;
import android.graphics.Point;
import android.hardware.Camera;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;
import android.util.Log;
import android.view.Display;
import android.view.Surface;

import com.vinaysshenoy.obscura.AutoFitTextureView;
import com.vinaysshenoy.obscura.Obscura;
import com.vinaysshenoy.obscura.ObscuraUtils;
import com.vinaysshenoy.obscura.camera.Size;
import com.vinaysshenoy.obscura.camera.controllers.BaseController;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Controller that uses the deprecated {@link android.hardware.Camera} APIs.
 * <p/>
 * This is meant to used on Kitkat and below
 * <p/>
 * Created by vinaysshenoy on 19/03/16.
 */
public class CameraControllerVintage extends BaseController {

    private static final String TAG = "CameraControllerVintage";

    private Camera camera;

    private int cameraId;

    public CameraControllerVintage(@NonNull AutoFitTextureView textureView) {
        super(textureView);
    }

    @WorkerThread
    private static Camera getInstance(int cameraId) throws CameraOpenException {

        try {
            return Camera.open(cameraId);
        } catch (Exception e) {
            throw new CameraOpenException("Unable to open camera", e);
        }
    }

    private static List<Size> readPictureSizes(Camera.Parameters parameters) {

        final List<Camera.Size> pictureSizes = parameters.getSupportedPictureSizes();
        final List<Size> cameraSizes = new ArrayList<>(pictureSizes.size());
        Camera.Size pictureSize;
        for (int i = 0; i < pictureSizes.size(); i++) {
            pictureSize = pictureSizes.get(i);
            cameraSizes.add(new Size(pictureSize.width, pictureSize.height));
        }
        return cameraSizes;
    }

    private static List<Size> readPreviewSizes(Camera.Parameters parameters) {

        final List<Camera.Size> previewSizes = parameters.getSupportedPreviewSizes();
        final List<Size> cameraSizes = new ArrayList<>(previewSizes.size());
        Camera.Size previewSize;
        for (int i = 0; i < previewSizes.size(); i++) {
            previewSize = previewSizes.get(i);
            cameraSizes.add(new Size(previewSize.width, previewSize.height));
        }
        return cameraSizes;
    }

    @Override
    protected void closeCamera() {

        if (camera != null) {
            camera.release();
            camera = null;
        }
    }

    @Override
    protected void setupCamera(@NonNull final Obscura.CameraDesc cameraDesc, final int width, final int height) {

        final Handler backgroundHandler = backgroundHandler();
        if (backgroundHandler != null) {
            backgroundHandler.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        cameraId = Integer.parseInt(cameraDesc.cameraId);
                        camera = getInstance(cameraId);
                    } catch (CameraOpenException e) {
                        e.printStackTrace();
                        //TODO: Post error message
                    }

                    mainHandler().post(new Runnable() {
                        @Override
                        public void run() {
                            if (camera != null) {
                                setupOutputs(camera, width, height);
                                configureTransform(width, height);
                                try {
                                    camera.setPreviewTexture(textureView.getSurfaceTexture());
                                    camera.startPreview();
                                } catch (IOException e) {
                                    Log.e(TAG, "Error setting camera preview", e);
                                }
                            }
                        }
                    });

                }
            });
        }

    }

    private void setupOutputs(Camera camera, int previewWidth, int previewHeight) {

        final Camera.Parameters parameters = camera.getParameters();
        final List<Size> supportedPictureSizes = readPictureSizes(parameters);
        final CompareSizesByArea compareByArea = new CompareSizesByArea();
        pictureSize = Collections.max(supportedPictureSizes, compareByArea);

        final List<Size> supportedPreviewSizes = readPreviewSizes(parameters);
        final Size largestPreviewSize = Collections.max(supportedPreviewSizes, compareByArea);
        setupPreviewSize(parameters, supportedPreviewSizes.toArray(new Size[supportedPreviewSizes.size()]), previewWidth, previewHeight, largestPreviewSize);
    }

    private void setupPreviewSize(Camera.Parameters parameters, Size[] previewSizes, int previewWidth, int previewHeight, Size largestPreviewSize) {

        final Display defaultDisplay = ObscuraUtils.windowManagerFromContext(context).getDefaultDisplay();
        final int displayRotation = defaultDisplay.getRotation();
        final Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);
        int sensorOrientation = info.orientation;
        boolean swappedDimensions = false;
        switch (displayRotation) {
            case Surface.ROTATION_0:
            case Surface.ROTATION_180:
                if (sensorOrientation == 90 || sensorOrientation == 270) {
                    swappedDimensions = true;
                }
                break;
            case Surface.ROTATION_90:
            case Surface.ROTATION_270:
                if (sensorOrientation == 0 || sensorOrientation == 180) {
                    swappedDimensions = true;
                }
                break;
            default:
                Log.e(TAG, "Display rotation is invalid: " + displayRotation);
        }

        Point displaySize = new Point();
        defaultDisplay.getSize(displaySize);
        int rotatedPreviewWidth = previewWidth;
        int rotatedPreviewHeight = previewHeight;
        int maxPreviewWidth = displaySize.x;
        int maxPreviewHeight = displaySize.y;

        if (swappedDimensions) {
            rotatedPreviewWidth = previewHeight;
            rotatedPreviewHeight = previewWidth;
            maxPreviewWidth = displaySize.y;
            maxPreviewHeight = displaySize.x;
        }

        // Danger, W.R.! Attempting to use too large a preview size could  exceed the camera
        // bus' bandwidth limitation, resulting in gorgeous previews but the storage of
        // garbage capture data.
        previewSize = chooseOptimalSize(previewSizes,
                rotatedPreviewWidth, rotatedPreviewHeight, maxPreviewWidth,
                maxPreviewHeight, largestPreviewSize);

        // We fit the aspect ratio of TextureView to the size of preview we picked.
        int orientation = context.getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            textureView.setAspectRatio(
                    previewSize.getWidth(), previewSize.getHeight());
        } else {
            textureView.setAspectRatio(
                    previewSize.getHeight(), previewSize.getWidth());
        }

        // Check if the flash is supported.
        final List<String> supportedFlashModes = parameters.getSupportedFlashModes();
        isFlashSupported = (supportedFlashModes != null && !supportedFlashModes.isEmpty());

    }

    @Override
    public boolean isModernCameraApi() {
        return false;
    }

    public static class CameraOpenException extends Exception {

        public CameraOpenException(String detailMessage, Throwable throwable) {
            super(detailMessage, throwable);
        }
    }
}
