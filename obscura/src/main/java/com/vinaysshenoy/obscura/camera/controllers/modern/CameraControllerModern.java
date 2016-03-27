package com.vinaysshenoy.obscura.camera.controllers.modern;

import android.support.annotation.NonNull;

import com.vinaysshenoy.obscura.AutoFitTextureView;
import com.vinaysshenoy.obscura.Obscura;
import com.vinaysshenoy.obscura.camera.controllers.BaseController;

/**
 * Created by vinaysshenoy on 20/03/16.
 */
public class CameraControllerModern extends BaseController {

    public CameraControllerModern(@NonNull AutoFitTextureView textureView) {
        super(textureView);
    }

    @Override
    protected void closeCamera() {

    }

    @Override
    protected void setupCamera(@NonNull Obscura.CameraDesc cameraDesc, int width, int height) {

    }

    @Override
    public boolean isModernCameraApi() {
        return true;
    }
}
