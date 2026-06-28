# Monorepo Rename Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rename the repository identity to `minecraft-monorepo` and move the clans Paper module into the `de.tiamop23.minecraft.clans.paper` JVM namespace.

**Architecture:** This is a metadata and namespace migration. Gradle module paths stay unchanged while the root project name, group ID, source package, source directory, plugin entrypoint, IDE display name, and README are updated consistently.

**Tech Stack:** Gradle Kotlin DSL, Kotlin/JVM 25, Paper plugin metadata, IntelliJ IDEA project metadata.

## Global Constraints

- Gradle root project name must be `minecraft-monorepo`.
- Gradle group must be `de.tiamop23.minecraft`.
- Source packages must follow `de.tiamop23.minecraft.<domain>.<module>`.
- Current clans Paper package must be `de.tiamop23.minecraft.clans.paper`.
- Existing Gradle module path `:modules:clans:clans-paper` must remain unchanged.
- Existing plugin name `Clans`, module directory `modules/clans/clans-paper`, and version `0.1.0-SNAPSHOT` must remain unchanged.
- Do not rewrite historical specs or plans as part of this migration.

---

## File Structure

- Modify `settings.gradle.kts`: Gradle root project name only.
- Modify `build.gradle.kts`: Maven group only.
- Move and modify `modules/clans/clans-paper/src/main/kotlin/net/clans/paper/Clans.kt` to `modules/clans/clans-paper/src/main/kotlin/de/tiamop23/minecraft/clans/paper/Clans.kt`: package declaration only.
- Modify `modules/clans/clans-paper/src/main/resources/plugin.yml`: Paper `main` class reference only.
- Modify `README.md`: title and description to reflect generic Minecraft monorepo scope.
- Modify `.idea/.name`: IntelliJ display name.
- Leave `docs/superpowers/specs/*.md` and `docs/superpowers/plans/*.md` historical references unchanged.

### Task 1: Rename Root Identity And JVM Namespace

**Files:**
- Modify: `settings.gradle.kts`
- Modify: `build.gradle.kts`
- Move: `modules/clans/clans-paper/src/main/kotlin/net/clans/paper/Clans.kt` -> `modules/clans/clans-paper/src/main/kotlin/de/tiamop23/minecraft/clans/paper/Clans.kt`
- Modify: `modules/clans/clans-paper/src/main/resources/plugin.yml`
- Modify: `README.md`
- Modify: `.idea/.name`

**Interfaces:**
- Consumes: current Gradle module path `:modules:clans:clans-paper`.
- Produces: root project name `minecraft-monorepo`, group `de.tiamop23.minecraft`, plugin main class `de.tiamop23.minecraft.clans.paper.Clans`.

- [ ] **Step 1: Capture current references**

Run: `rg "clans-ecosystem|net\.clans|net/clans|main: net\.clans|# Clans Ecosystem" settings.gradle.kts build.gradle.kts README.md .idea modules`

Expected: matches in `settings.gradle.kts`, `build.gradle.kts`, `README.md`, `.idea/.name`, `modules/clans/clans-paper/src/main/kotlin/net/clans/paper/Clans.kt`, and `modules/clans/clans-paper/src/main/resources/plugin.yml`.

- [ ] **Step 2: Update Gradle root name**

Change `settings.gradle.kts` line 20 from:

```kotlin
rootProject.name = "clans-ecosystem"
```

to:

```kotlin
rootProject.name = "minecraft-monorepo"
```

- [ ] **Step 3: Update Gradle group**

Change `build.gradle.kts` line 7 from:

```kotlin
group = "net.clans"
```

to:

```kotlin
group = "de.tiamop23.minecraft"
```

- [ ] **Step 4: Move Kotlin source file**

Create directory `modules/clans/clans-paper/src/main/kotlin/de/tiamop23/minecraft/clans/paper`.

Move `modules/clans/clans-paper/src/main/kotlin/net/clans/paper/Clans.kt` to `modules/clans/clans-paper/src/main/kotlin/de/tiamop23/minecraft/clans/paper/Clans.kt`.

