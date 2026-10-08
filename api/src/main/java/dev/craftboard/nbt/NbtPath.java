package dev.craftboard.nbt;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Typed lookups that tolerate missing keys and the type changes between game versions. */
public final class NbtPath {

    private NbtPath() {
    }

    @SuppressWarnings("unchecked")
    public static Optional<Map<String, Object>> compound(Map<String, Object> parent, String key) {
        return parent.get(key) instanceof Map<?, ?> map ? Optional.of((Map<String, Object>) map) : Optional.empty();
    }

    public static Optional<List<?>> list(Map<String, Object> parent, String key) {
        return parent.get(key) instanceof List<?> list ? Optional.of(list) : Optional.empty();
    }

    public static Optional<Number> number(Map<String, Object> parent, String key) {
        return parent.get(key) instanceof Number number ? Optional.of(number) : Optional.empty();
    }

    public static Optional<String> string(Map<String, Object> parent, String key) {
        return parent.get(key) instanceof String string ? Optional.of(string) : Optional.empty();
    }

    public static boolean flag(Map<String, Object> parent, String key) {
        return number(parent, key).map(n -> n.intValue() != 0).orElse(false);
    }
}
