**English** | [Português](README.pt-BR.md)

# Persistence

How the network stores and caches data. The goal is to handle heavy load, today and as the network grows, without making each server heavier than it needs to be, and without ever losing data the player was told is saved.

## Roles

| Layer | Technology | Role |
|---|---|---|
| Memory | Each server's own memory | Data of the players online on that server. The fastest read, used for almost everything during gameplay. |
| Shared cache | Redis | Hot data shared between servers, such as profiles of online players, rankings and server status. |
| Source of truth | PostgreSQL | Every piece of data that must survive a restart. If Redis and PostgreSQL disagree, PostgreSQL is right. |

Redis is also the transport for messages between servers, described in [Messaging](../messaging/README.md).

## Kinds of data

Not all data deserves the same guarantee. Each kind has its own write strategy:

| Kind | Examples | Strategy |
|---|---|---|
| Critical | Coins, purchases, rewards, MMR | Written to PostgreSQL in a transaction **before** the operation is confirmed to the player. |
| Non-critical | Cosmetic preferences, settings, toggles | Applied in memory and written in the background. A crash may lose the last few seconds of changes, and that is accepted. |
| Match state | Teams, kills, beds, timers of a running match | Memory only, on the server running the match. |
| Match end | Final result, critical rewards | Saved in a single transaction that also marks the match as finished. See [Game engine](../game-engine/README.md#results). |
| History | Detailed statistics | Written in batches, with a unique id per record, so a retry never creates a duplicate. |

## Reading data

Reads go from the fastest layer to the slowest, and fill the faster layers on the way back:

```text
memory → Redis → PostgreSQL
```

- A player's data is loaded once, when the player connects, and stays in memory while the player is on that server.
- During gameplay, code reads from memory. It never queries the database to answer something the server already knows.

## Writing data

### Critical data

- The operation runs as a PostgreSQL transaction outside the main thread. The player sees the result only after the transaction commits.
- The game does not freeze while it waits: the main thread keeps running, and the result is applied when the transaction returns.
- Balances change through atomic operations in the database, never by overwriting a value read earlier. A purchase is `subtract 2,000 if the balance is at least 2,000`, not `set the balance to 3,000`.
- If the transaction fails, the operation fails and the player is told so. Nothing is shown as done before it is saved.

### Non-critical data

- Changes are applied to memory first, so gameplay never waits for the database.
- Each object tracks what changed, and only the changed data is written.
- Changes are saved to PostgreSQL in the background, on a fixed interval and when the player leaves the server.

### History

- Append-only data, such as detailed statistics, is written in batches instead of one row at a time.
- The end of a match is not batched: its result and critical rewards are saved together in one transaction.
- Each record has a unique id generated when the event happens. Writing the same record twice has no effect.

## Session ownership

At any moment, each online player's data has exactly **one owner**: the server the player is on.

- Ownership is recorded in PostgreSQL, together with a **generation** number that only increases.
- A server takes ownership with a single atomic update that sets itself as the owner and increments the generation. The update only succeeds if the session is free or its current owner is no longer alive.
- Every write to that player's data carries the generation the server received. PostgreSQL rejects any write whose generation is not the current one.
- A server whose write is rejected has lost ownership: it stops changing that player's data and disconnects the player from itself.
- Each server publishes a heartbeat in Redis with a short expiration. A server is alive while its heartbeat exists.
- If the owner crashes, its heartbeat expires and the next server takes ownership with a new generation, loading the player from PostgreSQL. Critical data is already there; only recent non-critical changes may be lost.

This keeps a single writer even when a server freezes for a long time, such as during a long garbage collection pause. When it resumes, its generation is outdated and every write it tries is refused.

### When Redis is unavailable

- Sessions that already have an owner keep working, because ownership and the generation check live in PostgreSQL.
- Planned transfers keep working, because the origin releases the session explicitly.
- Taking over a session from a server that did not release it waits until Redis is back, since there is no way to tell whether the old owner is still alive. The player is told to try again shortly.

## Moving between servers

When a player goes from `bedwars` to `lobby`, the destination must load exactly what the origin had.

1. The origin stops accepting changes to that player's data.
2. The origin writes every pending change to PostgreSQL, with its generation.
3. The origin updates the player's data in Redis and releases the session in PostgreSQL.
4. The transfer is confirmed, and the destination takes ownership with a new generation and loads the data, from Redis first and PostgreSQL as a fallback.

If step 2 fails or takes longer than the timeout, the transfer is cancelled: the player stays on the origin, ownership does not change, and the player is told the transfer failed. A transfer never happens with unsaved data.

## Where each kind of data lives

| Data | Memory | Redis | PostgreSQL |
|---|---|---|---|
| Profile of an online player | Yes | Yes, while online | Yes |
| Profile of an offline player | No | No | Yes |
| Coins and other balances | Yes, as a read copy | Yes, as a read copy | Yes, every change in a transaction |
| Rankings | No | Yes, rebuilt from PostgreSQL | Yes |
| State of a running match | Yes | No | No |
| Match result and critical rewards | No | No | Yes, in one transaction |
| Detailed statistics | No | No | Yes, written in batches |
| Session ownership and generation | No | No | Yes |
| Server heartbeat | No | Yes, with expiration | No |
| Server status and player counts | No | Yes, with expiration | No |

## Rules

- **Never block the main thread.** Database and Redis calls always run outside the server's main thread. Results that touch the game are scheduled back to the main thread.
- **Critical data is confirmed only after it is saved.** Never show the player a purchase, reward or balance change that is not committed in PostgreSQL.
- **Every write of player data carries the owner's generation.** A write without it, or with an old one, is a bug.
- **Durable data never lives only in Redis.** Redis is a cache and can be emptied at any time without losing anything.
- **Ephemeral data in Redis always expires.** Every key that is not a cache of PostgreSQL data has an expiration time.
- **Redis keys share a prefix:** `hashmc:<area>:<id>`, such as `hashmc:profile:<uuid>`.
- **The schema only changes through versioned migrations.** No server creates or alters tables on its own at startup.
- **No queries inside loops.** Load what is needed in one query.
- **Every frequent query is backed by an index.**
- **Connection pools stay small.** Each PostgreSQL connection is a process on the database server, so a few connections per server are enough when no query blocks for long.

## Planned libraries

| Library | Use | Why |
|---|---|---|
| HikariCP | PostgreSQL connection pool | Fast, light and the standard pool on the JVM. |
| Flyway | Database migrations | Versioned and repeatable schema changes. |
| Lettuce | Redis client | Asynchronous, and a single connection can be shared by every thread. |
| Caffeine | Memory cache | Fast in-memory cache with expiration and size limits. |

No heavy ORM is used. Queries are written in SQL, so it is always clear what each one costs.

## When the network grows

These points are not needed in phase 1 and will be revisited in phase 2 of the [network architecture](../network/README.md):

- A connection pooler such as PgBouncer in front of PostgreSQL, once many game servers each keep their own connections.
- Partitioning large append-only tables, such as match history, by date.
- Read replicas for heavy read-only queries, such as rankings and statistics pages.
