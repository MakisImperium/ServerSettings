package org.nova.api;

/**
 * Immutable runtime metrics snapshot of the API engine.
 */
public final class ServerSettingsApiStats {

    private final int registrations;
    private final long requestEvents;
    private final long packetsSent;
    private final long responsePackets;
    private final long responsesProcessed;
    private final long responsesFailed;
    private final long responsesIgnored;

    /**
     * Creates a metrics snapshot.
     *
     * @param registrations active registration count
     * @param requestEvents number of settings request events
     * @param packetsSent number of sent {@code ServerSettingsResponsePacket} packets
     * @param responsePackets number of matching response packets seen
     * @param responsesProcessed number of successfully handled responses
     * @param responsesFailed number of handler failures
     * @param responsesIgnored number of ignored responses
     */
    public ServerSettingsApiStats(
            int registrations,
            long requestEvents,
            long packetsSent,
            long responsePackets,
            long responsesProcessed,
            long responsesFailed,
            long responsesIgnored
    ) {
        this.registrations = registrations;
        this.requestEvents = requestEvents;
        this.packetsSent = packetsSent;
        this.responsePackets = responsePackets;
        this.responsesProcessed = responsesProcessed;
        this.responsesFailed = responsesFailed;
        this.responsesIgnored = responsesIgnored;
    }

    /**
     * @return number of active registrations
     */
    public int getRegistrations() {
        return this.registrations;
    }

    /**
     * @return total number of server-settings request events
     */
    public long getRequestEvents() {
        return this.requestEvents;
    }

    /**
     * @return total number of sent server-settings response packets
     */
    public long getPacketsSent() {
        return this.packetsSent;
    }

    /**
     * @return total number of response packets observed for known form ids
     */
    public long getResponsePackets() {
        return this.responsePackets;
    }

    /**
     * @return total number of responses processed without error
     */
    public long getResponsesProcessed() {
        return this.responsesProcessed;
    }

    /**
     * @return total number of responses that failed in handler logic
     */
    public long getResponsesFailed() {
        return this.responsesFailed;
    }

    /**
     * @return total number of responses ignored by routing/filters
     */
    public long getResponsesIgnored() {
        return this.responsesIgnored;
    }
}
