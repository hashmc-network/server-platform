**English** | [Português](README.pt-BR.md)

# Documentation

Every feature of the platform is documented in its own folder under [`features/`](features/), in English and Portuguese. The rules are in [CONTRIBUTING.md](../CONTRIBUTING.md#documentation).

## Architecture

- [Network](architecture/network/README.md): how the network is split into servers, today and in the future.
- [Persistence](architecture/persistence/README.md): how data is stored in PostgreSQL and cached in memory and Redis.
- [Messaging](architecture/messaging/README.md): how servers exchange packets over Redis.
- [Game engine](architecture/game-engine/README.md): how matches run and stay isolated inside each game server.
- [Security](architecture/security/README.md): what is exposed, and how servers, services and secrets are protected.

## Features

No features documented yet.

<!-- One line per feature, in alphabetical order: - [Anticheat](features/anticheat/README.md): short summary. -->

## Documenting a new feature

1. Copy [`features/_template/`](features/_template/) to `features/<feature>/`, using the same name as the commit scope.
2. Fill in both `README.md` and `README.pt-BR.md`.
3. Add the feature to the list above, in both this file and [README.pt-BR.md](README.pt-BR.md).
