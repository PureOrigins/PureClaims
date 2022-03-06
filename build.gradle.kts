import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    val kotlinVersion: String by System.getProperties()
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.serialization") version kotlinVersion
    id("io.papermc.paperweight.userdev") version "1.3.4"
    id("net.minecrell.plugin-yml.bukkit") version "0.5.1"
}

bukkit {
    name = project.name
    version = project.version.toString()
    main = "it.pureorigins.${project.name.toLowerCase()}.${project.name}"
    depend = listOf("PureCommon")
}

repositories {
    maven("https://jitpack.io")
    mavenCentral()
}

dependencies {
    val spigotVersion: String by project
    paperDevBundle(spigotVersion)
    val commonVersion: String by project
    compileOnly("com.github.PureOrigins:PureCommon:$commonVersion")
}

kotlin {
    jvmToolchain { (this as JavaToolchainSpec).languageVersion.set(JavaLanguageVersion.of(17)) }
}

val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions { jvmTarget = "17" }

afterEvaluate {
    tasks {
        reobfJar {
            outputJar.set(jar.get().archiveFile)
        }

        build {
            dependsOn(reobfJar)
        }
    }
}