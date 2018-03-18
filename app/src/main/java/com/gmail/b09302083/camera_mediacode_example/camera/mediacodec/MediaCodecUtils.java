package com.gmail.b09302083.camera_mediacode_example.camera.mediacodec;

import com.gmail.b09302083.camera_mediacode_example.camera.utils.EncoderDebugger;
import com.gmail.b09302083.camera_mediacode_example.camera.utils.VideoQuality;

import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Environment;
import android.util.Log;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by b09302083 on 2018/3/17.
 */
public class MediaCodecUtils {

    private static final String TAG = "MediaCodecUtils";

    private static final int TIMEOUT_USEC = 10000;
    private static final String MIME_TYPE = "video/avc"; // H.264 Advanced Video

    private Context mContext;

    private MediaCodec mMediaCodec;

    private int mColorFormat;
    private static final int FRAME_RATE = 25; // 15fps
    private static final int IFRAME_INTERVAL = FRAME_RATE; // 10 between
    private static final int COMPRESS_RATIO = 256;
    private int BIT_RATE = 0;

    private long mStartTime = 0;
    private MediaMuxer mMuxer;
    private MediaCodec.BufferInfo mBufferInfo;
    private int mTrackIndex = -1;
    private boolean mMuxerStarted;
    private byte[] mFrameData;
    private FileOutputStream mFileOutputStream = null;
    private int width;
    private int height;

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

