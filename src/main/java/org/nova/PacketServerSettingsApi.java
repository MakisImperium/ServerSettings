package org.nova;

import cn.nukkit.Player;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerQuitEvent;
import cn.nukkit.event.player.PlayerServerSettingsRequestEvent;
import cn.nukkit.event.plugin.PluginDisableEvent;
import cn.nukkit.event.server.DataPacketReceiveEvent;
import cn.nukkit.network.protocol.DataPacket;
import cn.nukkit.network.protocol.ModalFormResponsePacket;
import cn.nukkit.network.protocol.ServerSettingsResponsePacket;
import cn.nukkit.plugin.Plugin;
import cn.nukkit.plugin.PluginBase;
import org.nova.api.ServerSettingsApi;
import org.nova.api.ServerSettingsApiStats;
import org.nova.api.ServerSettingsDefinition;
import org.nova.api.ServerSettingsRegistration;
import org.nova.api.ServerSettingsRequestContext;
import org.nova.api.ServerSettingsResponseContext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;

/**
 * Internal {@link ServerSettingsApi} implementation based on Bedrock packet events.
 * <p>
 * This class owns the full lifecycle:
 * <br>- register provider definitions
 * <br>- send {@link ServerSettingsResponsePacket} payloads when settings are requested
 * <br>- route matching {@link ModalFormResponsePacket} responses to registered handlers
 * <br>- keep runtime statistics and clean resources on plugin/player shutdown events
 */
final class PacketServerSettingsApi implements ServerSettingsApi, Listener {

    private static final int START_FORM_ID = 0x4E4F5600; // "NOVA" namespace for packet form ids.

    private final PluginBase plugin;
    private final AtomicInteger formCounter = new AtomicInteger(START_FORM_ID);

    private final Map<Integer, RegisteredEntry> registrationsByFormId = new HashMap<>();
    private final Map<String, Integer> formIdByOwnerAndKey = new HashMap<>();
    private final Map<Plugin, Set<Integer>> formIdsByOwner = new HashMap<>();
    private final Map<Long, Set<Integer>> pendingByPlayerId = new HashMap<>();

    private final LongAdder statRequestEvents = new LongAdder();
    private final LongAdder statPacketsSent = new LongAdder();
    private final LongAdder statResponsePackets = new LongAdder();
    private final LongAdder statResponsesProcessed = new LongAdder();
    private final LongAdder statResponsesFailed = new LongAdder();
    private final LongAdder statResponsesIgnored = new LongAdder();

    private boolean stopped;

