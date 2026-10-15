**English** | [Português](README.pt-BR.md)

# Observability

What the network measures, so performance problems are seen with numbers instead of guesses. This document defines **what** is measured. The tools that collect and display the metrics will be chosen later.

## Servers

| Metric | What it shows |
|---|---|
| MSPT (milliseconds per tick) | How long each tick takes on each Paper server. Above 50 ms the server can no longer keep 20 TPS. |
| TPS | Ticks per second actually reached by each Paper server. |
| Players online | Per server and in the whole network. |
| Memory and garbage collection pauses | Heap usage and the length of each pause. Long pauses freeze every match on the server and can make it lose session ownership. |

## Matches and arenas

| Metric | What it shows |
|---|---|
| Active matches | Per server and per game mode. |
| Players waiting for a match | Per game mode. |
| Arena reset time | How long each arena instance takes to be reset after a match. |
| Arena reset failures | Failed resets per arena, and arenas currently disabled. |
| Cancelled matches | Per game mode and per reason. |

## Persistence

| Metric | What it shows |
|---|---|
| Transaction time | How long PostgreSQL transactions take, per kind of operation. |
| Waiting for a connection | How long code waits for a free connection in the pool, and how many connections are in use. Waiting here means the pool is too small or a query is too slow. |
| Failed transactions | Per kind of operation. |
| Rejected writes | Writes refused because the server's session generation was outdated. Any value above zero needs attention. |
| Redis latency | How long Redis commands take. |

## Messaging

| Metric | What it shows |
|---|---|
| Packets sent and received | Per packet type and per server. |
| Request timeouts | Requests that got no answer in time, per packet type. |
| Rejected packets | Packets refused for being too large, or ignored for an unknown type or protocol version. |
| Redis reconnections | How often each server lost its connection to Redis. |

## Moving between servers

| Metric | What it shows |
|---|---|
| Transfers | Per origin and destination server. |
| Transfer time | From the moment the origin stops accepting changes until the destination loads the player. |
| Failed transfers | Per reason, such as a save timeout or a failed takeover. |

## Rules

- **Measuring never slows the game down.** Metrics are collected without blocking the main thread and with low cost per sample.
- **A feature documents its own metrics** when it adds any, in its feature documentation.
- **A number without a baseline means little.** Once the network runs, the normal value of each metric is recorded so that changes stand out.
