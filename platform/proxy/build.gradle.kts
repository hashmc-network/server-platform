plugins {
    id("hashmc.velocity-plugin")
}

dependencies {
    implementation(projects.common.model)
    implementation(projects.common.network)
    implementation(projects.platform.persistence)
    implementation(projects.platform.messaging)
}
