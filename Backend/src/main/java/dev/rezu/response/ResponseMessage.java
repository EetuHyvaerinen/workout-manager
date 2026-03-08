package dev.rezu.response;

import com.sun.net.httpserver.HttpExchange;
import dev.rezu.json.JsonWriter;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class ResponseMessage {

    public static void send(HttpExchange exchange, int statusCode, ResponseMessageType msgType, Object data) throws IOException {
        if (!isBodyAllowed(statusCode)) {
            exchange.sendResponseHeaders(statusCode, -1);
            return;
        }

        JsonWriter writer = new JsonWriter();
        writer.beginObject();
        switch (msgType) {
            case DATA -> writer.field(msgType.key, data);
            case MESSAGE, ERROR -> writer.field(msgType.key, data == null ? "" : data.toString());
        }
        writer.endObject();

        byte[] bytes = writer.toString().getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    public static void send(HttpExchange exchange, int statusCode, String rawJson) throws IOException {
        if (!isBodyAllowed(statusCode)) {
            exchange.sendResponseHeaders(statusCode, -1);
            return;
        }

        byte[] bytes = rawJson.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static boolean isBodyAllowed(int statusCode) {
        return statusCode != 204 && statusCode != 304 && !(statusCode >= 100 && statusCode < 200);
    }
}