package com.vinaysshenoy.obscura.camera.controllers.vintage;

import android.hardware.Camera;
import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;
import android.view.TextureView;

import com.vinaysshenoy.obscura.camera.controllers.BaseController;

/**
 * Controller that uses the deprecated {@link android.hardware.Camera} APIs.
 * <p/>
 * This is meant to used on Kitkat and below
 * <p/>
 * Created by vinaysshenoy on 19/03/16.
 */
public class CameraControllerVintage extends BaseController {


    public CameraControllerVintage(@NonNull TextureView textureView) {
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

    public static class CameraOpenException extends Exception {

        public CameraOpenException(String detailMessage, Throwable throwable) {
            super(detailMessage, throwable);
        }
    }

    @Override
    public boolean isModernCameraApi() {
        return false;
    }
}
