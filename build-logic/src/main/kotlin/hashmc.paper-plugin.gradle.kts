plugins {
    id("hashmc.server-plugin")
}

val libs = the<VersionCatalogsExtension>().named("libs")

dependencies {
    compileOnly(libs.findLibrary("paper-api").get())
}

tasks.processResources {
    val pluginProperties = mapOf(
        "version" to project.version.toString(),
        "apiVersion" to libs.findVersion("paper").get().requiredVersion.substringBefore(".build"),
    )
    inputs.properties(pluginProperties)
    filesMatching("plugin.yml") {
        expand(pluginProperties)
    }
}
