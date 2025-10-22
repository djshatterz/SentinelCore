# SentinelCore Technical Design Blueprint (Developer Edition)

> Version 1.0 — Fabric Server Mod for Minecraft 1.21.10  
> Author: SentinelCore System Plan (ChatGPT GPT-5)  
> Date: Generated Automatically

---

## Table of Contents
1. Introduction
2. Core Architecture Overview
3. Shared Systems
4. Feature Specifications
   - 1. SpawnElytra
   - 2. SpawnProtection
   - 3. Community Prefixes
   - 4. Permission Groups
   - 5. Team Prefix Integration
   - 6. Moderator Mode
   - 7. Command Audit
   - 8. Warps (Mod-Mode Only)
   - 9. Teleport Areas
   - 10. Hidden Commands
5. System Diagrams
6. Implementation Build Order
7. Future Extensions

---

## 1. Introduction

SentinelCore is a modular, permission-aware Fabric mod suite designed to extend server administration, gameplay customization, and staff control systems. It integrates seamlessly with **LuckPerms**, **Ledger**, and **InvView**, following a silent and low-UX philosophy.

Each subsystem operates independently but connects through unified managers for permissions, teleportation, auditing, and naming.

---

## 2. Core Architecture Overview

**Key architectural goals:**  
- Modular service design (each feature independent)  
- Unified context for permission and role state  
- Central audit pipeline for all staff actions  
- Consistent teleportation logic and Elytra handling  
- Automatic command visibility and contextual permissions  

### Primary Managers

| Manager | Responsibility |
|----------|----------------|
| `PermissionManager` | Wraps LuckPerms + SentinelCore groups |
| `RoleContext` | Holds player’s effective rank, mod-mode state, vanish, etc. |
| `AuditManager` | Routes events to Ledger + file logs |
| `MovementManager` | Handles teleports, safe-landing, Elytra removal |
| `NameFormatter` | Combines prefixes (Community, Team, ModMode colors) |
| `AdvancementBlocker` | Prevents advancements in Elytra/ModMode |
| `CommandVisibility` | Hides inaccessible commands dynamically |

---

## 3. Shared Systems

### 3.1 PermissionManager
**Purpose:** single interface for all permission lookups.  
Integrates LuckPerms bridge, SentinelCore internal groups, and vanilla op fallback.

**Responsibilities:**
- `hasPermission(player, node)` unified call.
- Context awareness (`modmode:true`, `world:<name>`).
- Fires event `PermissionContextChanged` on group change.

**Used by:** Hidden Commands, Mod Mode, Warps, Elytra, SpawnProtection.

---

### 3.2 RoleContext
Stores live state for each player:

```json
{
  "uuid": "player-uuid",
  "group": "moderator",
  "isOp": false,
  "inModMode": false,
  "isVanished": false,
  "contextFlags": ["modmode:false"]
}
```

Updated on:
- LuckPerms group change
- SentinelCore group command
- Mod mode enter/exit
- Vanish toggle

**Consumers:** HiddenCommands, NameFormatter, CommandAudit.

---

### 3.3 AuditManager
Provides consistent log & Ledger event interface:

```java
AuditManager.log("mod.give", actor, context, {
    "item": "minecraft:diamond_sword",
    "outcome": "success"
});
```

Outputs to:
- File log (`logs/sentinelcore/audit.jsonl`)
- Ledger events (if Ledger loaded)

**Consumers:** CommandAudit, TeleportAreas, Warps, ModMode, Elytra, SpawnProt.

---

### 3.4 MovementManager
Central teleportation handler.

Handles:
- Safe teleportation check (no lava, fire, powder snow)
- Elytra removal
- Velocity reset
- Glide cancel
- Cross-dimension teleportation

Used by: SpawnElytra, Warps, TeleportAreas.

---

### 3.5 NameFormatter
Centralizes player display names.  
Combines `[Community]`, `[Team]`, and ModMode recolor dynamically.

---

### 3.6 CommandVisibility
Dynamic Brigadier command-tree pruning for each player.  
Rebuilds tree on:
- Mod mode toggle
- LuckPerms permission update
- Op/deop event

---

## 4. Feature Specifications

### Feature 1 — SpawnElytra
*(full content continues for all features below…)*
