# Minecraft Plugin Ecosystem Monorepo Structure

Date: 2026-06-28

## Goal

Define the project layout and build configuration for a Gradle Kotlin monorepo that hosts multiple Minecraft-related projects: a clans Paper plugin, a Velocity proxy plugin with server management and friends, and future client mods.

The structure must support independent platform modules, shared core libraries, and a clear separation of concerns — without putting every module in the root directory.

## Directory Layout

All modules live under `modules/<domain>/<module-name>/`.

```
minecraft-plugin/
├── settings.gradle.kts
├── build.gradle.kts
├── gradle.properties
├── gradle/
│   └── libs.versions.toml
│
├── modules/
│   ├── clans/
│   │   ├── clans-core/          # Domain models & rules (clans, claims, homes, PvP)
│   │   ├── clans-paper/         # PaperMC plugin implementation
│   │   └── clans-velocity/      # Velocity clan proxy integration
│   │
│   ├── proxy/
│   │   ├── proxy-core/          # Server routing & lobby domain logic
│   │   └── proxy-velocity/      # Velocity proxy plugin
│   │
│   ├── friends/
│   │   ├── friends-core/        # Friends system domain logic
│   │   └── friends-velocity/    # Velocity friends plugin
│   │
│   └── client/                  # (added when concrete client work begins)
│       ├── fabric/
│       └── neoforge/
│
├── clans-core/src/              # (deprecated — content moved to modules/clans/clans-core/)
├── clans-paper/src/             # (deprecated — content moved to modules/clans/clans-paper/)
└── src/                         # (deprecated skeleton)
```

Gradle module paths follow the directory structure:

```
:modules:clans:clans-core
:modules:clans:clans-paper
:modules:clans:clans-velocity
:modules:proxy:proxy-core
:modules:proxy:proxy-velocity
:modules:friends:friends-core
:modules:friends:friends-velocity
```

## Root Build Configuration

### `settings.gradle.kts`

```kotlin
pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://maven.fabricmc.net/")
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://maven.fabricmc.net/")
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.9.0"
}

rootProject.name = "clans-ecosystem"

include(
    ":modules:clans:clans-core",
    ":modules:clans:clans-paper",
    ":modules:clans:clans-velocity",
    ":modules:proxy:proxy-core",
    ":modules:proxy:proxy-velocity",
    ":modules:friends:friends-core",
    ":modules:friends:friends-velocity",
)
```

### Root `build.gradle.kts`

```kotlin
plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.shadow) apply false
}

allprojects {
    group = "net.clans"
    version = "0.1.0-SNAPSHOT"
}

subprojects {
    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }
}
```

Root stays light. Platform plugins (Shadow, Fabric Loom) are applied only in the modules that need them.

### `gradle.properties`

```properties
kotlin.code.style=official
kotlin.stdlib.default.dependency=false
org.gradle.jvmargs=-Xmx2g -Dfile.encoding=UTF-8
```

### `gradle/libs.versions.toml`

Single version catalog for the whole ecosystem:

```toml
[versions]
kotlin = "2.1.0"
junit = "5.11.4"

paper = "26.2.build.34-alpha"
velocity = "3.5.0-SNAPSHOT"

hikari = "5.1.0"
postgres = "42.7.4"
flyway = "10.18.2"
testcontainers = "1.20.4"

shadow = "8.3.6"

[libraries]
kotlin-stdlib = { group = "org.jetbrains.kotlin", name = "kotlin-stdlib", version.ref = "kotlin" }

paper-api = { group = "io.papermc.paper", name = "paper-api", version.ref = "paper" }

velocity-api = { group = "com.velocitypowered", name = "velocity-api", version.ref = "velocity" }

hikari = { group = "com.zaxxer", name = "HikariCP", version.ref = "hikari" }
postgres = { group = "org.postgresql", name = "postgresql", version.ref = "postgres" }
flyway-core = { group = "org.flywaydb", name = "flyway-core", version.ref = "flyway" }
flyway-postgres = { group = "org.flywaydb", name = "flyway-database-postgresql", version.ref = "flyway" }

junit-bom = { group = "org.junit", name = "junit-bom", version.ref = "junit" }
junit-jupiter = { group = "org.junit.jupiter", name = "junit-jupiter" }

testcontainers-junit = { group = "org.testcontainers", name = "junit-jupiter", version.ref = "testcontainers" }
testcontainers-postgres = { group = "org.testcontainers", name = "postgresql", version.ref = "testcontainers" }

[plugins]
kotlin-jvm = { id = "org.jetbrains.kotlin.jvm", version.ref = "kotlin" }
shadow = { id = "com.gradleup.shadow", version.ref = "shadow" }
```

