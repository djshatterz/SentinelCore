# SentinelCore Technical Design Blueprint
### (Developer Edition — Full Implementation Reference)
**Author:** Shatterz  
**System Planning:** ChatGPT (GPT-5)  
**Version:** 1.0  
**Target:** Fabric Server – Minecraft 1.21.10  
**Date:** 2025-10-22  

---

## Table of Contents
1. Introduction  
2. Core Architecture Overview  
3. Shared Systems  
4. System Diagrams  
5. Feature Specifications (1 – 10)  
6. Developer Architecture and Build Notes  

---

# 1  Introduction
SentinelCore is a modular Fabric server framework for Minecraft 1.21.10 that extends administration and staff control with low-noise UX and tight integration with **LuckPerms**, **Ledger**, and **InvView**.  
All sub-systems operate independently but communicate through shared service managers for permissions, movement, auditing, and naming.

### Design Goals
- Modular service-based architecture.  
- Unified, context-aware permission evaluation.  
- Centralized auditing and telemetry.  
- Shared teleportation / Elytra logic.  
- Dynamic command visibility and contextual permissions.  

---

# 2  Core Architecture Overview

| Manager | Purpose |
|:--|:--|
| `PermissionManager` | Bridges LuckPerms + SentinelCore groups and vanilla ops. |
| `RoleContext` | Holds each player’s effective group, mod-mode flag, vanish state, and cached nodes. |
| `AuditManager` | Routes logs to Ledger and file storage. |
| `MovementManager` | Safe teleports and Elytra removal. |
| `NameFormatter` | Combines community and team prefixes + mod-mode color overrides. |
| `AdvancementBlocker` | Prevents advancement triggers while temporary Elytra or mod mode active. |
| `CommandVisibility` | Prunes Brigadier trees so players see only accessible commands. |

---

# 3  Shared Systems

## 3.1 Permission Manager
**Responsibilities**
- Merge LuckPerms, SentinelCore groups, and ops.  
- Provide context flags (`modmode:true`, `world:<id>`).  
- Dispatch `PermissionContextChanged` events.

**Example**
```java
if (PermissionManager.has(player, "sentinelcore.spawnelytra.admin")) {
  // allow command
}
````

**Used by:** Hidden Commands, Mod Mode, SpawnProt, TeleportAreas, Elytra.

---

## 3.2 Role Context

Stores per-player state:

```json
{
 "uuid":"player-uuid",
 "group":"moderator",
 "isOp":false,
 "modMode":false,
 "vanished":false,
 "contextFlags":["modmode:false"]
}
```

Updated on LuckPerms changes, group edits, mod-mode toggle, or vanish toggle.
Consumers: Hidden Cmds, NameFormatter, Audit.

---

## 3.3 Audit Manager

Central logging bridge.

```java
AuditManager.log("mod.vanish.toggle", actor, ctx, Map.of(
 "vanished", true,
 "world", actor.getWorld().getRegistryKey().getValue()
));
```

Outputs → `logs/sentinelcore/audit-YYYYMMDD.jsonl` and Ledger (if loaded).

---

## 3.4 Movement Manager

Shared teleport logic.
Checks safe landing (±6 Y), cancels glide, removes SpawnElytra, supports cross-dimension.

```yaml
movement:
 safeTeleport:
  searchUpDown: 6
  avoidLava: true
  avoidPowderSnow: true
  avoidCactusFire: true
  centerOnBlock: true
 dismountBeforeTeleport: true
 cancelGliding: true
 removeSpawnElytra: true
```

---

## 3.5 Name Formatter

Creates display name:
`[Community] [Team] PlayerName`
Colors: team = gold, mod = blue, admin = red.
Triggered on join and context change.

---

## 3.6 Command Visibility

Dynamic Brigadier filter.
Rebuild tree on permission or mod-mode changes.

```yaml
commandVisibility:
 enabled: true
 hideInaccessible: true
 respectAliases: true
 vanillaOpAsBypass: true
