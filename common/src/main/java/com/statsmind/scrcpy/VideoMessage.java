package com.statsmind.scrcpy;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.nio.ByteBuffer;

@Data
@EqualsAndHashCode(callSuper = false)
public class VideoMessage extends ControlMessage {
    private ByteBuffer buffer;

    public VideoMessage() {
        super(TYPE_VIDEO_STREAM);
    }
}
