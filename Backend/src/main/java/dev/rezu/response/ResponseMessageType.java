package dev.rezu.response;

public enum ResponseMessageType {
    ERROR("error"), MESSAGE("message"), DATA("data");
    public final String key;

    ResponseMessageType(String key) {
        this.key = key;
    }
}
