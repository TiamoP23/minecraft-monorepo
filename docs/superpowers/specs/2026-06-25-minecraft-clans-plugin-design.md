# Minecraft Clans Plugin Design

Date: 2026-06-25

## Goal

Build a minimal, customizable clan plugin for a PaperMC friends server on Minecraft 1.26.2, written in Kotlin. The plugin should provide clan membership, protected chunk claims, clan homes, guest access, and personal PvP controls. It should use PostgreSQL because the server network already has shared infrastructure, and it should keep a realistic path open for a future Fabric adapter without making Fabric part of the MVP.

The design prioritizes predictable friends-server behavior over competitive factions mechanics. Features such as duels, remote container protection, waystones, allies, and advanced anti-abuse protections are planned as later extensions.

## Architecture

Use a Paper-first Kotlin plugin with clear internal boundaries.

Modules:

```text
clans-core
- Domain models for clans, members, guests, claims, homes, and PvP
- Claim group/base calculation
- Claim and home limit calculation
- PvP decision logic
- Protection rule checks independent of Paper APIs

clans-paper
- Paper event listeners
- Paper commands
- PostgreSQL repository implementation
- Configuration loading
- Scheduler/cache integration
- Optional LuckPerms permission integration
```

The core module must not depend on Bukkit, Paper, Fabric, or Minecraft server classes. The Paper adapter converts platform objects into simple values such as player UUIDs, server IDs, world IDs, chunk coordinates, and block locations.

This keeps the MVP small while preserving a future Fabric path. A Fabric version would need a new adapter and command/event integration, not a rewrite of the core rules.

## Clan Model

Clan data is shared across the network.

Entities:

```text
Clan
- id
- name
- optional tag
- created_at

ClanMember
- clan_id
- player_uuid
- role: LEADER or MEMBER
- joined_at

ClanGuestAccess
- clan_id
- player_uuid
- scope: CLAN for MVP
- created_at
```

Roles:

```text
LEADER
- manage invites
- kick members and guests
- claim and unclaim chunks
- manage clan homes
- toggle clan friendly fire
- rename or disband the clan if enabled

MEMBER
- build and break in clan claims
- use protected containers in clan claims
- use clan homes
- count toward claim and home limits
- count as same-clan for /pvp clan

GUEST
- build and break in clan claims
- use protected containers in clan claims
- does not count toward claim or home limits
- cannot use clan homes
- cannot manage claims
- does not count as same-clan for /pvp clan
```

A clan may have one or more LEADER members. All leaders are equal in MVP; there is no separate owner/admin distinction. The plugin should prevent removing or demoting the last leader unless the clan is being disbanded.

A player can be a full member of at most one clan and can be a guest of multiple clans. MVP guest access is clan-wide. Later, guest access can be scoped to a specific claim group/base.

## Claims And Bases

Claims are server-local and chunk-based.

Claim identity:

```text
server_id
world_uuid
world_name
chunk_x
chunk_z
owner_clan_id
```

A base is not manually created. It is an automatically detected connected group of chunks owned by the same clan.

Connection rules:

```text
Chunks connect by north/east/south/west adjacency.
Diagonal contact does not connect groups.
```

Claim behavior:

```text
If the new chunk touches one existing claim group from the same clan:
- it joins that group.

If it touches no claim group from the same clan:
- it creates a new base/group.

If it touches multiple claim groups from the same clan:
- those groups merge.
- used base count decreases accordingly.
```

Unclaim behavior:

```text
After unclaiming, recalculate affected connected groups.
If the unclaim would split one group into multiple groups and exceed the clan's base limit:
- block the unclaim.
```

The later design may allow temporary over-limit split groups, but the MVP blocks them for consistency.

Limits:

```text
claim_chunks_limit = base_chunks_per_clan + member_count * chunks_per_member
base_group_limit = base_groups_per_clan + member_count * extra_groups_per_member
```

Preferred starting config:

```yaml
claims:
  base-chunks-per-clan: 0
  chunks-per-member: 9
  base-groups-per-clan: 0
  extra-groups-per-member: 1
```

Examples:

```text
1-player clan: 9 claimed chunks, 1 connected group/base.
3-player clan: 27 claimed chunks, 3 connected groups/bases.
```

Distance rules:

```text
Normal claim minimum distance from other clans' claims: N chunks.
New base/group minimum distance from other clans' claims: M chunks.
M can be greater than N.
```

