package dev.rezu.json;

import java.time.Instant;
import java.util.*;

public class JsonWriter {
    private final StringBuilder out = new StringBuilder(1024);
    private final Deque<Boolean> stack = new ArrayDeque<>();

    public JsonWriter beginObject() {
        prepareValue();
        out.append("{");
        stack.push(true);
        return this;
    }

    public JsonWriter endObject() {
        out.append("}");
        stack.pop();
        return this;
    }

    public JsonWriter beginArray() {
        prepareValue();
        out.append("[");
        stack.push(true);
        return this;
    }

    public JsonWriter endArray() {
        out.append("]");
        stack.pop();
        return this;
    }

    public JsonWriter field(String key, Object value) {
        prepareValue();
        out.append("\"");
        out.append(escape(key));
        out.append("\":");
        stack.push(true);
        writeValue(value);
        stack.pop();
        return this;
    }

    private void writeValue(Object value) {
        switch (value) {
            case null -> out.append("null");
            case String s -> {
                out.append("\"");
                out.append(escape(s));
                out.append("\"");
            }
            case Number n -> out.append(n);
            case Boolean b -> out.append(b);
            case Instant inst -> {
                out.append("\"");
                out.append(inst);
                out.append("\"");
            }
            case Collection<?> col -> {
                beginArray();
                for (Object item : col) {
                    prepareValue();
                    stack.push(true);
                    writeValue(item);
                    stack.pop();
                }
                endArray();
            }
            case Map<?, ?> m -> {
                beginObject();
                for (var entry : m.entrySet()) {
                    if (!(entry.getKey() instanceof String key)) {
                        throw new IllegalStateException("JSON object keys must be Strings");
                    }
                    field(key, entry.getValue());
                }
                endObject();
            }
            case Object o -> {
                if (!JsonMapper.writeRecord(this, o)) {
                    throw new IllegalStateException("No JSON mapper for type: " + o.getClass().getName());
                }
            }
        }
    }

    private void prepareValue() {
        if (!stack.isEmpty()) {
            boolean isFirst = stack.pop();
            if (!isFirst) out.append(",");
            stack.push(false);
        }
    }

    private String escape(String input) {
        if (input == null || input.isEmpty()) return "";
        if (!needsEscaping(input)) return input;
        StringBuilder escaped = new StringBuilder(input.length() + 16);
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            switch (c) {
                case '"' -> escaped.append("\\\"");
                case '\\' -> escaped.append("\\\\");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> {
                    if (c < ' ') escaped.append(String.format("\\u%04x", (int) c));
                    else escaped.append(c);
                }
            }
        }
        return escaped.toString();
    }

    private boolean needsEscaping(String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '"' || c == '\\' || c < ' ') return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return out.toString();
    }
}