plugins {
    id("hashmc.paper-plugin")
}

dependencies {
    api(projects.common.model)
    api(projects.common.network)
    implementation(projects.platform.persistence)
    implementation(projects.platform.messaging)
}
