package com.vinaysshenoy.obscura;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.vinaysshenoy.obscura.camera.controllers.BaseController;
import com.vinaysshenoy.obscura.camera.controllers.jellybean.CameraControllerJellybean;
import com.vinaysshenoy.obscura.camera.controllers.lollipop.CameraControllerLollipop;

/**
 * Created by vinaysshenoy on 19/03/16.
 */
public class ObscuraView extends FrameLayout {

    private AutoFitTextureView textureView;

    private BaseController controller;

    public ObscuraView(Context context) {
        super(context);
    }

    public ObscuraView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ObscuraView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public ObscuraView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    private void init(Context context, AttributeSet attrs) {
        addTextureView(context);
        initController();
    }

    private void initController() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            controller = new CameraControllerLollipop(textureView);
        } else {
            controller = new CameraControllerJellybean(textureView);
        }
    }

    private void addTextureView(Context context) {
        textureView = new AutoFitTextureView(context);
        addView(textureView, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    /**
     * Sets the aspect ratio for the camera view. The size of the view will be measured based on the ratio
     * calculated from the parameters. Note that the actual sizes of parameters don't matter, that
     * is, calling setAspectRatio(2, 3) and setAspectRatio(4, 6) make the same result.
     *
     * @param width  Relative horizontal size
     * @param height Relative vertical size
     */
    public void setAspectRatio(int width, int height) {
        textureView.setAspectRatio(width, height);
        requestLayout();
    }
}
