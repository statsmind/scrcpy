package com.statsmind.scrcpy.client;

import android.media.MediaCodec;
import android.media.MediaFormat;

import java.io.IOException;

import static android.media.MediaFormat.MIMETYPE_AUDIO_AAC;

public class AudioCodec {
    public static MediaCodec setupAudioCodec() {
        MediaFormat format = new MediaFormat();
        format.setString(MediaFormat.KEY_MIME, MIMETYPE_AUDIO_AAC);
        format.setInteger(MediaFormat.KEY_BIT_RATE, 128000);
        format.setInteger(MediaFormat.KEY_CHANNEL_COUNT, 2);
        format.setInteger(MediaFormat.KEY_SAMPLE_RATE, 48000);

        MediaCodec mediaCodec = null;
        try {
            mediaCodec = MediaCodec.createDecoderByType(MIMETYPE_AUDIO_AAC);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        mediaCodec.configure(format, null, null, 0);
        return mediaCodec;
    }
}
