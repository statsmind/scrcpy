package com.statsmind.scrcpy;

import lombok.Data;

import java.nio.ByteBuffer;

@Data
public final class DisplayInfo {
    public static final int FLAG_SUPPORTS_PROTECTED_BUFFERS = 0x00000001;
    private int displayId;
    private Size size;
    private int rotation;
    private int layerStack;
    private int flags;

    public DisplayInfo() {
    }

    public DisplayInfo(int displayId, Size size, int rotation, int layerStack, int flags) {
        this.displayId = displayId;
        this.size = size;
        this.rotation = rotation;
        this.layerStack = layerStack;
        this.flags = flags;
    }

    public ByteBuffer toBuffer() {
        try (ByteBufferWriter writer = new ByteBufferWriter()) {
            writer.putInt(displayId);
            writer.putInt(size.getWidth());
            writer.putInt(size.getHeight());
            writer.putInt(rotation);
            writer.putInt(layerStack);
            writer.putInt(flags);

            return writer.toBuffer();
        }
    }

    public static DisplayInfo fromBuffer(ByteBuffer buffer) {
        DisplayInfo displayInfo = new DisplayInfo();

        try (ByteBufferReader reader = new ByteBufferReader(buffer)) {
            displayInfo.displayId = reader.getInt();
            displayInfo.size = reader.getSize();
            displayInfo.rotation = reader.getInt();
            displayInfo.layerStack = reader.getInt();
            displayInfo.flags = reader.getInt();
        }

        return displayInfo;
    }
//
//    public int getDisplayId() {
//        return displayId;
//    }
//
//    public Size getSize() {
//        return size;
//    }
//
//    public int getRotation() {
//        return rotation;
//    }
//
//    public int getLayerStack() {
//        return layerStack;
//    }
//
//    public int getFlags() {
//        return flags;
//    }
//
//    @Override
//    public String toString() {
//        return "DisplayInfo{" +
//                "displayId=" + displayId +
//                ", size=" + size +
//                ", rotation=" + rotation +
//                ", layerStack=" + layerStack +
//                ", flags=" + flags +
//                '}';
//    }
}
