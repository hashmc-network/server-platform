plugins {
    id("hashmc.library")
    id("com.gradleup.shadow")
}

tasks.jar {
    archiveClassifier = "plain"
}

tasks.shadowJar {
    archiveFileName = "hashmc-${project.name}.jar"
    mergeServiceFiles()
}

tasks.assemble {
    dependsOn(tasks.shadowJar)
}

configurations.consumable("serverPlugin") {
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(SERVER_PLUGIN_USAGE))
    }
    outgoing.artifact(tasks.shadowJar)
}
