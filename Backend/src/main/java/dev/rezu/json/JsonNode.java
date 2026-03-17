package dev.rezu.json;

import java.time.Instant;
import java.util.*;
import java.util.function.Function;

public record JsonNode(Map<String, Object> data) {

    private Optional<String> strOptional(String key) {
        return Optional.ofNullable(data.get(key))
                .filter(String.class::isInstance)
                .map(String.class::cast);
    }

    private Optional<Number> numberOptional(String key) {
        return Optional.ofNullable(data.get(key))
                .filter(Number.class::isInstance)
                .map(Number.class::cast);
    }

    private Optional<Boolean> boolOptional(String key) {
        return Optional.ofNullable(data.get(key))
                .filter(Boolean.class::isInstance)
                .map(Boolean.class::cast);
    }

    private Optional<Instant> instantOptional(String key) {
        return Optional.ofNullable(data.get(key))
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .filter(s -> !s.isBlank() && !"null".equals(s))
                .flatMap(s -> {
                    try { return Optional.of(Instant.parse(s)); }
                    catch (Exception _) { return Optional.empty(); }
                });
    }

    public String str(String key) { return strOptional(key).orElse(""); }
    public int integer(String key) { return numberOptional(key).map(Number::intValue).orElse(0); }
    public long lng(String key) { return numberOptional(key).map(Number::longValue).orElse(0L); }
    public double dbl(String key) { return numberOptional(key).map(Number::doubleValue).orElse(0.0); }
    public boolean bool(String key) { return boolOptional(key).orElse(false); }
    public Instant instant(String key) { return instantOptional(key).orElse(null); }

    public <T> List<T> array(String key, Function<JsonNode, T> mapper) {
        List<JsonNode> jsonNodes = array(key);
        List<T> result = new ArrayList<>(jsonNodes.size());
        for (JsonNode jsonNode : jsonNodes) result.add(mapper.apply(jsonNode));
        return result;
    }

    //suppressing warning since we guarantee type to be <String, Object>
    @SuppressWarnings("unchecked")
    public List<JsonNode> array(String key) {
        Object val = data.get(key);
        if (!(val instanceof List<?> list)) return List.of();
        return list.stream()
                .filter(Map.class::isInstance)
                .map(Map.class::cast)
                .map(m -> new JsonNode((Map<String, Object>) m))
                .toList();
    }

    public <T> T map(Function<JsonNode, T> mapper) { return mapper.apply(this); }
}
