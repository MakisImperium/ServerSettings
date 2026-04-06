package org.nova.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;

/**
 * Typed accessor wrapper around custom-form response value arrays.
 */
public final class ServerSettingsResponseValues {

    private static final ServerSettingsResponseValues EMPTY = new ServerSettingsResponseValues(List.of());

    private final List<JsonElement> values;

    /**
     * Creates immutable wrapper over parsed elements.
     *
     * @param values parsed values
     */
    private ServerSettingsResponseValues(List<JsonElement> values) {
        this.values = Collections.unmodifiableList(new ArrayList<>(values));
    }

    /**
     * Parses raw response payload.
     *
     * @param rawData payload from {@code ModalFormResponsePacket#data}
     * @return optional parsed values wrapper
     */
    public static Optional<ServerSettingsResponseValues> parse(String rawData) {
        if (rawData == null) {
            return Optional.empty();
        }

        String trimmed = rawData.trim();
        if (trimmed.isEmpty() || "null".equalsIgnoreCase(trimmed)) {
            return Optional.empty();
        }

        try {
            JsonElement root = JsonParser.parseString(trimmed);
            if (!root.isJsonArray()) {
                return Optional.empty();
            }

            JsonArray array = root.getAsJsonArray();
            List<JsonElement> values = new ArrayList<>(array.size());
            for (JsonElement value : array) {
                values.add(value == null ? JsonNull.INSTANCE : value);
            }
            return Optional.of(values.isEmpty() ? EMPTY : new ServerSettingsResponseValues(values));
        } catch (JsonParseException ignored) {
            return Optional.empty();
        }
    }

    /**
     * @return number of values
     */
    public int size() {
        return this.values.size();
    }

    /**
     * @return {@code true} when no values are present
     */
    public boolean isEmpty() {
        return this.values.isEmpty();
    }

    /**
     * Checks index bounds.
     *
     * @param index index to test
     * @return {@code true} for valid index
     */
    public boolean hasIndex(int index) {
        return index >= 0 && index < this.values.size();
    }

    /**
     * Returns immutable raw element list.
     *
     * @return immutable list view
     */
    public List<JsonElement> asList() {
        return this.values;
    }

    /**
     * Iterates over all values with index.
     *
     * @param consumer index+element consumer
     */
    public void forEach(BiConsumer<Integer, JsonElement> consumer) {
        Objects.requireNonNull(consumer, "consumer");
        for (int i = 0; i < this.values.size(); i++) {
            consumer.accept(i, this.values.get(i));
        }
    }

    /**
     * Checks if value at index is JSON null or string literal "null".
     *
     * @param index target index
     * @return true if value semantically represents null
     */
    public boolean isNull(int index) {
        Optional<JsonElement> value = get(index);
        return value.isPresent() && (value.get().isJsonNull() || isNullLiteral(value.get()));
    }

    /**
     * Returns raw JSON element at index.
     *
     * @param index target index
     * @return optional element
     */
    public Optional<JsonElement> get(int index) {
        if (!hasIndex(index)) {
            return Optional.empty();
        }
        return Optional.ofNullable(this.values.get(index));
    }

    /**
     * Reads boolean with fallback.
     *
     * @param index target index
     * @param fallback fallback value
     * @return parsed boolean or fallback
     */
    public boolean getBoolean(int index, boolean fallback) {
        Optional<JsonPrimitive> primitive = asPrimitive(index);
        if (primitive.isEmpty()) {
            return fallback;
        }

        JsonPrimitive value = primitive.get();
        if (value.isBoolean()) {
            return value.getAsBoolean();
        }
        if (value.isString()) {
            String text = value.getAsString().trim();
            if ("true".equalsIgnoreCase(text)) {
                return true;
            }
            if ("false".equalsIgnoreCase(text)) {
                return false;
            }
        }
        return fallback;
    }

    /**
     * Reads integer with fallback.
     *
     * @param index target index
     * @param fallback fallback value
     * @return parsed int or fallback
     */
    public int getInt(int index, int fallback) {
        Optional<JsonPrimitive> primitive = asPrimitive(index);
        if (primitive.isEmpty()) {
            return fallback;
        }

        JsonPrimitive value = primitive.get();
        try {
            if (value.isNumber()) {
                return value.getAsInt();
            }
            if (value.isString()) {
                return Integer.parseInt(value.getAsString().trim());
            }
        } catch (Exception ignored) {
            return fallback;
        }

        return fallback;
    }

    /**
     * Reads double with fallback.
     *
     * @param index target index
     * @param fallback fallback value
     * @return parsed double or fallback
     */
    public double getDouble(int index, double fallback) {
        Optional<JsonPrimitive> primitive = asPrimitive(index);
        if (primitive.isEmpty()) {
            return fallback;
        }

        JsonPrimitive value = primitive.get();
        try {
            if (value.isNumber()) {
                return value.getAsDouble();
            }
            if (value.isString()) {
                return Double.parseDouble(value.getAsString().trim());
            }
        } catch (Exception ignored) {
            return fallback;
        }

        return fallback;
    }

    /**
     * Reads string with fallback.
     *
     * @param index target index
     * @param fallback fallback value
     * @return parsed string or fallback
     */
    public String getString(int index, String fallback) {
        Objects.requireNonNull(fallback, "fallback");

        Optional<JsonPrimitive> primitive = asPrimitive(index);
        if (primitive.isEmpty()) {
            return fallback;
        }

        JsonPrimitive value = primitive.get();
        if (value.isString() || value.isBoolean() || value.isNumber()) {
            return value.getAsString();
        }
        return fallback;
    }

    /**
     * Resolves primitive value at index while filtering null semantics.
     *
     * @param index target index
     * @return optional primitive element
     */
    private Optional<JsonPrimitive> asPrimitive(int index) {
        Optional<JsonElement> value = get(index);
        if (value.isEmpty()) {
            return Optional.empty();
        }

        JsonElement element = value.get();
        if (element == null || element.isJsonNull() || isNullLiteral(element) || !element.isJsonPrimitive()) {
            return Optional.empty();
        }

        return Optional.of(element.getAsJsonPrimitive());
    }

    /**
     * Checks if element is the string literal "null".
     *
     * @param element input element
     * @return true if element equals "null" case-insensitively
     */
    private static boolean isNullLiteral(JsonElement element) {
        return element.isJsonPrimitive()
                && element.getAsJsonPrimitive().isString()
                && "null".equalsIgnoreCase(element.getAsString().trim());
    }
}
