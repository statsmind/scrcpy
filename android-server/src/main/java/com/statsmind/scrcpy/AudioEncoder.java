package com.statsmind.scrcpy;

import android.annotation.SuppressLint;
import android.media.*;
import com.statsmind.scrcpy.wrappers.MediaProjectionManager;

import java.nio.ByteBuffer;

import static android.content.Context.MEDIA_PROJECTION_SERVICE;

public class AudioEncoder {
    public static interface OnAudioEncodeListener {
        void onAudioEncode(ByteBuffer bb, MediaCodec.BufferInfo bi);
    }

    private MediaCodec mMediaCodec;
    private OnAudioEncodeListener mListener;
    private AudioSettings mAudioConfiguration;
    MediaCodec.BufferInfo mBufferInfo = new MediaCodec.BufferInfo();

    public void setOnAudioEncodeListener(OnAudioEncodeListener listener) {
        mListener = listener;
    }

    public AudioEncoder(AudioSettings audioConfiguration) {
        mAudioConfiguration = audioConfiguration;
    }

    void prepareEncoder() {
        mMediaCodec = getAudioMediaCodec(mAudioConfiguration);
        mMediaCodec.start();
    }

    synchronized public void stop() {
        if (mMediaCodec != null) {
            mMediaCodec.stop();
            mMediaCodec.release();
            mMediaCodec = null;
        }
    }

    synchronized void offerEncoder(byte[] input) {
        if(mMediaCodec == null) {
            return;
        }

        int inputBufferIndex = mMediaCodec.dequeueInputBuffer(12000);
        if (inputBufferIndex >= 0) {
            ByteBuffer inputBuffer = mMediaCodec.getInputBuffer(inputBufferIndex);
            inputBuffer.clear();
            inputBuffer.put(input);
            mMediaCodec.queueInputBuffer(inputBufferIndex, 0, input.length, 0, 0);
        }

        int outputBufferIndex = mMediaCodec.dequeueOutputBuffer(mBufferInfo, 12000);
        while (outputBufferIndex >= 0) {
            ByteBuffer outputBuffer = mMediaCodec.getOutputBuffer(outputBufferIndex);
            if(mListener != null) {
                mListener.onAudioEncode(outputBuffer, mBufferInfo);
            }
            mMediaCodec.releaseOutputBuffer(outputBufferIndex, false);
            outputBufferIndex = mMediaCodec.dequeueOutputBuffer(mBufferInfo, 0);
        }
    }

    public static MediaCodec getAudioMediaCodec(AudioSettings configuration){
        MediaFormat format = MediaFormat.createAudioFormat(configuration.mime, configuration.frequency, configuration.channelCount);
        if(configuration.mime.equals(AudioSettings.DEFAULT_MIME)) {
            format.setInteger(MediaFormat.KEY_AAC_PROFILE, configuration.aacProfile);
        }
        format.setInteger(MediaFormat.KEY_BIT_RATE, configuration.maxBps * 1024);
        format.setInteger(MediaFormat.KEY_SAMPLE_RATE, configuration.frequency);
        int maxInputSize = getRecordBufferSize(configuration);
        format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, maxInputSize);
        format.setInteger(MediaFormat.KEY_CHANNEL_COUNT, configuration.channelCount);

        MediaCodec mediaCodec = null;
        try {
            mediaCodec = MediaCodec.createEncoderByType(configuration.mime);
            mediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        } catch (Exception e) {
            e.printStackTrace();
            if (mediaCodec != null) {
                mediaCodec.stop();
                mediaCodec.release();
                mediaCodec = null;
            }
        }
        return mediaCodec;
    }

    public static boolean checkMicSupport(AudioSettings audioConfiguration) {
        boolean result;
        int recordBufferSize = getRecordBufferSize(audioConfiguration);
        byte[] mRecordBuffer = new byte[recordBufferSize];
        AudioRecord audioRecord = getAudioRecord(audioConfiguration);
        try {
            audioRecord.startRecording();
        } catch (Exception e) {
            e.printStackTrace();
        }
        int readLen = audioRecord.read(mRecordBuffer, 0, recordBufferSize);
        result = readLen >= 0;
        try {
            audioRecord.release();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public static int getRecordBufferSize(AudioSettings audioConfiguration) {
        int frequency = audioConfiguration.frequency;
        int audioEncoding = audioConfiguration.encoding;
        int channelConfiguration = AudioFormat.CHANNEL_IN_MONO;
        if(audioConfiguration.channelCount == 2) {
            channelConfiguration = AudioFormat.CHANNEL_IN_STEREO;
        }
        int size = AudioRecord.getMinBufferSize(frequency, channelConfiguration, audioEncoding);
        return size;
    }

    public static AudioRecord getAudioRecord(AudioSettings audioConfiguration) {
        int frequency = audioConfiguration.frequency;
        int audioEncoding = audioConfiguration.encoding;
        int channelConfiguration = AudioFormat.CHANNEL_IN_MONO;
        if(audioConfiguration.channelCount == 2) {
            channelConfiguration = AudioFormat.CHANNEL_IN_STEREO;
        }

        int audioSource = MediaRecorder.AudioSource.DEFAULT;
        if(audioConfiguration.aec) {
            audioSource = MediaRecorder.AudioSource.VOICE_COMMUNICATION;
        }
        @SuppressLint("MissingPermission") AudioRecord audioRecord = new AudioRecord(audioSource, frequency,
                channelConfiguration, audioEncoding, getRecordBufferSize(audioConfiguration));
        return audioRecord;
    }

}
