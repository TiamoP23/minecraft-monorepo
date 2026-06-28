# Autonomous Agent Testing Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Set up Paperwright for persistent E2E tests and MCC MCP for ad-hoc interactive testing of Paper plugins.

**Architecture:** Paperwright Gradle plugin manages the full test lifecycle (Paper download, server start, Mineflayer bot tests, shutdown) via `./gradlew paperwrightTest`. MCC provides an MCP endpoint for ad-hoc agent-driven exploration via IntelliJ run configurations.

**Tech Stack:** Paperwright 1.3.3, Minecraft Console Client, TypeScript, Node.js, .NET (for MCC), IntelliJ MCP

## Global Constraints

- Minecraft version: `26.2` (from `libs.versions.minecraft`)
- JVM target: 25
- Paperwright version: `1.3.3`
- MCC: download latest release binary (C#/.NET)
- EULA: do NOT auto-accept; prompt user to accept manually
- All test files in `modules/clans/clans-paper/src/test/e2e/`

---

### Task 1: Add Paperwright Gradle Plugin

**Files:**
- Modify: `modules/clans/clans-paper/build.gradle.kts`

**Interfaces:**
- Consumes: existing `libs.versions.minecraft` for version
- Produces: `paperwrightTest` and `paperwrightInit` Gradle tasks

- [ ] **Step 1: Add Paperwright plugin to build.gradle.kts**

Add the Paperwright plugin and configuration block to `modules/clans/clans-paper/build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.shadow)
    alias(libs.plugins.run.paper)
    alias(libs.plugins.paperwright)
}
```

Add the Paperwright version to `gradle/libs.versions.toml`:

```toml
[versions]
paperwright = "1.3.3"

[plugins]
paperwright = { id = "io.github.drownek.paperwright", version.ref = "paperwright" }
```

Add the Paperwright configuration block in the `tasks` section of `build.gradle.kts`:

```kotlin
paperwright {
    minecraftVersion.set(libs.versions.minecraft.get())
    testsDir.set(file("src/test/e2e"))
}
```

Note: `acceptEula` is intentionally NOT set. The user must accept the EULA manually.

- [ ] **Step 2: Verify the plugin resolves**

Run: `./gradlew :modules:clans:clans-paper:tasks --all 2>&1 | grep paperwright`
Expected: `paperwrightInit` and `paperwrightTest` tasks appear in the output.

- [ ] **Step 3: Commit**

```bash
git add gradle/libs.versions.toml modules/clans/clans-paper/build.gradle.kts
git commit -m "build: add Paperwright Gradle plugin for E2E testing"
```

---

### Task 2: Initialize Paperwright Test Structure

**Files:**
- Create: `modules/clans/clans-paper/src/test/e2e/package.json`
- Create: `modules/clans/clans-paper/src/test/e2e/tsconfig.json`
- Create: `modules/clans/clans-paper/src/test/e2e/.gitignore`

**Interfaces:**
- Consumes: Paperwright plugin from Task 1
- Produces: TypeScript test scaffolding for writing E2E tests

- [ ] **Step 1: Initialize the test folder**

Run: `./gradlew :modules:clans:clans-paper:paperwrightInit`

This creates the `src/test/e2e/` directory with `package.json`, `tsconfig.json`, and an example test.

- [ ] **Step 2: Verify the generated files**

Run: `ls modules/clans/clans-paper/src/test/e2e/`
Expected: `package.json`, `tsconfig.json`, and at least one `.spec.ts` file.

- [ ] **Step 3: Add `.gitignore` for node_modules**

Create `modules/clans/clans-paper/src/test/e2e/.gitignore`:

```
node_modules/
dist/
```

- [ ] **Step 4: Commit**

```bash
git add modules/clans/clans-paper/src/test/e2e/
git commit -m "test: initialize Paperwright E2E test structure"
```

---

### Task 3: Write First Paperwright E2E Test

**Files:**
- Create: `modules/clans/clans-paper/src/test/e2e/clan-command.spec.ts`

**Interfaces:**
- Consumes: Paperwright test framework from Task 2, clans plugin commands
- Produces: A passing E2E test that validates basic plugin command behavior

- [ ] **Step 1: Write the test file**

Create `modules/clans/clans-paper/src/test/e2e/clan-command.spec.ts`:

```typescript
import { expect, test } from '@drownek/paperwright';

test('clan help command displays usage', async ({ player }) => {
  player.chat('/clan help');
  await expect(player).toHaveReceivedMessage('clan');
});
```

- [ ] **Step 2: Run the test**

Run: `./gradlew :modules:clans:clans-paper:paperwrightTest`

Expected: Paper downloads, server starts, plugin loads, test runs. The test may need adjustment based on actual plugin output — update the expected message to match real output.

- [ ] **Step 3: Adjust test if needed based on actual output**

Read the test output to see what the plugin actually responds with. Update the `toHaveReceivedMessage` matcher to match the real response.

- [ ] **Step 4: Commit**

```bash
git add modules/clans/clans-paper/src/test/e2e/clan-command.spec.ts
git commit -m "test: add first Paperwright E2E test for clan command"
```

---

### Task 4: Configure MCC Test Config File

**Files:**
- Create: `tools/mcc/mcc-test.toml`

**Interfaces:**
- Consumes: MCC binary (downloaded separately)
- Produces: MCC configuration for MCP-enabled test sessions

- [ ] **Step 1: Create MCC config directory and file**

Create `tools/mcc/mcc-test.toml`:

```toml
[Main]
ServerIP = "127.0.0.1:25565"
Login = "TestBot"

[ChatBot.McpServer]
Enabled = true
Transport = { BindHost = "127.0.0.1", Port = 33333, Route = "/mcp", RequireAuthToken = true, AuthTokenEnvVar = "MCC_MCP_AUTH_TOKEN" }
Capabilities = { SessionStatus = true, ChatAndCommands = true, Movement = true, Inventory = true, EntityWorld = true }

[Advanced]
InventoryHandling = true
EntityHandling = true
TerrainAndMovements = true
```

- [ ] **Step 2: Commit**

```bash
git add tools/mcc/mcc-test.toml
git commit -m "chore: add MCC test configuration for ad-hoc MCP testing"
```

---

### Task 5: Create IntelliJ Run Configurations

**Files:**
- Create: `.run/Paper Test Server.run.xml`
- Create: `.run/MCC Test Client.run.xml`

**Interfaces:**
- Consumes: Paper server (via run-paper), MCC binary, MCC config from Task 4
- Produces: IntelliJ run configurations launchable via IntelliJ MCP

- [ ] **Step 1: Create Paper Test Server run configuration**

Create `.run/Paper Test Server.run.xml`:

```xml
<component name="ProjectRunConfigurationManager">
  <configuration name="Paper Test Server" type="GradleRunConfiguration" factoryName="Gradle">
    <ExternalSystemSettings>
      <option name="executionName" />
      <option name="externalProjectPath" value="$PROJECT_DIR$/modules/clans/clans-paper" />
      <option name="externalSystemIdString" value="GRADLE" />
      <option name="scriptParameters" value="" />
      <option name="taskDescriptions">
        <list />
      </option>
      <option name="taskNames">
        <list>
          <option value="runServer" />
        </list>
      </option>
      <option name="vmOptions" />
    </ExternalSystemSettings>
    <GradleScriptDebugEnabled>true</GradleScriptDebugEnabled>
    <method v="2" />
  </configuration>
</component>
```

- [ ] **Step 2: Create MCC Test Client run configuration**

Create `.run/MCC Test Client.run.xml`:

```xml
<component name="ProjectRunConfigurationManager">
  <configuration name="MCC Test Client" type="Application" factoryName="Application">
    <option name="PROGRAM_PARAMETERS" value="--config $PROJECT_DIR$/tools/mcc/mcc-test.toml" />
    <option name="WORKING_DIRECTORY" value="$PROJECT_DIR$/tools/mcc" />
    <envs>
      <env name="MCC_MCP_AUTH_TOKEN" value="test-token" />
    </envs>
    <method v="2" />
  </configuration>
</component>
```

Note: The `PROGRAM_PARAMETERS` path to the MCC binary must be adjusted to where the binary is actually located. The agent should download MCC to `tools/mcc/` and update the configuration to point to the correct executable path.

- [ ] **Step 3: Verify run configurations appear in IntelliJ**

Open IntelliJ → Run → Edit Configurations. Both "Paper Test Server" and "MCC Test Client" should appear.

- [ ] **Step 4: Commit**

```bash
git add .run/
git commit -m "chore: add IntelliJ run configurations for ad-hoc testing"
```

---

### Task 6: Update OpenCode MCP Config for MCC

**Files:**
- Modify: `opencode.json`

**Interfaces:**
- Consumes: MCC MCP endpoint from Task 5
- Produces: Agent can connect to MCC via MCP tools

- [ ] **Step 1: Add MCC MCP server to opencode.json**

Update `opencode.json`:

```json
{
  "$schema": "https://opencode.ai/config.json",
  "mcp": {
    "intellij": {
      "type": "remote",
      "url": "http://127.0.0.1:64342/stream",
      "enabled": true
    },
    "mcc": {
      "type": "remote",
      "url": "http://127.0.0.1:33333/mcp",
      "enabled": true,
      "headers": {
        "Authorization": "Bearer {env:MCC_MCP_AUTH_TOKEN}"
      }
    }
  }
}
```

- [ ] **Step 2: Commit**

```bash
git add opencode.json
git commit -m "chore: add MCC MCP server to OpenCode config"
```

---

### Task 7: Add .gitignore Entries

**Files:**
- Modify: `.gitignore`

**Interfaces:**
- Consumes: existing `.gitignore`
- Produces: MCC binary and Paperwright artifacts excluded from git

- [ ] **Step 1: Add entries to .gitignore**

Append to `.gitignore`:

```
# MCC binary
tools/mcc/MinecraftClient*
!tools/mcc/mcc-test.toml

# Paperwright
**/src/test/e2e/node_modules/
**/src/test/e2e/dist/
```

- [ ] **Step 2: Commit**

```bash
git add .gitignore
git commit -m "chore: add MCC and Paperwright entries to .gitignore"
```