```

---

# 4  System Diagrams

### Mermaid — Core Dependency Graph

```mermaid
graph TD
 A[PermissionGroups] --> B[PermissionManager]
 B --> C[RoleContext]
 C --> D[HiddenCommands]
 C --> E[ModeratorMode]
 B --> F[SpawnProtection]
 E --> G[Warps (ModMode)]
 E --> H[CommandAudit]
 H --> I[Ledger]
 F --> J[SpawnElytra]
 F --> K[TeleportAreas]
 J --> L[MovementManager]
 K --> L
 E --> M[TeamPrefix]
 M --> N[CommunityPrefixes]
 C --> M
 C --> N
```

---

### ASCII Backup

```
PermissionGroups → PermissionManager → RoleContext
RoleContext → HiddenCommands, ModeratorMode
PermissionManager → SpawnProtection
ModeratorMode → Warps, CommandAudit
CommandAudit → Ledger
SpawnProtection → SpawnElytra → MovementManager
TeleportAreas → MovementManager
RoleContext → TeamPrefix + CommunityPrefixes
```

---

# 5  Feature Specifications

## Feature 1 — SpawnElytra

### 🎯 Purpose  
Provide a **single-use Elytra** at spawn for new players.

### 🧩 User Stories
- As a new player I receive a unique Elytra on first join or world-spawn respawn.  
- As an admin I can configure world-spawn, detection radius, boost strength, and count.  
- As a developer I must ensure the Elytra cannot be removed, swapped, or retained after death.

### ✅ Acceptance Criteria
- Auto-equips at world-spawn.  
- Locked in chest slot; not droppable.  
- Removed when touching ground/water below Y threshold.  
- Removed on disconnect/server shutdown.  
- One boost (Shift) by default.  
- “Sky’s the Limit” advancement blocked while active.  
- Removed on Mod-Mode entry.

### ⚙️ Commands
| Command | Description | Permission |
|:--|:--|:--|
| `/spawnelytra info` | Show current config | `sentinelcore.spawnelytra.info` |
| `/spawnelytra setworldspawn` | Define world-spawn | `sentinelcore.spawnelytra.admin` |
| `/spawnelytra setdetectionradius <blocks>` | Set detection radius | `sentinelcore.spawnelytra.admin` |
| `/spawnelytra setboostcount <n>` | Set boost count | `sentinelcore.spawnelytra.admin` |
| `/spawnelytra setbooststrength <float>` | Set boost strength | `sentinelcore.spawnelytra.admin` |

### 🧾 Configuration
```yaml
spawnelytra:
  enabled: true
  detection:
    worldspawn: "world:0,100,0"
    radius: 6
  boost:
    baseCount: 1
    strength: 1.0
    key: "shift"
  removal:
    groundTriggerY: -4
  restrictions:
    preventSwap: true
    preventDrop: true
    preventRocketUse: true
  advancements:
    blockWhileActive:
      - "minecraft:adventure/elytra"
  feedback:
    particles: false
    sound: false
  cooldown:
    perPlayerSec: 5
  limits:
    antiExploit:
      blockRocketUse: true
````

### 🧠 Developer Notes

* Elytra tracked by UUID; server-side only.
* Cleans via `MovementManager.safeTeleport()` removal.
* AdvancementBlocker handles blocked achievements.
* Only active in Survival/Adventure.
* Audit events: `elytra.equip`, `elytra.remove`, `elytra.boost`.

---

## Feature 2 — SpawnProtection

### 🎯 Purpose

Protect spawn region from building, explosions, or grief.

### 🧩 User Stories

* Admins toggle and size protection zone.
* Players can still open allowed blocks.
* Developer ensures mobs/explosions blocked.

### ✅ Acceptance Criteria

* Only admins modify.
* Blocks break/place prevented for non-creative.
* Allows: crafting table, chests, ender chest, enchanting table, shulker box.
* Blocks explosions, pistons, fluids, Endermen, mobs.
* Top Y = world max.

