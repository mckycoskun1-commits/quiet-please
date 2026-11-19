pluginManagement {
    repositories {
        maven("https://maven.fabricmc.net/")
        maven("https://maven.architectury.dev/")
        maven("https://maven.minecraftforge.net/")
        maven("https://maven.neoforged.net/releases/")
        maven("https://maven.kikugie.dev/releases")
        gradlePluginPortal()
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.10"
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

stonecutter {
    // Her Minecraft surumu ve yukleyici icin bir alt proje: versions/<surum>-<yukleyici>
    centralScript = "build.gradle"
    create(rootProject) {
        fun mc(version: String, vararg loaders: String) {
            for (loader in loaders) version("$version-$loader", version)
        }
        mc("1.18.2", "fabric", "forge")
        mc("1.19.2", "fabric", "forge")
        mc("1.20.1", "fabric", "forge")
        mc("1.21.1", "fabric", "forge", "neoforge")
        mc("26.1.2", "fabric", "forge", "neoforge")
        mc("26.2", "fabric", "forge", "neoforge")
        mc("26.3", "fabric", "forge", "neoforge")
        vcsVersion = "26.1.2-fabric"
    }
}

rootProject.name = "quietplease"
