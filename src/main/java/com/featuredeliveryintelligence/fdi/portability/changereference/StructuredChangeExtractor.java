package com.featuredeliveryintelligence.fdi.portability.changereference;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

/** Structured extraction (SF-BL-004 Task 2). For JSON and .properties changes, exact
 *  parsing produces stable JSON Pointer or configuration-key hints carried in the excerpt
 *  context. When exact parsing does not succeed, extraction falls back to verified text
 *  hunks. Inputs that parse but carry no semantic difference (metadata-only changes such
 *  as formatting) produce no excerpt. All other paths delegate to the text extractor. */
final class StructuredChangeExtractor {
    private static final JsonMapper MAPPER = new JsonMapper();
    private final TextChangeExtractor text;

    StructuredChangeExtractor(TextChangeExtractor text) {
        this.text = text;
    }

    List<ChangeExcerpt> extract(ChangedPath change) {
        String path = change.newPath() == null ? change.oldPath() : change.newPath();
        if (change.operation() == Operation.ADD) {
            return text.extract(change);
        }
        if (path.endsWith(".json")) {
            return hinted(text.extract(change), jsonPointers(change));
        }
        if (path.endsWith(".properties")) {
            return hinted(text.extract(change), propertyKeys(change));
        }
        return text.extract(change);
    }

    private static List<ChangeExcerpt> hinted(List<ChangeExcerpt> base, List<String> hints) {
        if (hints == null) {
            return base;
        }
        if (hints.isEmpty() || base.isEmpty()) {
            return List.of();
        }
        ChangeExcerpt first = base.get(0);
        return List.of(new ChangeExcerpt(first.oldStart(), first.oldEnd(), first.newStart(), first.newEnd(),
                String.join(",", hints), first.before(), first.after(), first.unifiedDiff(), first.truncated()));
    }

    private static List<String> jsonPointers(ChangedPath change) {
        JsonNode oldNode = parse(change.oldBytes());
        JsonNode newNode = parse(change.newBytes());
        if (oldNode == null || newNode == null) {
            return null;
        }
        List<String> pointers = new ArrayList<>();
        diff(oldNode, newNode, "", pointers);
        return pointers.stream().sorted().limit(5).toList();
    }

    private static JsonNode parse(byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        try {
            return MAPPER.readTree(new String(bytes, StandardCharsets.UTF_8));
        } catch (Exception e) {
            return null;
        }
    }

    private static void diff(JsonNode oldNode, JsonNode newNode, String pointer, List<String> out) {
        if (oldNode.equals(newNode)) {
            return;
        }
        if (oldNode.isObject() && newNode.isObject()) {
            java.util.Iterator<String> names = new TreeSet<>(collectFieldNames(oldNode, newNode)).iterator();
            while (names.hasNext()) {
                String name = names.next();
                String child = pointer + "/" + name.replace("~", "~0").replace("/", "~1");
                if (!oldNode.has(name) || !newNode.has(name)) {
                    out.add(child);
                } else {
                    diff(oldNode.get(name), newNode.get(name), child, out);
                }
            }
        } else if (oldNode.isArray() && newNode.isArray()) {
            int size = Math.max(oldNode.size(), newNode.size());
            for (int i = 0; i < size; i++) {
                String child = pointer + "/" + i;
                if (i >= oldNode.size() || i >= newNode.size()) {
                    out.add(child);
                } else {
                    diff(oldNode.get(i), newNode.get(i), child, out);
                }
            }
        } else {
            out.add(pointer.isEmpty() ? "/" : pointer);
        }
    }

    private static java.util.Set<String> collectFieldNames(JsonNode a, JsonNode b) {
        java.util.Set<String> names = new TreeSet<>();
        a.fieldNames().forEachRemaining(names::add);
        b.fieldNames().forEachRemaining(names::add);
        return names;
    }

    private static List<String> propertyKeys(ChangedPath change) {
        Map<String, String> oldProps = parseProperties(change.oldBytes());
        Map<String, String> newProps = parseProperties(change.newBytes());
        if (oldProps == null || newProps == null) {
            return null;
        }
        java.util.Set<String> keys = new TreeSet<>(oldProps.keySet());
        keys.addAll(newProps.keySet());
        keys.removeIf(key -> oldProps.getOrDefault(key, "").equals(newProps.getOrDefault(key, "")));
        return keys.stream().limit(5).toList();
    }

    private static Map<String, String> parseProperties(byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        Map<String, String> properties = new TreeMap<>();
        for (String line : new String(bytes, StandardCharsets.UTF_8).split("\n", -1)) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("!")) {
                continue;
            }
            int separator = trimmed.indexOf('=');
            if (separator <= 0) {
                return null;
            }
            properties.put(trimmed.substring(0, separator).trim(), trimmed.substring(separator + 1).trim());
        }
        return properties;
    }
}