### ⚙️ Commands

| Command                         | Description | Permission                     |                                |
| :------------------------------ | :---------- | :----------------------------- | ------------------------------ |
| `/spawnprot info`               | View config | `sentinelcore.spawnprot.info`  |                                |
| `/spawnprot enable              | disable`    | Toggle                         | `sentinelcore.spawnprot.admin` |
| `/spawnprot setcenter`          | Set center  | `sentinelcore.spawnprot.admin` |                                |
| `/spawnprot setradius <blocks>` | Set radius  | `sentinelcore.spawnprot.admin` |                                |
| `/spawnprot setbottomy <y>`     | Set bottom  | `sentinelcore.spawnprot.admin` |                                |

### 🧾 Configuration

```yaml
spawnprotection:
  enabled: true
  shape: "cylinder"
  radius: 25
  bottomY: 50
  whitelist:
    interact:
      - "minecraft:crafting_table"
      - "minecraft:chest"
      - "minecraft:ender_chest"
      - "minecraft:enchanting_table"
      - "minecraft:shulker_box"
  physics:
    blockExplosions: true
    blockPistons: true
    blockFluids: true
    blockEndermen: true
    blockMobSpawn: true
```

### 🧠 Developer Notes

* Integrate into block-place/destroy events.
* Creative-bypass via PermissionManager.
* Audited as `spawnprot.*`.
* Coexists with SpawnElytra region.

---

## Feature 3 — Community Prefixes

### 🎯 Purpose

Let players choose a chat/tab community tag.

### 🧩 User Stories

* Player chooses or removes prefix.
* Admin defines list and colors.
* Developer ensures sync between tab/chat.

### ✅ Acceptance Criteria

* `[Prefix] Player` in chat/tab.
* Named or RGB colors.
* Stored per-UUID.
* No per-prefix styles.

### ⚙️ Commands

| Command                   | Description   | Permission                     |
| :------------------------ | :------------ | :----------------------------- |
| `/community list`         | Show prefixes | `sentinelcore.community.use`   |
| `/community select <id>`  | Set prefix    | `sentinelcore.community.use`   |
| `/community none`         | Remove prefix | `sentinelcore.community.use`   |
| `/community admin reload` | Reload list   | `sentinelcore.community.admin` |

### 🧾 Configuration

```yaml
community:
  enabled: true
  formatting:
    brackets: { left: "[", right: "]", spaceAfter: true }
    tablist: true
    chat: true
  prefixes:
    Clymunity: { color: "dark_green" }
    Pawpack: { color: "aqua" }
    Flockecrew: { color: "green" }
    Pixelite: { color: "light_purple" }
    Blizzunity: { color: "gold" }
    Spaceschnittchen: { color: "red" }
```

### 🧠 Developer Notes

* Config → `config/sentinelcore/prefixes.yml`.
* Uses NameFormatter pipeline.
* LuckPerms optional node `sentinelcore.community.use.<id>`.

---

## Feature 4 — Permission Groups

### 🎯 Purpose

Provide built-in group hierarchy if LuckPerms missing.

### 🧩 User Stories

* Admin assigns groups.
* Dev maps groups to LuckPerms bridge.
* Mods gain mod tools; Admins full access.

### ✅ Acceptance Criteria

* Inheritance chain: default → mod → admin → dev → op.
* Admin+ can configure Elytra/Protection.
* Mods limited.
* Ops bypass.
* LuckPerms bridge supported.

### 🧾 Group Map

```yaml
permissions:
  groups:
    default:
      nodes:
        - "sentinelcore.community.use"
        - "sentinelcore.spawnelytra.use"
    moderator:
      inherits: ["default"]
    admin:
      inherits: ["moderator"]
      nodes:
        - "sentinelcore.spawnprot.admin"
        - "sentinelcore.spawnelytra.admin"
        - "sentinelcore.community.admin"
    developer:
      inherits: ["admin"]
      nodes:
        - "sentinelcore.dev.*"
    op:
      inherits: ["developer"]
      nodes:
        - "sentinelcore.*"
```

