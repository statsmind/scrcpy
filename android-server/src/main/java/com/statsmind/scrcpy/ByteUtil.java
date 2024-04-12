package com.statsmind.scrcpy;

import java.nio.ByteBuffer;

public class ByteUtil {
    /**
     * Byte字节转Hex
     *
     * @param b 字节
     * @return Hex
     */
    public static String byteToHex(byte b) {
        String hexString = Integer.toHexString(b & 0xFF);
        //由于十六进制是由0~9、A~F来表示1~16，所以如果Byte转换成Hex后如果是<16,就会是一个字符（比如A=10），通常是使用两个字符来表示16进制位的,
        //假如一个字符的话，遇到字符串11，这到底是1个字节，还是1和1两个字节，容易混淆，如果是补0，那么1和1补充后就是0101，11就表示纯粹的11
        if (hexString.length() < 2) {
            hexString = new StringBuilder(String.valueOf(0)).append(hexString).toString();
        }
        return hexString.toUpperCase();
    }


    /**
     * 字节数组转Hex
     *
     * @param bytes 字节数组
     * @return Hex
     */
    public static String bytesToHex(byte[] bytes) {
        StringBuffer sb = new StringBuffer();
        if (bytes != null && bytes.length > 0) {
            for (int i = 0; i < bytes.length; i++) {
                String hex = byteToHex(bytes[i]);
                sb.append(hex);
            }
        }
        return sb.toString();
    }

    public static byte[] readBytes(ByteBuffer buffer, int maxSize) {
        int size = Math.min(maxSize, buffer.remaining());
        byte[] bytes = new byte[size];
        buffer.get(bytes, 0, size);
        buffer.rewind();
        return bytes;
    }

}
