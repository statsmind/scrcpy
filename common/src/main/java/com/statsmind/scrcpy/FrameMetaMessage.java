package com.statsmind.scrcpy;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.nio.ByteBuffer;

@Data
@EqualsAndHashCode(callSuper = false)
public class FrameMetaMessage extends ControlMessage {
    private long pts;
    private int packetSize;

    public FrameMetaMessage() {
        super(TYPE_FRAME_META);
    }

    public static FrameMetaMessage fromBuffer(ByteBuffer buffer) {
        FrameMetaMessage msg = new FrameMetaMessage();

        try (ByteBufferReader reader = new ByteBufferReader(buffer)) {
            msg.pts = reader.getLong();
            msg.packetSize = reader.getInt();
        }

        return msg;
    }

    public ByteBuffer toBuffer() {
        try (ByteBufferWriter writer = new ByteBufferWriter()) {
            writer.putByte(getType());
            writer.putLong(this.pts);
            writer.putInt(this.packetSize);
            return writer.toBuffer();
        }
    }
}
