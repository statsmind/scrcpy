//package com.statsmind.scrcpy;
//
//import android.util.Log;
//
//import java.io.EOFException;
//import java.io.IOException;
//import java.io.InputStream;
//import java.nio.ByteBuffer;
//import java.nio.charset.StandardCharsets;
//
//import static com.statsmind.scrcpy.Consts.TAG;
//
//public class ControlMessageReader {
//
//    public static final int INJECT_TEXT_MAX_LENGTH = 300;
//    static final int INJECT_KEYCODE_PAYLOAD_LENGTH = 13;
//    static final int INJECT_TOUCH_EVENT_PAYLOAD_LENGTH = 27;
//    static final int INJECT_SCROLL_EVENT_PAYLOAD_LENGTH = 20;
//    static final int BACK_OR_SCREEN_ON_LENGTH = 1;
//    static final int SET_SCREEN_POWER_MODE_PAYLOAD_LENGTH = 1;
//    static final int SET_CLIPBOARD_FIXED_PAYLOAD_LENGTH = 1;
//    private static final int MESSAGE_MAX_SIZE = 1 << 18; // 256k
//    public static final int CLIPBOARD_TEXT_MAX_LENGTH = MESSAGE_MAX_SIZE - 6; // type: 1 byte; paste flag: 1 byte; length: 4 bytes
//    private final byte[] rawBuffer = new byte[MESSAGE_MAX_SIZE];
//    private final ByteBuffer buffer = ByteBuffer.wrap(rawBuffer);
//
//    public ControlMessageReader() {
//        // invariant: the buffer is always in "get" mode
//        buffer.limit(0);
//    }
//
//    private static Position readPosition(ByteBuffer buffer) {
//        int x = buffer.getInt();
//        int y = buffer.getInt();
//        int screenWidth = toUnsigned(buffer.getShort());
//        int screenHeight = toUnsigned(buffer.getShort());
//        return new Position(x, y, screenWidth, screenHeight);
//    }
//
//    private static int toUnsigned(short value) {
//        return value & 0xffff;
//    }
//
//    private static int toUnsigned(byte value) {
//        return value & 0xff;
//    }
//
//    public boolean isFull() {
//        return buffer.remaining() == rawBuffer.length;
//    }
//
//    public void readFrom(InputStream input) throws IOException {
//        if (isFull()) {
//            throw new IllegalStateException("Buffer full, call next() to consume");
//        }
//        buffer.compact();
//        int head = buffer.position();
//        int r = input.read(rawBuffer, head, rawBuffer.length - head);
//        if (r == -1) {
//            throw new EOFException("Controller socket closed");
//        }
//        buffer.position(head + r);
//        buffer.flip();
//    }
//
//    public ControlMessage next() {
//        return parseEvent(buffer);
//    }
//
//
////
////    private ControlMessage parseChangeStreamParameters(ByteBuffer buffer) {
////        int re = buffer.remaining();
////        byte[] bytes = new byte[re];
////        if (re > 0) {
////            buffer.get(bytes, 0, re);
////        }
////        return ControlMessage.createChangeSteamParameters(bytes);
////    }
////
////    private ControlMessage parsePushFile(ByteBuffer buffer) {
////        int re = buffer.remaining();
////        byte[] bytes = new byte[re];
////        if (re > 0) {
////            buffer.get(bytes, 0, re);
////        }
////        return ControlMessage.createFilePush(bytes);
////    }
////
////    private ControlMessage parseInjectKeycode(ByteBuffer buffer) {
////        if (buffer.remaining() < INJECT_KEYCODE_PAYLOAD_LENGTH) {
////            return null;
////        }
////        int action = toUnsigned(buffer.get());
////        int keycode = buffer.getInt();
////        int repeat = buffer.getInt();
////        int metaState = buffer.getInt();
////        return ControlMessage.createInjectKeycode(action, keycode, repeat, metaState);
////    }
////
////    private String parseString(ByteBuffer buffer) {
////        if (buffer.remaining() < 4) {
////            return null;
////        }
////        int len = buffer.getInt();
////        if (buffer.remaining() < len) {
////            return null;
////        }
////        buffer.get(rawBuffer, 0, len);
////        return new String(rawBuffer, 0, len, StandardCharsets.UTF_8);
////    }
////
////    private ControlMessage parseInjectText(ByteBuffer buffer) {
////        String text = parseString(buffer);
////        if (text == null) {
////            return null;
////        }
////        return ControlMessage.createInjectText(text);
////    }
////
////    private ControlMessage parseInjectTouchEvent(ByteBuffer buffer) {
////        Log.w(TAG, "parseInjectTouchEvent: remaining=" + buffer.remaining());
////        if (buffer.remaining() < INJECT_TOUCH_EVENT_PAYLOAD_LENGTH) {
////            return null;
////        }
////        int action = toUnsigned(buffer.get());
////        long pointerId = buffer.getLong();
////        Position position = readPosition(buffer);
////        // 16 bits fixed-point
////        int pressureInt = toUnsigned(buffer.getShort());
////        // convert it to a float between 0 and 1 (0x1p16f is 2^16 as float)
////        float pressure = pressureInt == 0xffff ? 1f : (pressureInt / 0x1p16f);
////        int buttons = buffer.getInt();
////        return ControlMessage.createInjectTouchEvent(action, pointerId, position, pressure, buttons);
////    }
////
////    private ControlMessage parseInjectScrollEvent(ByteBuffer buffer) {
////        if (buffer.remaining() < INJECT_SCROLL_EVENT_PAYLOAD_LENGTH) {
////            return null;
////        }
////        Position position = readPosition(buffer);
////        int hScroll = buffer.getInt();
////        int vScroll = buffer.getInt();
////        return ControlMessage.createInjectScrollEvent(position, hScroll, vScroll);
////    }
////
////    private ControlMessage parseBackOrScreenOnEvent(ByteBuffer buffer) {
////        if (buffer.remaining() < BACK_OR_SCREEN_ON_LENGTH) {
////            return null;
////        }
////        int action = toUnsigned(buffer.get());
////        return ControlMessage.createBackOrScreenOn(action);
////    }
////
////    private ControlMessage parseSetClipboard(ByteBuffer buffer) {
////        if (buffer.remaining() < SET_CLIPBOARD_FIXED_PAYLOAD_LENGTH) {
////            return null;
////        }
////        boolean paste = buffer.get() != 0;
////        String text = parseString(buffer);
////        if (text == null) {
////            return null;
////        }
////        return ControlMessage.createSetClipboard(text, paste);
////    }
////
////    private ControlMessage parseSetScreenPowerMode(ByteBuffer buffer) {
////        if (buffer.remaining() < SET_SCREEN_POWER_MODE_PAYLOAD_LENGTH) {
////            return null;
////        }
////        int mode = buffer.get();
////        return ControlMessage.createSetScreenPowerMode(mode);
////    }
//}
