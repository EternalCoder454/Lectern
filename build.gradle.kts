plugins {
    java
    id("net.neoforged.moddev")
}

val modVersion: String = property("mod_version") as String
val javaVersion: String = property("java_version") as String

// The loader this branch builds for, and the name of the source set holding its code.
// Set in gradle.properties; a second loader is a second source set, not a second branch of ifs.
val loaderName: String = property("lectern.loader") as String

group = property("group") as String
version = modVersion
base.archivesName = "lectern"

java {
    toolchain.languageVersion = JavaLanguageVersion.of(javaVersion.toInt())
}

repositories {
    mavenCentral()
    maven("https://maven.neoforged.net/releases/") { name = "NeoForged" }
    // Scoped to the one group, so a personal maven can never shadow a Minecraft or NeoForge
    // artifact by accident.
    maven("https://maven.shedaniel.me/") {
        name = "shedaniel"
        content { includeGroup("me.shedaniel.cloth") }
    }
}

dependencies {
    // Optional at runtime, compile-only here. Nothing outside net.lectern.screen.cloth mentions
    // it, and that package is only reached once the mod is confirmed loaded -- resolving a class
    // whose signatures name an absent mod throws NoClassDefFoundError, so the guard has to keep
    // every mention behind it rather than merely guard the call.
    compileOnly("me.shedaniel.cloth:cloth-config-neoforge:${property("cloth_config_version")}")
}

// Every loader gets a source set beside src/main. src/main compiles against a loader that may not
// be present at runtime on another target, so a stray import there has to be a build failure here
// rather than a discovery on someone else's machine -- see checkMainIsLoaderNeutral below.
val loaderSourceSet: SourceSet = sourceSets.create(loaderName) {
    // ModDevGradle only puts Minecraft and NeoForge on `main`, so inheriting main's compile
    // classpath is what lets this set see them at all; main's output is what lets it extend
    // main's classes. Taking the output alone compiles src/main fine and then fails here with
    // "package net.neoforged.fml does not exist", which reads like a missing dependency and is not.
    compileClasspath += sourceSets.main.get().compileClasspath + sourceSets.main.get().output
    runtimeClasspath += sourceSets.main.get().runtimeClasspath + sourceSets.main.get().output
}

configurations {
    named(loaderSourceSet.implementationConfigurationName) {
        extendsFrom(configurations.implementation.get())
    }
}

neoForge {
    version = property("neoforge_version") as String

    parchment {
        mappingsVersion = property("parchment_version") as String
        minecraftVersion = property("parchment_mc_version") as String
    }

    mods {
        create("lectern") {
            sourceSet(sourceSets.main.get())
            sourceSet(loaderSourceSet)
        }
    }

    runs {
        create("client") { client() }
    }
}

tasks.named<Jar>("jar") {
    from(loaderSourceSet.output)
}

// Read from the project here, not inside the task block: inside it `property(...)` resolves
// against the Task, which has no idea what minecraft_version is.
val expansions = mapOf(
    "mod_version" to modVersion,
    "minecraft_version" to (project.property("minecraft_version") as String),
    "neoforge_version" to (project.property("neoforge_version") as String),
)

tasks.named<ProcessResources>("processResources") {
    inputs.properties(expansions)
    filesMatching("META-INF/neoforge.mods.toml") { expand(expansions) }
}

/**
 * Fails the build if src/main names a mod loader.
 *
 * ModDevGradle puts Minecraft *and* NeoForge on the main compile classpath, so a `net.neoforged`
 * import in src/main compiles happily here and only fails once somebody builds another target.
 * Checking it is what makes the split real rather than aspirational.
 */
val checkMainIsLoaderNeutral by tasks.registering {
    group = "verification"
    description = "Fails if src/main imports a mod loader package."
    val mainJava = sourceSets.main.get().allJava
    inputs.files(mainJava)
    doLast {
        val banned = Regex("""^\s*import\s+(net\.neoforged|net\.fabricmc|net\.minecraftforge)\b.*$""")
        val offences = mutableListOf<String>()
        mainJava.forEach { file ->
            file.readLines().forEachIndexed { i, line ->
                if (banned.containsMatchIn(line)) {
                    offences += "    ${file.name}:${i + 1}  ${line.trim()}"
                }
            }
        }
        if (offences.isNotEmpty()) {
            throw GradleException(
                "src/main must not name a mod loader; move this to src/$loaderName:\n"
                        + offences.joinToString("\n")
            )
        }
    }
}

tasks.named("check") { dependsOn(checkMainIsLoaderNeutral) }
