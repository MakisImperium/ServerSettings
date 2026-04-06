package org.nova.api;

/**
 * Processor-style variant for typed/parsed response handling.
 */
@FunctionalInterface
public interface ServerSettingsParsedResponseProcessor {

    /**
     * Processes a parsed settings response.
     *
     * @param context raw context metadata
     * @param values parsed value accessor
     * @throws Exception any processing error
     */
    void process(ServerSettingsResponseContext context, ServerSettingsResponseValues values) throws Exception;
}
