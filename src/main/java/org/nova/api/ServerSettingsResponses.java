package org.nova.api;

import cn.nukkit.plugin.Plugin;

import java.util.Objects;

/**
 * Factory helpers for response processors and standard error handling adapters.
 */
public final class ServerSettingsResponses {

    /**
     * Utility class.
     */
    private ServerSettingsResponses() {
    }

    /**
     * Wraps a raw handler as processor.
     *
     * @param handler raw response handler
     * @return processor adapter
     */
    public static ServerSettingsResponseProcessor processor(ServerSettingsResponseHandler handler) {
        Objects.requireNonNull(handler, "handler");
        return handler::handle;
    }

    /**
     * Wraps a parsed handler as processor.
     *
     * @param handler parsed handler
     * @return processor adapter
     */
    public static ServerSettingsResponseProcessor parsed(ServerSettingsParsedResponseHandler handler) {
        Objects.requireNonNull(handler, "handler");
        return context -> handler.handle(context, context.requireValues());
    }

    /**
     * Wraps a parsed processor as processor.
     *
     * @param processor parsed processor
     * @return processor adapter
     */
    public static ServerSettingsResponseProcessor parsed(ServerSettingsParsedResponseProcessor processor) {
        Objects.requireNonNull(processor, "processor");
        return context -> processor.process(context, context.requireValues());
    }

    /**
     * Creates a standard logger-backed error handler.
     *
     * @param plugin plugin used for logging
     * @param prefix optional message prefix
     * @return error handler
     */
    public static ServerSettingsResponseErrorHandler loggingErrors(Plugin plugin, String prefix) {
        Objects.requireNonNull(plugin, "plugin");
        String safePrefix = prefix == null ? "" : prefix;

        return (context, exception) -> plugin.getLogger().warning(
                safePrefix + "Response processing failed for " + context.getOwner().getName() + "/" + context.getKey(),
                exception
        );
    }
}
