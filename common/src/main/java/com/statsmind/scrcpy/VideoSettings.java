package com.statsmind.scrcpy;

import android.graphics.Rect;
import lombok.Data;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Data
public class VideoSettings {
    private static final int DEFAULT_BIT_RATE = 8000000;
    private static final byte DEFAULT_MAX_FPS = 60;

    private static final byte DEFAULT_I_FRAME_INTERVAL = 10; // seconds

    private static final String DEFAULT_ENCODER_NAME = "OMX.google.h264.encoder";

    private Size bounds;
    private int bitRate = DEFAULT_BIT_RATE;
    private int maxFps = DEFAULT_MAX_FPS;
    private int lockedVideoOrientation = -1;
    private byte iFrameInterval = DEFAULT_I_FRAME_INTERVAL;
    private Rect crop;
    private boolean sendFrameMeta = false; // send PTS so that the client may record properly
    private int displayId;
    private String codecOptionsString = "";
    private List<CodecOption> codecOptions = new ArrayList<>();
    private String encoderName = DEFAULT_ENCODER_NAME;

    public static VideoSettings fromBuffer(ByteBuffer buffer) {
        VideoSettings msg = new VideoSettings();

        try (ByteBufferReader data = new ByteBufferReader(buffer)) {
            msg.bitRate = data.getInt();
            msg.maxFps = data.getInt();
            msg.iFrameInterval = data.getByte();
            msg.bounds = data.getSize();
            msg.crop = data.getRect();
            msg.sendFrameMeta = data.getByte() != 0;
            msg.lockedVideoOrientation = data.getByte();
            msg.displayId = data.getInt();

            msg.codecOptions = new ArrayList<>();
            int codecOptionNum = data.getInt();
            for (int i = 0; i < codecOptionNum; ++i) {
                msg.codecOptions.add(CodecOption.fromBuffer(data.getBuffer()));
            }

            msg.encoderName = data.getString();
        }

        return msg;
    }

    public ByteBuffer toBuffer() {
        try (ByteBufferWriter writer = new ByteBufferWriter()) {
            writer.putInt(this.getBitRate());
            writer.putInt(this.getMaxFps());
            writer.putByte(this.getIFrameInterval());
            writer.putSize(this.getBounds());
            writer.putRect(this.getCrop());
            writer.putByte(this.isSendFrameMeta() ? 1 : 0);
            writer.putByte(this.getLockedVideoOrientation());
            writer.putInt(this.getDisplayId());

            writer.putInt(this.getCodecOptions().size());
            for (int i = 0; i < this.getCodecOptions().size(); ++i) {
                writer.putBuffer(this.getCodecOptions().get(i).toBuffer());
            }

            writer.putString(this.encoderName);

            return writer.toBuffer();
        }
    }

    public void setBounds(int width, int height) {
        this.bounds = new Size(width & ~15, height & ~15); // multiple of 16
    }

    public void setEncoderName(String encoderName) {
        if (encoderName != null && encoderName.isEmpty() || encoderName.equals("-")) {
            this.encoderName = DEFAULT_ENCODER_NAME;
        } else {
            this.encoderName = encoderName;
        }
    }

    public void merge(VideoSettings source) {
        codecOptions = source.codecOptions;
        codecOptionsString = source.codecOptionsString;
        encoderName = source.encoderName;
        bitRate = source.bitRate;
        maxFps = source.maxFps;
        iFrameInterval = source.iFrameInterval;
        bounds = source.bounds;
        crop = source.crop;
        sendFrameMeta = source.sendFrameMeta;
        lockedVideoOrientation = source.lockedVideoOrientation;
        displayId = source.displayId;
    }

    public List<CodecOption> getCodecOptions() {
        if (codecOptions == null) {
            codecOptions = new ArrayList<>();
        }

        return codecOptions;
    }

    @Override
    public String toString() {
        return "VideoSettings{"
                + "bitRate=" + bitRate
                + ", maxFps=" + maxFps
                + ", iFrameInterval=" + iFrameInterval
                + ", bounds=" + bounds
                + ", crop=" + crop
                + ", metaFrame=" + sendFrameMeta
                + ", lockedVideoOrientation=" + lockedVideoOrientation
                + ", displayId=" + displayId
                + ", codecOptions=" + this.codecOptions
                + ", encoderName=" + this.encoderName
                + "}";
    }

}
