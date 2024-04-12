package com.statsmind.scrcpy;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = false)
public class DeviceInfoMessage extends ControlMessage {
    private String deviceName;
    private short clientId;
    private List<Display> displays = new ArrayList<>();
    private List<String> encoderNames = new ArrayList<>();

    public DeviceInfoMessage() {
        super(ControlMessage.TYPE_DEVICE_INFO);
    }

    public static DeviceInfoMessage fromBuffer(ByteBuffer buffer) {
        DeviceInfoMessage msg = new DeviceInfoMessage();
        msg.displays = new ArrayList<>();
        msg.encoderNames = new ArrayList<>();

        try (ByteBufferReader reader = new ByteBufferReader(buffer)) {
            msg.deviceName = reader.getString();
            msg.clientId = reader.getShort();

            int displayNum = reader.getInt();
            for (int i = 0; i < displayNum; ++i) {
                msg.displays.add(Display.fromBuffer(reader.getBuffer()));
            }

            int encoderNameNum = reader.getInt();
            for (int i = 0; i < encoderNameNum; ++i) {
                msg.encoderNames.add(reader.getString());
            }
        }

        return msg;
    }

    public ByteBuffer toBuffer() {
        try (ByteBufferWriter writer = new ByteBufferWriter()) {
            writer.putByte(getType());

            writer.putString(this.deviceName);
            writer.putShort(this.clientId);

            writer.putInt(this.displays.size());
            for (Display display : displays) {
                writer.putBytes(display.toBuffer().array());
            }

            writer.putInt(this.encoderNames.size());
            for (String encoderName : this.encoderNames) {
                writer.putString(encoderName);
            }

            return writer.toBuffer();
        }
    }

    @Data
    public static class Display {
        private DisplayInfo displayInfo;
        private ScreenInfo screenInfo;
        private VideoSettings videoSettings;
        private int connectionsCount;

        public static Display fromBuffer(ByteBuffer buffer) {
            Display msg = new Display();

            try (ByteBufferReader reader = new ByteBufferReader(buffer)) {
                int displayInfoBytesSize = reader.getInt();
                if (displayInfoBytesSize > 0) {
                    msg.displayInfo = DisplayInfo.fromBuffer(reader.getBuffer(displayInfoBytesSize));
                }

                int screenInfoBytesSize = reader.getInt();
                if (screenInfoBytesSize > 0) {
                    msg.screenInfo = ScreenInfo.fromBuffer(reader.getBuffer(screenInfoBytesSize));
                }

                int videoSettingsBytesSize = reader.getInt();
                if (videoSettingsBytesSize > 0) {
                    msg.videoSettings = VideoSettings.fromBuffer(reader.getBuffer(videoSettingsBytesSize));
                }

                msg.connectionsCount = reader.getInt();
            }

            return msg;
        }

        public ByteBuffer toBuffer() {
            try (ByteBufferWriter writer = new ByteBufferWriter()) {
                if (displayInfo == null) {
                    writer.putInt(0);
                } else {
                    writer.putBuffer(displayInfo.toBuffer(), true);
                }

                if (screenInfo == null) {
                    writer.putInt(0);
                } else {
                    writer.putBuffer(screenInfo.toBuffer(), true);
                }

                if (videoSettings == null) {
                    writer.putInt(0);
                } else {
                    writer.putBuffer(videoSettings.toBuffer(), true);
                }

                writer.putInt(connectionsCount);

                return writer.toBuffer();
            }
        }

//        @Override
//        public String toString() {
//            return "Display{" +
//                    "displayInfo=" + displayInfo +
//                    ", screenInfo=" + screenInfo +
//                    ", videoSettings=" + videoSettings +
//                    ", connectionsCount=" + connectionsCount +
//                    '}';
//        }
    }
}
