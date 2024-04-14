package com.genymobile.scrcpy;

import java.nio.ByteBuffer;

public final class Binary {
    private Binary() {
        // not instantiable
    }

    public static int toUnsigned(short value) {
        return value & 0xffff;
    }

    public static int toUnsigned(byte value) {
        return value & 0xff;
    }

    /**
     * Convert unsigned 16-bit fixed-point to a float between 0 and 1
     *
     * @param value encoded value
     * @return Float value between 0 and 1
     */
    public static float u16FixedPointToFloat(short value) {
        int unsignedShort = Binary.toUnsigned(value);
        // 0x1p16f is 2^16 as float
        return unsignedShort == 0xffff ? 1f : (unsignedShort / 0x1p16f);
    }

    /**
     * Convert signed 16-bit fixed-point to a float between -1 and 1
     *
     * @param value encoded value
     * @return Float value between -1 and 1
     */
    public static float i16FixedPointToFloat(short value) {
        // 0x1p15f is 2^15 as float
        return value == 0x7fff ? 1f : (value / 0x1p15f);
    }

    public static String byteToHex(byte num) {
        char[] hexDigits = new char[2];
        hexDigits[0] = Character.forDigit((num >> 4) & 0xF, 16);
        hexDigits[1] = Character.forDigit((num & 0xF), 16);
        return new String(hexDigits);
    }

    public static String bytesToHex(ByteBuffer buffer) {
        return bytesToHex(byteBufferToBytes(buffer));
    }

    public static String bytesToHex(byte[] bytes) {
        StringBuilder formatted = new StringBuilder();

        for (int i = 0; i < bytes.length; ++i) {
            formatted.append(byteToHex(bytes[i])).append(" ");
            if (i % 32 == 31) {
                formatted.append("\n");
            } else if (i % 4 == 3) {
                formatted.append("   ");
            }

        }

        return formatted.toString();
    }

    public static byte[] byteBufferToBytes(ByteBuffer buffer) {
        byte[] out = new byte[buffer.remaining()];
        int position = buffer.position();
        buffer.get(out);
        buffer.position(position);

        return out;
    }

    public static ByteBuffer duplicate(ByteBuffer buffer) {
        return ByteBuffer.wrap(byteBufferToBytes(buffer));
    }
}