### 🧠 Developer Notes

* Syncs with LuckPerms on startup.
* Exposed via RoleContext.
* Admin = Elytra/Prot rights.
* Mod mode adds temp nodes.

---

## Feature 5 — Team Prefix Integration

### 🎯 Purpose

Add `[Team]` prefix for moderator/admin groups.

### 🧩 User Stories

* Staff names show `[Team] Player`.
* Admin configures color.
* Dev merges with Community prefix.

### ✅ Acceptance Criteria

* `[Team]` for configured groups.
* Order `[Community] [Team] Player`.
* Default gold color.
* Auto-updates on group change.

### 🧾 Configuration

```yaml
community:
  teamPrefix:
    enabled: true
    label: "Team"
    color: "gold"
    appliesTo: ["moderator", "admin"]
    style: { bold: false, italic: false }
    position: "afterCommunity"
```

### 🧠 Developer Notes

* Uses NameFormatter.
* Reads from RoleContext.group.
* Updates on join or perm change.
---

## Feature 6 — Moderator Mode
### 🎯 Purpose  
Provide a dedicated moderation state with temporary powers, safety, and audit logging.

### 🧩 User Stories
- Moderator: toggle Mod Mode to moderate safely.  
- Admin: full access with unrestricted `/give` and `/view`.  
- Developer: restore original player state when exiting.

### ✅ Acceptance Criteria
- `/mod`, `/m`, `/tst` toggle Mod Mode.  
- Saves & restores inventory, XP, gamemode, position, effects.  
- Invulnerable + no hunger/air loss.  
- Removes SpawnElytra.  
- True vanish (fake leave/join and tab hide).  
- Grants mod tools while active.  
- No advancements during mode.  
- All commands audited.

### ⚙️ Commands (while in Mod Mode)

| Command | Alias | Description |
|:--|:--|:--|
| `/pickup on|off` | — | Toggle item pickup |
| `/instantbreak` | — | Toggle instant block break |
| `/fly` | — | Toggle flight |
| `/vanish` | `/v` | Toggle vanish |
| `/mute <player>` | — | Mute player |
| `/unmute <player>` | — | Unmute player |
| `/view inv <player>` | — | View inventory |
| `/view echest <player>` | — | View Ender Chest |
| `/ledger …` | — | Access Ledger toolkit |
| `/tp …` | — | Teleport |
| `/kick …` | — | Kick player |
| `/ban …` | — | Ban player |
| `/whitelist …` | — | Manage whitelist |
| `/give …` | — | Give items (admin unrestricted) |

### 🧾 Configuration
```yaml
modmode:
  commandAliases: ["tst","m"]
  give:
    moderator:
      mode: "blacklist"
      blacklist:
        - "minecraft:bedrock"
        - "minecraft:command_block"
        - "minecraft:structure_block"
        - "minecraft:debug_stick"
        - "minecraft:barrier"
        - "minecraft:light"
    admin:
      mode: "unrestricted"
  vanish:
    fakeLeaveJoin: true
  audit:
    logAllCommands: true
````

### 🧠 Developer Notes

* Enter → snapshot inventory/xp/gamemode/location.
* Exit → restore snapshot.
* Temp LuckPerms context `modmode:true`.
* Hidden Commands shows tools only while active.
* Logs Ledger `mod.enter`, `mod.exit`.

---

## Feature 7 — Command Audit

### 🎯 Purpose

Record every staff command for accountability.

### 🧩 User Stories

* Admin: review staff actions.
* Developer: mask sensitive data.
* Owner: mirror to Ledger.

### ✅ Acceptance Criteria

* Logs actor, timestamp, world, command, args.
* Redacts tokens and IPs.
* Daily rotation, 30-day retention.
* Optional Ledger mirror.
* Async non-blocking writes.

### ⚙️ Configuration

```yaml
audit:
  commands:
    enabled: true
    groups: ["moderator","admin","developer","op"]
    ledger: true
    file:
      path: "logs/sentinelcore"
      rotateDaily: true
      retentionDays: 30
    redact:
      patterns:
        - "(?i)token:[^\\s]+"
        - "(?i)password=[^\\s]+"
      ipAddresses: "redact"
    exclude:
      commands: ["login","register"]
    categories:
      kick: "audit.cmd.kick"
      ban: "audit.cmd.ban"
      mute: "audit.cmd.mute"
      tp: "audit.cmd.tp"
      give: "audit.cmd.give"
