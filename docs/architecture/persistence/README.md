**English** | [Português](README.pt-BR.md)

# Persistence

How the network stores and caches data. The goal is to handle heavy load, today and as the network grows, without making each server heavier than it needs to be.

## Roles

| Layer | Technology | Role |
|---|---|---|
| Memory | Each server's own memory | Data of the players online on that server. The fastest read, used for almost everything during gameplay. |
| Shared cache | Redis | Hot data shared between servers, such as profiles of online players, rankings and server status. |
| Source of truth | PostgreSQL | Every piece of data that must survive a restart. If Redis and PostgreSQL disagree, PostgreSQL is right. |

Redis is also the transport for messages between servers, described in [Messaging](../messaging/README.md).

## Reading data

Reads go from the fastest layer to the slowest, and fill the faster layers on the way back:

```text
memory → Redis → PostgreSQL
```

- A player's data is loaded once, when the player connects, and stays in memory while the player is on that server.
- During gameplay, code reads from memory. It never queries the database to answer something the server already knows.

## Writing data

- Changes are applied to memory first, so gameplay never waits for the database.
- Each object tracks what changed, and only the changed data is written.
- Changes are saved to PostgreSQL in the background, on a fixed interval and right away when the player leaves the server.
- Append-only data, such as match history and statistics, is written in batches instead of one row at a time.

## Moving between servers

When a player goes from `bedwars` to `lobby`, the new server must not load data that the old one has not saved yet.

1. The server the player is leaving saves the player's data to Redis and then to PostgreSQL.
2. The server the player is joining reads from Redis first, which already has the latest data, and falls back to PostgreSQL.
3. Every saved record carries a version number. A write is only applied if its version is newer than the stored one, so a late save can never overwrite newer data.

## Where each kind of data lives

| Data | Memory | Redis | PostgreSQL |
|---|---|---|---|
| Profile of an online player | Yes | Yes, while online | Yes |
| Profile of an offline player | No | No | Yes |
| Rankings | No | Yes, rebuilt from PostgreSQL | Yes |
| Match history and statistics | No | No | Yes, written in batches |
| Server status and player counts | No | Yes, with expiration | No |

## Rules

- **Never block the main thread.** Database and Redis calls always run outside the server's main thread. Results that touch the game are scheduled back to the main thread.
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
