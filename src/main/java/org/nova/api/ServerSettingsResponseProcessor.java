package org.nova.api;

/**
 * Processor-style variant for raw response handling.
 */
@FunctionalInterface
public interface ServerSettingsResponseProcessor {

    /**
     * Processes one response callback.
     *
     * @param context response context
     * @throws Exception any processing error
     */
    void process(ServerSettingsResponseContext context) throws Exception;
}
