package org.nova.api;

import java.util.List;
import java.util.Objects;

/**
 * Lightweight JSON builder for Bedrock {@code custom_form} payloads sent via {@code ServerSettingsResponsePacket}.
 */
public final class ServerSettingsJson {

    /**
     * Utility class.
     */
    private ServerSettingsJson() {
    }

    /**
     * Starts a custom form JSON builder.
     *
     * @param title form title
     * @return custom form builder
     */
    public static CustomFormBuilder customForm(String title) {
        return new CustomFormBuilder(title);
    }

    /**
     * Builder for a single {@code custom_form} payload.
     */
    public static final class CustomFormBuilder {

        private final String title;
        private final StringBuilder content = new StringBuilder();
        private boolean firstElement = true;

        /**
         * Creates a builder.
         *
         * @param title form title
         */
        private CustomFormBuilder(String title) {
            this.title = Objects.requireNonNull(title, "title");
        }

        /**
         * Adds a label component.
         *
         * @param text label text
         * @return this builder
         */
        public CustomFormBuilder label(String text) {
            appendElement("{\"type\":\"label\",\"text\":" + quote(text) + "}");
            return this;
        }

        /**
         * Adds a toggle component.
         *
         * @param text toggle text
         * @param defaultValue default value
         * @return this builder
         */
        public CustomFormBuilder toggle(String text, boolean defaultValue) {
            appendElement(
                    "{\"type\":\"toggle\",\"text\":" + quote(text) + ",\"default\":" + defaultValue + "}"
            );
            return this;
        }

        /**
         * Adds an input component.
         *
         * @param text input title
         * @param placeholder placeholder text
         * @param defaultValue default value
         * @return this builder
         */
        public CustomFormBuilder input(String text, String placeholder, String defaultValue) {
            appendElement(
                    "{\"type\":\"input\",\"text\":"
                            + quote(text)
                            + ",\"placeholder\":"
                            + quote(placeholder)
                            + ",\"default\":"
                            + quote(defaultValue)
                            + "}"
            );
            return this;
        }

        /**
         * Adds a slider component.
         *
         * @param text slider title
         * @param min minimum
         * @param max maximum
         * @param step step width
         * @param defaultValue default value
         * @return this builder
         */
        public CustomFormBuilder slider(String text, double min, double max, double step, double defaultValue) {
            requireFinite(min, "min");
            requireFinite(max, "max");
            requireFinite(step, "step");
            requireFinite(defaultValue, "defaultValue");

            if (max < min) {
                throw new IllegalArgumentException("max must be >= min");
            }
            if (step <= 0d) {
                throw new IllegalArgumentException("step must be > 0");
            }

            appendElement(
                    "{\"type\":\"slider\",\"text\":"
                            + quote(text)
                            + ",\"min\":"
                            + min
                            + ",\"max\":"
                            + max
                            + ",\"step\":"
                            + step
                            + ",\"default\":"
                            + defaultValue
                            + "}"
            );
            return this;
        }

        /**
         * Adds a step slider component.
         *
         * @param text component title
         * @param steps available values
         * @param defaultIndex default selected index
         * @return this builder
         */
        public CustomFormBuilder stepSlider(String text, List<String> steps, int defaultIndex) {
            validateOptions(steps, defaultIndex, "steps");

            appendElement(
                    "{\"type\":\"step_slider\",\"text\":"
                            + quote(text)
                            + ",\"steps\":"
                            + toStringArray(steps)
                            + ",\"default\":"
                            + defaultIndex
                            + "}"
            );
            return this;
        }

        /**
         * Adds a dropdown component.
         *
         * @param text component title
         * @param options available options
         * @param defaultIndex default selected index
         * @return this builder
         */
        public CustomFormBuilder dropdown(String text, List<String> options, int defaultIndex) {
            validateOptions(options, defaultIndex, "options");

            appendElement(
                    "{\"type\":\"dropdown\",\"text\":"
                            + quote(text)
                            + ",\"options\":"
                            + toStringArray(options)
                            + ",\"default\":"
                            + defaultIndex
                            + "}"
            );
            return this;
        }

        /**
         * Builds the final {@code custom_form} JSON payload.
         *
         * @return JSON payload
         */
        public String build() {
            return "{\"type\":\"custom_form\",\"title\":" + quote(this.title) + ",\"content\":[" + this.content + "]}";
        }

        /**
         * Appends one component JSON element.
         *
         * @param jsonElement raw element JSON
         */
        private void appendElement(String jsonElement) {
            if (!this.firstElement) {
                this.content.append(',');
            }
            this.content.append(jsonElement);
            this.firstElement = false;
        }

        /**
         * Validates list options and default index.
         *
         * @param options options list
         * @param defaultIndex default index
         * @param fieldName field name for error message
         */
        private static void validateOptions(List<String> options, int defaultIndex, String fieldName) {
            Objects.requireNonNull(options, fieldName);
            if (options.isEmpty()) {
                throw new IllegalArgumentException(fieldName + " must not be empty");
            }
            if (defaultIndex < 0 || defaultIndex >= options.size()) {
                throw new IllegalArgumentException("defaultIndex out of range for " + fieldName);
            }
        }
    }

    /**
     * Serializes a string list as JSON array.
     *
     * @param values source values
     * @return serialized JSON array
     */
    private static String toStringArray(List<String> values) {
        StringBuilder builder = new StringBuilder();
        builder.append('[');

        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append(quote(values.get(i)));
        }

        builder.append(']');
        return builder.toString();
    }

    /**
     * Ensures a numeric value is finite.
     *
     * @param value value to check
     * @param fieldName field name for error message
     */
    private static void requireFinite(double value, String fieldName) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(fieldName + " must be finite");
        }
    }

    /**
     * Escapes and quotes a string as JSON literal.
     *
     * @param value input value
     * @return JSON string literal
     */
    private static String quote(String value) {
        if (value == null) {
            return "\"\"";
        }

        StringBuilder escaped = new StringBuilder(value.length() + 16);
        escaped.append('"');

        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);

            switch (c) {
                case '\\' -> escaped.append("\\\\");
                case '"' -> escaped.append("\\\"");
                case '\b' -> escaped.append("\\b");
                case '\f' -> escaped.append("\\f");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> {
                    if (c < 0x20) {
                        escaped.append("\\u");
                        appendHex4(escaped, c);
                    } else {
                        escaped.append(c);
                    }
                }
            }
        }

        escaped.append('"');
        return escaped.toString();
    }

    /**
     * Appends a four-digit lower-case hex unicode escape.
     *
     * @param target output buffer
     * @param value codepoint value
     */
    private static void appendHex4(StringBuilder target, int value) {
        String hex = Integer.toHexString(value);
        for (int i = hex.length(); i < 4; i++) {
            target.append('0');
        }
        target.append(hex);
    }
}
