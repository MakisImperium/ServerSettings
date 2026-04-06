package org.nova.api;

import cn.nukkit.plugin.Plugin;

/**
 * Handle to one active settings registration.
 */
public interface ServerSettingsRegistration {

    /**
     * @return assigned packet form id
     */
    int getFormId();

    /**
     * @return plugin-local registration key
     */
    String getKey();

    /**
     * @return owner plugin
     */
    Plugin getOwner();

    /**
     * @return {@code true} if this handle is still active
     */
    boolean isRegistered();

    /**
     * Unregisters this definition.
     *
     * @return {@code true} if unregistered by this call
     */
    boolean unregister();
}
