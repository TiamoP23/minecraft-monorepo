# Minecraft Clans Plugin Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a PaperMC Kotlin MVP plugin for shared-network clans, server-local chunk claims, clan homes, guest access, and personal PvP controls backed by PostgreSQL.

**Architecture:** Use a multi-module Gradle project. `clans-core` contains platform-independent domain models and rules; `clans-paper` contains PaperMC adapters, PostgreSQL repositories, commands, caches, and event listeners. Event handlers must use in-memory caches only; database reads/writes run outside the main server thread.

**Tech Stack:** Kotlin/JVM, Gradle Kotlin DSL, Paper API 26.1.2, PostgreSQL JDBC, HikariCP, Flyway, JUnit 5, Testcontainers for PostgreSQL integration tests.

## Global Constraints

- Target server: PaperMC for Minecraft 1.26.2.
- Primary language: Kotlin.
- Runtime Kotlin libraries are provided by the Modrinth Kotlin plugin installed on the server; do not shade Kotlin stdlib into the Clans plugin jar.
- Database: PostgreSQL, not SQLite/YAML for MVP persistence.
- `clans-core` must not depend on Bukkit, Paper, Fabric, or Minecraft server classes.
- Claims are server-local and chunk-based, keyed by `server_id`, `world_uuid`, `chunk_x`, and `chunk_z`.
- Clan membership, clan guest access, and player PvP preference are shared across the network.
- Claim groups/bases are computed from connected chunks; do not persist claim group IDs in MVP.
- Chunk connectivity uses north/east/south/west adjacency only; diagonal chunks are not connected.
- Minimum claim distance uses Chebyshev chunk distance.
- Protection checks in Paper event listeners must not hit PostgreSQL synchronously.
- MVP protects block break, block place, protected container interaction, and explosion block lists.
- MVP does not implement Fabric, remote protected containers, waystones, allies/enemies, duels, per-base guest access, or advanced protection hardening.

---

## File Structure

Create this structure:

```text
settings.gradle.kts
build.gradle.kts
gradle.properties
gradle/libs.versions.toml

clans-core/build.gradle.kts
clans-core/src/main/kotlin/net/clans/core/model/Ids.kt
clans-core/src/main/kotlin/net/clans/core/model/ClanModels.kt
clans-core/src/main/kotlin/net/clans/core/model/ClaimModels.kt
clans-core/src/main/kotlin/net/clans/core/model/HomeModels.kt
clans-core/src/main/kotlin/net/clans/core/model/PvpModels.kt
clans-core/src/main/kotlin/net/clans/core/model/ProtectionModels.kt
clans-core/src/main/kotlin/net/clans/core/claim/ClaimGraph.kt
clans-core/src/main/kotlin/net/clans/core/claim/ClaimLimitCalculator.kt
clans-core/src/main/kotlin/net/clans/core/claim/MinimumDistanceValidator.kt
clans-core/src/main/kotlin/net/clans/core/claim/ClaimDecisionService.kt
clans-core/src/main/kotlin/net/clans/core/home/HomeLimitCalculator.kt
clans-core/src/main/kotlin/net/clans/core/protection/AuthorizationEngine.kt
clans-core/src/main/kotlin/net/clans/core/pvp/PvpDecisionEngine.kt
clans-core/src/main/kotlin/net/clans/core/repository/Repositories.kt
clans-core/src/test/kotlin/net/clans/core/claim/ClaimGraphTest.kt
clans-core/src/test/kotlin/net/clans/core/claim/ClaimLimitCalculatorTest.kt
clans-core/src/test/kotlin/net/clans/core/claim/MinimumDistanceValidatorTest.kt
clans-core/src/test/kotlin/net/clans/core/claim/ClaimDecisionServiceTest.kt
clans-core/src/test/kotlin/net/clans/core/home/HomeLimitCalculatorTest.kt
clans-core/src/test/kotlin/net/clans/core/protection/AuthorizationEngineTest.kt
clans-core/src/test/kotlin/net/clans/core/pvp/PvpDecisionEngineTest.kt

clans-paper/build.gradle.kts
clans-paper/src/main/resources/paper-plugin.yml
clans-paper/src/main/resources/config.yml
clans-paper/src/main/resources/db/migration/V1__create_clans.sql
clans-paper/src/main/resources/db/migration/V2__create_guest_access.sql
clans-paper/src/main/resources/db/migration/V3__create_claims.sql
clans-paper/src/main/resources/db/migration/V4__create_homes.sql
clans-paper/src/main/resources/db/migration/V5__create_pvp_settings.sql
clans-paper/src/main/kotlin/net/clans/paper/ClansPlugin.kt
clans-paper/src/main/kotlin/net/clans/paper/ClansBootstrap.kt
clans-paper/src/main/kotlin/net/clans/paper/ClansLoader.kt
clans-paper/src/main/kotlin/net/clans/paper/config/PluginConfig.kt
clans-paper/src/main/kotlin/net/clans/paper/db/DatabaseManager.kt
clans-paper/src/main/kotlin/net/clans/paper/db/Migrations.kt
clans-paper/src/main/kotlin/net/clans/paper/repository/PostgresRepositories.kt
clans-paper/src/main/kotlin/net/clans/paper/cache/ClanRuntimeCache.kt
clans-paper/src/main/kotlin/net/clans/paper/service/AsyncDatabaseExecutor.kt
clans-paper/src/main/kotlin/net/clans/paper/command/ClanCommands.kt
clans-paper/src/main/kotlin/net/clans/paper/command/ClaimCommands.kt
clans-paper/src/main/kotlin/net/clans/paper/command/HomeCommands.kt
clans-paper/src/main/kotlin/net/clans/paper/command/PvpCommands.kt
clans-paper/src/main/kotlin/net/clans/paper/command/AdminCommands.kt
clans-paper/src/main/kotlin/net/clans/paper/listener/ProtectionListener.kt
clans-paper/src/main/kotlin/net/clans/paper/listener/ExplosionListener.kt
clans-paper/src/main/kotlin/net/clans/paper/listener/PvpListener.kt
clans-paper/src/test/kotlin/net/clans/paper/db/DatabaseMigrationIntegrationTest.kt
clans-paper/src/test/kotlin/net/clans/paper/repository/PostgresRepositoryIntegrationTest.kt
docs/manual-test-plan.md
```

---

### Task 1: Gradle Multi-Module Skeleton

**Files:**
- Create: `settings.gradle.kts`
- Create: `build.gradle.kts`
- Create: `gradle.properties`
- Create: `gradle/libs.versions.toml`
- Create: `clans-core/build.gradle.kts`
- Create: `clans-paper/build.gradle.kts`

**Interfaces:**
- Consumes: none.
- Produces: Gradle modules `:clans-core` and `:clans-paper`; test command `./gradlew test`.

- [ ] **Step 1: Write the initial build files**

Create `settings.gradle.kts`:

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

