plugins {
    id("hashmc.paper-plugin")
}

dependencies {
    compileOnly(projects.platform.core)
    implementation(projects.gameEngine)
}
