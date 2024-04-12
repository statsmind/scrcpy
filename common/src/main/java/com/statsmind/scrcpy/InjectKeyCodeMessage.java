package com.statsmind.scrcpy;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.nio.ByteBuffer;

import static com.statsmind.scrcpy.ByteBufferReader.toUnsigned;

@Data
@EqualsAndHashCode(callSuper = false)
public class InjectKeyCodeMessage extends ControlMessage {
    private int action;
    private int keycode;
    private int repeat;
    private int metaState;

    public InjectKeyCodeMessage() {
        super(TYPE_INJECT_KEYCODE_EVENT);
    }

    public static InjectKeyCodeMessage fromBuffer(ByteBuffer buffer) {
        InjectKeyCodeMessage msg = new InjectKeyCodeMessage();

        try (ByteBufferReader reader = new ByteBufferReader(buffer)) {
            msg.action = toUnsigned(reader.getByte());
            msg.keycode = reader.getByte();
            msg.repeat = reader.getByte();
            msg.metaState = reader.getByte();
        }

        return msg;
    }

    public ByteBuffer toBuffer() {
        try (ByteBufferWriter writer = new ByteBufferWriter()) {
            writer.putByte(getType());
            writer.putByte(this.action);
            writer.putByte(this.keycode);
            writer.putByte(this.repeat);
            writer.putByte(this.metaState);
            return writer.toBuffer();
        }
    }
}
