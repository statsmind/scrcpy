package com.statsmind.scrcpy.client;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.util.Size;
import android.view.Surface;
import android.view.SurfaceView;

import java.io.IOException;

import static android.media.MediaFormat.MIMETYPE_VIDEO_AVC;

public class VideoCodec {
    public static MediaCodec setupVideoCodec(Surface surface, int width, int height) {
        MediaFormat videoFormat = MediaFormat.createVideoFormat(MIMETYPE_VIDEO_AVC, width, height);
//        videoFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 60);
        videoFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);

        MediaCodec videoMediaCodec = null;
        try {
            videoMediaCodec = MediaCodec.createDecoderByType(MIMETYPE_VIDEO_AVC);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        videoMediaCodec.configure(videoFormat, surface, null, 0);
        return videoMediaCodec;
    }

}
