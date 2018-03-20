package com.gmail.b09302083.camera_mediacode_example.camera.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * Created by b09302083 on 2018/3/17.
 */
public class MediaCodeTest {
    private static final String TAG = "MediaCodeTest";

    protected static VideoQuality mRequestedQuality = null;
    protected static VideoQuality mQuality = null;
    protected static SharedPreferences mSettings = null;
    protected static NV21Convertor n21Convertor = null;
    protected static EncoderDebugger debugger = null;

    private Context mContext;

    private Thread mProcessingThread;
    private FrameProcessingRunnable mFrameProcessor;

    public static class Builder {
        private MediaCodeTest mMediaCodeTest = new MediaCodeTest();


        public Builder(Context context) {
            mMediaCodeTest.mContext = context;
            mMediaCodeTest.mSettings = PreferenceManager.getDefaultSharedPreferences(context);
        }


        public MediaCodeTest build(int resX, int resY, int framerate, int bitrate) {
            mMediaCodeTest.mFrameProcessor = mMediaCodeTest.new FrameProcessingRunnable();

            mRequestedQuality = new VideoQuality(resX, resY, framerate, bitrate);
            mQuality = mRequestedQuality.clone();

            if(mRequestedQuality == null) {
                mRequestedQuality = VideoQuality.DEFAULT_VIDEO_QUALITY.clone();
                mQuality = mRequestedQuality.clone();
            }

            return mMediaCodeTest;
        }

    }

    public void onStartTest() {
        mProcessingThread = new Thread(mFrameProcessor);
        mProcessingThread.start();
    }

    public VideoQuality getQuality() {
        return mQuality;
    }

    public SharedPreferences getSettings() {
        return mSettings;
    }

    public EncoderDebugger getDebugger() {
        return debugger;
    }

    public NV21Convertor getN21Convertor() {
        return n21Convertor;
    }

    private class FrameProcessingRunnable implements Runnable {

        @Override
        public void run() {
            Log.d(TAG,"FrameProcessingRunnable run.");
            debugger = EncoderDebugger.debug(mSettings, mQuality.resX, mQuality.resY);
            n21Convertor = debugger.getNV21Convertor();
            Log.d(TAG,"FrameProcessingRunnable end.");
        }
    }


}
