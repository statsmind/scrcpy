package com.statsmind.scrcpy;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.nio.ByteBuffer;

@Data
@EqualsAndHashCode(callSuper = false)
public class UpdateVideoSettingsMessage extends ControlMessage {
    private VideoSettings videoSettings;

    public UpdateVideoSettingsMessage() {
        super(ControlMessage.TYPE_UPDATE_VIDEO_SETTINGS);
    }

    public static UpdateVideoSettingsMessage fromBuffer(ByteBuffer buffer) {
        UpdateVideoSettingsMessage msg = new UpdateVideoSettingsMessage();
        msg.videoSettings = VideoSettings.fromBuffer(buffer);

        return msg;
    }

    public ByteBuffer toBuffer() {
        try (ByteBufferWriter writer = new ByteBufferWriter()) {
            writer.putByte(getType());
            writer.putBuffer(this.videoSettings.toBuffer());
            return writer.toBuffer();
        }
    }
}
