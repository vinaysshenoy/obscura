package com.vinaysshenoy.obscura.camera.controllers.modern;

import android.support.annotation.NonNull;
import android.view.TextureView;

import com.vinaysshenoy.obscura.camera.controllers.BaseController;

/**
 * Created by vinaysshenoy on 20/03/16.
 */
public class CameraControllerModern extends BaseController {

    public CameraControllerModern(@NonNull TextureView textureView) {
        super(textureView);
    }

    @Override
    public boolean isModernCameraApi() {
        return true;
    }
}
