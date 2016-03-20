package com.vinaysshenoy.obscura;


import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.view.WindowManager;

/**
 * Created by vinaysshenoy on 19/03/16.
 */
public final class Utils {

    private static final String TAG = "Utils";

    private Utils() {

    }

    public static boolean doesDeviceSupportCameras(@NonNull Context context) {

        final PackageManager packageManager = context.getPackageManager();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            return packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY);
        } else {
            return packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA) || packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT);
        }
    }

    public static boolean isCameraPermissionGranted(@NonNull Context context) {

        return ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED;
    }

    public static WindowManager windowManagerFromContext(@NonNull Context context) {

        if(context instanceof Activity) {
            return ((Activity) context).getWindowManager();
        }

        return (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
    }

}
