plugins {
    id("hashmc.network")
}

network {
    server("proxy", projects.platform.proxy)
    server("auth", projects.platform.core, projects.servers.auth)
    server("lobby", projects.platform.core, projects.servers.lobby)
    server("bedwars", projects.platform.core, projects.games.bedwars)
    server("duels", projects.platform.core, projects.games.duels)
}
