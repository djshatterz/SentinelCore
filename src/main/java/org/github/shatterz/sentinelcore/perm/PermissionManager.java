package org.github.shatterz.sentinelcore.perm;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.network.ServerPlayerEntity;
import org.github.shatterz.sentinelcore.log.SentinelCategories;
import org.github.shatterz.sentinelcore.log.SentinelLogger;
import org.slf4j.Logger;

/**
 * High-level permission manager that bridges LuckPerms + SentinelCore groups and vanilla ops.
 * Provides context-aware permission checking using RoleContext.
 */
public final class PermissionManager {
  private static final Logger LOG = SentinelLogger.cat(SentinelCategories.PERM);

  private PermissionManager() {}

  /**
   * Check if a player has a permission node. Considers: RoleContext (group, modmode, etc.), op
   * status, and underlying permission service.
   */
  public static boolean has(ServerPlayerEntity player, String node) {
    return has(player.getUuid(), node, buildContext(player));
  }

  /** Check if a player has a permission node with custom context. */
  public static boolean has(UUID uuid, String node, Map<String, String> context) {
    // Check underlying permission service
    boolean hasPermission = Perms.check(uuid, node, context);

    if (hasPermission) {
      LOG.debug("Permission check ALLOW: {} -> {}", uuid, node);
      return true;
    }

    // Check if player is op (ops bypass permission checks for certain nodes)
    RoleContext ctx = RoleContextManager.get(uuid);
    if (ctx.isOp() && shouldOpBypass(node)) {
      LOG.debug("Permission check ALLOW (op bypass): {} -> {}", uuid, node);
      return true;
    }

    LOG.debug("Permission check DENY: {} -> {}", uuid, node);
    return false;
  }

  /** Check if a player has a permission node (UUID-only version). */
  public static boolean has(UUID uuid, String node) {
    RoleContext ctx = RoleContextManager.get(uuid);
    return has(uuid, node, ctx.getContextFlags());
  }

  /** Build context map from player's RoleContext. */
  private static Map<String, String> buildContext(ServerPlayerEntity player) {
    RoleContext ctx = RoleContextManager.get(player);
    Map<String, String> context = new HashMap<>(ctx.getContextFlags());

    // Add world context
    String worldId = player.getEntityWorld().getRegistryKey().getValue().toString();
    context.put("world", worldId);

    return context;
  }

  /**
   * Determine if op status should bypass permission check for this node. By default, ops bypass
   * most checks, but you can add exceptions here.
   */
  private static boolean shouldOpBypass(String node) {
    // Ops should NOT bypass certain dangerous operations
    if (node.contains(".admin.dangerous")) {
      return false;
    }
    // Ops bypass most other permission checks
    return true;
  }

  /** Get the effective group for a player (from permission service + RoleContext). */
  public static String getGroup(UUID uuid) {
    PermissionService svc = getService();
    if (svc != null) {
      return svc.getGroup(uuid);
    }
    return "default";
  }

  /** Set the group for a player. */
  public static void setGroup(UUID uuid, String group) {
    PermissionService svc = getService();
    if (svc != null) {
      svc.setGroup(uuid, group);
      RoleContextManager.setGroup(uuid, group);
    } else {
      LOG.warn("Cannot set group for {}: no permission service available", uuid);
    }
  }

  /** Get all groups a player inherits from. */
  public static List<String> getInheritedGroups(UUID uuid) {
    PermissionService svc = getService();
    if (svc != null) {
      return svc.getInheritedGroups(uuid);
    }
    return List.of("default");
  }

  /** Check if a group exists. */
  public static boolean groupExists(String group) {
    PermissionService svc = getService();
    return svc != null && svc.groupExists(group);
  }

  /** Get the current permission service (internal use). */
  private static PermissionService getService() {
    return Perms.getService();
  }
}