    /**
     * Creates a new packet API engine.
     *
     * @param plugin owning plugin instance
     */
    PacketServerSettingsApi(PluginBase plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    /**
     * Registers a new server-settings definition and assigns a unique form id.
     *
     * @param definition immutable definition to register
     * @return active registration handle
     */
    @Override
    public ServerSettingsRegistration register(ServerSettingsDefinition definition) {
        Objects.requireNonNull(definition, "definition");

        synchronized (this) {
            ensureRunning();

            Plugin owner = definition.getOwner();
            if (!owner.isEnabled()) {
                throw new IllegalArgumentException("Owner plugin is not enabled: " + owner.getName());
            }

            String ownerKey = ownerKey(owner, definition.getKey());
            if (this.formIdByOwnerAndKey.containsKey(ownerKey)) {
                throw new IllegalArgumentException(
                        "Registration already exists for owner/key: " + owner.getName() + "/" + definition.getKey()
                );
            }

            int formId = allocateFormIdLocked();
            RegisteredEntry entry = new RegisteredEntry(this, formId, definition);

            this.registrationsByFormId.put(formId, entry);
            this.formIdByOwnerAndKey.put(ownerKey, formId);
            this.formIdsByOwner.computeIfAbsent(owner, ignored -> new HashSet<>()).add(formId);

            return entry;
        }
    }

    /**
     * Unregisters a definition by form id and clears all related runtime state.
     *
     * @param formId packet form id
     * @return {@code true} when a registration was removed
     */
    @Override
    public synchronized boolean unregister(int formId) {
        RegisteredEntry removed = this.registrationsByFormId.remove(formId);
        if (removed == null) {
            return false;
        }

        removed.markUnregistered();
        this.formIdByOwnerAndKey.remove(ownerKey(removed.getOwner(), removed.getKey()));

        Set<Integer> ownerSet = this.formIdsByOwner.get(removed.getOwner());
        if (ownerSet != null) {
            ownerSet.remove(formId);
            if (ownerSet.isEmpty()) {
                this.formIdsByOwner.remove(removed.getOwner());
            }
        }

        this.pendingByPlayerId.values().removeIf(formIds -> {
            formIds.remove(formId);
            return formIds.isEmpty();
        });

        return true;
    }

    /**
     * Unregisters all definitions owned by a plugin.
     *
     * @param owner owning plugin
     * @return number of removed registrations
     */
    @Override
    public synchronized int unregisterOwnedBy(Plugin owner) {
        Objects.requireNonNull(owner, "owner");

        Set<Integer> ids = this.formIdsByOwner.get(owner);
        if (ids == null || ids.isEmpty()) {
            return 0;
        }

        int removed = 0;
        for (int formId : new HashSet<>(ids)) {
            if (unregister(formId)) {
                removed++;
            }
        }

        return removed;
    }

    /**
     * Looks up a registration by form id.
     *
     * @param formId packet form id
     * @return optional registration
     */
    @Override
    public synchronized Optional<ServerSettingsRegistration> findRegistration(int formId) {
        return Optional.ofNullable(this.registrationsByFormId.get(formId));
    }

    /**
     * Returns a snapshot of all active registrations.
     *
     * @return immutable snapshot collection
     */
    @Override
    public synchronized Collection<ServerSettingsRegistration> getRegistrations() {
        return List.copyOf(this.registrationsByFormId.values());
    }

    /**
     * Returns a live statistics snapshot for observability and diagnostics.
     *
     * @return immutable metrics snapshot
     */
    @Override
    public ServerSettingsApiStats getStats() {
        int registrations;
        synchronized (this) {
            registrations = this.registrationsByFormId.size();
        }

        return new ServerSettingsApiStats(
                registrations,
                this.statRequestEvents.sum(),
                this.statPacketsSent.sum(),
                this.statResponsePackets.sum(),
                this.statResponsesProcessed.sum(),
                this.statResponsesFailed.sum(),
                this.statResponsesIgnored.sum()
        );
    }

    /**
     * Sends all configured settings payloads when the Bedrock client requests server settings.
     *
     * @param event request event originating from packet handling
     */
    @EventHandler(ignoreCancelled = true)
    public void onServerSettingsRequest(PlayerServerSettingsRequestEvent event) {
        this.statRequestEvents.increment();

        List<RegisteredEntry> entries = snapshotEntries();
        if (entries.isEmpty()) {
            return;
        }

        Player player = event.getPlayer();
        Set<Integer> sentFormIds = new HashSet<>();

        for (RegisteredEntry entry : entries) {
            if (!entry.isRegistered()) {
                continue;
            }
            if (!entry.getOwner().isEnabled()) {
                unregister(entry.getFormId());
                continue;
            }

            String payload;
            try {
                payload = entry.getDefinition().getPayloadProvider().createPayload(
                        new ServerSettingsRequestContext(player, entry.getFormId(), entry.getKey(), entry.getOwner())
                );
            } catch (Exception exception) {
                this.plugin.getLogger().warning(
                        "Payload provider failed for " + entry.getOwner().getName() + "/" + entry.getKey(),
                        exception
                );
                continue;
            }

            if (payload == null || payload.isBlank()) {
                continue;
            }

            ServerSettingsResponsePacket packet = new ServerSettingsResponsePacket();
            packet.formId = entry.getFormId();
            packet.data = payload;

            // This packet can only send JSON payload + formId. It cannot execute server logic by itself.
            if (player.dataPacket(packet)) {
                sentFormIds.add(entry.getFormId());
                this.statPacketsSent.increment();
            }
        }

        synchronized (this) {
            if (sentFormIds.isEmpty()) {
                this.pendingByPlayerId.remove(player.getId());
            } else {
                this.pendingByPlayerId.put(player.getId(), sentFormIds);
            }
        }
    }

    /**
     * Processes client response packets and dispatches them to matching definitions.
     *
     * @param event inbound packet event
     */
    @EventHandler(ignoreCancelled = true)
    public void onDataPacketReceive(DataPacketReceiveEvent event) {
        DataPacket incoming = event.getPacket();
        if (!(incoming instanceof ModalFormResponsePacket response)) {
            return;
        }

        RegisteredEntry entry;
        long playerId = event.getPlayer().getId();

        synchronized (this) {
            entry = this.registrationsByFormId.get(response.formId);
            if (entry == null) {
                this.statResponsesIgnored.increment();
                return;
            }

            this.statResponsePackets.increment();

            Set<Integer> pending = this.pendingByPlayerId.get(playerId);
            if (pending == null || !pending.remove(response.formId)) {
                this.statResponsesIgnored.increment();
                return;
            }
            if (pending.isEmpty()) {
                this.pendingByPlayerId.remove(playerId);
            }
        }

        if (!entry.getOwner().isEnabled()) {
            unregister(entry.getFormId());
            this.statResponsesIgnored.increment();
            return;
        }

        boolean closed = response.cancelReason != 0 || isClosedByData(response.data);
        if (closed && !entry.getDefinition().isAcceptClosedResponses()) {
            this.statResponsesIgnored.increment();
            return;
        }

        try {
            entry.getDefinition().getResponseHandler().handle(
                    new ServerSettingsResponseContext(
                            event.getPlayer(),
                            entry.getFormId(),
                            entry.getKey(),
                            entry.getOwner(),
                            response.data,
                            response.cancelReason,
                            closed
                    )
            );
            this.statResponsesProcessed.increment();
        } catch (Exception exception) {
            this.statResponsesFailed.increment();
            this.plugin.getLogger().warning(
                    "Response handler failed for " + entry.getOwner().getName() + "/" + entry.getKey(),
                    exception
            );
        }
    }

    /**
     * Removes all registrations owned by a plugin that is being disabled.
     *
     * @param event plugin disable event
     */
    @EventHandler
    public void onPluginDisable(PluginDisableEvent event) {
        unregisterOwnedBy(event.getPlugin());
    }

    /**
     * Clears pending response tracking for disconnected players.
     *
     * @param event player quit event
     */
    @EventHandler
    public synchronized void onPlayerQuit(PlayerQuitEvent event) {
        this.pendingByPlayerId.remove(event.getPlayer().getId());
    }

    /**
     * Stops the engine and clears all internal state.
     */
    synchronized void shutdown() {
        this.stopped = true;
        for (RegisteredEntry entry : this.registrationsByFormId.values()) {
            entry.markUnregistered();
        }
        this.registrationsByFormId.clear();
        this.formIdByOwnerAndKey.clear();
        this.formIdsByOwner.clear();
        this.pendingByPlayerId.clear();
    }

    /**
     * Creates a snapshot list of active entries for iteration without holding lock during handler execution.
     *
     * @return registration entry snapshot
     */
    private synchronized List<RegisteredEntry> snapshotEntries() {
        return new ArrayList<>(this.registrationsByFormId.values());
    }

    /**
     * Checks whether an entry is still active in the registry.
     *
     * @param entry registration entry
     * @return {@code true} if the same entry instance is still registered
     */
    private synchronized boolean isStillRegistered(RegisteredEntry entry) {
        return this.registrationsByFormId.get(entry.getFormId()) == entry;
    }

    /**
     * Allocates a free positive form id from the API namespace.
     *
     * @return unique form id
     */
    private int allocateFormIdLocked() {
        for (int i = 0; i < Integer.MAX_VALUE; i++) {
            int candidate = this.formCounter.getAndIncrement();
            if (candidate <= 0) {
                this.formCounter.set(START_FORM_ID);
                candidate = this.formCounter.getAndIncrement();
            }
            if (!this.registrationsByFormId.containsKey(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("No free ServerSettings formId available");
    }

    /**
     * Ensures this engine is still active.
     */
    private void ensureRunning() {
        if (this.stopped) {
            throw new IllegalStateException("API is already stopped");
        }
    }

    /**
     * Builds a unique owner+key index identifier.
     *
     * @param owner owning plugin
     * @param key registration key
     * @return normalized owner key
     */
    private static String ownerKey(Plugin owner, String key) {
        return owner.getName().toLowerCase(Locale.ROOT) + ":" + key;
    }

    /**
     * Checks if response payload represents a closed form with no submitted values.
     *
     * @param data raw packet payload
     * @return {@code true} if payload is null/empty/"null"
     */
    private static boolean isClosedByData(String data) {
        if (data == null) {
            return true;
        }
        String trimmed = data.trim();
        return trimmed.isEmpty() || "null".equalsIgnoreCase(trimmed);
    }

    /**
     * Concrete registration handle implementation.
     */
    private static final class RegisteredEntry implements ServerSettingsRegistration {

        private final PacketServerSettingsApi api;
        private final int formId;
        private final ServerSettingsDefinition definition;
        private volatile boolean registered = true;

        /**
         * Creates a registration handle.
         *
         * @param api owning API
         * @param formId assigned form id
         * @param definition immutable definition
         */
        private RegisteredEntry(PacketServerSettingsApi api, int formId, ServerSettingsDefinition definition) {
            this.api = api;
            this.formId = formId;
            this.definition = definition;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getFormId() {
            return this.formId;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getKey() {
            return this.definition.getKey();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Plugin getOwner() {
            return this.definition.getOwner();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isRegistered() {
            return this.registered && this.api.isStillRegistered(this);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean unregister() {
            return this.api.unregister(this.formId);
        }

        /**
         * Marks this handle as inactive.
         */
        private void markUnregistered() {
            this.registered = false;
        }

        /**
         * Returns the immutable definition behind this handle.
         *
         * @return definition instance
         */
        private ServerSettingsDefinition getDefinition() {
            return this.definition;
        }
    }
}
