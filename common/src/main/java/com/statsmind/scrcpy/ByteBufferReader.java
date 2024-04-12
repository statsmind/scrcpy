package com.statsmind.scrcpy;

import android.graphics.Rect;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class ByteBufferReader implements AutoCloseable {
    private ByteBuffer buffer;

    public ByteBufferReader(ByteBuffer buffer) {
        this.buffer = buffer;
    }

    public ByteBuffer getBuffer() {
        return buffer;
    }

    public ByteBuffer getBuffer(int size) {
        return ByteBuffer.wrap(getBytes(size));
    }

    public byte getByte() {
        return buffer.get();
    }

    public int getInt() {
        return buffer.getInt();
    }

    public long getLong() {
        return buffer.getLong();
    }

    public float getFloat() {
        return buffer.getFloat();
    }

    public short getShort() {
        return buffer.getShort();
    }

    public void position(int pos) {
        buffer.position(pos);
    }

    public void rewind() {
        buffer.rewind();
    }

    public int remaining() {
        return buffer.remaining();
    }

    public byte[] getBytes() {
        return getBytes(remaining());
    }

    public byte[] getBytes(int size) {
        byte[] bytes = new byte[size];
        buffer.get(bytes);

        return bytes;
    }

    public Rect getRect() {
        Rect rect = new Rect(getInt(), getInt(), getInt(), getInt());
        if (rect.top == 0 && rect.bottom == 0 && rect.left == 0 && rect.right == 0) {
            return null;
        } else {
            return rect;
        }
    }

    public Size getSize() {
        Size size = new Size(getInt(), getInt());
        if (size.getWidth() == 0 && size.getHeight() == 0) {
            return null;
        } else {
            return size;
        }
    }

    public Position getPosition() {
        return new Position(getInt(), getInt(), toUnsigned(getShort()), toUnsigned(getShort()));
    }

    public String getString() {
        int len = getInt();
        if (len == 0) {
            return "";
        }

        byte[] bytes = getBytes(len);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public static int toUnsigned(short value) {
        return value & 0xffff;
    }

    public static int toUnsigned(byte value) {
        return value & 0xff;
    }

    @Override
    public void close() {
        this.buffer = null;
    }
}
