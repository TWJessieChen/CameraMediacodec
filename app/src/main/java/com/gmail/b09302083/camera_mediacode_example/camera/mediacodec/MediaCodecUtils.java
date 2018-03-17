package com.gmail.b09302083.camera_mediacode_example.camera.mediacodec;

import com.gmail.b09302083.camera_mediacode_example.camera.utils.EncoderDebugger;
import com.gmail.b09302083.camera_mediacode_example.camera.utils.VideoQuality;

import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaFormat;

import java.io.IOException;

/**
 * Created by b09302083 on 2018/3/17.
 */
public class MediaCodecUtils {

    private static final String TAG = "MediaCodecUtils";

    private Context mContext;

    private MediaCodec mMediaCodec;

    public static class Builder {
        private MediaCodecUtils mMediaCodecUtils = new MediaCodecUtils();

        public Builder(Context context) {
            mMediaCodecUtils.mContext = context;
        }

        public MediaCodecUtils build() {
            return mMediaCodecUtils;
        }

    }

    public MediaCodec createMediaCodec(EncoderDebugger debugger, VideoQuality mQuality)
            throws IOException {
        mMediaCodec = MediaCodec.createByCodecName(debugger.getEncoderName());
        MediaFormat mediaFormat = MediaFormat.createVideoFormat("video/avc", mQuality.resX, mQuality.resY);
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, mQuality.bitrate);
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, mQuality.framerate);
        mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT,debugger.getEncoderColorFormat());
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
        mMediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);

        return mMediaCodec;
    }

    public void start() {
        mMediaCodec.start();
    }

    public void stop() {
        mMediaCodec.stop();
        mMediaCodec.release();
        mMediaCodec = null;
    }
}