These rules prevent troll claims inside or directly beside another clan's area while still allowing neighboring clans if the configured distance is modest.

## Claim Protection

MVP protection is moderate rather than full lockdown.

In clan-owned chunks:

```text
Clan members can build, break, and use protected containers.
Clan guests can build, break, and use protected containers.
Other players cannot build, break, or use protected containers.
```

Protected container-like blocks include:

```text
chests
trapped chests
barrels
shulker boxes
furnaces
blast furnaces
smokers
hoppers
dispensers
droppers
brewing stands
lecterns with books, if enabled
```

MVP does not block every interaction by default:

```text
doors allowed by default
buttons allowed by default
pressure plates allowed by default
beds allowed by default
non-container interaction allowed by default
```

Configuration should allow these rules to become stricter later:

```yaml
protection:
  block-break: true
  block-place: true
  containers: true
  doors: false
  buttons: false
  pressure-plates: false
  beds: false
```

MVP events to cover:

```text
BlockBreakEvent
BlockPlaceEvent
PlayerInteractEvent for protected containers
EntityExplodeEvent / BlockExplodeEvent filtering claimed blocks
```

Required later protections:

```text
pistons crossing claim borders
bucket fill and empty
armor stands
item frames
vehicles
redstone/container automation edge cases
mob griefing
TNT cannon and cross-border explosion abuse
```

These are not part of the first MVP implementation but should be planned as follow-up work because they matter for a robust protection plugin.

## Remote Containers

Remote container protection is not part of MVP.

MVP rule:

```text
Containers are automatically protected only inside clan claims.
Unclaimed land has no container protection in MVP.
```

Later intended feature:

```text
Clan-owned containers outside claims can be protected.
Protection decays when the owning clan stops interacting with them.
```

Later configuration:

```yaml
remote-containers:
  enabled: false
  claim-mode: explicit # explicit | on-place | on-open
```

Default later behavior should be explicit protection only. Simply opening an unclaimed container should not claim it unless the server explicitly enables `on-open` mode.

Later remote-container behavior:

```text
When a clan member explicitly protects a supported container in unclaimed land:
- the container becomes owned by that clan.
- last_interacted_at is recorded.

If no owning clan member interacts for the configured decay time:
- protection expires.

If another clan claims the chunk containing the remote container:
- the container enters claimed-over decay.
- the original owner clan cannot reset the timer anymore.
- after expiry, protection is removed.
```

Remote containers do not permanently reserve land, do not block another clan from claiming the chunk, and do not guarantee physical access if someone later builds around them.

## PvP

Personal PvP has three states:

```text
OFF
- player cannot deal or receive normal PvP damage.

CLAN
- player allows PvP only with full members of their own clan.

ON
- player allows PvP with anyone whose setting also allows that interaction.
```

Commands:

```text
/pvp
- toggles OFF <-> ON

/pvp on
/pvp off
/pvp clan
/pvp status
```

Normal PvP damage is allowed only if both players' settings allow that specific attacker/victim pairing.

Same-clan friendly fire additionally requires:

```text
both players are full members of the same clan
both players' PvP modes allow same-clan PvP
the clan's friendly-fire setting is enabled
```

No automatic overrides:

```text
Clan claims do not override PvP settings.
Different clans do not override PvP settings.
Future enemies/allies do not override PvP settings unless explicitly designed later.
```

Future accepted duels can override personal PvP settings only for the duel participants and only during the duel.

Combat timeout configuration:

```yaml
pvp:
  combat-timeout-seconds: 60
  combat-tag-mode: attacker-only # attacker-only | both-players
```

Combat tag behavior:

```text
attacker-only:
- only the player who successfully dealt PvP damage is combat-tagged.
- recommended default for this friends server.

both-players:
- attacker and victim are combat-tagged.
- useful for more competitive servers.
```

While combat-tagged, a player cannot switch to a more restrictive PvP mode.

Examples:

```text
ON -> CLAN is blocked while combat-tagged.
ON -> OFF is blocked while combat-tagged.
CLAN -> OFF is blocked while combat-tagged.
OFF -> ON is allowed.
OFF -> CLAN is allowed.
CLAN -> ON is allowed.
```

## Clan Homes

MVP homes are command-based only.

Commands:

```text
/clan sethome <name>
/clan home <name>
/clan delhome <name>
/clan homes

/c sethome <name>
/c home <name>
/c delhome <name>
/c homes
```

Rules:

