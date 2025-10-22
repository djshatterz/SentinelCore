package org.github.shatterz.sentinelcore.core;

import net.fabricmc.loader.api.FabricLoader;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.cacheddata.CachedPermissionData;
import net.minecraft.server.network.ServerPlayerEntity;
import org.github.shatterz.sentinelcore.SentinelCore;
import org.jetbrains.annotations.NotNull;

public final class PermissionMgr {
  private static volatile LuckPerms LP = null;

  private PermissionMgr() {}

  public static void init() {
    try {
      // Only try to use LP if the mod is actually loaded at runtime
      if (FabricLoader.getInstance().isModLoaded("luckperms")) {
        LP = LuckPermsProvider.get();
        SentinelCore.LOGGER.info("[SentinelCore/Perm] LuckPerms detected.");
      } else {
        LP = null;
        SentinelCore.LOGGER.info(
            "[SentinelCore/Perm] LuckPerms not present; using fallback (vanilla op levels).");
      }
    } catch (Throwable t) { // catch NoClassDefFoundError, IllegalStateException, etc.
      LP = null;
      SentinelCore.LOGGER.warn(
          "[SentinelCore/Perm] Failed to initialize LuckPerms bridge; using fallback.", t);
    }
  }

  /** Basic permission check that works with or without LP. */
  public static boolean has(@NotNull ServerPlayerEntity player, @NotNull String node) {
    if (LP != null) {
      var user = LP.getUserManager().getUser(player.getUuid());
      if (user != null) {
        CachedPermissionData perms = user.getCachedData().getPermissionData();
        return perms.checkPermission(node).asBoolean();
      }
      // user not yet loaded -> fall through to fallback
    }
    // Fallback — treat op level >=2 as admin-ish (refine with SC groups later)
    return player.hasPermissionLevel(2);
  }

  /** Convenience: admin-only nodes. */
  public static boolean isAdmin(@NotNull ServerPlayerEntity player) {
    if (LP != null && has(player, "sentinelcore.admin")) return true;
    return player.hasPermissionLevel(2);
  }
}
