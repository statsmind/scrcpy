package com.statsmind.scrcpy;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.nio.ByteBuffer;

@Data
@EqualsAndHashCode(callSuper = false)
public class InjectTextMessage extends ControlMessage {
    private String text;

    public InjectTextMessage() {
        super(TYPE_INJECT_TEXT);
    }

    public static InjectTextMessage fromBuffer(ByteBuffer buffer) {
        InjectTextMessage msg = new InjectTextMessage();

        try (ByteBufferReader reader = new ByteBufferReader(buffer)) {
            msg.text = reader.getString();
        }

        return msg;
    }

    public ByteBuffer toBuffer() {
        try (ByteBufferWriter writer = new ByteBufferWriter()) {
            writer.putByte(getType());
            writer.putString(this.text);
            return writer.toBuffer();
        }
    }
}
