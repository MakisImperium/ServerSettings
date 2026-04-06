package org.nova.api;

/**
 * Produces raw JSON payload for {@code ServerSettingsResponsePacket#data}.
 */
@FunctionalInterface
public interface ServerSettingsPayloadProvider {

    /**
     * Builds payload for one player request.
     *
     * @param context request context
     * @return JSON payload string
     * @throws Exception any payload creation error
     */
    String createPayload(ServerSettingsRequestContext context) throws Exception;
}
