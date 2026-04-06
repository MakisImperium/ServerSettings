package org.nova.api;

/**
 * Callback for raw response handling.
 */
@FunctionalInterface
public interface ServerSettingsResponseHandler {

    /**
     * Handles one response callback.
     *
     * @param context response context
     * @throws Exception any processing error
     */
    void handle(ServerSettingsResponseContext context) throws Exception;
}