```

### ⚙️ Commands (Admin Only)

| Command                          | Description              | Permission                |
| :------------------------------- | :----------------------- | :------------------------ |
| `/sclogs tail [n]`               | Tail recent entries      | `sentinelcore.logs.admin` |
| `/sclogs find <filter>`          | Search by player/command | `sentinelcore.logs.admin` |
| `/sclogs export <since> <until>` | Export range             | `sentinelcore.logs.admin` |

### 🧠 Developer Notes

* Implemented through `AuditManager`.
* Ledger events `audit.cmd.*`.
* Redaction pre-write filter.
* Integration with Mod Mode + Ledger.
* Rotation thread daily.

---

## Feature 8 — Warps (Mod-Mode Only)
### 🎯 Purpose  
Staff-only teleport system available exclusively in Mod Mode.

### 🧩 User Stories
- Admin can add/remove warps while in mod mode.  
- Moderator can use or list them while in mod mode.  
- Developer must ensure safe teleportation and Elytra removal.

### ✅ Acceptance Criteria
- Commands usable **only in Mod Mode**.  
- Admin: add/remove; Mod: list/use.  
- Safe teleports via MovementManager.  
- Removes Elytra, preserves vanish.  
- Instant, cross-dimension capable.

### ⚙️ Commands
| Command | Description | Permission |
|:--|:--|:--|
| `/warp list` | List mod warps | `sentinelcore.mod.warp.list` |
| `/warp <name>` | Teleport to warp | `sentinelcore.mod.warp.use` |
| `/warp add <name>` | Create warp | `sentinelcore.mod.warp.admin` |
| `/warp remove <name>` | Delete warp | `sentinelcore.mod.warp.admin` |
| `/warp info <name>` | Show warp info | `sentinelcore.mod.warp.list` |

### 🧾 Configuration
```yaml
modmode:
  warps:
    enabled: true
    requireModMode: true
    permissions:
      use: "sentinelcore.mod.warp.use"
      list: "sentinelcore.mod.warp.list"
      admin: "sentinelcore.mod.warp.admin"
    behavior:
      allowCrossDimension: true
      safeTeleport:
        searchUpDown: 6
        avoidLava: true
        avoidCactusFire: true
        avoidPowderSnow: true
      removeSpawnElytraOnWarp: true
    storage:
      file: "config/sentinelcore/modwarps.json"
    audit:
      ledger: true
      category: "mod.warp"
````

### 🧠 Developer Notes

* Uses MovementManager.safeTeleport().
* Requires context `modmode:true`.
* Hidden outside Mod Mode.
* Logs through AuditManager + Ledger (`mod.warp.*`).
* Warp data stored as JSON (see schema section).

---

## Feature 9 — Teleport Areas

### 🎯 Purpose

Define trigger zones that teleport players after holding **Shift** for a configured duration.

### 🧩 User Stories

* Players use Shift to teleport from predefined regions.
* Admins create/edit/remove areas.
* Developer ensures compatibility with SpawnElytra and safe teleports.

### ✅ Acceptance Criteria

* Holding Shift ≥ 0.5 s triggers teleport.
* Default players and mods can use; only admins configure.
* Safe teleports and Elytra removal.
* Silent UX, configurable cooldown.

### ⚙️ Commands (Admin Only)