```text
Clan homes can be set anywhere the player can stand.
Clan homes are shared by the clan.
Only LEADER can set/delete homes in MVP.
MEMBER can use homes.
GUEST cannot use homes.
```

Limit:

```text
home_limit = base_homes_per_clan + member_count * homes_per_member
```

Preferred starting config:

```yaml
homes:
  base-homes-per-clan: 0
  homes-per-member: 1
  teleport-delay-seconds: 3
  teleport-cooldown-seconds: 60
  cancel-on-move: true
```

Homes may later be linked to waystones, portals, or physical anchors. The data model should allow optional anchor metadata later.

## Commands

MVP user commands:

```text
/clan create <name>
/clan disband
/clan invite <player>
/clan join <clan>
/clan leave
/clan kick <player>

/clan guest add <player>
/clan guest remove <player>
/clan guest list

/clan claim
/clan unclaim
/clan claims
/clan inspect

/clan sethome <name>
/clan home <name>
/clan delhome <name>
/clan homes

/clan friendlyfire <on|off|status>

/pvp
/pvp on
/pvp off
/pvp clan
/pvp status
```

Aliases:

```text
/c -> /clan
/clans -> /clan
```

Admin commands:

```text
/clanadmin reload
/clanadmin bypass
/clanadmin inspect
/clanadmin forceunclaim
/clanadmin deleteclan <clan>
/clanadmin setrole <player> <leader|member>
```

Commands should explain why an action failed.

Examples:

```text
Cannot claim this chunk: it would create a new base, but your clan has 1/1 bases.
Cannot claim this chunk: too close to another clan's claim.
Cannot unclaim this chunk: it would split your claims into too many bases.
Cannot use this home: guests cannot use clan homes.
```

## Storage And Network Scope

Use PostgreSQL as the MVP database.

Shared across the network:

```text
clans
clan members
clan guests
player PvP preference, unless server-local PvP is desired later
```

Server-local:

```text
claims
claim groups, if materialized
clan homes
future waystones/anchors
```

Every server-local row includes:

```text
server_id
world_uuid
world_name
```

Claims use:

```text
server_id
world_uuid
chunk_x
chunk_z
```

Homes use:

```text
server_id
world_uuid
world_name
x
y
z
yaw
pitch
```

Migrations:

```text
Plugin owns its schema.
Run migrations at startup.
Fail startup if migrations fail.
Do not silently recreate or drop tables.
```

Caching:

```text
Load claims for active worlds into memory for fast event checks.
Cache clan membership and guest access with short invalidation or reload-on-command.
Writes go to PostgreSQL, then update cache.
```

Because multiple servers may share clan membership, membership changes should be reflected across servers by short TTL cache or explicit reload command in MVP. Later, PostgreSQL LISTEN/NOTIFY can push invalidations.

## MVP Scope

MVP includes:

```text
PaperMC Kotlin plugin
PostgreSQL persistence and migrations
configured server_id
shared network clans
LEADER/MEMBER roles
clan-wide guest access
chunk claims
connected claim groups as automatic bases
claim limit by member count
base/group limit by member count
minimum distance rules
moderate claim protection
clan homes with /c alias
personal PvP OFF/CLAN/ON
attacker-only or both-player combat timeout config
clan friendly fire setting
admin inspect/bypass/reload tools
```

Explicitly not MVP, but designed for later:

```text
Fabric adapter
remote protected containers with decay
door-specific locks
waystones/portals
allies/enemies
duels/1v1 challenges
per-base guest access
```

Required later protections:

```text
pistons crossing claim borders
buckets
armor stands
item frames
vehicles
redstone/container automation edge cases
mob griefing
TNT cannon and cross-border explosion abuse
```

## Testing Strategy

Unit-test core logic:

```text
claim group creation
claim group merging
claim group splitting on unclaim
claim and base limit calculations
minimum distance decisions
PvP OFF/CLAN/ON decisions
combat timeout restriction decisions
role and guest access decisions
```

Integration-test where practical:

```text
PostgreSQL migrations
repository read/write behavior
cache update behavior after writes
```

Manual-test on a local Paper server:

```text
claim and unclaim commands
claim protection events
container access checks
explosion filtering
clan homes
PvP modes and combat timeout
admin inspect and bypass
```

## Open Later Design Topics

These topics are intentionally deferred and should each get their own focused design before implementation:

```text
duel challenge flow and arena/safety rules
remote protected container decay details
waystone or portal UX
allies/enemies and clan diplomacy
Fabric adapter feasibility
advanced protection event coverage
```
