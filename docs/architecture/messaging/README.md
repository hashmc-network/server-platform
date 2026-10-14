**English** | [Português](README.pt-BR.md)

# Messaging

How servers talk to each other. Every message is a **packet**: a small object with a fixed id, serialized to binary and sent over Redis.

## Transport: Redis Pub/Sub

Packets between servers travel over Redis Pub/Sub. Plugin Messaging was considered and is not used for this.

| | Redis Pub/Sub | Plugin Messaging |
|---|---|---|
| Needs a player online | No | Yes. Messages travel through a player's connection, so a server with no players can neither send nor receive. |
| Server to server | Direct | Only through the proxy, which has to relay each message. |
| Broadcast to many servers | One publish reaches every subscriber | One message per server, each one needing a player there. |
| Payload size | No practical limit for packets | About 32 KB per message sent to the proxy. |
| Latency | Below a millisecond on the same network | Also low, but tied to the player's connection. |
| Extra infrastructure | Redis, which the network already uses for cache | None. |

Both are fast enough that speed does not decide between them. Reliability does: Plugin Messaging stops working exactly when a server is empty, which is common for game servers waiting for players and for servers that just started.

Plugin Messaging stays reserved for the cases that really need the player's connection, such as talking to client-side mods.

## Packets

- Each packet type has a fixed, unique id and its own binary serializer. Binary keeps messages small and cheap to read.
- Every packet travels inside an envelope with the packet id, the server that sent it and, for requests, a correlation id.
- A server that receives a packet id it does not know ignores it and logs it. This allows servers to be updated one at a time without breaking each other.
- Packets carry ids and small values, not whole objects. When the receiver needs more, it reads from the cache or the database.

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
- The sender waits asynchronously and gives up after a timeout, so a server that never answers does not hold anything up.

## Delivery

Redis Pub/Sub delivers each packet **at most once**. A server that is offline or reconnecting when a packet is published never receives it.

- Packets that can be lost without harm are sent directly, such as server status updates and chat between servers.
- Packets that must not be lost are backed by PostgreSQL. The data is written to the database first and the packet only notifies the servers. A server that missed the notification finds the pending data on its own, for example when the player joins.

Example: a store purchase is saved in PostgreSQL as a pending delivery, and a packet tells the player's current server to deliver it. If the packet is lost, the delivery happens the next time the player joins any server.

## Rules

- **Packet handlers never run on the main thread.** Anything that touches the game is scheduled back to the main thread.
- **Code depends on the packet API, never on Redis directly.** The transport stays in one place and can change without touching features.
- **A new packet type is documented in the feature that uses it.**