rootProject.name = "minecraft-clans-plugin"
include(":clans-core", ":clans-paper")
```

Create `gradle.properties`:

```properties
kotlin.code.style=official
kotlin.stdlib.default.dependency=false
org.gradle.jvmargs=-Xmx2g -Dfile.encoding=UTF-8
```

Create `gradle/libs.versions.toml`:

```toml
[versions]
kotlin = "2.1.0"
paper = "26.1.2-R0.1-SNAPSHOT"
junit = "5.11.4"
hikari = "5.1.0"
postgres = "42.7.4"
flyway = "10.18.2"
testcontainers = "1.20.4"

[libraries]
paper-api = { group = "io.papermc.paper", name = "paper-api", version.ref = "paper" }
junit-bom = { group = "org.junit", name = "junit-bom", version.ref = "junit" }
junit-jupiter = { group = "org.junit.jupiter", name = "junit-jupiter" }
hikari = { group = "com.zaxxer", name = "HikariCP", version.ref = "hikari" }
postgres = { group = "org.postgresql", name = "postgresql", version.ref = "postgres" }
flyway-core = { group = "org.flywaydb", name = "flyway-core", version.ref = "flyway" }
flyway-postgres = { group = "org.flywaydb", name = "flyway-database-postgresql", version.ref = "flyway" }
testcontainers-postgres = { group = "org.testcontainers", name = "postgresql", version.ref = "testcontainers" }
testcontainers-junit = { group = "org.testcontainers", name = "junit-jupiter", version.ref = "testcontainers" }

[plugins]
kotlin-jvm = { id = "org.jetbrains.kotlin.jvm", version.ref = "kotlin" }
```

Create root `build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.kotlin.jvm) apply false
}

subprojects {
    group = "net.clans"
    version = "0.1.0-SNAPSHOT"

    plugins.withId("org.jetbrains.kotlin.jvm") {
        extensions.configure<org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension> {
            jvmToolchain(25)
        }
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }
}
```

Create `clans-core/build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(kotlin("test"))
}
```

Create `clans-paper/build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    implementation(project(":clans-core"))
    compileOnly(libs.paper.api)
    implementation(libs.hikari)
    implementation(libs.postgres)
    implementation(libs.flyway.core)
    implementation(libs.flyway.postgres)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.testcontainers.junit)
    testImplementation(libs.testcontainers.postgres)
    testImplementation(kotlin("test"))
}
```

- [ ] **Step 2: Run Gradle tests to verify skeleton**

Run: `./gradlew test`

Expected: build succeeds with no tests or empty test tasks.

- [ ] **Step 3: Commit**

```bash
git add settings.gradle.kts build.gradle.kts gradle.properties gradle/libs.versions.toml clans-core/build.gradle.kts clans-paper/build.gradle.kts
git commit -m "chore: add Gradle multi-module skeleton"
```

---

### Task 2: Core Domain Models And Repository Interfaces

**Files:**
- Create: `clans-core/src/main/kotlin/net/clans/core/model/Ids.kt`
- Create: `clans-core/src/main/kotlin/net/clans/core/model/ClanModels.kt`
- Create: `clans-core/src/main/kotlin/net/clans/core/model/ClaimModels.kt`
- Create: `clans-core/src/main/kotlin/net/clans/core/model/HomeModels.kt`
- Create: `clans-core/src/main/kotlin/net/clans/core/model/PvpModels.kt`
- Create: `clans-core/src/main/kotlin/net/clans/core/model/ProtectionModels.kt`
- Create: `clans-core/src/main/kotlin/net/clans/core/repository/Repositories.kt`

**Interfaces:**
- Consumes: Kotlin/JVM module from Task 1.
- Produces: model types `ClanId`, `HomeId`, `Clan`, `ClanMember`, `ClanGuestAccess`, `Claim`, `ClanHome`, `PvpMode`, `ProtectionConfig`; repository interfaces used by database tasks.

- [ ] **Step 1: Write a compile test for model construction**

Create `clans-core/src/test/kotlin/net/clans/core/model/ModelConstructionTest.kt`:

```kotlin
package net.clans.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import java.time.Instant
import java.util.UUID

