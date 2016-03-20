package com.vinaysshenoy.obscura;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Context;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresPermission;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by vinaysshenoy on 19/03/16.
 */
public final class Obscura {

    private static Obscura instance;

    private List<CameraDesc> cachedCamerasInfo;

    private Obscura() {

    }

    public static Obscura get() {
        if (instance == null) {
            synchronized (Obscura.class) {
                if (instance == null) {
                    instance = new Obscura();
                }
            }
        }
        return instance;
    }

    /**
     * Gets the list of supported cameras and their configuration on this device
     */
    @RequiresPermission(Manifest.permission.CAMERA)
    @Nullable
    public List<CameraDesc> camerasInfo(@NonNull Context context) {

        if (cachedCamerasInfo == null) {
            synchronized (this) {
                if (cachedCamerasInfo == null) {
                    cachedCamerasInfo = initCameraInfo(context.getApplicationContext());
                }
            }
        }

        return cachedCamerasInfo;
    }

    @Nullable
    private List<CameraDesc> initCameraInfo(Context context) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return buildLollipopCameraInfo(context);
        } else {
            return buildJellybeanCameraInfo();
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Nullable
    private List<CameraDesc> buildLollipopCameraInfo(@NonNull Context context) {

        final CameraManager cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);

        try {

            final String[] cameraIdList = cameraManager.getCameraIdList();
            final List<CameraDesc> cameraDescList = new ArrayList<>(cameraIdList.length);
            CameraCharacteristics cameraCharacteristics;
            for (String cameraId : cameraIdList) {
                cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId);
                final Integer facing = cameraCharacteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing != null) {
                    cameraDescList.add(new CameraDesc(
                            cameraId,
                            facing == CameraCharacteristics.LENS_FACING_FRONT ? (CameraDesc.FACING_FRONT) :
                                    facing == CameraCharacteristics.LENS_FACING_BACK ? CameraDesc.FACING_BACK : CameraDesc.FACING_EXTERNAL
                    ));
                }
            }
            return cameraDescList;

        } catch (CameraAccessException e) {
            return null;
        }
    }

    @Nullable
    private List<CameraDesc> buildJellybeanCameraInfo() {

        final int numCameras = Camera.getNumberOfCameras();
        final List<CameraDesc> cameraDescList = new ArrayList<>(numCameras);
        final Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        for (int cameraIndex = 0; cameraIndex < Camera.getNumberOfCameras(); cameraIndex++) {

            Camera.getCameraInfo(cameraIndex, cameraInfo);
            cameraDescList.add(new CameraDesc(String.valueOf(cameraIndex), cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT ? CameraDesc.FACING_FRONT : CameraDesc.FACING_BACK));
        }
        return cameraDescList;
    }

    public static final class CameraDesc {

        public static final int FACING_FRONT = 1;
        public static final int FACING_BACK = 2;
        public static final int FACING_EXTERNAL = 3;
        public final String cameraId;

        @Facing
        public final int facing;

        private CameraDesc(@NonNull String cameraId, @Facing int facing) {
            this.cameraId = cameraId;
            this.facing = facing;
        }

        @IntDef({FACING_FRONT, FACING_BACK, FACING_EXTERNAL})
        @interface Facing {
        }


    }
}
