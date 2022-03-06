pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://papermc.io/repo/repository/maven-public/")
    }
    plugins {
        val kotlinVersion: String by System.getProperties()
        kotlin("jvm").version(kotlinVersion)
    }


}
