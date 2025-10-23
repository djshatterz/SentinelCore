package org.github.shatterz.sentinelcore.perm.memory;

import org.github.shatterz.sentinelcore.config.CoreConfig;
import org.github.shatterz.sentinelcore.perm.PermissionService;

import java.util.*;

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

    public MemoryPermissionService(CoreConfig cfg) { reload(cfg); }

    public void reload(CoreConfig cfg) {
        roles.clear();
        userRoles.clear();
        if (cfg.permissions != null) {
            defaultRole = Optional.ofNullable(cfg.permissions.defaultRole).orElse("default");
            if (cfg.permissions.roles != null) {
                cfg.permissions.roles.forEach((name, role) -> {
                    RoleView v = new RoleView();
                    if (role.allow != null) v.allow.addAll(role.allow);
                    if (role.deny != null) v.deny.addAll(role.deny);
                    if (role.inherits != null) v.inherits.addAll(role.inherits);
                    roles.put(name, v);
                });
            }
            if (cfg.permissions.userRoles != null) {
                cfg.permissions.userRoles.forEach((uuid, role) -> {
                    try { userRoles.put(UUID.fromString(uuid), role); } catch (Exception ignored) {}
                });
            }
        }
    }

    @Override public String name() { return name; }

    @Override
    public boolean check(UUID subject, String node, Map<String, String> ctx) {
        // resolve subject role
        String role = userRoles.getOrDefault(subject, defaultRole);
        Set<String> visited = new HashSet<>();
        return resolve(role, node, visited);
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