| Command                           | Description   | Permission                  |                             |
| :-------------------------------- | :------------ | :-------------------------- | --------------------------- |
| `/tparea list`                    | List areas    | `sentinelcore.tparea.list`  |                             |
| `/tparea info <id>`               | Show details  | `sentinelcore.tparea.list`  |                             |
| `/tparea add <id>`                | Create area   | `sentinelcore.tparea.admin` |                             |
| `/tparea setshape <id> sphere     | cylinder`     | Change shape                | `sentinelcore.tparea.admin` |
| `/tparea setcenter <id>`          | Set center    | `sentinelcore.tparea.admin` |                             |
| `/tparea setradius <id> <blocks>` | Set radius    | `sentinelcore.tparea.admin` |                             |
| `/tparea setbottomy <id> <y>`     | Set bottom    | `sentinelcore.tparea.admin` |                             |
| `/tparea setdestination <id>`     | Set target    | `sentinelcore.tparea.admin` |                             |
| `/tparea sethold <id> <ms>`       | Hold duration | `sentinelcore.tparea.admin` |                             |
| `/tparea setcooldown <id> <sec>`  | Cooldown      | `sentinelcore.tparea.admin` |                             |
| `/tparea enable                   | disable <id>` | Toggle area                 | `sentinelcore.tparea.admin` |
| `/tparea remove <id>`             | Delete area   | `sentinelcore.tparea.admin` |                             |

### 🧾 Configuration

```yaml
tparea:
  enabled: true
  usage:
    permissionNode: null
  trigger:
    defaultHoldMs: 500
  teleport:
    allowCrossDimension: true
    safeTeleport:
      searchUpDown: 6
      avoidLava: true
      avoidPowderSnow: true
      avoidCactusFire: true
    removeSpawnElytraOnTeleport: true
  storage:
    file: "config/sentinelcore/tpareas.json"
  audit:
    ledger: true
```

### 🧠 Developer Notes

* Uses MovementManager.safeTeleport().
* Elytra disables trigger until removal.
* Shape: sphere or cylinder.
* Asynchronous detection loop; per-player cooldown.
* Audit category: `tparea.use` and `tparea.manage`.

---

## Feature 10 — Hidden Commands

### 🎯 Purpose

Hide commands the player cannot execute from `/help` and tab completion.

### 🧩 User Stories

* Player sees only usable commands.
* Moderator sees mod tools only while in mod mode.
* Developer ensures live refresh on permission change.

### ✅ Acceptance Criteria

* Hidden from help and tab if inaccessible.
* Context “modmode:true” reveals mod tools.
* Always-show/always-hide lists supported.
* Aliases pruned with root commands.
* Instant update on permission or role changes.

### ⚙️ Configuration

```yaml
commandVisibility:
  enabled: true
  hideInaccessible: true
  includeInHelp: false
  respectAliases: true
  vanillaOpAsBypass: true
  alwaysShow: ["help","msg"]
  alwaysHide: ["minecraft:debug"]
  contexts:
    modMode:
      restrictToModMode:
        - "pickup"
        - "instantbreak"
        - "fly"
        - "vanish"
        - "mute"
        - "unmute"
        - "tp"
        - "give"
        - "kick"
        - "ban"
        - "whitelist"
        - "ledger"
        - "view"
        - "warp"
  cache:
    perPlayerTreeMillis: 2000
    clearOnPermChange: true
```

### 🧠 Developer Notes

* Uses RoleContext for `modmode:true`.
* Hooks into Brigadier dispatcher rebuild.
* Aliases hidden with root.
* Console/RCON unaffected.
* Refresh triggered by `PermissionContextChanged`.

---

# 6  Developer Architecture and Build Notes

---

