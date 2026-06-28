# Monorepo Rename Design

## Goal

Rename the repository identity from `clans-ecosystem` to `minecraft-monorepo` so the project can contain unrelated Minecraft plugins and mods in addition to the clans plugin.

## Scope

The rename covers repository-level identity and JVM namespace identity:

- Gradle root project name becomes `minecraft-monorepo`.
- User-facing documentation describes a generic Minecraft plugin/mod monorepo.
- Project package and artifact namespace moves from clans-specific `net.clans` to `de.tiamop23.minecraft`.
- Source packages follow `de.tiamop23.minecraft.<domain>.<module>`.
- Existing Gradle module paths remain domain-based, such as `:modules:clans:clans-paper`.

## Current Module Mapping

The current clans Paper module maps as follows:

- Gradle module path: `:modules:clans:clans-paper` remains unchanged.
- Gradle group: `net.clans` becomes `de.tiamop23.minecraft`.
- Kotlin package: `net.clans.paper` becomes `de.tiamop23.minecraft.clans.paper`.
- Source directory: `src/main/kotlin/net/clans/paper` becomes `src/main/kotlin/de/tiamop23/minecraft/clans/paper`.
- Main plugin class reference in `plugin.yml` is updated to the new package.

## Out Of Scope

The rename does not change the clans plugin name, plugin domain folder, module name, or build version. Historical design and plan documents may continue to mention older names as project history unless a separate documentation cleanup is requested.

## Verification

After implementation, run Gradle checks that prove the renamed project still configures and compiles:

- `./gradlew projects`
- `./gradlew :modules:clans:clans-paper:compileKotlin`
