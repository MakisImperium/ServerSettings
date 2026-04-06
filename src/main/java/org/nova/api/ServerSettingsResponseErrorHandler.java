package org.nova.api;

/**
 * Callback invoked when response processing throws an exception.
 */
@FunctionalInterface
public interface ServerSettingsResponseErrorHandler {

    /**
     * Handles a processing error.
     *
     * @param context response context
     * @param exception thrown exception
     */
    void onError(ServerSettingsResponseContext context, Exception exception);
}
