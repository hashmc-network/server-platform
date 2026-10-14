**English** | [Português](README.pt-BR.md)

# Messaging

How servers talk to each other. Every message is a **packet**: a small object with a type, sent over Redis inside a standard envelope.

## Transport: Redis Pub/Sub

Packets between servers travel over Redis Pub/Sub. Plugin Messaging was considered and is not used for this.

| | Redis Pub/Sub | Plugin Messaging |
|---|---|---|
| Needs a player online | No | Yes. Messages travel through a player's connection, so a server with no players can neither send nor receive. |
| Server to server | Direct | Only through the proxy, which has to relay each message. |
| Broadcast to many servers | One publish reaches every subscriber | One message per server, each one needing a player there. |
| Payload size | Limited by the platform's own maximum packet size | About 32 KB per message sent to the proxy. |
| Latency | Usually around a millisecond or less on the same network. To be measured on the real infrastructure. | Also low, but tied to the player's connection. |
| Extra infrastructure | Redis, which the network already uses for cache | None. |

Both are fast enough that speed does not decide between them. Reliability does: Plugin Messaging stops working exactly when a server is empty, which is common for game servers waiting for players and for servers that just started.

Plugin Messaging stays reserved for the cases that really need the player's connection, such as talking to client-side mods.

## Envelope

Every packet travels inside an envelope with:

| Field | Purpose |
|---|---|
| Protocol version | Lets a server detect packets from a newer or older platform version. |
| Packet type | Tells the receiver how to read the payload. |
| Operation id | Unique per operation. A receiver that sees the same operation id twice applies it only once. |
| Origin | The server that sent the packet. |
| Correlation id | Present in requests and their responses, to match one to the other. |
| Payload | The packet's own data. |

- A packet larger than the platform's maximum size is rejected when it is sent, never published.
- A server that receives a packet type or protocol version it does not understand ignores it and logs it. This allows servers to be updated one at a time without breaking each other.
- Packets carry ids and small values, not whole objects. When the receiver needs more, it reads from the cache or the database.

### Serialization format

The contract above comes first; the format is chosen when the first packets are implemented. It starts with an existing format, such as JSON or MessagePack. A custom binary format is only adopted if a measurement shows the existing format is a real bottleneck.

## Channels

Each server only subscribes to the channels that concern it, so no server spends time reading packets meant for others.

| Channel | Who receives |
|---|---|
| `hashmc:packets:broadcast` | Every server, including the proxy. |
| `hashmc:packets:proxy` | The proxy. |
| `hashmc:packets:group:<group>` | Every server in a group, such as `bedwars` or `duels`. |
| `hashmc:packets:server:<server>` | A single server, such as `bedwars-01`. |

## Requests and responses

Some packets expect an answer, such as "is this player online on your server?".

- The request carries a correlation id. The response carries the same id back.
- The sender waits asynchronously and gives up after a timeout. Every request defines what happens on timeout: retry, fall back to another source, or fail the operation and tell the player.

## Delivery

Redis Pub/Sub delivers each packet **at most once**. A server that is offline or reconnecting when a packet is published never receives it.

- Packets that can be lost without harm are sent directly, such as server status updates and chat between servers.
- Packets that must not be lost are backed by PostgreSQL. The data is written to the database first, and the packet only notifies the servers so they act right away.
- A lost notification can delay an action, never lose it. Servers reconcile on their own:
  - when a player joins, the server checks the player's pending items;
  - while the network runs, each server periodically checks the pending items of its online players.

Example: a store purchase is saved in PostgreSQL as a pending delivery, and a packet tells the player's current server to deliver it. If the packet is lost, the periodic check finds the delivery a few seconds later, even if the player never leaves the server. The delivery is marked as done in the same transaction that grants it, so it is never delivered twice.

## Reconnection

When a server loses its connection to Redis:

1. It keeps running the game. Anything that needs Redis waits or fails with a clear message.
2. It reconnects and subscribes to its channels again.
3. It reconciles what it may have missed: it republishes its own status and checks pending items for its online players.

## Rules

- **Packet handlers never run on the main thread.** Anything that touches the game is scheduled back to the main thread.
- **Handlers with effects are idempotent.** Receiving the same operation id twice changes nothing the second time.
- **Code depends on the packet API, never on Redis directly.** The transport stays in one place and can change without touching features.
- **A new packet type is documented in the feature that uses it.**
