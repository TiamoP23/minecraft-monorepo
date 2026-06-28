# Autonomous Agent Testing with Paperwright & MCC MCP

## Goal

Enable AI agents to autonomously write, run, and debug end-to-end tests for Paper plugins, plus perform ad-hoc interactive testing via MCP tools.

## Architecture

The system has 3 components:

1. **Paperwright** — Gradle plugin that manages the entire test lifecycle:
   - Downloads Paper server automatically
   - Starts server with plugin jar
   - Runs TypeScript/JavaScript tests using Mineflayer bots
   - Provides Playwright-inspired API (locators, assertions, retries)
   - Single command: `./gradlew paperwrightTest`

2. **MCC + MCP Server** — For ad-hoc interactive testing:
   - Headless Minecraft client with MCP endpoint
   - Agent connects via MCP tools to explore, debug, investigate
   - No automated test execution — agent drives manually

3. **Agent workflow**:
   - **Persistent tests**: Write TypeScript tests in `src/test/e2e/`, run via Gradle
   - **Ad-hoc exploration**: Start MCC manually, use MCP tools to investigate
   - **Debugging**: When tests fail, use MCC MCP to explore state in real-time

**Data flow**:
- **Persistent**: Agent → TypeScript test → Paperwright → Paper server → plugin → Mineflayer bot → assertions
- **Ad-hoc**: Agent → MCP tools → MCC → Paper server → plugin → response → MCP tools → Agent

## Environment Setup & Configuration

### Paperwright Setup

Add the plugin to `modules/clans/clans-paper/build.gradle.kts`:

```kotlin
plugins {
    id("io.github.drownek.paperwright") version "1.3.3"
}

paperwright {
    minecraftVersion.set(libs.versions.minecraft.get())
    testsDir.set(file("src/test/e2e"))
    acceptEula.set(true)
}
```

Initialize test structure:
```bash
./gradlew paperwrightInit
```

Run tests:
```bash
./gradlew paperwrightTest
```

Paperwright handles everything: downloads Paper, starts server, deploys plugin, runs tests, shuts down.

### IntelliJ Run Configurations

Two run configurations for ad-hoc testing:

1. **`Paper Test Server`** — extends the existing `runServer` task:
   - `run-paper` plugin handles Paper download, plugin jar deployment
   - `serverProperties` → `online-mode=false`, `enable-rcon=true`, `rcon.password=test`
   - User accepts EULA manually on first run if not already accepted

2. **`MCC Test Client`** — Application run configuration:
   - Launches downloaded MCC binary
   - Points to test config with MCP enabled (`127.0.0.1:33333/mcp`)
   - Auth token passed via `MCC_MCP_AUTH_TOKEN` env var

### Agent Orchestration via IntelliJ MCP

For ad-hoc testing, the agent uses IntelliJ MCP tools:

```
1. execute_run_configuration("Paper Test Server", waitForExit=false)
2. Poll Paper output until "Done" appears
3. execute_run_configuration("MCC Test Client", waitForExit=false)
4. Call mcc_session_status until connected
5. Use MCC MCP tools to explore/debug
6. Stop both via IntelliJ MCP
```

### OpenCode MCP Config

Register MCC endpoint in `opencode.json`:
```json
{
  "mcp": {
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

## Test Execution Patterns

### Persistent Tests (Paperwright)

Tests live in `modules/clans/clans-paper/src/test/e2e/` as TypeScript files.

**Command test**:
```typescript
import { expect, test } from '@drownek/paperwright';

test('create clan', async ({ player }) => {
  player.chat('/clan create TestClan');
  await expect(player).toHaveReceivedMessage("Clan 'TestClan' created");
});
```

**Multi-bot test**:
```typescript
test('invite and join clan', async ({ player, createPlayer }) => {
  await player.makeOp();
  const friend = await createPlayer({ username: 'FriendBot' });

  player.chat('/clan create TestClan');
  await expect(player).toHaveReceivedMessage("Clan 'TestClan' created");

  player.chat('/clan invite FriendBot');
  friend.chat('/clan accept TestClan');
  await expect(friend).toHaveReceivedMessage('joined the clan');
});
```

**GUI/inventory test**:
```typescript
test('clan GUI shows members', async ({ player }) => {
  player.chat('/clan gui');
  const gui = await player.gui({ title: 'Clan Members' });
  await expect(gui.locator(item => item.getDisplayName().includes('Player'))).toBeVisible();
});
```

**Item test**:
```typescript
test('clan item reward', async ({ player }) => {
  player.chat('/clan reward');
  await expect(player).toContainItem('diamond', { count: 5 });
});
```

Run: `./gradlew paperwrightTest`

### Ad-hoc Testing (MCC MCP)

When exploring or debugging, agent uses MCP tools directly:

```
mcc_send_chat("/clan create TestClan")
mcc_chat_history() → check response
mcc_inventory_snapshot() → check inventory state
mcc_entities_list() → check entities
mcc_player_stats() → check player state
```

### Debugging Loop

1. Paperwright test fails → agent reads test output
2. Agent starts Paper + MCC via IntelliJ MCP
3. Agent uses MCP tools to explore real-time state
4. Agent adjusts plugin code
5. Agent rebuilds: `./gradlew :modules:clans:clans-paper:build`
6. Agent re-runs: `./gradlew paperwrightTest`

## Error Handling & Edge Cases

### Startup Failures

**Paper fails to start**:
- Agent reads IntelliJ MCP output for error messages
- Common issues: port already in use, EULA not accepted, missing dependencies
- Agent reports to user if manual intervention needed (e.g., "Please accept EULA in run/eula.txt")

**MCC fails to connect**:
- Agent checks `mcc_session_status` for connection state
- Common issues: Paper not ready yet, wrong port, offline mode mismatch
- Agent waits and retries with exponential backoff (Paper startup can take 10-30 seconds)

### Test Timeouts

**Async operations** (e.g., waiting for a chat response):
- Paperwright has built-in retry logic with configurable timeouts
- For ad-hoc MCP testing, agent polls with a timeout (default 5 seconds)
- If timeout exceeded, agent captures logs and reports failure

**Movement/pathfinding** (e.g., `mcc_move_to`):
- Tool returns success/failure
- On failure, agent checks `mcc_player_stats()` for current location and terrain obstacles

### Resource Cleanup

**Between test runs**:
- Paperwright handles cleanup automatically (stops server after tests)
- For ad-hoc testing, agent stops MCC first (graceful disconnect), then Paper via IntelliJ MCP
- If agent crashes, user manually kills processes via IntelliJ run configuration stop button

**Server state reset**:
- Paperwright creates fresh server state per test run
- For ad-hoc testing, agent can delete `run/world/` between runs (Paper regenerates a fresh world)

### Security Considerations

**MCC MCP auth token**:
- Stored in environment variable `MCC_MCP_AUTH_TOKEN`
- Never committed to git
- Agent reads from `{env:MCC_MCP_AUTH_TOKEN}` in `opencode.json`

**Paper RCON password**:
- Set to `test` in `server.properties` (not production-safe, but fine for local testing)
- Agent can use RCON via Paper console for server-side commands if needed

## Technology Stack

- **Paperwright**: End-to-end testing framework for Paper plugins
- **Mineflayer**: Minecraft bot library (used by Paperwright)
- **MCC (Minecraft Console Client)**: Headless Minecraft client with MCP server
- **IntelliJ MCP**: Agent orchestration of run configurations
- **OpenCode MCP**: Agent connection to MCC MCP endpoint
- **TypeScript/JavaScript**: Test language for Paperwright
