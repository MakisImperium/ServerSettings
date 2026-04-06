package org.nova.api;

/**
 * Callback for typed/parsed response handling.
 */
@FunctionalInterface
public interface ServerSettingsParsedResponseHandler {

    /**
     * Handles a parsed settings response.
     *
     * @param context raw context metadata
     * @param values parsed value accessor
     * @throws Exception any processing error
     */
    void handle(ServerSettingsResponseContext context, ServerSettingsResponseValues values) throws Exception;
}
