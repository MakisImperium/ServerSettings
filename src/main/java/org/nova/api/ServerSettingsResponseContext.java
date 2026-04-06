package org.nova.api;

import cn.nukkit.Player;
import cn.nukkit.plugin.Plugin;

import java.util.Objects;
import java.util.Optional;

/**
 * Immutable response metadata plus lazy parsed accessors for custom-form value arrays.
 */
public final class ServerSettingsResponseContext {

    private final Player player;
    private final int formId;
    private final String key;
    private final Plugin owner;
    private final String rawData;
    private final int cancelReason;
    private final boolean closed;
    private volatile boolean valuesParsed;
    private volatile ServerSettingsResponseValues parsedValues;

    /**
     * Creates response context.
     *
     * @param player player that submitted the response
     * @param formId packet form id
     * @param key plugin-local registration key
     * @param owner owner plugin
     * @param rawData raw payload from {@code ModalFormResponsePacket#data}
     * @param cancelReason packet cancel reason
     * @param closed true when response represents a closed form
     */
    public ServerSettingsResponseContext(
            Player player,
            int formId,
            String key,
            Plugin owner,
            String rawData,
            int cancelReason,
            boolean closed
    ) {
        this.player = Objects.requireNonNull(player, "player");
        this.formId = formId;
        this.key = Objects.requireNonNull(key, "key");
        this.owner = Objects.requireNonNull(owner, "owner");
        this.rawData = rawData;
        this.cancelReason = cancelReason;
        this.closed = closed;
    }

    /**
     * @return source player
     */
    public Player getPlayer() {
        return this.player;
    }

    /**
     * @return packet form id
     */
    public int getFormId() {
        return this.formId;
    }

    /**
     * @return plugin-local registration key
     */
    public String getKey() {
        return this.key;
    }

    /**
     * @return owner plugin
     */
    public Plugin getOwner() {
        return this.owner;
    }

    /**
     * @return raw response payload string
     */
    public String getRawData() {
        return this.rawData;
    }

    /**
     * @return packet cancel reason value
     */
    public int getCancelReason() {
        return this.cancelReason;
    }

    /**
     * @return {@code true} when the form was closed/cancelled
     */
    public boolean isClosed() {
        return this.closed;
    }

    /**
     * @return {@code true} if raw payload contains non-empty data
     */
    public boolean hasRawData() {
        return this.rawData != null && !this.rawData.trim().isEmpty() && !"null".equalsIgnoreCase(this.rawData.trim());
    }

    /**
     * Parses and caches typed response values.
     *
     * @return optional parsed values wrapper
     */
    public Optional<ServerSettingsResponseValues> parseValues() {
        if (!this.valuesParsed) {
            synchronized (this) {
                if (!this.valuesParsed) {
                    this.parsedValues = ServerSettingsResponseValues.parse(this.rawData).orElse(null);
                    this.valuesParsed = true;
                }
            }
        }
        return Optional.ofNullable(this.parsedValues);
    }

    /**
     * Parses values or throws when payload is not a valid custom-form response array.
     *
     * @return parsed values
     */
    public ServerSettingsResponseValues requireValues() {
        return parseValues().orElseThrow(
                () -> new IllegalStateException(
                        "Response payload is not a valid custom_form array: " + this.rawData
                )
        );
    }

    /**
     * Reads boolean at index with fallback.
     *
     * @param index value index
     * @param fallback fallback value
     * @return parsed or fallback value
     */
    public boolean booleanAt(int index, boolean fallback) {
        return parseValues().map(values -> values.getBoolean(index, fallback)).orElse(fallback);
    }

    /**
     * Reads integer at index with fallback.
     *
     * @param index value index
     * @param fallback fallback value
     * @return parsed or fallback value
     */
    public int intAt(int index, int fallback) {
        return parseValues().map(values -> values.getInt(index, fallback)).orElse(fallback);
    }

    /**
     * Reads double at index with fallback.
     *
     * @param index value index
     * @param fallback fallback value
     * @return parsed or fallback value
     */
    public double doubleAt(int index, double fallback) {
        return parseValues().map(values -> values.getDouble(index, fallback)).orElse(fallback);
    }

    /**
     * Reads string at index with fallback.
     *
     * @param index value index
     * @param fallback fallback value
     * @return parsed or fallback value
     */
    public String stringAt(int index, String fallback) {
        return parseValues().map(values -> values.getString(index, fallback)).orElse(fallback);
    }
}
