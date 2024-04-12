package com.statsmind.scrcpy;

import android.graphics.Rect;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class ByteBufferWriter implements AutoCloseable {
    private ByteArrayOutputStream bao;
    private DataOutputStream buffer;

    public ByteBufferWriter() {
        bao = new ByteArrayOutputStream();
        buffer = new DataOutputStream(bao);
    }

    public void putByte(int v) {
        putByte((byte) v);
    }

    public void putByte(byte v) {
        try {
            buffer.writeByte(v);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void putInt(int v) {
        try {
            buffer.writeInt(v);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void putLong(long v) {
        try {
            buffer.writeLong(v);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void putFloat(float v) {
        try {
            buffer.writeFloat(v);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void putShort(int v) {
        try {
            buffer.writeShort(v);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void putBytes(byte[] v) {
        putBytes(v, false);
    }

    public void putBuffer(ByteBuffer buffer) {
        putBuffer(buffer, false);
    }

    public void putBuffer(ByteBuffer buffer, boolean withSize) {
        putBytes(buffer.array(), withSize);
    }

    public void putBytes(byte[] v, boolean withSize) {
        try {
            if (withSize) {
                putInt(v.length);
            }

            buffer.write(v);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void putString(String v) {
        if (v == null || v.isEmpty()) {
            putInt(0);
        } else {
            byte[] bytes = v.getBytes(StandardCharsets.UTF_8);
            putBytes(bytes, true);
        }
    }

    public void putRect(Rect rect) {
        if (rect == null) {
            putInt(0);
            putInt(0);
            putInt(0);
            putInt(0);
        } else {
            putInt(rect.left);
            putInt(rect.top);
            putInt(rect.right);
            putInt(rect.bottom);
        }
    }

    public void putSize(Size size) {
        if (size == null) {
            putInt(0);
            putInt(0);
        } else {
            putInt(size.getWidth());
            putInt(size.getHeight());
        }
    }

    public void putPosition(Position position) {
        putInt(position.getPoint().getX());
        putInt(position.getPoint().getY());
        putShort(position.getScreenSize().getWidth());
        putShort(position.getScreenSize().getHeight());
    }

    public ByteBuffer toBuffer() {
        return ByteBuffer.wrap(bao.toByteArray());
    }

    @Override
    public void close() {
        try {
            buffer.close();
        } catch (Exception ex) {
        }
        buffer = null;

        try {
            bao.close();
        } catch (Exception ex) {
        }
        bao = null;
    }
}
