package org.github.shatterz.sentinelcore.perm;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.network.ServerPlayerEntity;
import org.github.shatterz.sentinelcore.log.SentinelCategories;
import org.github.shatterz.sentinelcore.log.SentinelLogger;
import org.github.shatterz.sentinelcore.perm.events.PermissionContextChangedCallback;
import org.slf4j.Logger;

/**
 * Manages RoleContext instances for all online players. Provides methods to get, update, and remove
 * player contexts.
 */
public final class RoleContextManager {
  private static final Logger LOG = SentinelLogger.cat(SentinelCategories.PERM);
  private static final Map<UUID, RoleContext> contexts = new ConcurrentHashMap<>();

  private RoleContextManager() {}

  /** Get or create a RoleContext for the given player. */
  public static RoleContext get(UUID uuid) {
    return contexts.computeIfAbsent(uuid, RoleContext::new);
  }

  /** Get or create a RoleContext for the given player. */
  public static RoleContext get(ServerPlayerEntity player) {
    return get(player.getUuid());
  }

  /** Update the group for a player. */
  public static void setGroup(UUID uuid, String group) {
    RoleContext ctx = get(uuid);
    String oldGroup = ctx.getGroup();
    ctx.setGroup(group);
    LOG.info("Player {} group changed: {} -> {}", uuid, oldGroup, group);
    fireEvent(uuid, PermissionContextChangedCallback.ChangeType.GROUP_CHANGED);
  }

  /** Update the op status for a player. */
  public static void setOp(UUID uuid, boolean isOp) {
    RoleContext ctx = get(uuid);
    ctx.setOp(isOp);
    LOG.info("Player {} op status changed: {}", uuid, isOp);
    fireEvent(uuid, PermissionContextChangedCallback.ChangeType.OP_CHANGED);
  }

  /** Update the mod-mode status for a player. */
  public static void setModMode(UUID uuid, boolean modMode) {
    RoleContext ctx = get(uuid);
    ctx.setModMode(modMode);
    LOG.info("Player {} mod-mode changed: {}", uuid, modMode);
    fireEvent(uuid, PermissionContextChangedCallback.ChangeType.MODMODE_CHANGED);
  }

  /** Update the vanish status for a player. */
  public static void setVanished(UUID uuid, boolean vanished) {
    RoleContext ctx = get(uuid);
    ctx.setVanished(vanished);
    LOG.info("Player {} vanish status changed: {}", uuid, vanished);
    fireEvent(uuid, PermissionContextChangedCallback.ChangeType.VANISH_CHANGED);
  }

  /** Remove a player's context (e.g., on disconnect). */
  public static void remove(UUID uuid) {
    RoleContext removed = contexts.remove(uuid);
    if (removed != null) {
      LOG.debug("Removed RoleContext for {}", uuid);
    }
  }

  /** Clear all contexts (e.g., on server shutdown). */
  public static void clear() {
    contexts.clear();
    LOG.info("Cleared all RoleContexts");
  }

  /** Fire a permission context changed event. */
  private static void fireEvent(UUID uuid, PermissionContextChangedCallback.ChangeType changeType) {
    // Try to find the player and fire the event
    // Note: This requires access to the server instance, which we'll need to handle
    // For now, we'll just log that we would fire the event
    LOG.debug("Would fire {} event for player {}", changeType, uuid);
    // TODO: Fire actual event when player instance is available
  }
}
