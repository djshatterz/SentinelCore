package org.github.shatterz.sentinelcore.perm.events;

import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import org.github.shatterz.sentinelcore.log.SentinelCategories;
import org.github.shatterz.sentinelcore.log.SentinelLogger;
import org.github.shatterz.sentinelcore.names.NameFormatter;
import org.github.shatterz.sentinelcore.perm.PermissionManager;
import org.github.shatterz.sentinelcore.perm.RoleContextManager;
import org.slf4j.Logger;

/** Listens for player join/leave events to manage RoleContexts. */
public final class PlayerConnectionListener {
  private static final Logger LOG = SentinelLogger.cat(SentinelCategories.PERM);

  private PlayerConnectionListener() {}

  public static void register() {
    // Player join - create and initialize RoleContext
    ServerPlayConnectionEvents.JOIN.register(
        (handler, sender, server) -> {
          var player = handler.getPlayer();
          var uuid = player.getUuid();

          // Initialize RoleContext
          var ctx = RoleContextManager.get(uuid);

          // Load group from permission service
          String group = PermissionManager.getGroup(uuid);
          ctx.setGroup(group);

          // Check op status (use hasPermissionLevel as a proxy for op status)
          boolean isOp = player.hasPermissionLevel(2); // Level 2 = operator
          ctx.setOp(isOp);

          // Set world context
          String worldId = player.getEntityWorld().getRegistryKey().getValue().toString();
          ctx.setWorldContext(worldId);

          // Ensure display name is applied after context is fully initialized (group/op/world)
          NameFormatter.updateDisplayName(player);

          LOG.info(
              "Player {} joined - initialized RoleContext: {}", player.getName().getString(), ctx);
        });

    // Player leave - cleanup RoleContext
    ServerPlayConnectionEvents.DISCONNECT.register(
        (handler, server) -> {
          var player = handler.getPlayer();
          var uuid = player.getUuid();

          RoleContextManager.remove(uuid);
          LOG.debug("Player {} left - removed RoleContext", player.getName().getString());
        });

    LOG.info("Player connection listeners registered");
  }
}