class ModelConstructionTest {
    @Test
    fun `constructs clan and claim models`() {
        val clanId = ClanId(UUID.fromString("00000000-0000-0000-0000-000000000001"))
        val worldId = UUID.fromString("00000000-0000-0000-0000-000000000002")
        val playerId = UUID.fromString("00000000-0000-0000-0000-000000000003")
        val now = Instant.parse("2026-06-25T00:00:00Z")

        val clan = Clan(clanId, "Builders", null, false, now)
        val member = ClanMember(clanId, playerId, ClanRole.LEADER, now)
        val claim = Claim(ChunkCoordinate("survival", worldId, 3, -2), clanId, now, playerId)

        assertEquals("Builders", clan.name)
        assertEquals(ClanRole.LEADER, member.role)
        assertEquals(3, claim.chunk.chunkX)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :clans-core:test --tests net.clans.core.model.ModelConstructionTest`

Expected: FAIL with unresolved references for model types.

- [ ] **Step 3: Add model and repository code**

Create `Ids.kt`:

```kotlin
package net.clans.core.model

import java.util.UUID

@JvmInline value class ClanId(val raw: UUID)
@JvmInline value class HomeId(val raw: Long)
```

Create `ClanModels.kt`:

```kotlin
package net.clans.core.model

import java.time.Instant
import java.util.UUID

data class Clan(val id: ClanId, val name: String, val tag: String?, val friendlyFire: Boolean, val createdAt: Instant)
enum class ClanRole { LEADER, MEMBER }
enum class GuestScope { CLAN }
data class ClanMember(val clanId: ClanId, val playerId: UUID, val role: ClanRole, val joinedAt: Instant)
data class ClanGuestAccess(val clanId: ClanId, val playerId: UUID, val scope: GuestScope, val createdAt: Instant)
```

Create `ClaimModels.kt`:

```kotlin
package net.clans.core.model
import java.time.Instant
import java.util.UUID

data class ChunkCoordinate(val serverId: String, val worldUuid: UUID, val chunkX: Int, val chunkZ: Int)
data class Claim(val chunk: ChunkCoordinate, val clanId: ClanId, val claimedAt: Instant, val claimedBy: UUID)
data class ClaimGroup(val clanId: ClanId, val serverId: String, val worldUuid: UUID, val chunks: Set<ChunkCoordinate>)
data class ClaimLimitsConfig(val baseChunksPerClan: Int, val chunksPerMember: Int, val baseGroupsPerClan: Int, val extraGroupsPerMember: Int)
```

Create `HomeModels.kt`:

```kotlin
package net.clans.core.model
import java.time.Instant
import java.util.UUID

data class ServerLocation(val serverId: String, val worldUuid: UUID, val worldName: String, val x: Double, val y: Double, val z: Double, val yaw: Float, val pitch: Float)
data class ClanHome(val id: HomeId, val clanId: ClanId, val name: String, val location: ServerLocation, val createdAt: Instant)
data class HomeLimitsConfig(val baseHomesPerClan: Int, val homesPerMember: Int)
```

Create `PvpModels.kt`:

```kotlin
package net.clans.core.model
import java.time.Instant
import java.util.UUID

enum class PvpMode { OFF, CLAN, ON }
enum class CombatTagMode { ATTACKER_ONLY, BOTH_PLAYERS }
data class PvpSettings(val playerId: UUID, val mode: PvpMode, val combatTaggedUntil: Instant?)
```

Create `ProtectionModels.kt`:

```kotlin
package net.clans.core.model

enum class ProtectionFlag { BLOCK_BREAK, BLOCK_PLACE, CONTAINERS, DOORS, BUTTONS, PRESSURE_PLATES, BEDS }
data class ProtectionConfig(val flags: Map<ProtectionFlag, Boolean>) {
    fun enabled(flag: ProtectionFlag): Boolean = flags[flag] ?: false
}
```

Create `Repositories.kt`:

```kotlin
package net.clans.core.repository

import net.clans.core.model.*
import java.util.UUID

interface ClanRepository {
    fun findById(id: ClanId): Clan?
    fun findByName(name: String): Clan?
    fun createClan(clan: Clan, leader: ClanMember)
    fun deleteClan(id: ClanId)
    fun membersOf(clanId: ClanId): List<ClanMember>
    fun guestsOf(clanId: ClanId): List<ClanGuestAccess>
    fun addMember(member: ClanMember)
    fun removeMember(clanId: ClanId, playerId: UUID)
    fun addGuest(access: ClanGuestAccess)
    fun removeGuest(clanId: ClanId, playerId: UUID)
}

interface ClaimRepository {
    fun findByWorld(serverId: String, worldUuid: UUID): Set<Claim>
    fun findByClan(clanId: ClanId, serverId: String): Set<Claim>
    fun save(claim: Claim)
    fun delete(chunk: ChunkCoordinate)
}

interface HomeRepository {
    fun findByClan(clanId: ClanId, serverId: String): List<ClanHome>
    fun save(home: ClanHome)
    fun delete(clanId: ClanId, serverId: String, name: String)
}

interface PvpRepository {
    fun get(playerId: UUID): PvpSettings?
    fun upsert(settings: PvpSettings)
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :clans-core:test --tests net.clans.core.model.ModelConstructionTest`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add clans-core/src/main/kotlin/net/clans/core/model clans-core/src/main/kotlin/net/clans/core/repository clans-core/src/test/kotlin/net/clans/core/model
git commit -m "feat: add core clan domain models"
```

---

### Task 3: Claim Graph, Limits, And Distance Rules

**Files:**
- Create: `clans-core/src/main/kotlin/net/clans/core/claim/ClaimGraph.kt`
- Create: `clans-core/src/main/kotlin/net/clans/core/claim/ClaimLimitCalculator.kt`
- Create: `clans-core/src/main/kotlin/net/clans/core/claim/MinimumDistanceValidator.kt`
- Create: `clans-core/src/test/kotlin/net/clans/core/claim/ClaimGraphTest.kt`
- Create: `clans-core/src/test/kotlin/net/clans/core/claim/ClaimLimitCalculatorTest.kt`
- Create: `clans-core/src/test/kotlin/net/clans/core/claim/MinimumDistanceValidatorTest.kt`

**Interfaces:**
- Consumes: `ChunkCoordinate`, `ClaimLimitsConfig`.
- Produces: `ClaimGraph.connectedComponents`, `ClaimGraph.componentContaining`, `ClaimLimitCalculator.chunkLimit`, `ClaimLimitCalculator.baseGroupLimit`, `MinimumDistanceValidator.isAllowed`.

- [ ] **Step 1: Write failing tests**

Create tests covering these exact cases:

```kotlin
// ClaimGraphTest.kt assertions:
// single chunk has one component
// cardinally adjacent chunks form one component
// diagonal chunks form two components
// bridge chunk merges two components
// removing bridge chunk splits into two components

// ClaimLimitCalculatorTest.kt assertions:
// default config and 1 member gives 9 chunks and 1 group
// default config and 3 members gives 27 chunks and 3 groups

// MinimumDistanceValidatorTest.kt assertions:
// empty other-clan set is allowed
// Chebyshev distance below min is blocked
// Chebyshev distance equal to min is blocked
// Chebyshev distance greater than min is allowed
```

Use helper values:

```kotlin
private val world = UUID.fromString("00000000-0000-0000-0000-000000000010")
private fun c(x: Int, z: Int) = ChunkCoordinate("survival", world, x, z)
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :clans-core:test --tests 'net.clans.core.claim.*'`

Expected: FAIL with unresolved references for claim rule classes.

- [ ] **Step 3: Implement claim graph and calculators**

Create `ClaimGraph.kt`:

```kotlin
package net.clans.core.claim

import net.clans.core.model.ChunkCoordinate

object ClaimGraph {
    fun connectedComponents(claims: Collection<ChunkCoordinate>): Set<Set<ChunkCoordinate>> {
        val unvisited = claims.toMutableSet()
        val result = mutableSetOf<Set<ChunkCoordinate>>()
        while (unvisited.isNotEmpty()) {
            val start = unvisited.first()
            val component = mutableSetOf<ChunkCoordinate>()
            val queue = ArrayDeque<ChunkCoordinate>()
            queue.add(start)
            unvisited.remove(start)
            while (queue.isNotEmpty()) {
                val current = queue.removeFirst()
                component.add(current)
                neighbors(current).filter { it in unvisited }.forEach {
                    unvisited.remove(it)
                    queue.add(it)
                }
            }
            result.add(component)
        }
        return result
    }

    fun componentContaining(chunk: ChunkCoordinate, claims: Set<ChunkCoordinate>): Set<ChunkCoordinate>? =
        connectedComponents(claims).firstOrNull { chunk in it }

    fun neighbors(chunk: ChunkCoordinate): Set<ChunkCoordinate> = setOf(
        chunk.copy(chunkX = chunk.chunkX + 1),
        chunk.copy(chunkX = chunk.chunkX - 1),
        chunk.copy(chunkZ = chunk.chunkZ + 1),
        chunk.copy(chunkZ = chunk.chunkZ - 1),
    )
}
```

Create `ClaimLimitCalculator.kt`:

```kotlin
package net.clans.core.claim

import net.clans.core.model.ClaimLimitsConfig

object ClaimLimitCalculator {
    fun chunkLimit(config: ClaimLimitsConfig, memberCount: Int): Int = config.baseChunksPerClan + memberCount * config.chunksPerMember
    fun baseGroupLimit(config: ClaimLimitsConfig, memberCount: Int): Int = config.baseGroupsPerClan + memberCount * config.extraGroupsPerMember
}
```

Create `MinimumDistanceValidator.kt`:

```kotlin
package net.clans.core.claim

import net.clans.core.model.ChunkCoordinate
import kotlin.math.abs

object MinimumDistanceValidator {
    fun isAllowed(chunk: ChunkCoordinate, otherClanClaims: Set<ChunkCoordinate>, minDistance: Int): Boolean =
        otherClanClaims.none { distance(chunk, it) <= minDistance }

    fun distance(a: ChunkCoordinate, b: ChunkCoordinate): Int =
        maxOf(abs(a.chunkX - b.chunkX), abs(a.chunkZ - b.chunkZ))
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew :clans-core:test --tests 'net.clans.core.claim.*'`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add clans-core/src/main/kotlin/net/clans/core/claim clans-core/src/test/kotlin/net/clans/core/claim
git commit -m "feat: add core claim graph rules"
```

---

### Task 4: Claim Decision Service

**Files:**
- Create: `clans-core/src/main/kotlin/net/clans/core/claim/ClaimDecisionService.kt`
- Create: `clans-core/src/test/kotlin/net/clans/core/claim/ClaimDecisionServiceTest.kt`

**Interfaces:**
- Consumes: `ClaimGraph`, `ClaimLimitCalculator`, `MinimumDistanceValidator`, `Claim`, `ClaimLimitsConfig`.
- Produces: `ClaimDecisionService.validateClaim` and `ClaimDecisionService.validateUnclaim` result types.

- [ ] **Step 1: Write failing tests**

Test these cases:

```text
claim empty world succeeds and creates a new group
claim too close to another clan fails
claim exceeding chunk limit fails
claim creating too many groups fails
claim bridge between own groups succeeds and reduces group count
unclaim chunk not owned by clan fails
unclaim bridge that would exceed base limit fails
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :clans-core:test --tests net.clans.core.claim.ClaimDecisionServiceTest`

Expected: FAIL with unresolved reference `ClaimDecisionService`.

- [ ] **Step 3: Implement service and result types**

```kotlin
package net.clans.core.claim

import net.clans.core.model.*

class ClaimDecisionService(
    private val limits: ClaimLimitsConfig,
    private val normalMinDistance: Int,
    private val newGroupMinDistance: Int,
) {
    fun validateClaim(clanId: ClanId, memberCount: Int, target: ChunkCoordinate, worldClaims: Set<Claim>): ClaimDecision {
        if (worldClaims.any { it.chunk == target }) return ClaimDecision.Failure.AlreadyClaimed
        val ownChunks = worldClaims.filter { it.clanId == clanId }.map { it.chunk }.toSet()
        val otherChunks = worldClaims.filter { it.clanId != clanId }.map { it.chunk }.toSet()
        val touchingOwnGroups = ClaimGraph.connectedComponents(ownChunks).count { group -> ClaimGraph.neighbors(target).any { it in group } }
        val createsNewGroup = touchingOwnGroups == 0
        val minDistance = if (createsNewGroup) newGroupMinDistance else normalMinDistance
        if (!MinimumDistanceValidator.isAllowed(target, otherChunks, minDistance)) return ClaimDecision.Failure.TooCloseToOtherClan
        if (ownChunks.size + 1 > ClaimLimitCalculator.chunkLimit(limits, memberCount)) return ClaimDecision.Failure.ChunkLimitExceeded
        val groupsAfter = ClaimGraph.connectedComponents(ownChunks + target).size
        if (groupsAfter > ClaimLimitCalculator.baseGroupLimit(limits, memberCount)) return ClaimDecision.Failure.BaseGroupLimitExceeded
        return ClaimDecision.Success(groupsAfter)
    }

    fun validateUnclaim(clanId: ClanId, memberCount: Int, target: ChunkCoordinate, worldClaims: Set<Claim>): UnclaimDecision {
        val targetClaim = worldClaims.firstOrNull { it.chunk == target } ?: return UnclaimDecision.Failure.NotClaimed
        if (targetClaim.clanId != clanId) return UnclaimDecision.Failure.NotOwnedByClan
        val remainingOwnChunks = worldClaims.filter { it.clanId == clanId && it.chunk != target }.map { it.chunk }.toSet()
        val groupsAfter = ClaimGraph.connectedComponents(remainingOwnChunks).size
        if (groupsAfter > ClaimLimitCalculator.baseGroupLimit(limits, memberCount)) return UnclaimDecision.Failure.WouldExceedBaseGroupLimit
        return UnclaimDecision.Success(groupsAfter)
    }
}

sealed interface ClaimDecision {
    data class Success(val groupsAfter: Int) : ClaimDecision
    enum class Failure : ClaimDecision { AlreadyClaimed, TooCloseToOtherClan, ChunkLimitExceeded, BaseGroupLimitExceeded }
}

sealed interface UnclaimDecision {
    data class Success(val groupsAfter: Int) : UnclaimDecision
    enum class Failure : UnclaimDecision { NotClaimed, NotOwnedByClan, WouldExceedBaseGroupLimit }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew :clans-core:test --tests net.clans.core.claim.ClaimDecisionServiceTest`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add clans-core/src/main/kotlin/net/clans/core/claim/ClaimDecisionService.kt clans-core/src/test/kotlin/net/clans/core/claim/ClaimDecisionServiceTest.kt
git commit -m "feat: add claim decision service"
```

---

### Task 5: PvP And Authorization Core

**Files:**
- Create: `clans-core/src/main/kotlin/net/clans/core/pvp/PvpDecisionEngine.kt`
- Create: `clans-core/src/main/kotlin/net/clans/core/protection/AuthorizationEngine.kt`
- Create: `clans-core/src/main/kotlin/net/clans/core/home/HomeLimitCalculator.kt`
- Create: `clans-core/src/test/kotlin/net/clans/core/pvp/PvpDecisionEngineTest.kt`
- Create: `clans-core/src/test/kotlin/net/clans/core/protection/AuthorizationEngineTest.kt`
- Create: `clans-core/src/test/kotlin/net/clans/core/home/HomeLimitCalculatorTest.kt`

**Interfaces:**
- Consumes: model types from Task 2.
- Produces: `PvpDecisionEngine.isPvpAllowed`, `PvpDecisionEngine.isModeTransitionAllowed`, `AuthorizationEngine.isAuthorized`, `HomeLimitCalculator.homeLimit`.

- [ ] **Step 1: Write failing tests**

Cover this PvP matrix:

```text
OFF with any target mode is false
ON vs ON is true
ON vs CLAN is false
CLAN vs CLAN same full clan and friendly-fire on is true
CLAN vs CLAN same full clan and friendly-fire off is false
CLAN vs CLAN different clans is false
ON -> OFF is blocked while combat-tagged
ON -> CLAN is blocked while combat-tagged
CLAN -> ON is allowed while combat-tagged
OFF -> CLAN is allowed while combat-tagged
```

Cover this authorization matrix:

```text
leader can manage invites, claims, homes, friendly fire, and disband
member can build, break, use containers, and use homes in own clan
guest can build, break, and use containers in host clan claims
guest cannot use homes or manage claims
outsider cannot build in claimed chunk
outsider can build in unclaimed chunk
disabled protection flag allows the action
admin bypass allows every action
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :clans-core:test --tests 'net.clans.core.pvp.*' --tests 'net.clans.core.protection.*' --tests 'net.clans.core.home.*'`

Expected: FAIL with unresolved references for engines.

- [ ] **Step 3: Implement engines**

Create `PvpDecisionEngine.kt`:

```kotlin
package net.clans.core.pvp

import net.clans.core.model.ClanId
import net.clans.core.model.PvpMode

object PvpDecisionEngine {
    fun isPvpAllowed(attackerMode: PvpMode, victimMode: PvpMode, attackerClanId: ClanId?, victimClanId: ClanId?, clanFriendlyFire: Boolean): Boolean =
        when {
            attackerMode == PvpMode.OFF || victimMode == PvpMode.OFF -> false
            attackerMode == PvpMode.ON && victimMode == PvpMode.ON -> true
            attackerMode == PvpMode.CLAN && victimMode == PvpMode.CLAN -> attackerClanId != null && attackerClanId == victimClanId && clanFriendlyFire
            else -> false
        }

    fun isModeTransitionAllowed(currentMode: PvpMode, targetMode: PvpMode, combatTagged: Boolean): Boolean {
        if (!combatTagged) return true
        return when (currentMode) {
            PvpMode.OFF -> true
            PvpMode.CLAN -> targetMode == PvpMode.ON || targetMode == PvpMode.CLAN
            PvpMode.ON -> targetMode == PvpMode.ON
        }
    }
}
```

Create `HomeLimitCalculator.kt`:

```kotlin
package net.clans.core.home

import net.clans.core.model.HomeLimitsConfig

object HomeLimitCalculator {
    fun homeLimit(config: HomeLimitsConfig, memberCount: Int): Int = config.baseHomesPerClan + memberCount * config.homesPerMember
}
```

Create `AuthorizationEngine.kt`:

```kotlin
package net.clans.core.protection

import net.clans.core.model.*
import java.util.UUID

sealed interface Principal {
    data class ClanMember(val playerId: UUID, val clanId: ClanId, val role: ClanRole) : Principal
    data class ClanGuest(val playerId: UUID, val clanId: ClanId) : Principal
    data class Outsider(val playerId: UUID) : Principal
    data class AdminBypass(val playerId: UUID) : Principal
}

sealed interface Action {
    data object Build : Action
    data object Break : Action
    data object UseContainer : Action
    data object UseClanHome : Action
    data object ManageInvites : Action
    data object KickMembers : Action
    data object KickGuests : Action
    data object ClaimChunk : Action
    data object UnclaimChunk : Action
    data object SetHome : Action
    data object DeleteHome : Action
    data object ToggleFriendlyFire : Action
    data object Disband : Action
}

data class ActionContext(val claimOwner: ClanId?, val protectionConfig: ProtectionConfig)

object AuthorizationEngine {
    fun isAuthorized(principal: Principal, action: Action, context: ActionContext): Boolean {
        if (principal is Principal.AdminBypass) return true
        if (!isProtected(action, context.protectionConfig)) return true
        return when (principal) {
            is Principal.ClanMember -> authorizeMember(principal, action, context.claimOwner)
            is Principal.ClanGuest -> action in setOf(Action.Build, Action.Break, Action.UseContainer) && principal.clanId == context.claimOwner
            is Principal.Outsider -> context.claimOwner == null && action in setOf(Action.Build, Action.Break)
            is Principal.AdminBypass -> true
        }
    }

    private fun authorizeMember(member: Principal.ClanMember, action: Action, claimOwner: ClanId?): Boolean {
        val ownClaim = claimOwner == null || claimOwner == member.clanId
        if (!ownClaim) return false
        if (action in setOf(Action.Build, Action.Break, Action.UseContainer, Action.UseClanHome)) return true
        return member.role == ClanRole.LEADER
    }

    private fun isProtected(action: Action, config: ProtectionConfig): Boolean = when (action) {
        Action.Build -> config.enabled(ProtectionFlag.BLOCK_PLACE)
        Action.Break -> config.enabled(ProtectionFlag.BLOCK_BREAK)
        Action.UseContainer -> config.enabled(ProtectionFlag.CONTAINERS)
        else -> true
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew :clans-core:test --tests 'net.clans.core.pvp.*' --tests 'net.clans.core.protection.*' --tests 'net.clans.core.home.*'`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add clans-core/src/main/kotlin/net/clans/core/pvp clans-core/src/main/kotlin/net/clans/core/protection clans-core/src/main/kotlin/net/clans/core/home clans-core/src/test/kotlin/net/clans/core/pvp clans-core/src/test/kotlin/net/clans/core/protection clans-core/src/test/kotlin/net/clans/core/home
git commit -m "feat: add pvp and authorization rules"
```

---

### Task 6: Paper Plugin Metadata And Configuration

**Files:**
- Create: `clans-paper/src/main/resources/paper-plugin.yml`
- Create: `clans-paper/src/main/resources/config.yml`
- Create: `clans-paper/src/main/kotlin/net/clans/paper/ClansLoader.kt`
- Create: `clans-paper/src/main/kotlin/net/clans/paper/config/PluginConfig.kt`
- Create: `clans-paper/src/main/kotlin/net/clans/paper/ClansPlugin.kt`

**Interfaces:**
- Consumes: Gradle `:clans-paper` module.
- Produces: loadable Paper plugin metadata and parsed config values.

- [ ] **Step 1: Add metadata and config files**

Create `paper-plugin.yml`:

```yaml
name: Clans
version: '${version}'
main: net.clans.paper.ClansPlugin
api-version: '26.1.2'
description: Minimal customizable clans plugin
loader: net.clans.paper.ClansLoader
bootstrapper: net.clans.paper.ClansBootstrap
authors:
  - server-owner
dependencies:
  server:
    kotlin:
      load: BEFORE
      required: true
```

Create `config.yml`:

```yaml
server-id: survival

database:
  host: localhost
  port: 5432
  database: minecraft
  user: clans
  password: clans
  maximum-pool-size: 4

claims:
  base-chunks-per-clan: 0
  chunks-per-member: 9
  base-groups-per-clan: 0
  extra-groups-per-member: 1
  normal-min-distance-chunks: 1
  new-group-min-distance-chunks: 3
  allowed-worlds: []
  denied-worlds: []

protection:
  block-break: true
  block-place: true
  containers: true
  doors: false
  buttons: false
  pressure-plates: false
  beds: false

homes:
  base-homes-per-clan: 0
  homes-per-member: 1
  teleport-delay-seconds: 3
  teleport-cooldown-seconds: 60
  cancel-on-move: true

pvp:
  default-mode: OFF
  combat-timeout-seconds: 60
  combat-tag-mode: attacker-only
```

- [ ] **Step 2: Add minimal plugin classes**

Create `ClansLoader.kt`:

```kotlin
package net.clans.paper

import io.papermc.paper.plugin.loader.PluginClasspathBuilder
import io.papermc.paper.plugin.loader.PluginLoader

class ClansLoader : PluginLoader {
    override fun classloader(classpathBuilder: PluginClasspathBuilder) {
        // Kotlin runtime is provided by the required Modrinth Kotlin plugin.
        // Later tasks may add HikariCP, PostgreSQL, and Flyway library resolution here.
    }
}
```

Create `ClansBootstrap.kt`:

```kotlin
package net.clans.paper

import io.papermc.paper.plugin.bootstrap.BootstrapContext
import io.papermc.paper.plugin.bootstrap.PluginBootstrap

class ClansBootstrap : PluginBootstrap {
    override fun bootstrap(context: BootstrapContext) {
    }
}
```

Create `PluginConfig.kt`:

```kotlin
package net.clans.paper.config

import org.bukkit.configuration.file.FileConfiguration

data class PluginConfig(val serverId: String)

object PluginConfigLoader {
    fun load(config: FileConfiguration): PluginConfig = PluginConfig(serverId = config.getString("server-id") ?: "survival")
}
```

Create `ClansPlugin.kt`:

```kotlin
package net.clans.paper

import net.clans.paper.config.PluginConfig
import net.clans.paper.config.PluginConfigLoader
import org.bukkit.plugin.java.JavaPlugin

class ClansPlugin : JavaPlugin() {
    lateinit var pluginConfig: PluginConfig
        private set

    override fun onEnable() {
        saveDefaultConfig()
        pluginConfig = PluginConfigLoader.load(config)
        logger.info("Clans enabled for server '${pluginConfig.serverId}'")
    }
}
```

- [ ] **Step 3: Run compile**

Run: `./gradlew :clans-paper:compileKotlin`

Expected: PASS.

- [ ] **Step 4: Commit**

```bash
git add clans-paper/src/main/resources/paper-plugin.yml clans-paper/src/main/resources/config.yml clans-paper/src/main/kotlin/net/clans/paper
git commit -m "feat: add Paper plugin metadata and config"
```

---

### Task 7: PostgreSQL Migrations And Database Manager

**Files:**
- Create: `clans-paper/src/main/resources/db/migration/V1__create_clans.sql`
- Create: `clans-paper/src/main/resources/db/migration/V2__create_guest_access.sql`
- Create: `clans-paper/src/main/resources/db/migration/V3__create_claims.sql`
- Create: `clans-paper/src/main/resources/db/migration/V4__create_homes.sql`
- Create: `clans-paper/src/main/resources/db/migration/V5__create_pvp_settings.sql`
- Create: `clans-paper/src/main/kotlin/net/clans/paper/db/DatabaseManager.kt`
- Create: `clans-paper/src/main/kotlin/net/clans/paper/db/Migrations.kt`
- Create: `clans-paper/src/test/kotlin/net/clans/paper/db/DatabaseMigrationIntegrationTest.kt`

**Interfaces:**
- Consumes: `PluginConfig` database values.
- Produces: HikariCP `DataSource` and Flyway migration runner.

- [ ] **Step 1: Write migration integration test**

Create a Testcontainers PostgreSQL test that starts `postgres:16-alpine`, builds a Hikari datasource, runs `Migrations.run(dataSource)`, and asserts these tables exist: `clans`, `clan_members`, `clan_guest_access`, `clan_claims`, `clan_homes`, `player_pvp_settings`.

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :clans-paper:test --tests net.clans.paper.db.DatabaseMigrationIntegrationTest`

Expected: FAIL because migrations and migration runner do not exist.

- [ ] **Step 3: Add migrations**

Create `V1__create_clans.sql`:

```sql
CREATE TABLE clans (
    id UUID PRIMARY KEY,
    name VARCHAR(32) NOT NULL,
    tag VARCHAR(6),
    friendly_fire BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT clans_name_length CHECK (char_length(name) BETWEEN 1 AND 32),
    CONSTRAINT clans_tag_length CHECK (tag IS NULL OR char_length(tag) BETWEEN 1 AND 6)
);
CREATE UNIQUE INDEX clans_name_lower_unique ON clans (lower(name));
CREATE UNIQUE INDEX clans_tag_unique ON clans (tag) WHERE tag IS NOT NULL;

CREATE TABLE clan_members (
    clan_id UUID NOT NULL REFERENCES clans(id) ON DELETE CASCADE,
    player_uuid UUID NOT NULL,
    role VARCHAR(10) NOT NULL CHECK (role IN ('LEADER', 'MEMBER')),
    joined_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (clan_id, player_uuid),
    UNIQUE (player_uuid)
);
```

Create `V2__create_guest_access.sql`:

```sql
CREATE TABLE clan_guest_access (
    clan_id UUID NOT NULL REFERENCES clans(id) ON DELETE CASCADE,
    player_uuid UUID NOT NULL,
    scope VARCHAR(10) NOT NULL DEFAULT 'CLAN' CHECK (scope IN ('CLAN')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (clan_id, player_uuid)
);
```

Create `V3__create_claims.sql`:

```sql
CREATE TABLE clan_claims (
    id BIGSERIAL PRIMARY KEY,
    clan_id UUID NOT NULL REFERENCES clans(id) ON DELETE CASCADE,
    server_id VARCHAR(64) NOT NULL,
    world_uuid UUID NOT NULL,
    world_name VARCHAR(64) NOT NULL,
    chunk_x INTEGER NOT NULL,
    chunk_z INTEGER NOT NULL,
    claimed_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    claimed_by UUID NOT NULL,
    UNIQUE (server_id, world_uuid, chunk_x, chunk_z)
);
```

Create `V4__create_homes.sql`:

```sql
CREATE TABLE clan_homes (
    id BIGSERIAL PRIMARY KEY,
    clan_id UUID NOT NULL REFERENCES clans(id) ON DELETE CASCADE,
    name VARCHAR(32) NOT NULL,
    server_id VARCHAR(64) NOT NULL,
    world_uuid UUID NOT NULL,
    world_name VARCHAR(64) NOT NULL,
    x DOUBLE PRECISION NOT NULL,
    y DOUBLE PRECISION NOT NULL,
    z DOUBLE PRECISION NOT NULL,
    yaw REAL NOT NULL DEFAULT 0,
    pitch REAL NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (clan_id, server_id, name)
);
```

Create `V5__create_pvp_settings.sql`:

```sql
CREATE TABLE player_pvp_settings (
    player_uuid UUID PRIMARY KEY,
    pvp_mode VARCHAR(6) NOT NULL CHECK (pvp_mode IN ('OFF', 'CLAN', 'ON')),
    combat_tagged_until TIMESTAMPTZ,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

- [ ] **Step 4: Add database manager and migration runner**

`DatabaseManager` must create `HikariDataSource` with `maximumPoolSize`, `minimumIdle = 1`, `connectionTimeout = 5000`, `validationTimeout = 3000`, `isAutoCommit = false`, and `applicationName = "ClansPlugin-$serverId"`.

`Migrations.run(dataSource)` must call Flyway with `classpath:db/migration` and return normally only after successful migration.

- [ ] **Step 5: Run test to verify it passes**

Run: `./gradlew :clans-paper:test --tests net.clans.paper.db.DatabaseMigrationIntegrationTest`

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add clans-paper/src/main/resources/db clans-paper/src/main/kotlin/net/clans/paper/db clans-paper/src/test/kotlin/net/clans/paper/db
git commit -m "feat: add PostgreSQL migrations"
```

---

### Task 8: PostgreSQL Repository Implementations

**Files:**
- Create: `clans-paper/src/main/kotlin/net/clans/paper/repository/PostgresRepositories.kt`
- Create: `clans-paper/src/test/kotlin/net/clans/paper/repository/PostgresRepositoryIntegrationTest.kt`

**Interfaces:**
- Consumes: core repository interfaces and migrated schema.
- Produces: JDBC implementations for clan, claim, home, and PvP repositories.

- [ ] **Step 1: Write integration tests**

Test these exact behaviors:

```text
create clan inserts clan and leader in one transaction
unique player membership prevents joining two clans
delete clan cascades members, guests, claims, and homes
claim unique constraint prevents two clans claiming one chunk
home names are unique per clan and server
PvP settings upsert OFF, CLAN, and ON
guest access allows one player to guest multiple clans
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :clans-paper:test --tests net.clans.paper.repository.PostgresRepositoryIntegrationTest`

Expected: FAIL because repository implementations do not exist.

- [ ] **Step 3: Implement repositories**

Implement one class per interface inside `PostgresRepositories.kt`. Each method must use prepared statements. Multi-row operations must wrap statements in a transaction and call `rollback()` on exceptions. Do not call Bukkit/Paper APIs from repository code.

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew :clans-paper:test --tests net.clans.paper.repository.PostgresRepositoryIntegrationTest`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add clans-paper/src/main/kotlin/net/clans/paper/repository clans-paper/src/test/kotlin/net/clans/paper/repository
git commit -m "feat: add PostgreSQL repositories"
```

---

### Task 9: Runtime Cache And Async Database Executor

**Files:**
- Create: `clans-paper/src/main/kotlin/net/clans/paper/cache/ClanRuntimeCache.kt`
- Create: `clans-paper/src/main/kotlin/net/clans/paper/service/AsyncDatabaseExecutor.kt`

**Interfaces:**
- Consumes: repositories from Task 8.
- Produces: cache methods for claims, clan membership, guests, PvP settings, and admin bypass; async DB wrapper for command code.

- [ ] **Step 1: Add cache tests or compile-time service tests**

Test that writing a claim to the cache makes `claimAt(chunk)` return that claim, clearing the cache removes it, and membership lookup distinguishes member, guest, and outsider.

- [ ] **Step 2: Implement cache**

Use `ConcurrentHashMap<ChunkCoordinate, Claim>` for claims, `ConcurrentHashMap<UUID, ClanMember>` for player membership, `ConcurrentHashMap<Pair<ClanId, UUID>, ClanGuestAccess>` for guests, `ConcurrentHashMap<UUID, PvpSettings>` for PvP, and `MutableSet<UUID>` guarded for admin bypass.

- [ ] **Step 3: Implement async executor**

Use Paper `AsyncScheduler` for database work and `GlobalRegionScheduler` for returning to main-thread callbacks. The executor must expose `runDatabaseTask(description: String, work: () -> T, onSuccess: (T) -> Unit, onFailure: (Throwable) -> Unit)`.

- [ ] **Step 4: Run compile/tests**

Run: `./gradlew :clans-paper:test`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add clans-paper/src/main/kotlin/net/clans/paper/cache clans-paper/src/main/kotlin/net/clans/paper/service
git commit -m "feat: add runtime cache and async db executor"
```

---

### Task 10: Clan And Guest Commands

**Files:**
- Create: `clans-paper/src/main/kotlin/net/clans/paper/command/ClanCommands.kt`
- Modify: `clans-paper/src/main/kotlin/net/clans/paper/ClansBootstrap.kt`

**Interfaces:**
- Consumes: repositories, cache, async executor.
- Produces: `/clan create|disband|invite|join|leave|kick|guest add|guest remove|guest list` and aliases `/c`, `/clans`.

- [ ] **Step 1: Register command tree**

Use Paper lifecycle command registration in `ClansBootstrap`. Commands must reject console senders for player-only actions and send clear error messages.

- [ ] **Step 2: Implement clan commands**

Implement create/disband/invite/join/leave/kick using async database writes and local cache updates after success. Leader leave must fail if the player is the last leader. Invites must be stored in memory for MVP with a 10-minute expiry and cleared on join/disband.

- [ ] **Step 3: Implement guest commands**

`/clan guest add <player>` grants clan-wide guest access with a warning message that guests can build, break, and access containers in all clan claims. `/clan guest remove <player>` revokes it. `/clan guest list` lists UUID/name values known to the server.

- [ ] **Step 4: Run compile**

Run: `./gradlew :clans-paper:compileKotlin`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add clans-paper/src/main/kotlin/net/clans/paper/command/ClanCommands.kt clans-paper/src/main/kotlin/net/clans/paper/ClansBootstrap.kt
git commit -m "feat: add clan and guest commands"
```

---

### Task 11: Claim Commands And Protection Listeners

**Files:**
- Create: `clans-paper/src/main/kotlin/net/clans/paper/command/ClaimCommands.kt`
- Create: `clans-paper/src/main/kotlin/net/clans/paper/listener/ProtectionListener.kt`
- Create: `clans-paper/src/main/kotlin/net/clans/paper/listener/ExplosionListener.kt`
- Modify: `clans-paper/src/main/kotlin/net/clans/paper/ClansPlugin.kt`
- Modify: `clans-paper/src/main/kotlin/net/clans/paper/ClansBootstrap.kt`

**Interfaces:**
- Consumes: `ClaimDecisionService`, cache, claim repository, authorization engine.
- Produces: `/clan claim|unclaim|claims|inspect`, block break/place/container protection, explosion filtering.

- [ ] **Step 1: Implement claim commands**

`/clan claim` and `/clan unclaim` must validate membership and role, check world allow/deny config, call core decision service using cached world claims, write changes async, then update cache on success. Failure messages must include the specific reason.

- [ ] **Step 2: Implement protection listener**

Handle `BlockBreakEvent`, `BlockPlaceEvent`, and `PlayerInteractEvent`. For container interaction, check supported container block types and cancel if the actor is not a member or guest of the owning clan. Do not perform database reads inside event handlers.

- [ ] **Step 3: Implement explosion listener**

For `EntityExplodeEvent` and `BlockExplodeEvent`, remove claimed blocks from the explosion block list instead of cancelling the entire explosion.

- [ ] **Step 4: Run compile/tests**

Run: `./gradlew test`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add clans-paper/src/main/kotlin/net/clans/paper/command/ClaimCommands.kt clans-paper/src/main/kotlin/net/clans/paper/listener/ProtectionListener.kt clans-paper/src/main/kotlin/net/clans/paper/listener/ExplosionListener.kt clans-paper/src/main/kotlin/net/clans/paper/ClansPlugin.kt clans-paper/src/main/kotlin/net/clans/paper/ClansBootstrap.kt
git commit -m "feat: add claim commands and protection listeners"
```

---

### Task 12: Clan Homes

**Files:**
- Create: `clans-paper/src/main/kotlin/net/clans/paper/command/HomeCommands.kt`
- Modify: `clans-paper/src/main/kotlin/net/clans/paper/ClansBootstrap.kt`

**Interfaces:**
- Consumes: `HomeRepository`, `HomeLimitCalculator`, cache, config.
- Produces: `/clan sethome|home|delhome|homes` and `/c` aliases.

- [ ] **Step 1: Implement home commands**

Leaders can set/delete homes. Members can use homes. Guests cannot use homes. Home count must respect `base-homes-per-clan + member_count * homes-per-member`.

- [ ] **Step 2: Implement teleport delay and cooldown**

Use Paper scheduling. Store pending teleport state by player UUID. If `cancel-on-move` is true, cancel when the player changes block coordinates before the delay ends. After successful teleport, apply cooldown.

- [ ] **Step 3: Run compile/tests**

Run: `./gradlew test`

Expected: PASS.

- [ ] **Step 4: Commit**

```bash
git add clans-paper/src/main/kotlin/net/clans/paper/command/HomeCommands.kt clans-paper/src/main/kotlin/net/clans/paper/ClansBootstrap.kt
git commit -m "feat: add clan homes"
```

---

### Task 13: PvP Commands And Damage Listener

**Files:**
- Create: `clans-paper/src/main/kotlin/net/clans/paper/command/PvpCommands.kt`
- Create: `clans-paper/src/main/kotlin/net/clans/paper/listener/PvpListener.kt`
- Modify: `clans-paper/src/main/kotlin/net/clans/paper/ClansPlugin.kt`
- Modify: `clans-paper/src/main/kotlin/net/clans/paper/ClansBootstrap.kt`

**Interfaces:**
- Consumes: `PvpDecisionEngine`, `PvpRepository`, cache, clan membership cache.
- Produces: `/pvp`, `/pvp on`, `/pvp off`, `/pvp clan`, `/pvp status`, damage cancellation and combat tagging.

- [ ] **Step 1: Implement PvP commands**

`/pvp` toggles OFF and ON. Explicit arguments set the requested mode. Restrictive transitions while combat-tagged must be rejected using `PvpDecisionEngine.isModeTransitionAllowed`.

- [ ] **Step 2: Implement damage listener**

Handle `EntityDamageByEntityEvent`. Resolve players from direct attacks and projectiles with player shooters. If `PvpDecisionEngine.isPvpAllowed` returns false, cancel the event. If damage is allowed, combat-tag the attacker only or both players according to config.

- [ ] **Step 3: Run compile/tests**

Run: `./gradlew test`

Expected: PASS.

- [ ] **Step 4: Commit**

```bash
git add clans-paper/src/main/kotlin/net/clans/paper/command/PvpCommands.kt clans-paper/src/main/kotlin/net/clans/paper/listener/PvpListener.kt clans-paper/src/main/kotlin/net/clans/paper/ClansPlugin.kt clans-paper/src/main/kotlin/net/clans/paper/ClansBootstrap.kt
git commit -m "feat: add pvp modes and combat tagging"
```

---

### Task 14: Admin Commands, Reload, And Manual Test Plan

**Files:**
- Create: `clans-paper/src/main/kotlin/net/clans/paper/command/AdminCommands.kt`
- Modify: `clans-paper/src/main/kotlin/net/clans/paper/ClansBootstrap.kt`
- Create: `docs/manual-test-plan.md`

**Interfaces:**
- Consumes: runtime cache, repositories, config loader.
- Produces: `/clanadmin reload|bypass|inspect|forceunclaim|deleteclan|setrole` and manual verification steps.

- [ ] **Step 1: Implement admin commands**

`/clanadmin bypass` toggles bypass for the sender. `inspect` reports current chunk claim owner, actor membership, guest status, and whether build/container actions would be allowed. `reload` reloads config and refreshes caches from PostgreSQL. `forceunclaim`, `deleteclan`, and `setrole` perform async database writes and cache updates.

- [ ] **Step 2: Write manual test plan**

Create `docs/manual-test-plan.md` with these sections:

```text
1. Start local Paper server with plugin installed.
2. Create two clans with two players.
3. Claim adjacent and separated chunks; verify group limits.
4. Attempt claim too close to another clan; verify failure.
5. Verify member, guest, and outsider block/container access.
6. Verify explosion filtering leaves claimed blocks intact.
7. Set and use clan home; verify guest denial.
8. Exercise /pvp OFF, CLAN, ON between same clan and different clans.
9. Verify attacker-only combat tag blocks ON -> OFF for attacker.
10. Verify /clanadmin bypass and inspect output.
```

- [ ] **Step 3: Run full verification**

Run: `./gradlew test`

Expected: PASS.

Run: `./gradlew :clans-paper:jar`

Expected: PASS and produces a plugin jar under `clans-paper/build/libs/`.

- [ ] **Step 4: Commit**

```bash
git add clans-paper/src/main/kotlin/net/clans/paper/command/AdminCommands.kt clans-paper/src/main/kotlin/net/clans/paper/ClansBootstrap.kt docs/manual-test-plan.md
git commit -m "feat: add admin commands and manual test plan"
```

---

## Self-Review Notes

- Spec coverage: architecture, clan roles, guest access, claims, connected groups, distance rules, protection, PostgreSQL, homes, PvP, admin commands, and testing are represented in tasks.
- Deferred scope is explicitly excluded from implementation tasks: Fabric, remote containers, waystones, allies/enemies, duels, per-base guests, and advanced protection hardening.
- PostgreSQL constraints and transaction boundaries are included before repositories and command work.
- Claim groups are computed from cached claims, not persisted.
- Event listeners are required to use cache-only checks and avoid synchronous database work.
- The plan uses Chebyshev distance and specifies explosion filtering by removing claimed blocks from explosion lists.
