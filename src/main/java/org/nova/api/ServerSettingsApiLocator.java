package org.nova.api;

import cn.nukkit.Server;
import cn.nukkit.plugin.service.RegisteredServiceProvider;

import java.util.Objects;
import java.util.Optional;

/**
 * Helper utility for resolving the {@link ServerSettingsApi} service from Nukkit's service manager.
 */
public final class ServerSettingsApiLocator {

    /**
     * Utility class.
     */
    private ServerSettingsApiLocator() {
    }

    /**
     * Tries to find a registered {@link ServerSettingsApi} service.
     *
     * @param server nukkit server
     * @return optional service instance
     */
    public static Optional<ServerSettingsApi> find(Server server) {
        Objects.requireNonNull(server, "server");

        RegisteredServiceProvider<ServerSettingsApi> provider =
                server.getServiceManager().getProvider(ServerSettingsApi.class);

        if (provider == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(provider.getProvider());
    }

    /**
     * Resolves the API or throws if the provider plugin is missing/not enabled.
     *
     * @param server nukkit server
     * @return resolved service
     */
    public static ServerSettingsApi require(Server server) {
        return find(server).orElseThrow(
                () -> new IllegalStateException("ServerSettingsApi service is not registered")
        );
    }
}