## Per-Module Build Patterns

### Core library modules

Group: `net.clans`, JVM target: 21. No Minecraft dependencies. Published as `java-library`.

```kotlin
// modules/clans/clans-core/build.gradle.kts
plugins {
    `java-library`
    alias(libs.plugins.kotlin.jvm)
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(21)
}

dependencies {
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(kotlin("test"))
}
```

Same pattern for `proxy-core` and `friends-core`.

### Paper plugin modules

Depend on their core module, Paper API as `compileOnly`, shaded jar for deployment.

```kotlin
// modules/clans/clans-paper/build.gradle.kts
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.shadow)
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

dependencies {
    implementation(project(":modules:clans:clans-core"))
    compileOnly(libs.paper.api)
    implementation(libs.hikari)
    implementation(libs.postgres)
    implementation(libs.flyway.core)
    implementation(libs.flyway.postgres)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(kotlin("test"))
}

tasks.named<ShadowJar>("shadowJar") {
    archiveClassifier.set("")
    minimize()
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
```

### Velocity plugin modules

Depend on their core module, Velocity API as `compileOnly`, annotated with `@Plugin`.

```kotlin
// modules/clans/clans-velocity/build.gradle.kts
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.shadow)
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

dependencies {
    implementation(project(":modules:clans:clans-core"))
    compileOnly(libs.velocity.api)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(kotlin("test"))
}

tasks.named<ShadowJar>("shadowJar") {
    archiveClassifier.set("")
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
```

Same pattern for `proxy-velocity` and `friends-velocity`.

## Kotlin Stdlib Strategy

- Core library modules: no explicit stdlib dependency (Gradle adds it automatically).
- Paper plugin: bundle Kotlin stdlib into the shaded jar. Do not rely on server-provided Kotlin runtime.
- Velocity plugin: bundle Kotlin stdlib into the shaded jar.

## JVM Target Summary

| Module | JVM Target |
|---|---|
| `clans-core`, `proxy-core`, `friends-core` | 21 |
| `clans-paper` | 21 |
| `clans-velocity`, `proxy-velocity`, `friends-velocity` | 21 |

All core and server/proxy modules target Java 21, matching Velocity 3.5's minimum requirement and Paper's current runtime baseline. No module targets a higher JVM than its consumers can run on.

## IntelliJ Integration

- Import as Gradle project — IntelliJ discovers modules from `settings.gradle.kts`.
- Keep platform metadata (`paper-plugin.yml`, `@Plugin` annotation, `fabric.mod.json`) inside each platform module.
- Module names in Gradle (`:modules:clans:clans-paper`) are verbose but clear in run configs and CI.

## Migration Path from Current State

The current worktree at `.worktrees/implement-clans-mvp/` has `clans-core/` and `clans-paper/` at the root. The migration to the new structure:

1. Scaffold the new `modules/` layout with Gradle build files (no source code yet).
2. Copy `clans-core/src/` → `modules/clans/clans-core/src/`.
3. Copy `clans-paper/src/` → `modules/clans/clans-paper/src/`.
4. Migrate root build files (`settings.gradle.kts`, `build.gradle.kts`, `gradle/libs.versions.toml`).
5. Verify `./gradlew test` and `./gradlew :modules:clans:clans-paper:jar` pass.
6. Remove the old root-level module directories and deprecated `src/` directory.

The old worktree branch is preserved in `.worktrees/` for reference; the new structure is built on the current main branch.

## Naming Convention

**Gradle module paths** follow the directory exactly: `:modules:<domain>:<module-name>`.

**Module artifact names** default to the module directory name (e.g., `clans-core`), producing jars like `clans-core-0.1.0-SNAPSHOT.jar`.

**Package names** for core modules follow `net.clans.<domain>`:
- `net.clans.core` — clans domain models and rules
- `net.clans.proxy` — server routing and lobby logic
- `net.clans.friends` — friends system logic

Package names for platform modules follow `net.clans.<domain>.<platform>`:
- `net.clans.core.paper` — Paper clans implementation
- `net.clans.core.velocity` — Velocity clans integration
- `net.clans.proxy.velocity` — Velocity proxy plugin
- `net.clans.friends.velocity` — Velocity friends plugin

## Scope Notes

This spec covers only the **project structure and build configuration**. It does not define or modify any plugin features, domain models, commands, or database schemas — those are covered by the existing `2026-06-25-minecraft-clans-plugin-design.md` spec.

Future additions (Fabric mods, NeoForge mods) will get their own module directories under `modules/client/` with separate build configuration when concrete work begins.
