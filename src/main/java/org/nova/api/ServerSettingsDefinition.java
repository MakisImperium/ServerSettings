package org.nova.api;

import cn.nukkit.plugin.Plugin;

import java.util.Objects;

/**
 * Immutable registration definition for one server-settings payload/response pair.
 */
public final class ServerSettingsDefinition {

    private final Plugin owner;
    private final String key;
    private final ServerSettingsPayloadProvider payloadProvider;
    private final ServerSettingsResponseHandler responseHandler;
    private final boolean acceptClosedResponses;

    /**
     * Creates a definition from builder state.
     *
     * @param builder prepared builder
     */
    private ServerSettingsDefinition(Builder builder) {
        this.owner = builder.owner;
        this.key = builder.key;
        this.payloadProvider = builder.payloadProvider;
        this.responseHandler = builder.responseHandler;
        this.acceptClosedResponses = builder.acceptClosedResponses;
    }

    /**
     * Starts a new definition builder.
     *
     * @param owner owner plugin
     * @param key plugin-local key
     * @return builder
     */
    public static Builder builder(Plugin owner, String key) {
        return new Builder(owner, key);
    }

    /**
     * @return plugin that owns this definition
     */
    public Plugin getOwner() {
        return this.owner;
    }

    /**
     * @return stable plugin-local definition key
     */
    public String getKey() {
        return this.key;
    }

    /**
     * @return payload producer used to build packet JSON
     */
    public ServerSettingsPayloadProvider getPayloadProvider() {
        return this.payloadProvider;
    }

    /**
     * @return response handler used after client submits values
     */
    public ServerSettingsResponseHandler getResponseHandler() {
        return this.responseHandler;
    }

    /**
     * @return {@code true} if closed forms should still be forwarded to the handler
     */
    public boolean isAcceptClosedResponses() {
        return this.acceptClosedResponses;
    }

    /**
     * Builder for {@link ServerSettingsDefinition}.
     */
    public static final class Builder {

        private final Plugin owner;
        private final String key;
        private ServerSettingsPayloadProvider payloadProvider;
        private ServerSettingsResponseHandler responseHandler = ignored -> {
        };
        private boolean acceptClosedResponses;

        /**
         * Creates a new builder.
         *
         * @param owner owner plugin
         * @param key plugin-local key
         */
        private Builder(Plugin owner, String key) {
            this.owner = Objects.requireNonNull(owner, "owner");
            String normalizedKey = Objects.requireNonNull(key, "key").trim();
            if (normalizedKey.isEmpty()) {
                throw new IllegalArgumentException("key must not be empty");
            }
            this.key = normalizedKey;
        }

        /**
         * Sets payload producer.
         *
         * @param payloadProvider payload provider callback
         * @return this builder
         */
        public Builder payloadProvider(ServerSettingsPayloadProvider payloadProvider) {
            this.payloadProvider = Objects.requireNonNull(payloadProvider, "payloadProvider");
            return this;
        }

        /**
         * Sets raw response handler.
         *
         * @param responseHandler response handler callback
         * @return this builder
         */
        public Builder responseHandler(ServerSettingsResponseHandler responseHandler) {
            this.responseHandler = Objects.requireNonNull(responseHandler, "responseHandler");
            return this;
        }

        /**
         * Sets response processor class/function.
         *
         * @param responseProcessor response processor
         * @return this builder
         */
        public Builder responseProcessor(ServerSettingsResponseProcessor responseProcessor) {
            Objects.requireNonNull(responseProcessor, "responseProcessor");
            this.responseHandler = responseProcessor::process;
            return this;
        }

        /**
         * Sets parsed response handler.
         *
         * @param responseHandler parsed response handler
         * @return this builder
         */
        public Builder parsedResponseHandler(ServerSettingsParsedResponseHandler responseHandler) {
            Objects.requireNonNull(responseHandler, "responseHandler");
            this.responseHandler = context -> {
                ServerSettingsResponseValues values = context.requireValues();
                responseHandler.handle(context, values);
            };
            return this;
        }

        /**
         * Sets parsed response processor class/function.
         *
         * @param responseProcessor parsed response processor
         * @return this builder
         */
        public Builder parsedResponseProcessor(ServerSettingsParsedResponseProcessor responseProcessor) {
            Objects.requireNonNull(responseProcessor, "responseProcessor");
            this.responseHandler = context -> responseProcessor.process(context, context.requireValues());
            return this;
        }

        /**
         * Sets a composed pipeline for enterprise processing chains.
         *
         * @param pipeline response pipeline
         * @return this builder
         */
        public Builder responsePipeline(ServerSettingsResponsePipeline pipeline) {
            Objects.requireNonNull(pipeline, "pipeline");
            this.responseHandler = pipeline::process;
            return this;
        }

        /**
         * Controls whether closed forms should trigger response callbacks.
         *
         * @param acceptClosedResponses {@code true} to forward closed responses
         * @return this builder
         */
        public Builder acceptClosedResponses(boolean acceptClosedResponses) {
            this.acceptClosedResponses = acceptClosedResponses;
            return this;
        }

        /**
         * Creates the immutable definition.
         *
         * @return definition
         */
        public ServerSettingsDefinition build() {
            if (this.payloadProvider == null) {
                throw new IllegalStateException("payloadProvider is required");
            }
            return new ServerSettingsDefinition(this);
        }
    }
}
