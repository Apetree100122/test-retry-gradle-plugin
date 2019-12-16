plugins {
    id("nebula.release") version "13.2.1"
}

buildScan {
    val buildUrl = System.getenv("BUILD_URL") ?: ""
    if (buildUrl.isNotBlank()) {
        link("Build URL", buildUrl)
    }
}

group = "org.gradle"
description = "Mitigate flaky tests by retrying tests when they fail"

evaluationDependsOn("plugin")

val publishPlugins = tasks.findByPath(":plugin:publishPlugins")

tasks.named("final") {
    dependsOn(publishPlugins)
}

tasks.named("candidate") {
    dependsOn(publishPlugins)
}
