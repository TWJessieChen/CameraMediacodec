package com.gmail.b09302083.camera_mediacode_example.camera;

import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by lzp on 2017/2/12.
 */

public class CameraManager {
    private static final String TAG = CameraManager.class.getSimpleName();
    public static CameraManager sInstance = null;
    private Camera mCamera;
    private int mBackCameraID, mFrontCameraID;
    private int mCameraID;
    private boolean mIsPreviewing;
    private boolean mIsRecord;
    private byte[] mImageCallbackBuffer;
    private CameraPreviewCallback mCameraPreviewCallback;
    private Camera.Size mPreviewSize;

    private CameraManager() {
        int nums = Camera.getNumberOfCameras();
        Camera.CameraInfo info = new Camera.CameraInfo();
        for (int i = 0; i < nums; i++) {
            Camera.getCameraInfo(i, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                mBackCameraID = i;
            } else if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                mFrontCameraID = i;
            }
        }
    }

    public static CameraManager getInstance() {
        if (sInstance == null) {
            sInstance = new CameraManager();
        }
        return sInstance;
    }

    public int getCameraID() {
        return mCameraID;
    }

    public Camera getCamera() {
        return mCamera;
    }

    public void setRecord(boolean record) {
        mIsRecord = record;
    }

    public Camera.Size getPreviewSize() {
        return mPreviewSize;
    }

    /**
     * @param cameraId true后置相机，false前置相机
     * @return
     */
    public void openCamera(boolean cameraId) {
        mCameraID = cameraId ? mBackCameraID : mFrontCameraID;
        mCamera = Camera.open(mCameraID);
        if (mCamera == null) {
            mCamera = Camera.open();
        }
        if (mCamera == null) {
            throw new RuntimeException("Open camera error");
        }
    }

    public void startPreview(int rotation, SurfaceHolder surfaceHolder, int preViewWidth, int preViewHeight) {
        if (mIsPreviewing) {
            mCamera.stopPreview();
            return;
        }
        if (mCamera != null) {
            try {
                Log.e(TAG, "startPreview width=" + preViewWidth + " height=" + preViewHeight);
                mCamera.setPreviewDisplay(surfaceHolder);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            initCamera(rotation, preViewWidth, preViewHeight);
        }
    }

    private void initCamera(int rotation, int previewWidth, int preViewHeight) {
        if (mCamera != null) {
            Camera.Parameters parameters = mCamera.getParameters();
            List<Camera.Size> supportedPreviewSizes = parameters.getSupportedPreviewSizes();
            mPreviewSize = getOptimalPreviewSize(supportedPreviewSizes, Math
                    .max(preViewHeight, previewWidth), Math.min(preViewHeight, previewWidth));
            Log.e(TAG, "preview width=" + mPreviewSize.width + " height=" + mPreviewSize.height);

            try {
                parameters.setPreviewFormat(ImageFormat.YV12);
//                parameters.setFlashMode("off");
//                parameters.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_AUTO);
//                parameters.setSceneMode(Camera.Parameters.SCENE_MODE_AUTO);
                parameters.setPreviewSize(mPreviewSize.width, mPreviewSize.height);
                parameters.setRecordingHint(true);
                mCamera.setParameters(parameters);
            } catch (Exception e) {
                e.printStackTrace();
            }
            //这个单独处理，因为有些手机设置自动聚焦会失败，如果放在上面，会导致其他设置也都失败.
            try {
                parameters = mCamera.getParameters();
                //录制时使用系统设置自动聚焦
                if (mIsRecord) {
                    List<String> supportedFocusModes = parameters.getSupportedFocusModes();
                    if (supportedFocusModes.contains(Camera.Parameters
                            .FOCUS_MODE_CONTINUOUS_VIDEO)) {
                        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
                        mCamera.setParameters(parameters);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (mIsRecord) {
                mCameraPreviewCallback = new CameraPreviewCallback(mPreviewSize.width, mPreviewSize.height);
                mImageCallbackBuffer = new byte[mPreviewSize.width * mPreviewSize.height * 3 / 2];
                mCamera.addCallbackBuffer(mImageCallbackBuffer);
                mCamera.setPreviewCallbackWithBuffer(mCameraPreviewCallback);
            }

            mCamera.setDisplayOrientation(getCameraDisplayOrientation(rotation, mCameraID));
            mCamera.startPreview();
            mIsPreviewing = true;
        }
    }

    public int getCameraDisplayOrientation(int rotation, int cameraId) {
        android.hardware.Camera.CameraInfo info =
                new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(cameraId, info);

        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        return result;
    }

    private Camera.Size getOptimalPreviewSize(List<Camera.Size> supportedPreviewSizes, int w, int h) {
        final double ASPECT_TOLERANCE = 0.1;
        double targetRation = (double) w / h;
        Camera.Size optimalSize = null;
//        Log.e(TAG, "targetRation=" + targetRation);
        double minDiff = Double.MAX_VALUE;
        int targetHeight = h;
        List<Camera.Size> filterPreviewSize = getFilterSize(supportedPreviewSizes, false);
        for (Camera.Size size : filterPreviewSize) {
            double ratio = (double) size.width / size.height;
//            Log.e(TAG, "ratio=" + ratio + " width = " + size.width + " height=" + size.height);
            if (Math.abs(ratio - targetRation) > ASPECT_TOLERANCE) {
                continue;
            }
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }
        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : filterPreviewSize) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }

    /* 获取大部分手机支持的分辨率比例 4：3 或 16：9 (将一些奇葩比例的分辨率给过滤掉)，
            * 如果没有正常比例（4：3或16：9）的分辨率，则不进行过滤
    * 在录制视频时，因video尺寸是4：3的，所以只要4：3的尺寸，因为有些手机如果不设置同样的尺寸，点击录制时会变形
    */
    public List<Camera.Size> getFilterSize(List<Camera.Size> orignSize, boolean isOnly43) {
        List<Camera.Size> filterSizes = new ArrayList<>();
        for (Camera.Size size : orignSize) {
            double ratio = (double) size.height / size.width;
            if (ratio == 0.75) {//4:3
                filterSizes.add(size);
            }
            if (!isOnly43 && ratio == 0.5625f) {//16:9
                filterSizes.add(size);
            }
        }
        if (filterSizes.size() == 0) {
            return orignSize;
        }
        return filterSizes;
    }

    public void stopCamera() {
        mIsPreviewing = false;
        mIsRecord = false;
        if (mCameraPreviewCallback != null) {
            mCameraPreviewCallback.close();
        }
        if (mCamera != null) {
            mCamera.setPreviewCallback(null);
            mCamera.setPreviewCallbackWithBuffer(null);
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    class CameraPreviewCallback implements Camera.PreviewCallback {
        private AVcodec mAVCodec = null;

        private CameraPreviewCallback(int width, int height) {
            mAVCodec = new AVcodec(width, height);
        }

        void close() {
            mAVCodec.close();
        }

        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {
//            Log.e(TAG, "onPreviewFrame");
            mAVCodec.encodeFrame(data);
            camera.addCallbackBuffer(data);
        }
    }
}