## Dependency Cluster Diagram (Mermaid)
```mermaid
graph TD
 PermissionManager --> RoleContext
 RoleContext --> HiddenCommands
 RoleContext --> ModeratorMode
 ModeratorMode --> Warps
 ModeratorMode --> CommandAudit
 CommandAudit --> Ledger
 SpawnProtection --> SpawnElytra
 SpawnElytra --> MovementManager
 TeleportAreas --> MovementManager
 RoleContext --> TeamPrefix
 TeamPrefix --> CommunityPrefixes
````

---

## Unified Manager Architecture

| Manager                | Responsibility                                       | Used By                    |
| :--------------------- | :--------------------------------------------------- | :------------------------- |
| **PermissionManager**  | Unified permission resolution (LuckPerms + internal) | All systems                |
| **RoleContext**        | Player group + mod/vanish context cache              | HiddenCmds, Audit, NameFmt |
| **MovementManager**    | Safe teleport, Elytra cleanup                        | Elytra, Warps, TPAreas     |
| **AuditManager**       | Ledger + JSONL event logging                         | All staff actions          |
| **NameFormatter**      | Combines community/team/modmode colors               | Prefix & display systems   |
| **CommandVisibility**  | Brigadier tree filtering                             | All commands               |
| **AdvancementBlocker** | Cancels Elytra/ModMode advancements                  | Elytra, ModMode            |

---

## Implementation Build Order

1. **PermissionManager + RoleContext**
   Foundation for all role and permission logic.

2. **AuditManager + CommandAudit**
   Adds visibility and safe logging for every subsystem.

3. **NameFormatter + Prefix Systems**
   Required before ModMode color overrides.

4. **Hidden Commands**
   Depends on RoleContext + PermissionManager.

5. **MovementManager**
   Shared teleport, Elytra removal, glide cancel.

6. **SpawnProtection**
   Establish baseline safe region.

7. **SpawnElytra**
   Connect to MovementManager + AdvancementBlocker.

8. **Moderator Mode**
   Integrate all staff tools + audit visibility.

9. **Warps**
   ModMode-only teleport layer.

10. **Teleport Areas**
    Public Shift-hold teleports using MovementManager.

---

## Storage Schema Examples

### `modwarps.json`

```json
{
  "hub": {
    "world": "overworld",
    "x": 0.5,
    "y": 100.0,
    "z": 0.5,
    "yaw": 0.0,
    "pitch": 0.0,
    "createdBy": "Shatterz",
    "createdAt": "2025-10-22T12:00:00Z"
  }
}
```

### `tpareas.json`

```json
{
  "spawn_fall": {
    "shape": "cylinder",
    "center": [0, 100, 0],
    "radius": 8,
    "bottomY": 70,
    "destination": {
      "world": "overworld",
      "x": 0.5,
      "y": 64.0,
      "z": 0.5
    },
    "holdMs": 500,
    "cooldownSec": 0,
    "enabled": true
  }
}
```

---

## Development Standards

| Category              | Standard                                                |
| :-------------------- | :------------------------------------------------------ |
| **Language**          | Java (Fabric API)                                       |
| **Command Framework** | Brigadier                                               |
| **Persistence**       | JSON/YAML hybrid via Gson + Configurate                 |
| **Permissions**       | LuckPerms bridge w/ internal fallback                   |
| **Logging**           | Asynchronous JSONL                                      |
| **Testing**           | Integration with MockFabricEnvironment                  |
| **Coding Style**      | Minimal side effects; managers stateless where possible |

---

## Behavior Rules and UX

* **Silent UX:** no spammy feedback; messages only on error or config reload.
* **Elytra & Teleports:** unified cleanup; no item drops or exploits.
* **ModMode safety:** disables hunger, air loss, damage.
* **Audit coverage:** all admin/mod actions visible in Ledger or audit logs.
* **Extensibility:** new systems register via ServiceLoader to hook into managers.

---

## Final Notes

SentinelCore serves as a foundational admin layer for Minecraft Fabric 1.21.10+.
Every subsystem operates independently but integrates through the shared managers defined above.
All configuration is reloadable at runtime.
All commands respect both internal and external (LuckPerms) permission nodes.

---

**End of SentinelCore Technical Design Blueprint — Developer Edition (v1.0)**
© 2025 Shatterz. All rights reserved for design documentation.

