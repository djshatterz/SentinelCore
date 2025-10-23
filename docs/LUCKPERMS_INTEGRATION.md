# LuckPerms Integration Guide

## Overview
SentinelCore supports integration with LuckPerms for advanced permission management. The integration is designed to work in two modes:

- **BRIDGE** - Query LuckPerms directly for all permission checks
- **MIRROR** - Cache LuckPerms data in memory for performance

## Current Status
🚧 **Placeholder implementation** - The LuckPerms integration skeleton is in place, but the actual API calls are not yet implemented.

## Implementation Checklist

### 1. Add LuckPerms API Dependency
Add to `build.gradle`:
```gradle
repositories {
    maven { url 'https://oss.sonatype.org/content/repositories/snapshots' }
}

dependencies {
    modImplementation 'net.luckperms:api:5.4'
}
```

### 2. Implement LuckPermsService
File: `src/main/java/org/github/shatterz/sentinelcore/perm/luckperms/LuckPermsService.java`

**TODO locations marked with `// TODO: Implement actual LuckPerms API integration`**

Key methods to implement:
- `check(UUID, String, Map<String, String>)` - Query permission with context
- `getGroup(UUID)` - Get primary group
- `setGroup(UUID, String)` - Set primary group
- `getInheritedGroups(UUID)` - Get all inherited groups
- `groupExists(String)` - Check if group exists

**API References:**
```java
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.types.InheritanceNode;
import net.luckperms.api.query.QueryOptions;
import net.luckperms.api.context.ImmutableContextSet;

// Get API instance
LuckPerms api = LuckPermsProvider.get();

// Load user
User user = api.getUserManager().loadUser(uuid).join();

// Check permission with context
ImmutableContextSet contextSet = ImmutableContextSet.builder()
    .add("world", worldName)
    .add("modmode", "true")
    .build();
QueryOptions queryOptions = QueryOptions.contextual(contextSet);
boolean hasPermission = user.getCachedData().getPermissionData(queryOptions).checkPermission(node).asBoolean();

// Get primary group
String primaryGroup = user.getPrimaryGroup();

// Set primary group
user.data().clear(NodeType.INHERITANCE::matches);
user.data().add(InheritanceNode.builder(groupName).build());
api.getUserManager().saveUser(user);
```

### 3. Implement Event Listeners
File: `src/main/java/org/github/shatterz/sentinelcore/perm/luckperms/LuckPermsEventListener.java`

**Events to listen for:**
- `UserDataRecalculateEvent` - When user permissions are recalculated
- `GroupDataRecalculateEvent` - When group permissions are recalculated
- `UserFirstLoginEvent` - When a user joins for the first time
- `NodeAddEvent` / `NodeRemoveEvent` - When nodes are added/removed

**Integration points:**
- Update `RoleContextManager` when groups change
- Fire `PermissionContextChangedCallback` events
- Invalidate caches in MIRROR mode

### 4. Configuration
Update `run/config/sentinelcore/config.yaml` to use LuckPerms:

```yaml
permissions:
  backend: "luckperms"  # or "luckperms-bridge" or "luckperms-mirror"
  defaultRole: "default"
  # roles section is ignored when using LuckPerms backend
```

### 5. Fallback Behavior
If LuckPerms is configured but not available:
- ✅ Already implemented: Falls back to memory backend
- ✅ Logs warning: "LuckPerms not available, falling back to memory backend"

### 6. Testing
Once implemented, test:
1. Create groups in LuckPerms via `/lp creategroup <name>`
2. Assign permissions via `/lp group <name> permission set <node>`
3. Assign users to groups via `/lp user <player> parent set <group>`
4. Verify SentinelCore commands respect LP permissions
5. Test context-aware permissions (world, modmode)
6. Test permission inheritance
7. Test hot-reload when LP data changes

## Architecture Notes

### Bridge Mode
- Every permission check queries LuckPerms directly
- No caching in SentinelCore
- Pros: Always in sync with LP
- Cons: Slight performance overhead

### Mirror Mode
- Cache LP data in SentinelCore's MemoryPermissionService
- Sync on LP events
- Pros: Better performance
- Cons: Slight delay on permission updates

### Context Integration
SentinelCore provides these contexts to LuckPerms:
- `world` - Current world ID
- `modmode` - "true" when in mod mode
- `vanished` - "true" when vanished
- Custom contexts can be added via `RoleContext.updateContextFlag()`

## Related Files
- `src/main/java/org/github/shatterz/sentinelcore/perm/luckperms/LuckPermsService.java`
- `src/main/java/org/github/shatterz/sentinelcore/perm/PermissionBootstrap.java`
- `src/main/java/org/github/shatterz/sentinelcore/perm/PermissionManager.java`
- `src/main/java/org/github/shatterz/sentinelcore/perm/RoleContextManager.java`

## Future Enhancements
- [ ] Auto-sync SentinelCore groups to LP groups
- [ ] Web UI integration for permission management
- [ ] Audit log integration with LP actions
- [ ] Migration tool from memory backend to LP
