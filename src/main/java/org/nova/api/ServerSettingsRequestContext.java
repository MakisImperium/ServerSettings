package org.nova.api;

import cn.nukkit.Player;
import cn.nukkit.plugin.Plugin;

import java.util.Objects;

/**
 * Immutable context passed to payload providers for one player request.
 */
public final class ServerSettingsRequestContext {

    private final Player player;
    private final int formId;
    private final String key;
    private final Plugin owner;

    /**
     * Creates request context.
     *
     * @param player target player
     * @param formId assigned packet form id
     * @param key plugin-local registration key
     * @param owner owner plugin
     */
    public ServerSettingsRequestContext(Player player, int formId, String key, Plugin owner) {
        this.player = Objects.requireNonNull(player, "player");
        this.formId = formId;
        this.key = Objects.requireNonNull(key, "key");
        this.owner = Objects.requireNonNull(owner, "owner");
    }

    /**
     * @return target player
     */
    public Player getPlayer() {
        return this.player;
    }

    /**
     * @return assigned packet form id
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
}