        mFrameData = new byte[mQuality.resX * mQuality.resY * 3 / 2];
        width = mQuality.resX;
        height = mQuality.resY;
        mBufferInfo = new MediaCodec.BufferInfo();

        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, mQuality.bitrate);
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, mQuality.framerate);
        mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT,debugger.getEncoderColorFormat());
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
        mMediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);

        return mMediaCodec;
    }

    public MediaCodec createMediaCodec(int width, int height)
            throws IOException {

        this.width = width;
        this.height = height;

        MediaCodecInfo codecInfo = selectCodec(MIME_TYPE);
        if (codecInfo == null) {
            // Don't fail CTS if they don't have an AVC codec (not here,
            // anyway).
            Log.e(TAG, "Unable to find an appropriate codec for " + MIME_TYPE);
            return null;
        }
        Log.e(TAG, "found codec: " + codecInfo.getName());
        mColorFormat = selectColorFormat(codecInfo, MIME_TYPE);

        Log.e(TAG, "found colorFormat: " + mColorFormat);
        MediaFormat mediaFormat = MediaFormat.createVideoFormat(MIME_TYPE,
                this.width, this.height);

        BIT_RATE = this.height * this.width * 3 * 8 * FRAME_RATE / COMPRESS_RATIO;

        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE);
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE);
        mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, mColorFormat);
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL,
                IFRAME_INTERVAL);
        Log.e(TAG, "format: " + mediaFormat);

        mMediaCodec = MediaCodec.createByCodecName(codecInfo.getName());
        mMediaCodec.configure(mediaFormat, null, null,
                MediaCodec.CONFIGURE_FLAG_ENCODE);

        mFrameData = new byte[this.width * this.height * 3 / 2];
        mBufferInfo = new MediaCodec.BufferInfo();

        return mMediaCodec;
    }

    public void encodeFrame(byte[] input/* , byte[] output */) {
        Log.i(TAG, "encodeFrame()");
        long encodedSize = 0;
//        NV21toI420SemiPlanar(input, mFrameData, this.width, this.height);

        ByteBuffer[] inputBuffers = mMediaCodec.getInputBuffers();
        ByteBuffer[] outputBuffers = mMediaCodec.getOutputBuffers();
        int inputBufferIndex = mMediaCodec.dequeueInputBuffer(TIMEOUT_USEC);
        Log.i(TAG, "inputBufferIndex-->" + inputBufferIndex);
        if (inputBufferIndex >= 0) {
            long endTime = System.nanoTime();
            long ptsUsec = (endTime - mStartTime) / 1000;
            Log.i(TAG, "resentationTime: " + ptsUsec);
            ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
            inputBuffer.clear();
            inputBuffer.put(mFrameData);
            mMediaCodec.queueInputBuffer(inputBufferIndex, 0,
                    mFrameData.length, System.nanoTime() / 1000, 0);
        } else {
            // either all in use, or we timed out during initial setup
           Log.d(TAG, "input buffer not available");
        }

        int outputBufferIndex = mMediaCodec.dequeueOutputBuffer(mBufferInfo, TIMEOUT_USEC);
        Log.i(TAG, "outputBufferIndex-->" + outputBufferIndex);
        do {
            if (outputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                // no output available yet
                Log.d(TAG, "no output from encoder available");
            } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                // not expected for an encoder
                outputBuffers = mMediaCodec.getOutputBuffers();
                Log.d(TAG, "encoder output buffers changed");
            } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                // not expected for an encoder

                MediaFormat newFormat = mMediaCodec.getOutputFormat();
                Log.d(TAG, "encoder output format changed: " + newFormat);

                // now that we have the Magic Goodies, start the muxer
                mTrackIndex = mMuxer.addTrack(newFormat);
                mMuxer.start();
                mMuxerStarted = true;
            } else if (outputBufferIndex < 0) {
                Log.w(TAG, "unexpected result from encoder.dequeueOutputBuffer: " +
                        outputBufferIndex);
                // let's ignore it
            } else {
//                if (VERBOSE)
                Log.d(TAG, "perform encoding");
                ByteBuffer outputBuffer = outputBuffers[outputBufferIndex];
                if (outputBuffer == null) {
                    throw new RuntimeException("encoderOutputBuffer " + outputBufferIndex +
                            " was null");
                }

                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    // The codec config data was pulled out and fed to the muxer when we got
                    // the INFO_OUTPUT_FORMAT_CHANGED status.  Ignore it.
                    Log.d(TAG, "ignoring BUFFER_FLAG_CODEC_CONFIG");
                    mBufferInfo.size = 0;
                }

                if (mBufferInfo.size != 0) {
                    if (!mMuxerStarted) {
//						throw new RuntimeException("muxer hasn't started");
                        MediaFormat newFormat = mMediaCodec.getOutputFormat();
                        mTrackIndex = mMuxer.addTrack(newFormat);
                        mMuxer.start();
                        mMuxerStarted = true;
                    }

                    // adjust the ByteBuffer values to match BufferInfo (not needed?)
                    outputBuffer.position(mBufferInfo.offset);
                    outputBuffer.limit(mBufferInfo.offset + mBufferInfo.size);

//					write raw data
//					byte[] outData = new byte[bufferInfo.size];
//					outputBuffer.get(outData);
//					outputBuffer.position(bufferInfo.offset);

//					try {
//						mFileOutputStream.write(outData);
//						Log.i(TAG, "output data size -- > " + outData.length);
//					} catch (IOException ioe) {
//						Log.w(TAG, "failed writing debug data to file");
//						throw new RuntimeException(ioe);
//					}
                    mMuxer.writeSampleData(mTrackIndex, outputBuffer, mBufferInfo);
                    Log.d(TAG, "sent " + mBufferInfo.size + " bytes to muxer");

                }

                mMediaCodec.releaseOutputBuffer(outputBufferIndex, false);
            }
            outputBufferIndex = mMediaCodec.dequeueOutputBuffer(mBufferInfo, TIMEOUT_USEC);
        } while (outputBufferIndex >= 0);
    }

    private static MediaCodecInfo selectCodec(String mimeType) {
        int numCodecs = MediaCodecList.getCodecCount();
        for (int i = 0; i < numCodecs; i++) {
            MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);
            if (!codecInfo.isEncoder()) {
                continue;
            }
            String[] types = codecInfo.getSupportedTypes();
            for (int j = 0; j < types.length; j++) {
                if (types[j].equalsIgnoreCase(mimeType)) {
                    return codecInfo;
                }
            }
        }
        return null;
    }

    private static int selectColorFormat(MediaCodecInfo codecInfo,
            String mimeType) {
        MediaCodecInfo.CodecCapabilities capabilities = codecInfo
                .getCapabilitiesForType(mimeType);
        for (int i = 0; i < capabilities.colorFormats.length; i++) {
            int colorFormat = capabilities.colorFormats[i];
            if (isRecognizedFormat(colorFormat)) {
                return colorFormat;
            }
        }
        Log.e(TAG,
                "couldn't find a good color format for " + codecInfo.getName()
                        + " / " + mimeType);
        return 0; // not reached
    }

    /**
     * Returns true if this is a color format that this test code understands
     * (i.e. we know how to read and generate frames in this format).
     */
    private static boolean isRecognizedFormat(int colorFormat) {
        switch (colorFormat) {
            // these are the formats we know how to handle for this test
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar:
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedPlanar:
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar:
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedSemiPlanar:
            case MediaCodecInfo.CodecCapabilities.COLOR_TI_FormatYUV420PackedSemiPlanar:
                return true;
            default:
                return false;
        }
    }

    private void NV21toI420SemiPlanar(byte[] nv21bytes, byte[] i420bytes,
            int width, int height) {
        System.arraycopy(nv21bytes, 0, i420bytes, 0, width * height);
        for (int i = width * height; i < nv21bytes.length; i += 2) {
            i420bytes[i] = nv21bytes[i + 1];
            i420bytes[i + 1] = nv21bytes[i];
        }
    }

    public void start() {
        mMediaCodec.start();
        String fileName = Environment.getExternalStorageDirectory().getAbsolutePath().toString() + "/" + this.width + "x"
                + this.height + ".mp4";
        try {
            mMuxer = new MediaMuxer(fileName.toString(),
                    MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        } catch (IOException e) {
            e.printStackTrace();
        }

        mTrackIndex = -1;
        mMuxerStarted = false;

    }

    public void stop() {
        if(mMediaCodec != null) {
            mMediaCodec.stop();
            mMediaCodec.release();
            mMediaCodec = null;
        }

        if (mMuxer != null) {
            mMuxer.stop();
            mMuxer.release();
            mMuxer = null;
        }

    }
}
