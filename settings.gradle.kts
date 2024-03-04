rootProject.name = "tribot-script-template"

include("libraries:my-library")
include("scripts:my-script")
include("scripts:muler")
include("scripts:revs")
include("scripts:woodcutter")
include("scripts")
include("scripts:promercher")

pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}
