**English** | [Português](README.pt-BR.md)

# Network security

How the network protects its servers and services. The principle is simple: players reach only the proxy, and everything else is private.

## What is exposed

| Service | Reachable by players | Reachable by |
|---|---|---|
| Velocity | Yes, it is the only public entry point | Everyone |
| Paper servers | No | The proxy only |
| PostgreSQL | No | The network's servers only |
| Redis | No | The network's servers only |

## Proxy and Paper servers

- Velocity authenticates players with Mojang (`online-mode` enabled on the proxy).
- Velocity forwards player identities to the Paper servers using **modern forwarding**, signed with a secret shared between the proxy and each server. A Paper server only accepts connections that carry a valid signature.
- The Paper servers' ports are closed to the internet by the firewall, or bound to a private network. Even with modern forwarding, a Paper server reachable from the internet is a risk that is not accepted.

## PostgreSQL

- Listens only on the private network, and accepts connections only from the network's servers.
- The servers connect with a dedicated user that has only the privileges they need. It is never a superuser.

## Redis

- Listens only on the private network, with authentication enabled.
- **Access to Redis means control of the network:** any process that can publish to Redis can send packets to every server. Servers trust the packets they receive because only the network's own servers can reach Redis, so keeping Redis private is mandatory.

## Secrets

- Passwords, the forwarding secret and any other credential are never committed.
- They are provided through environment variables or local configuration files ignored by git.
- A secret that was exposed, even briefly, is replaced.
