package com.statsmind.scrcpy;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.nio.ByteBuffer;

@Data
@EqualsAndHashCode(callSuper = false)
public class InjectScrollMessage extends ControlMessage {
    private Position position;
    private int hScroll;
    private int vScroll;

    public InjectScrollMessage() {
        super(TYPE_INJECT_SCROLL_EVENT);
    }

    public static InjectScrollMessage fromBuffer(ByteBuffer buffer) {
        InjectScrollMessage msg = new InjectScrollMessage();

        try (ByteBufferReader reader = new ByteBufferReader(buffer)) {
            msg.position = reader.getPosition();
            msg.hScroll = reader.getInt();
            msg.vScroll = reader.getInt();
        }

        return msg;
    }

    public ByteBuffer toBuffer() {
        try (ByteBufferWriter writer = new ByteBufferWriter()) {
            writer.putByte(getType());
            writer.putPosition(this.position);
            writer.putInt(this.hScroll);
            writer.putInt(this.vScroll);
            return writer.toBuffer();
        }
    }
}
