package com.statsmind.scrcpy;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.nio.ByteBuffer;

@Data
@EqualsAndHashCode(callSuper = false)
public class TextMessage extends ControlMessage {
    /**
     * 后面可以考虑加入更多的用户信息
     */
    private short clientId;
    private String text;

    public TextMessage() {
        super(TYPE_TEXT);
    }

    public static TextMessage fromBuffer(ByteBuffer buffer) {
        TextMessage msg = new TextMessage();

        try (ByteBufferReader reader = new ByteBufferReader(buffer)) {
            msg.clientId = reader.getShort();
            msg.text = reader.getString();
        }

        return msg;
    }

    public ByteBuffer toBuffer() {
        try (ByteBufferWriter writer = new ByteBufferWriter()) {
            writer.putByte(getType());
            writer.putShort(this.clientId);
            writer.putString(this.text);
            return writer.toBuffer();
        }
    }
}
