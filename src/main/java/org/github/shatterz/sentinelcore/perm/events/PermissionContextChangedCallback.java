package org.github.shatterz.sentinelcore.perm.events;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Event fired when a player's permission context changes. This includes group changes, op status
 * changes, mod-mode toggles, etc.
 */
public interface PermissionContextChangedCallback {
  Event<PermissionContextChangedCallback> EVENT =
      EventFactory.createArrayBacked(
          PermissionContextChangedCallback.class,
          (listeners) ->
              (player, changeType) -> {
                for (PermissionContextChangedCallback listener : listeners) {
                  listener.onContextChanged(player, changeType);
                }
              });

  /**
   * Called when a player's permission context changes.
   *
   * @param player the player whose context changed
   * @param changeType the type of change that occurred
   */
  void onContextChanged(ServerPlayerEntity player, ChangeType changeType);

  enum ChangeType {
    GROUP_CHANGED,
    OP_CHANGED,
    MODMODE_CHANGED,
    VANISH_CHANGED,
    PERMISSION_CHANGED
  }
}
