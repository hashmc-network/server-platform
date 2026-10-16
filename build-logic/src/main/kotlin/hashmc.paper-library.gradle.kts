plugins {
    id("hashmc.library")
}

val libs = the<VersionCatalogsExtension>().named("libs")

dependencies {
    compileOnly(libs.findLibrary("paper-api").get())
}
