package com.gmail.b09302083.camera_mediacode_example.camera;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by lzp on 2017/2/12.
 */

public class AVcodec {
    private static final String TAG = AVcodec.class.getSimpleName();
    private static final String MIME_TYPE = "video/avc"; // H.264 Advanced Video
    private static final int FRAME_RATE = 25; // 15fps
    private static final int IFRAME_INTERVAL = FRAME_RATE; // 10 between

    private static final int TIMEOUT_USEC = 10000;
    private static final int COMPRESS_RATIO = 256;
    private static int BIT_RATE;

    private int mWidth;
    private int mHeight;
    private MediaCodec mMediaCodec;
    private MediaMuxer mMuxer;
    private MediaCodec.BufferInfo mBufferInfo;
    private int mTrackIndex = -1;
    private boolean mMuxerStarted;
    byte[] mFrameData;
    FileOutputStream mFileOutputStream = null;
    private int mColorFormat;
    private long mStartTime = 0;

    public AVcodec(int width, int height) {
        this.mWidth = width;
        this.mHeight = height;
        mFrameData = new byte[this.mWidth * this.mHeight * 3 / 2];
        BIT_RATE = this.mHeight * this.mWidth * 3 * 8 * FRAME_RATE / COMPRESS_RATIO;

        mBufferInfo = new MediaCodec.BufferInfo();
        MediaCodecInfo codecInfo = selectCodec(MIME_TYPE);
        if (codecInfo == null) {
            throw new RuntimeException("unable to find an appropriate codec for " + MIME_TYPE);
        }
        Log.e(TAG, "found codec " + codecInfo.getName());
        mColorFormat = selectColorFormat(codecInfo, MIME_TYPE);
        Log.e(TAG, "found colorFormat:" + mColorFormat);
        MediaFormat mediaFormat = MediaFormat.createVideoFormat(MIME_TYPE, this.mWidth, this.mHeight);
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE);
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE);
        mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, mColorFormat);
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, IFRAME_INTERVAL);
        Log.e(TAG, "format:" + mediaFormat);
        try {
            mMediaCodec = MediaCodec.createByCodecName("OMX.google.h264.encoder");
//            mMediaCodec = MediaCodec.createByCodecName(codecInfo.getName());
            mMediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            mMediaCodec.start();

            String fileName = Environment.getExternalStorageDirectory().getAbsolutePath().toString() + "/" + this.mWidth + "x" + this.mHeight + ".h264";


            try {
                File file = new File(fileName);
//            mFileOutputStream = mContext.openFileOutput(fileRawData,Context.MODE_WORLD_READABLE);

                if (!file.exists()) {
                    file.createNewFile();
                }

                mFileOutputStream = new FileOutputStream(file);

            } catch (IOException e) {
                System.out.println(e);
            } catch (Exception e) {
                System.out.println(e);
            }

            mStartTime = System.nanoTime();

//            mMuxer = new MediaMuxer(fileName.toString(), MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
//        mTrackIndex = -1;
//        mMuxerStarted = false;
    }

    public void encodeFrame(byte[] input) {
//        mFrameData = input;
//        NV21toI420SemiPlanar(input, mFrameData, this.mWidth, this.mHeight);
//        mFrameData = swapYV12toI420(input, this.mWidth, this.mHeight);

        ByteBuffer[] inputBuffers = mMediaCodec.getInputBuffers();
        ByteBuffer[] outputBuufers = mMediaCodec.getOutputBuffers();
        int inputBufferIndex = mMediaCodec.dequeueInputBuffer(TIMEOUT_USEC);
        if (inputBufferIndex >= 0) {
            ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
            inputBuffer.clear();
            inputBuffer.put(input);
//            inputBuffer.put(mFrameData);
            mMediaCodec.queueInputBuffer(inputBufferIndex, 0, input.length, System.nanoTime() / 1000, 0);
        } else {
            Log.e(TAG, "input buffer not available");
        }

        int outputBufferIndex = mMediaCodec.dequeueOutputBuffer(mBufferInfo, TIMEOUT_USEC);
        do {
            if (outputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                Log.e(TAG, "no output from encoder available");
            } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                outputBuufers = mMediaCodec.getOutputBuffers();
                Log.e(TAG, "encoder output buffers changed");
            } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                MediaFormat newFormat = mMediaCodec.getOutputFormat();
                Log.e(TAG, "encoder output format changed: " + newFormat);

//                mTrackIndex = mMuxer.addTrack(newFormat);
//                mMuxer.start();
//                mMuxerStarted = true;
            } else if (outputBufferIndex < 0) {
                Log.e(TAG, "unexpected result from encoder.dequeueOutputBuffer: " +
                        outputBufferIndex);
            } else {
                Log.d(TAG, "perform encoding");
                ByteBuffer outputBuffer = outputBuufers[outputBufferIndex];
                if (outputBuffer == null) {
                    throw new RuntimeException("encoderOutputBuffer " + outputBufferIndex +
                            " was null");
                }

                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    mBufferInfo.size = 0;
                }

                if (mBufferInfo.size != 0) {
//                    if (!mMuxerStarted) {
//                        MediaFormat newFormat = mMediaCodec.getOutputFormat();
//                        mTrackIndex = mMuxer.addTrack(newFormat);
//                        mMuxer.start();
//                        mMuxerStarted = true;
//                    }

                    outputBuffer.position(mBufferInfo.offset);
                    outputBuffer.limit(mBufferInfo.offset + mBufferInfo.size);

                    int remainingSize = outputBuffer.remaining();
                    byte[] frameByteData  = new byte[remainingSize];

                    outputBuffer.get(frameByteData, 0,  remainingSize);

                    if(frameByteData != null) {
                        try {
                            mFileOutputStream.write(frameByteData);
                            mFileOutputStream.flush();
                            Log.i(TAG, "output data size -- > " + frameByteData.length);
                        } catch (IOException ioe) {
                            Log.w(TAG, "failed writing debug data to file");
                            throw new RuntimeException(ioe);
                        }
                    } else {
                        Log.e(TAG, "outData == null!!!");
                    }


//                    mMuxer.writeSampleData(mTrackIndex, outputBuffer, mBufferInfo);
                }
                mMediaCodec.releaseOutputBuffer(outputBufferIndex, false);
            }
            outputBufferIndex = mMediaCodec.dequeueOutputBuffer(mBufferInfo, TIMEOUT_USEC);
        } while (outputBufferIndex >= 0);
    }

    public void close() {

        try {
            mFileOutputStream.close();
        } catch (IOException e) {
            System.out.println(e);
        } catch (Exception e) {
            System.out.println(e);
        }

        if (mMediaCodec != null) {
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

    public byte[] swapYV12toI420(byte[] yv12bytes, int width, int height) {
        byte[] i420bytes = new byte[yv12bytes.length];
        for (int i = 0; i < width * height; i++)
            i420bytes[i] = yv12bytes[i];
        for (int i = width * height; i < width * height + (width / 2 * height / 2); i++)
            i420bytes[i] = yv12bytes[i + (width / 2 * height / 2)];
        for (int i = width * height + (width / 2 * height / 2); i < width * height + 2 * (width / 2 * height / 2); i++)
            i420bytes[i] = yv12bytes[i - (width / 2 * height / 2)];
        return i420bytes;
    }

    /**
     * NV21 is a 4:2:0 YCbCr, For 1 NV21 pixel: YYYYYYYY VUVU I420YUVSemiPlanar
     * is a 4:2:0 YUV, For a single I420 pixel: YYYYYYYY UVUV Apply NV21 to
     * I420YUVSemiPlanar(NV12) Refer to https://wiki.videolan.org/YUV/
     */
    private void NV21toI420SemiPlanar(byte[] nv21bytes, byte[] i420bytes,
                                      int width, int height) {
        System.arraycopy(nv21bytes, 0, i420bytes, 0, width * height);
        for (int i = width * height; i < nv21bytes.length; i += 2) {
            i420bytes[i] = nv21bytes[i + 1];
            i420bytes[i + 1] = nv21bytes[i];
        }
    }

    private static int selectColorFormat(MediaCodecInfo codecInfo, String mimeType) {
        MediaCodecInfo.CodecCapabilities capabilities = codecInfo.getCapabilitiesForType(mimeType);
        for (int i = 0; i < capabilities.colorFormats.length; i++) {
            int colorFormat = capabilities.colorFormats[i];
            if (isRecognizedFormat(colorFormat)) {
                return colorFormat;
            }
        }
        Log.e(TAG, "couldn't find a good color format for " + codecInfo.getName() + "/" + mimeType);
        return 0;
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
}
