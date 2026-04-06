package org.nova;

import cn.nukkit.plugin.PluginBase;
import cn.nukkit.plugin.service.ServicePriority;
import org.nova.api.ServerSettingsApi;

/**
 * Bootstrap plugin that registers and exposes the {@link ServerSettingsApi} service.
 */
public final class Main extends PluginBase {

    private PacketServerSettingsApi api;

    /**
     * Initializes the packet engine, registers listeners and publishes the API service.
     */
    @Override
    public void onEnable() {
        this.api = new PacketServerSettingsApi(this);

        getServer().getPluginManager().registerEvents(this.api, this);
        getServer().getServiceManager().register(
                ServerSettingsApi.class,
                this.api,
                this,
                ServicePriority.NORMAL
        );
    }

    /**
     * Unregisters the API service and clears all runtime state.
     */
    @Override
    public void onDisable() {
        getServer().getServiceManager().cancel(this);

        if (this.api != null) {
            this.api.shutdown();
            this.api = null;
        }
    }
}
