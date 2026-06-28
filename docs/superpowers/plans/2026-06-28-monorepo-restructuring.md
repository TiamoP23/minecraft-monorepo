# Monorepo Restructuring Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Restructure the project from a single-module Gradle project into a monorepo root with the existing plugin code moved into `modules/clans/clans-paper/`.

**Architecture:** The root becomes a lightweight Gradle composite build root. All module code lives under `modules/<domain>/<module-name>/`. Only `modules/clans/clans-paper/` is created (no empty scaffolded modules). Root build files declare shared config; the version catalog centralizes dependency versions.

**Tech Stack:** Gradle Kotlin DSL, Kotlin 2.4.0, Paper API 26.2.build.+, Shadow 9.4.3, run-paper 3.0.2, JUnit 5

## Global Constraints

- Do not create any new empty modules. Only move the existing plugin code.
- Use actual dependency versions from the IntelliJ template (kotlin 2.4.0, paper-api 26.2.build.+, shadow 9.4.3, run-paper 3.0.2).
- After restructuring, `./gradlew :modules:clans:clans-paper:jar` must produce a valid plugin jar.
- Group: `net.clans`, version: `0.1.0-SNAPSHOT`.
- Package: `net.clans.paper` (matching the spec's naming convention).
- JVM target: 21.

---

### Task 1: Create directory structure and move source files

**Files:**
- Move: `src/` → `modules/clans/clans-paper/src/`
- Delete: `src/` (after move)

- [ ] **Step 1: Create the target directory structure**

```bash
mkdir -p modules/clans/clans-paper
```

- [ ] **Step 2: Move the source tree**

```bash
mv src modules/clans/clans-paper/src
```

- [ ] **Step 3: Verify the move**

```bash
ls modules/clans/clans-paper/src/main/kotlin/de/tiamop23/clans/Clans.kt
ls modules/clans/clans-paper/src/main/resources/plugin.yml
```

Expected: Both files listed.

---

### Task 2: Update package names and plugin metadata

**Files:**
- Modify: `modules/clans/clans-paper/src/main/kotlin/de/tiamop23/clans/Clans.kt`
- Modify: `modules/clans/clans-paper/src/main/resources/plugin.yml`

- [ ] **Step 1: Create the new package directory**

```bash
mkdir -p modules/clans/clans-paper/src/main/kotlin/net/clans/paper
```

- [ ] **Step 2: Move the source file to the new package path**

```bash
mv modules/clans/clans-paper/src/main/kotlin/de/tiamop23/clans/Clans.kt modules/clans/clans-paper/src/main/kotlin/net/clans/paper/Clans.kt
```

- [ ] **Step 3: Remove the old empty package directories**

```bash
rm -rf modules/clans/clans-paper/src/main/kotlin/de
```

- [ ] **Step 4: Update the package declaration in Clans.kt**

Change the first line from `package de.tiamop23.clans` to `package net.clans.paper`.

Full file content for `modules/clans/clans-paper/src/main/kotlin/net/clans/paper/Clans.kt`:

```kotlin
package net.clans.paper

import org.bukkit.plugin.java.JavaPlugin

class Clans : JavaPlugin() {

    override fun onEnable() {
        logger.info("${this.name} enabled2")
    }

    override fun onDisable() {
        // Plugin shutdown logic
    }
}
```

- [ ] **Step 5: Update plugin.yml**

Full content for `modules/clans/clans-paper/src/main/resources/plugin.yml`:

```yaml
name: Clans
version: '${version}'

main: net.clans.paper.Clans
api-version: '26.2'
load: POSTWORLD

authors: [ TiamoP23 ]
```

---

### Task 3: Create the module build file

**Files:**
- Create: `modules/clans/clans-paper/build.gradle.kts`

- [ ] **Step 1: Create `modules/clans/clans-paper/build.gradle.kts`**

```kotlin
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.shadow)
    alias(libs.plugins.run.paper)
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly(libs.paper.api)
    implementation(libs.kotlin.stdlib)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(21)
}

tasks {
    build {
        dependsOn(shadowJar)
    }

    runServer {
        minecraftVersion(libs.versions.minecraft.get())
        jvmArgs("-Xms2G", "-Xmx2G")
    }

    processResources {
        val props = mapOf("version" to version)
        filesMatching("plugin.yml") {
            expand(props)
        }
    }
}
```

---

### Task 4: Update the version catalog

**Files:**
- Modify: `gradle/libs.versions.toml`

- [ ] **Step 1: Replace `gradle/libs.versions.toml` with the updated catalog**

```toml
[versions]
kotlin = "2.4.0"

paper-api = "26.2.build.+"
minecraft = "26.2"

shadow = "9.4.3"
run-task = "3.0.2"
junit = "5.11.4"

[libraries]
paper-api = { module = "io.papermc.paper:paper-api", version.ref = "paper-api" }
kotlin-stdlib = { module = "org.jetbrains.kotlin:kotlin-stdlib-jdk8", version.ref = "kotlin" }

junit-bom = { group = "org.junit", name = "junit-bom", version.ref = "junit" }
junit-jupiter = { group = "org.junit.jupiter", name = "junit-jupiter" }

[plugins]
kotlin-jvm = { id = "org.jetbrains.kotlin.jvm", version.ref = "kotlin" }
shadow = { id = "com.gradleup.shadow", version.ref = "shadow" }
run-paper = { id = "xyz.jpenilla.run-paper", version.ref = "run-task" }
```

---

### Task 5: Update root build files

**Files:**
- Modify: `settings.gradle.kts`
- Modify: `build.gradle.kts`
- Modify: `gradle.properties`

- [ ] **Step 1: Replace `settings.gradle.kts`**

```kotlin
pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://repo.papermc.io/repository/maven-public/")
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.9.0"
}

rootProject.name = "clans-ecosystem"

include(
    ":modules:clans:clans-paper",
)
```

- [ ] **Step 2: Replace root `build.gradle.kts`**

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

- [ ] **Step 3: Replace `gradle.properties`**

```properties
kotlin.code.style=official
kotlin.stdlib.default.dependency=false
org.gradle.jvmargs=-Xmx2g -Dfile.encoding=UTF-8
org.gradle.configuration-cache=true
org.gradle.parallel=true
org.gradle.caching=true
```

---

### Task 6: Create README.md and AGENTS.md

**Files:**
- Create: `README.md`
- Create: `AGENTS.md`

- [ ] **Step 1: Create `README.md`**

```markdown
# Clans Ecosystem

A Gradle monorepo for a Minecraft plugin ecosystem built on Paper and Velocity.

## Structure

All modules live under `modules/<domain>/<module-name>/`:

```
modules/
├── clans/
│   └── clans-paper/      # PaperMC clans plugin
├── proxy/                 # (future)
└── friends/               # (future)
```

Gradle module paths follow the directory structure: `:modules:clans:clans-paper`.

## Building

```bash
./gradlew :modules:clans:clans-paper:jar        # Build the Paper plugin
./gradlew :modules:clans:clans-paper:runServer   # Run a test server
```

## Adding a new module

1. Create the directory: `modules/<domain>/<module-name>/`
2. Add a `build.gradle.kts` in the module directory.
3. Include it in `settings.gradle.kts`: `":modules:<domain>:<module-name>"`.
4. Add any new dependencies to `gradle/libs.versions.toml`.
```

- [ ] **Step 2: Create `AGENTS.md`**

```markdown
# AGENTS.md

## Project Structure

This is a Gradle monorepo. All modules follow the directory convention:

```
modules/<domain>/<module-name>/
```

Gradle module paths: `:modules:<domain>:<module-name>` (e.g., `:modules:clans:clans-paper`).

## Version Catalog

All dependency versions are declared in `gradle/libs.versions.toml`. Use `libs.*` accessors in build files — never hardcode versions.

## JVM Target

All modules target JVM 21. Use `jvmToolchain(21)` in each module's `build.gradle.kts`.

## Core Module Policy

Core modules (e.g., `clans-core`, `proxy-core`, `friends-core`) must not depend on Minecraft APIs (Paper, Velocity, etc.). They contain only domain models and business logic.

## Build Commands

```bash
./gradlew projects                                              # List all modules
./gradlew :modules:clans:clans-paper:compileKotlin              # Compile a module
./gradlew :modules:clans:clans-paper:jar                        # Build a plugin jar
./gradlew :modules:clans:clans-paper:runServer                  # Run test server
```
```

---

### Task 7: Verify the build

- [ ] **Step 1: Run `./gradlew projects`**

```bash
./gradlew projects
```

Expected output includes `:modules:clans:clans-paper`.

- [ ] **Step 2: Run `./gradlew :modules:clans:clans-paper:compileKotlin`**

```bash
./gradlew :modules:clans:clans-paper:compileKotlin
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Run `./gradlew :modules:clans:clans-paper:jar`**

```bash
./gradlew :modules:clans:clans-paper:jar
```

Expected: BUILD SUCCESSFUL, jar produced in `modules/clans/clans-paper/build/libs/`.
