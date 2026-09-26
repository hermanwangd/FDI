package com.featuredeliveryintelligence.fdi.shared;

import java.util.*;

public final class RuntimeMaps {
    private RuntimeMaps() {}

    public static String requiredString(Map<String, ?> value, String key) {
        Object found = value.get(key);
        if (!(found instanceof String text) || text.isBlank()) throw new RuntimeContractException(key + " is required");
        return text;
    }

    public static int requiredPositiveInt(Map<String, ?> value, String key) {
        Object found = value.get(key);
        if (!(found instanceof Number number) || number.intValue() < 1) throw new RuntimeContractException(key + " must be positive");
        return number.intValue();
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> requiredMap(Map<String, ?> value, String key) {
        Object found = value.get(key);
        if (!(found instanceof Map<?, ?>)) throw new RuntimeContractException(key + " must be an object");
        return (Map<String, Object>) found;
    }

    @SuppressWarnings("unchecked")
    public static List<Object> list(Map<String, ?> value, String key) {
        Object found = value.get(key);
        return found instanceof List<?> items ? (List<Object>) items : List.of();
    }

    public static Map<String, Object> copy(Map<String, ?> source) {
        return new LinkedHashMap<>(source);
    }
}
