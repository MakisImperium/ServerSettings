# ServerSettingsAPI

Production-grade packet-level API for Bedrock Server Settings on Nukkit.

`ServerSettingsApi` provides a clean service layer on top of the native packet flow:

`PlayerServerSettingsRequestEvent -> ServerSettingsResponsePacket -> ModalFormResponsePacket -> response routing`

The result is a stable integration contract for plugin teams that need predictable behavior, typed parsing, and clear runtime observability.

## Value Proposition

- Packet-first architecture with no abstraction leaks into non-packet form systems.
- Strong ownership model (`owner + key`) for multi-plugin server environments.
- Multiple response styles: handler, processor, parsed processor, or pipeline.
- Built-in runtime metrics for operations, incident analysis, and capacity tracking.
- Automatic lifecycle cleanup on player quit and plugin disable.

## Architecture

```text
Consumer Plugin(s)
  -> ServerSettingsApi (service contract)
     -> PacketServerSettingsApi (engine)
        -> listens: PlayerServerSettingsRequestEvent
        -> sends:   ServerSettingsResponsePacket
        -> listens: DataPacketReceiveEvent (ModalFormResponsePacket)
        -> routes response to registered definition
        -> exports metrics via ServerSettingsApiStats
```

## Compatibility

| Component | Version |
|---|---|
| Java | 17 |
| Nukkit API | 1.0.0 |
| Plugin Name | `NovaServerSettingsApi` |
| Maven Compiler | `--release 17` |

## Installation

### 1. Deploy provider plugin

Build this project and place the JAR into your server `plugins/` directory.

### 2. Add dependency in consumer plugin

```yaml
depend: [NovaServerSettingsApi]
```

### 3. Resolve service

```java
import org.nova.api.ServerSettingsApi;
import org.nova.api.ServerSettingsApiLocator;

ServerSettingsApi api = ServerSettingsApiLocator.require(getServer());
```

## Quick Start

The following example registers one form definition and handles typed values with fallbacks.

```java
import cn.nukkit.plugin.PluginBase;
import org.nova.api.ServerSettingsApi;
import org.nova.api.ServerSettingsApiLocator;
import org.nova.api.ServerSettingsDefinition;
import org.nova.api.ServerSettingsJson;
import org.nova.api.ServerSettingsRegistration;

public final class ExamplePlugin extends PluginBase {

    private ServerSettingsRegistration registration;

    @Override
    public void onEnable() {
        ServerSettingsApi api = ServerSettingsApiLocator.require(getServer());

        this.registration = api.register(
                ServerSettingsDefinition.builder(this, "global-settings")
                        .payloadProvider(ctx -> ServerSettingsJson.customForm("Global Settings")
                                .label("Configure runtime behavior")
                                .toggle("Feature enabled", true)
                                .slider("Rate", 0, 100, 1, 25)
                                .input("Tag", "e.g. PROD", "")
                                .build())
                        .parsedResponseHandler((ctx, values) -> {
                            boolean featureEnabled = values.getBoolean(1, false);
                            int rate = values.getInt(2, 0);
                            String tag = values.getString(3, "");

                            getLogger().info(
                                    "Settings updated: player=" + ctx.getPlayer().getName()
                                            + ", featureEnabled=" + featureEnabled
                                            + ", rate=" + rate
                                            + ", tag=" + tag
                            );
                        })
                        .acceptClosedResponses(false)
                        .build()
        );
    }

    @Override
    public void onDisable() {
        if (this.registration != null) {
            this.registration.unregister();
            this.registration = null;
        }
    }
}
```

## Advanced Response Orchestration

`ServerSettingsResponsePipeline` is designed for enterprise-style multi-step handling with explicit failure policy.

```java
import org.nova.api.ServerSettingsResponsePipeline;
import org.nova.api.ServerSettingsResponses;

ServerSettingsResponsePipeline pipeline = ServerSettingsResponsePipeline.builder()
        .thenParsed((ctx, values) -> {
            boolean enabled = values.getBoolean(0, false);
            int threshold = values.getInt(1, 50);
            // 1) validate and map
        })
        .then(ctx -> {
            // 2) persist, publish domain events, audit
        })
        .onError(ServerSettingsResponses.loggingErrors(this, "[ServerSettings] "))
        .continueOnError(false)
        .build();

api.register(
        ServerSettingsDefinition.builder(this, "policy-settings")
                .payloadProvider(ctx -> ServerSettingsJson.customForm("Policy")
                        .toggle("Enabled", true)
                        .slider("Threshold", 0, 100, 1, 50)
                        .build())
                .responsePipeline(pipeline)
                .build()
);
```

## Dynamic Payload by Player Context

Payload providers receive a `ServerSettingsRequestContext`, allowing targeted forms per player or per runtime state.

```java
api.register(
        ServerSettingsDefinition.builder(this, "personalized-settings")
                .payloadProvider(ctx -> ServerSettingsJson.customForm("Hello " + ctx.getPlayer().getName())
                        .label("FormId: " + ctx.getFormId())
                        .toggle("Receive notifications", true)
                        .build())
                .responseHandler(ctx -> {
                    boolean enabled = ctx.booleanAt(1, true);
                    // apply player-scoped configuration
                })
                .build()
);
```

## Observability

Use `ServerSettingsApiStats` to expose API health and throughput in your own monitoring logs.

```java
var stats = api.getStats();
getLogger().info(
        "serverSettingsApi"
                + " registrations=" + stats.getRegistrations()
                + " requestEvents=" + stats.getRequestEvents()
                + " packetsSent=" + stats.getPacketsSent()
                + " responsePackets=" + stats.getResponsePackets()
                + " processed=" + stats.getResponsesProcessed()
                + " failed=" + stats.getResponsesFailed()
                + " ignored=" + stats.getResponsesIgnored()
);
```

## Production Guidelines

- Keep handlers deterministic and fast inside the event context.
- Offload heavy CPU or I/O to your own async scheduler.
- Return to main thread only for server-critical operations.
- Use stable keys (`owner + key`) as long-term integration identifiers.
- Prefer parsed handlers for strict type handling and explicit defaults.
- Track `responsesFailed` and `responsesIgnored` metrics for early anomaly detection.

## Public API Surface

- `ServerSettingsApi`: service contract used by consumer plugins.
- `ServerSettingsApiLocator`: service resolution helper.
- `ServerSettingsDefinition`: immutable registration definition + builder.
- `ServerSettingsRegistration`: registration handle with explicit unregister.
- `ServerSettingsRequestContext`: payload creation context.
- `ServerSettingsResponseContext`: response metadata + lazy parsed access.
- `ServerSettingsResponseValues`: typed value accessor (`boolean/int/double/string`).
- `ServerSettingsResponsePipeline`: composable multi-step response processing.
- `ServerSettingsResponses`: adapters and logging error handler factory.
- `ServerSettingsApiStats`: runtime metrics snapshot.

## Build

```bash
mvn clean package
```
