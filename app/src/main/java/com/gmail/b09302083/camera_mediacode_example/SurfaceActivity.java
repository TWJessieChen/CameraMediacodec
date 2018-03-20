package com.gmail.b09302083.camera_mediacode_example;

import com.gmail.b09302083.camera_mediacode_example.camera.CameraManager;

import android.app.Activity;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup;

/**
 * Created by lzp on 2017/2/11.
 */

public class SurfaceActivity extends Activity implements SurfaceHolder.Callback {
    private SurfaceView mPreview;
    private SurfaceHolder mHolder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.surface_activity);
        mPreview = (SurfaceView) findViewById(R.id.surface);
        mPreview.getHolder().addCallback(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        CameraManager.getInstance().openCamera(true);
        CameraManager.getInstance().setRecord(true);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mHolder = holder;
        startCamera();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.e("Test","surfaceChanged");

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        mHolder = null;
        CameraManager.getInstance().stopCamera();
    }

    public void startCamera() {
        if (mHolder != null) {
            Log.e("Test","startCamera");
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            CameraManager.getInstance().startPreview(rotation, mHolder, mPreview.getWidth(), mPreview.getHeight());
            updatePreviewLayout(mPreview.getWidth(), mPreview.getHeight());
        }
    }

    //设置预览Frame的尺寸，包含preview和遮罩层
    private void updatePreviewLayout(int width, int height) {
        ViewGroup.LayoutParams params = mPreview.getLayoutParams();
        Camera.Size previewSize = CameraManager.getInstance().getPreviewSize();
        double w = previewSize.width;
        double h = previewSize.height;
        //保证surface的尺寸与预览尺寸比例一致，另需注意预览尺寸是w>h，所以计算比例是w/h
        //换算时因小数点可能会存在误差，这里采用Math.round()四舍五入取整
        if (width > height) {
            params.width = Math.round((float) (height * w / h));
            params.height = height;
//            if (params.width > width) {
//                params.width = width;
//                params.height = Math.round((float) (width * h / w));
//            }
        } else {
            params.width = width;
            params.height = Math.round((float) (width * w / h));
//            if (params.height > height) {
//                params.height = height;
//                params.width = Math.round((float) (height * h / w));
//            }
        }
        mPreview.setLayoutParams(params);
    }
}
