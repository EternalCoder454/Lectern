pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.neoforged.net/releases")
        maven("https://maven.parchmentmc.org")
    }
}

plugins {
    id("net.neoforged.moddev") version "2.0.144" apply false
}

rootProject.name = "lectern"
