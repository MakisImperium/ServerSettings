package org.nova.api;

import cn.nukkit.plugin.Plugin;

import java.util.Collection;
import java.util.Optional;

/**
 * Public service contract for packet-level server settings integrations.
 */
public interface ServerSettingsApi {

    /**
     * Registers a settings definition and assigns a form id.
     *
     * @param definition immutable definition
     * @return registration handle
     */
    ServerSettingsRegistration register(ServerSettingsDefinition definition);

    /**
     * Unregisters one definition by form id.
     *
     * @param formId packet form id
     * @return {@code true} when a registration was removed
     */
    boolean unregister(int formId);

    /**
     * Unregisters all definitions owned by a plugin.
     *
     * @param owner plugin owner
     * @return number of removed definitions
     */
    int unregisterOwnedBy(Plugin owner);

    /**
     * Finds one registration by form id.
     *
     * @param formId packet form id
     * @return optional registration
     */
    Optional<ServerSettingsRegistration> findRegistration(int formId);

    /**
     * Returns an immutable snapshot of active registrations.
     *
     * @return registration snapshot
     */
    Collection<ServerSettingsRegistration> getRegistrations();

    /**
     * Returns current runtime statistics.
     *
     * @return immutable statistics snapshot
     */
    ServerSettingsApiStats getStats();
}
