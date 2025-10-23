package org.github.shatterz.sentinelcore.names;

import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import org.github.shatterz.sentinelcore.log.SentinelLogger;
import org.github.shatterz.sentinelcore.perm.events.PermissionContextChangedCallback;
import org.slf4j.Logger;

/** Listens for player join and permission context changes to update display names in real-time. */
public final class NameUpdateListener {
  private static final Logger LOG = SentinelLogger.root();

  private NameUpdateListener() {}

  /** Register event listeners for automatic display name updates. */
  public static void register() {
    // Update display name on player join
    ServerPlayConnectionEvents.JOIN.register(
        (handler, sender, server) -> {
          ServerPlayerEntity player = handler.getPlayer();
          NameFormatter.updateDisplayName(player);
          LOG.debug("Updated display name for {} on join.", player.getName().getString());
        });

    // Update display name when permission context changes (group change, mod-mode toggle, etc.)
    PermissionContextChangedCallback.EVENT.register(
        (player, changeType) -> {
          NameFormatter.updateDisplayName(player);
          LOG.debug(
              "Updated display name for {} due to context change: {}",
              player.getName().getString(),
              changeType);
        });

    LOG.info("Name update listener registered.");
  }
}
