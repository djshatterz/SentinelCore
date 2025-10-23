package org.github.shatterz.sentinelcore.perm.memory;

import java.util.*;
import org.github.shatterz.sentinelcore.config.CoreConfig;
import org.github.shatterz.sentinelcore.perm.PermissionService;

public final class MemoryPermissionService implements PermissionService {
  private final String name = "memory";
  private final Map<String, RoleView> roles = new HashMap<>();
  private String defaultRole = "default";
  private final Map<UUID, String> userRoles = new HashMap<>();

  static final class RoleView {
    Set<String> allow = new HashSet<>();
    Set<String> deny = new HashSet<>();
    List<String> inherits = new ArrayList<>();
  }

  public MemoryPermissionService(CoreConfig cfg) {
    reload(cfg);
  }

  public void reload(CoreConfig cfg) {
    roles.clear();
    userRoles.clear();
    // update defaultRole from config
    if (cfg.permissions != null && cfg.permissions.defaultRole != null) {
      this.defaultRole = cfg.permissions.defaultRole;
    } else {
      this.defaultRole = "default";
    }

    if (cfg.permissions != null && cfg.permissions.roles != null) {
      cfg.permissions.roles.forEach(
          (name, role) -> { // role is CoreConfig.Role
            RoleView v = new RoleView();
            if (role.allow != null) v.allow.addAll(role.allow);
            if (role.deny != null) v.deny.addAll(role.deny);
            if (role.inherits != null) v.inherits.addAll(role.inherits);
            roles.put(name, v);
          });
    }

    // load explicit user role assignments from config
    if (cfg.permissions != null && cfg.permissions.userRoles != null) {
      cfg.permissions.userRoles.forEach(
          (uuidStr, roleName) -> {
            try {
              java.util.UUID uuid = java.util.UUID.fromString(uuidStr);
              if (roleName != null && roles.containsKey(roleName)) {
                userRoles.put(uuid, roleName);
              }
            } catch (IllegalArgumentException ignored) {
              // skip invalid UUID strings
            }
          });
    }
  }

  @Override
  public String name() {
    return name;
  }

  @Override
  public boolean check(UUID subject, String node, Map<String, String> ctx) {
    // resolve subject role
    String role = userRoles.getOrDefault(subject, defaultRole);
    Set<String> visited = new HashSet<>();
    return resolve(role, node, visited);
  }

  @Override
  public String getGroup(UUID subject) {
    return userRoles.getOrDefault(subject, defaultRole);
  }

  @Override
  public void setGroup(UUID subject, String group) {
    if (group == null || !roles.containsKey(group)) {
      userRoles.remove(subject);
    } else {
      userRoles.put(subject, group);
    }
  }

  @Override
  public List<String> getInheritedGroups(UUID subject) {
    String primaryGroup = getGroup(subject);
    List<String> inherited = new ArrayList<>();
    Set<String> visited = new HashSet<>();
    collectInherited(primaryGroup, inherited, visited);
    return inherited;
  }

  @Override
  public boolean groupExists(String group) {
    return roles.containsKey(group);
  }

  private void collectInherited(String role, List<String> result, Set<String> visited) {
    if (role == null || !roles.containsKey(role) || !visited.add(role)) return;
    result.add(role);
    RoleView r = roles.get(role);
    for (String parent : r.inherits) {
      collectInherited(parent, result, visited);
    }
  }

  private boolean resolve(String role, String node, Set<String> visited) {
    if (role == null || !roles.containsKey(role) || !visited.add(role)) return false;
    RoleView r = roles.get(role);

    // Explicit deny beats allow
    if (match(r.deny, node)) return false;
    if (match(r.allow, node)) return true;

    // Inherit
    for (String p : r.inherits) {
      if (resolve(p, node, visited)) return true;
    }
    return false;
  }

  // Support simple wildcard matching: "sentinelcore.*"
  private boolean match(Set<String> patterns, String node) {
    for (String p : patterns) {
      if (p.endsWith(".*")) {
        String prefix = p.substring(0, p.length() - 2);
        if (node.startsWith(prefix)) return true;
      } else if (p.equalsIgnoreCase(node)) {
        return true;
      }
    }
    return false;
  }
}
