plugins {
    id("hashmc.server-plugin")
}

val libs = the<VersionCatalogsExtension>().named("libs")

dependencies {
    compileOnly(libs.findLibrary("velocity-api").get())
}

tasks.processResources {
    val pluginProperties = mapOf(
        "version" to project.version.toString(),
    )
    inputs.properties(pluginProperties)
    filesMatching("velocity-plugin.json") {
        expand(pluginProperties)
    }
}
