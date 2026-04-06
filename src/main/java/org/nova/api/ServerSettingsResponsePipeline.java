package org.nova.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Composable enterprise pipeline for multi-step response processing.
 */
public final class ServerSettingsResponsePipeline implements ServerSettingsResponseProcessor {

    private final List<ServerSettingsResponseProcessor> steps;
    private final ServerSettingsResponseErrorHandler errorHandler;
    private final boolean continueOnError;

    /**
     * Creates an immutable pipeline from builder state.
     *
     * @param builder configured builder
     */
    private ServerSettingsResponsePipeline(Builder builder) {
        this.steps = List.copyOf(builder.steps);
        this.errorHandler = builder.errorHandler;
        this.continueOnError = builder.continueOnError;
    }

    /**
     * Starts a pipeline builder.
     *
     * @return builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Executes all configured steps in order.
     *
     * @param context response context
     * @throws Exception rethrown when a step fails and continue-on-error is disabled
     */
    @Override
    public void process(ServerSettingsResponseContext context) throws Exception {
        for (ServerSettingsResponseProcessor step : this.steps) {
            try {
                step.process(context);
            } catch (Exception exception) {
                if (this.errorHandler != null) {
                    this.errorHandler.onError(context, exception);
                }
                if (!this.continueOnError) {
                    throw exception;
                }
            }
        }
    }

    /**
     * Pipeline builder.
     */
    public static final class Builder {

        private final List<ServerSettingsResponseProcessor> steps = new ArrayList<>();
        private ServerSettingsResponseErrorHandler errorHandler;
        private boolean continueOnError;

        /**
         * Creates an empty builder.
         */
        private Builder() {
        }

        /**
         * Adds a processor step.
         *
         * @param step processor
         * @return this builder
         */
        public Builder then(ServerSettingsResponseProcessor step) {
            this.steps.add(Objects.requireNonNull(step, "step"));
            return this;
        }

        /**
         * Adds a raw handler step.
         *
         * @param handler raw handler
         * @return this builder
         */
        public Builder then(ServerSettingsResponseHandler handler) {
            Objects.requireNonNull(handler, "handler");
            this.steps.add(handler::handle);
            return this;
        }

        /**
         * Adds a parsed processor step.
         *
         * @param processor parsed processor
         * @return this builder
         */
        public Builder thenParsed(ServerSettingsParsedResponseProcessor processor) {
            Objects.requireNonNull(processor, "processor");
            this.steps.add(context -> processor.process(context, context.requireValues()));
            return this;
        }

        /**
         * Adds a parsed handler step.
         *
         * @param handler parsed handler
         * @return this builder
         */
        public Builder thenParsed(ServerSettingsParsedResponseHandler handler) {
            Objects.requireNonNull(handler, "handler");
            this.steps.add(context -> handler.handle(context, context.requireValues()));
            return this;
        }

        /**
         * Sets error callback used when a step throws.
         *
         * @param errorHandler error callback
         * @return this builder
         */
        public Builder onError(ServerSettingsResponseErrorHandler errorHandler) {
            this.errorHandler = Objects.requireNonNull(errorHandler, "errorHandler");
            return this;
        }

        /**
         * Sets failure behavior.
         *
         * @param continueOnError true to keep executing remaining steps after error
         * @return this builder
         */
        public Builder continueOnError(boolean continueOnError) {
            this.continueOnError = continueOnError;
            return this;
        }

        /**
         * Creates immutable pipeline instance.
         *
         * @return pipeline
         */
        public ServerSettingsResponsePipeline build() {
            if (this.steps.isEmpty()) {
                throw new IllegalStateException("At least one response step is required");
            }
            return new ServerSettingsResponsePipeline(this);
        }
    }
}
