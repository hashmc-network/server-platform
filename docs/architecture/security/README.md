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
- Permissions are separated by service. The game servers and the proxy connect with a user that can read and write data but cannot change the schema. Migrations run with a separate user. Neither is a superuser.

## Redis

- Listens only on the private network.
- Uses ACLs: each service has its own Redis user, limited to the commands, keys and channels it needs. A service that only reads server status cannot publish packets.
- **Access to Redis means control of the network:** a user allowed to publish packets can send them to every server. The private network is the first barrier and the ACLs are the second; both are mandatory.

## Traffic between machines

While every service runs on the same machine, traffic stays local. Once PostgreSQL, Redis or the servers run on different machines, the traffic between them uses TLS, unless it already travels over an encrypted private network.

## Secrets

- Passwords, the forwarding secret and any other credential are never committed.
- They are provided through environment variables or local configuration files ignored by git.
- A secret that was exposed, even briefly, is replaced.
