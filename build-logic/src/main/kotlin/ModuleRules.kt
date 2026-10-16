object ModuleRules {

    private val allowedDependencies: Map<String, List<String>> = linkedMapOf(
        ":common" to listOf(":common"),
        ":platform:persistence" to listOf(":common"),
        ":platform:messaging" to listOf(":common"),
        ":platform:core" to listOf(":common", ":platform:persistence", ":platform:messaging"),
        ":platform:proxy" to listOf(":common", ":platform:persistence", ":platform:messaging"),
        ":game-engine" to listOf(":common", ":platform:core"),
        ":games" to listOf(":common", ":platform:core", ":game-engine"),
        ":servers" to listOf(":common", ":platform:core"),
    )

    fun allowedFor(module: String): List<String>? =
        allowedDependencies.entries.firstOrNull { belongsTo(module, it.key) }?.value

    fun belongsTo(module: String, group: String): Boolean =
        module == group || module.startsWith("$group:")
}
