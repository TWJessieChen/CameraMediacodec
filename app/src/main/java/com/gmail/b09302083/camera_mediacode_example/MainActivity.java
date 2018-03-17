package com.gmail.b09302083.camera_mediacode_example;


import com.gmail.b09302083.camera_mediacode_example.camera.CameraSource;
import com.gmail.b09302083.camera_mediacode_example.camera.CameraSourcePreview;
import com.gmail.b09302083.camera_mediacode_example.camera.utils.MediaCodeTest;

import android.hardware.Camera;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private MediaCodeTest mMediaCodeTest;

    private CameraSource mCameraSource;
    private CameraSourcePreview mPreview;

    private boolean autoFocus = true;
    private boolean useFlash = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MediaCodeTest.Builder builder = new MediaCodeTest.Builder(getApplicationContext());

        mMediaCodeTest = builder.build();
        mMediaCodeTest.onStartTest();

        mPreview = (CameraSourcePreview) findViewById(R.id.preview);

        createCameraSource(autoFocus, useFlash);
    }


    private void createCameraSource(boolean autoFocus, boolean useFlash) {
        CameraSource.Builder builder = new CameraSource.Builder(getApplicationContext())
                .setFacing(CameraSource.CAMERA_FACING_BACK)
                .setRequestedPreviewSize(1600, 1024)
                .setRequestedFps(15.0f)
                .setMediaCodexTestClass(mMediaCodeTest);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            builder = builder.setFocusMode(
                    autoFocus ? Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE : null);
        }

        mCameraSource = builder
                .setFlashMode(useFlash ? Camera.Parameters.FLASH_MODE_TORCH : null)
                .build();

    }

    private void startCameraSource() throws SecurityException {
        // check that the device has play services available.
//        int code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(
//                getApplicationContext());
//        if (code != ConnectionResult.SUCCESS) {
//            Dialog dlg =
//                    GoogleApiAvailability.getInstance().getErrorDialog(this, code, RC_HANDLE_GMS);
//            dlg.show();
//        }

        if (mCameraSource != null) {
            try {
                mPreview.start(mCameraSource);
            } catch (IOException e) {
                Log.e(TAG, "Unable to start camera source.", e);
                mCameraSource.release();
                mCameraSource = null;
            }
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        startCameraSource();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mPreview != null) {
            mPreview.stop();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mPreview != null) {
            mPreview.release();
        }
    }
}