Remove the now-empty `modules/clans/clans-paper/src/main/kotlin/net/clans/paper`, `modules/clans/clans-paper/src/main/kotlin/net/clans`, and `modules/clans/clans-paper/src/main/kotlin/net` directories if they are empty.

- [ ] **Step 5: Update Kotlin package declaration**

Change the first line of `modules/clans/clans-paper/src/main/kotlin/de/tiamop23/minecraft/clans/paper/Clans.kt` from:

```kotlin
package net.clans.paper
```

to:

```kotlin
package de.tiamop23.minecraft.clans.paper
```

Leave the class name and lifecycle methods unchanged:

```kotlin
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

- [ ] **Step 6: Update Paper plugin entrypoint**

Change `modules/clans/clans-paper/src/main/resources/plugin.yml` line 4 from:

```yaml
main: net.clans.paper.Clans
```

to:

```yaml
main: de.tiamop23.minecraft.clans.paper.Clans
```

- [ ] **Step 7: Update README identity**

Replace the full contents of `README.md` with:

````markdown
# Minecraft Monorepo

A Gradle monorepo for Minecraft plugins and mods. The current modules target Paper, with room for unrelated plugin or mod domains over time.

## Structure

All modules live under `modules/<domain>/<module-name>/`:

```text
modules/
├── clans/
│   └── clans-paper/      # PaperMC clans plugin
├── proxy/                 # (future)
└── friends/               # (future)
```

Gradle module paths follow the directory structure: `:modules:clans:clans-paper`.

Kotlin/JVM packages follow `de.tiamop23.minecraft.<domain>.<module>`.

## Building

```bash
./gradlew :modules:clans:clans-paper:jar        # Build the Paper plugin
./gradlew :modules:clans:clans-paper:runServer   # Run a test server
```

## Adding a new module

1. Create the directory: `modules/<domain>/<module-name>/`
2. Add a `build.gradle.kts` in the module directory.
3. Include it in `settings.gradle.kts`: `":modules:<domain>:<module-name>"`.
4. Use package names under `de.tiamop23.minecraft.<domain>.<module>`.
5. Add any new dependencies to `gradle/libs.versions.toml`.
````

- [ ] **Step 8: Update IntelliJ project display name**

Replace `.idea/.name` contents with:

```text
minecraft-monorepo
```

- [ ] **Step 9: Verify no active references remain**

Run: `rg "clans-ecosystem|net\.clans|net/clans|main: net\.clans|# Clans Ecosystem" settings.gradle.kts build.gradle.kts README.md .idea modules`

Expected: no output and exit code 1 from `rg`, because all active references have been migrated.

- [ ] **Step 10: Verify Gradle project configuration**

Run: `./gradlew projects`

Expected: command succeeds and reports root project `minecraft-monorepo` with project `:modules:clans:clans-paper`.

- [ ] **Step 11: Verify Kotlin compilation**

Run: `./gradlew :modules:clans:clans-paper:compileKotlin`

Expected: command succeeds.

- [ ] **Step 12: Review changed files**

Run: `git diff -- settings.gradle.kts build.gradle.kts README.md .idea/.name modules/clans/clans-paper/src/main/kotlin modules/clans/clans-paper/src/main/resources/plugin.yml`

Expected: diff contains only the approved rename, namespace migration, source path move, plugin entrypoint update, README update, and IDE display name update.

- [ ] **Step 13: Commit if requested**

Only commit if the user explicitly requests it. Use this command after reviewing `git status` and staging only intended files:

```bash
git add settings.gradle.kts build.gradle.kts README.md .idea/.name modules/clans/clans-paper/src/main/kotlin modules/clans/clans-paper/src/main/resources/plugin.yml docs/superpowers/specs/2026-06-28-monorepo-rename-design.md docs/superpowers/plans/2026-06-28-monorepo-rename.md
git commit -m "refactor: rename minecraft monorepo"
```
