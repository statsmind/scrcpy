package com.statsmind.scrcpy;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.nio.ByteBuffer;

@Data
@EqualsAndHashCode(callSuper = false)
public class AudioMessage extends ControlMessage {
    private ByteBuffer buffer;

    public AudioMessage() {
        super(ControlMessage.TYPE_AUDIO_STREAM);
    }
}
