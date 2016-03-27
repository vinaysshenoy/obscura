package com.vinaysshenoy.obscura.sample;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;

import com.vinaysshenoy.obscura.ObscuraUtils;
import com.vinaysshenoy.obscura.ObscuraView;
import com.vinaysshenoy.obscura.camera.controllers.BaseController;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private static final int PERM_CAMERA = 100;

    private ObscuraView obscuraView;

    private BaseController controller;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        obscuraView = (ObscuraView) findViewById(R.id.obscura);
        controller = obscuraView.controller();

    }

    @Override
    protected void onResume() {
        super.onResume();

        if (ObscuraUtils.isCameraPermissionGranted(this)) {
            controller.startPreview();
        } else {
            requestCameraPermission();
        }
    }

    private void requestCameraPermission() {

        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, PERM_CAMERA);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERM_CAMERA) {

            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                controller.startPreview();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (ObscuraUtils.isCameraPermissionGranted(this)) {
            controller.stopPreview();
        }
    }
}
