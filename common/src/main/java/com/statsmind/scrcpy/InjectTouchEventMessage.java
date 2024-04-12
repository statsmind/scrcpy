package com.statsmind.scrcpy;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.nio.ByteBuffer;

import static com.statsmind.scrcpy.ByteBufferReader.toUnsigned;

@Data
@EqualsAndHashCode(callSuper = false)
public class InjectTouchEventMessage extends ControlMessage {
    private int action;
    private long pointerId;
    private Position position;
    private float pressure;
    private int buttons;

    public InjectTouchEventMessage() {
        super(TYPE_INJECT_TOUCH_EVENT);
    }

    public static InjectTouchEventMessage fromBuffer(ByteBuffer buffer) {
        InjectTouchEventMessage msg = new InjectTouchEventMessage();

        try (ByteBufferReader reader = new ByteBufferReader(buffer)) {
            msg.action = toUnsigned(reader.getByte());
            msg.pointerId = reader.getLong();
            msg.position = reader.getPosition();
            int pressureInt = toUnsigned(reader.getShort());
            msg.pressure = pressureInt == 0xffff ? 1f : (pressureInt / 0x1p16f);
            msg.buttons = reader.getInt();
        }

        return msg;
    }

    public ByteBuffer toBuffer() {
        try (ByteBufferWriter writer = new ByteBufferWriter()) {
            writer.putByte(getType());
            writer.putByte(this.action);
            writer.putLong(this.pointerId);
            writer.putPosition(this.position);
            writer.putShort((short)this.pressure);
            writer.putInt(this.buttons);

            return writer.toBuffer();
        }
    }
}
