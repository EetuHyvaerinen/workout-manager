package dev.rezu.json;

import java.io.Reader;
import java.util.*;

public final class JsonReader {
    private static final int MAX_JSON_SIZE = 2 * 1024 * 1024;

    private final String src;
    private int pos = 0;
    private final int length;

    private JsonReader(String src) {
        this.src = src;
        this.length = src.length();
    }

    public static JsonNode parse(Reader reader) {
        try {
            String content = readerToString(reader).trim();
            return new JsonReader(content).parse();
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse JSON", e);
        }
    }
    //suppress warning since we guarantee the type to be <String, Object>
    @SuppressWarnings("unchecked")
    private JsonNode parse() {
        skipWhitespace();
        Object result = parseValue();
        skipWhitespace();
        if (pos < length) {
            throw new IllegalArgumentException("Extra data after JSON at index " + pos);
        }
        return new JsonNode(result instanceof Map ? (Map<String, Object>) result : Map.of("value", result));
    }

    private Object parseValue() {
        char c = peek();
        return switch (c) {
            case '{' -> parseObject();
            case '[' -> parseArray();
            case '"' -> parseString();
            case 't', 'f' -> parseBoolean();
            case 'n' -> parseNull();
            default -> {
                if (Character.isDigit(c) || c == '-') yield parseNumber();
                throw new IllegalArgumentException("Unexpected character '" + c + "' at " + pos);
            }
        };
    }

    private Map<String, Object> parseObject() {
        consume('{');
        Map<String, Object> map = new LinkedHashMap<>(); // Preserves insertion order
        skipWhitespace();

        if (peek() == '}') {
            consume('}');
            return map;
        }

        while (true) {
            String key = parseString();
            skipWhitespace();
            consume(':');
            map.put(key, parseValue());

            skipWhitespace();
            char next = peek();
            if (next == '}') {
                consume('}');
                break;
            }
            consume(',');
            skipWhitespace();
        }
        return map;
    }

    private List<Object> parseArray() {
        consume('[');
        List<Object> list = new ArrayList<>();
        skipWhitespace();

        if (peek() == ']') {
            consume(']');
            return list;
        }

        while (true) {
            list.add(parseValue());
            skipWhitespace();
            char next = peek();
            if (next == ']') {
                consume(']');
                break;
            }
            consume(',');
            skipWhitespace();
        }
        return list;
    }

    private String parseString() {
        consume('"');
        StringBuilder sb = new StringBuilder();
        while (pos < length) {
            char c = src.charAt(pos++);
            if (c == '"') return sb.toString();
            if (c == '\\') {
                char esc = src.charAt(pos++);
                sb.append(switch (esc) {
                    case 'b' -> '\b';
                    case 'f' -> '\f';
                    case 'n' -> '\n';
                    case 'r' -> '\r';
                    case 't' -> '\t';
                    case 'u' -> {
                        String hex = src.substring(pos, pos + 4);
                        pos += 4;
                        yield (char) Integer.parseInt(hex, 16);
                    }
                    default -> esc;
                });
            } else sb.append(c);
        }
        throw new IllegalArgumentException("Unterminated string");
    }

    private Number parseNumber() {
        int start = pos;
        if (peek() == '-') pos++;
        while (pos < length && (Character.isDigit(peek()) || peek() == '.' || peek() == 'e' || peek() == 'E' || peek() == '+')) {
            pos++;
        }
        String raw = src.substring(start, pos);
        if (raw.contains(".") || raw.contains("e") || raw.contains("E")) {
            return Double.parseDouble(raw);
        }
        return Long.parseLong(raw);
    }

    private boolean parseBoolean() {
        if (src.startsWith("true", pos)) { pos += 4; return true; }
        if (src.startsWith("false", pos)) { pos += 5; return false; }
        throw new IllegalArgumentException("Expected boolean at " + pos);
    }

    private Object parseNull() {
        if (src.startsWith("null", pos)) { pos += 4; return null; }
        throw new IllegalArgumentException("Expected null at " + pos);
    }

    private char peek() { return pos < length ? src.charAt(pos) : 0; }

    private void consume(char expected) {
        if (peek() != expected) throw new IllegalArgumentException("Expected '" + expected + "' but got '" + peek() + "' at index " + pos);
        pos++;
    }

    private void skipWhitespace() {
        while (pos < length && Character.isWhitespace(src.charAt(pos))) pos++;
    }

    private static String readerToString(Reader reader) throws Exception {
        StringBuilder sb = new StringBuilder(4096);
        char[] buf = new char[4096];
        int r, total = 0;
        while ((r = reader.read(buf)) != -1) {
            total += r;
            if (total > MAX_JSON_SIZE) throw new SecurityException("Payload too large");
            sb.append(buf, 0, r);
        }
        return sb.toString();
    }
}