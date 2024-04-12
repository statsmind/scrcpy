package com.statsmind.scrcpy;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class CodecOption {
    private String key;
    private Object value;

    public CodecOption() {
    }

    public CodecOption(String key, Object value) {
        this.key = key;
        this.value = value;
    }

    public static CodecOption fromBuffer(ByteBuffer buffer) {
        CodecOption msg = new CodecOption();

        try (ByteBufferReader reader = new ByteBufferReader(buffer)) {
            msg.key = reader.getString();

            switch (reader.getByte()) {
                case 0:
                    msg.value = reader.getInt();
                    break;
                case 1:
                    msg.value = reader.getLong();
                    break;
                case 2:
                    msg.value = reader.getFloat();
                    break;
                case 3:
                    msg.value = reader.getString();
                    break;
                default:
                    throw new RuntimeException("unknown type");
            }

            return msg;
        }
    }

    public static List<CodecOption> parse(String codecOptions) {
        if ("-".equals(codecOptions)) {
            return null;
        }

        List<CodecOption> result = new ArrayList<>();

        boolean escape = false;
        StringBuilder buf = new StringBuilder();

        for (char c : codecOptions.toCharArray()) {
            switch (c) {
                case '\\':
                    if (escape) {
                        buf.append('\\');
                        escape = false;
                    } else {
                        escape = true;
                    }
                    break;
                case ',':
                    if (escape) {
                        buf.append(',');
                        escape = false;
                    } else {
                        // This comma is a separator between codec options
                        String codecOption = buf.toString();
                        result.add(parseOption(codecOption));
                        // Clear buf
                        buf.setLength(0);
                    }
                    break;
                default:
                    buf.append(c);
                    break;
            }
        }

        if (buf.length() > 0) {
            String codecOption = buf.toString();
            result.add(parseOption(codecOption));
        }

        return result;
    }

    private static CodecOption parseOption(String option) {
        int equalSignIndex = option.indexOf('=');
        if (equalSignIndex == -1) {
            throw new IllegalArgumentException("'=' expected");
        }
        String keyAndType = option.substring(0, equalSignIndex);
        if (keyAndType.length() == 0) {
            throw new IllegalArgumentException("Key may not be null");
        }

        String key;
        String type;

        int colonIndex = keyAndType.indexOf(':');
        if (colonIndex != -1) {
            key = keyAndType.substring(0, colonIndex);
            type = keyAndType.substring(colonIndex + 1);
        } else {
            key = keyAndType;
            type = "int"; // assume int by default
        }

        Object value;
        String valueString = option.substring(equalSignIndex + 1);
        switch (type) {
            case "int":
                value = Integer.parseInt(valueString);
                break;
            case "long":
                value = Long.parseLong(valueString);
                break;
            case "float":
                value = Float.parseFloat(valueString);
                break;
            case "string":
                value = valueString;
                break;
            default:
                throw new IllegalArgumentException("Invalid codec option type (int, long, float, str): " + type);
        }

        return new CodecOption(key, value);
    }

    public String getKey() {
        return key;
    }

    public Object getValue() {
        return value;
    }

    public ByteBuffer toBuffer() {
        try (ByteBufferWriter writer = new ByteBufferWriter()) {
            writer.putString(this.key);

            if (this.value instanceof Integer) {
                writer.putByte(0);
                writer.putInt((int) this.value);
            } else if (this.value instanceof Long) {
                writer.putByte(1);
                writer.putLong((long) this.value);
            } else if (this.value instanceof Float) {
                writer.putByte(2);
                writer.putFloat((float) this.value);
            } else if (this.value instanceof String) {
                writer.putByte(3);
                writer.putString((String) this.value);
            } else {
                throw new RuntimeException("unknown type");
            }

            return writer.toBuffer();
        }
    }
}
