package org.github.shatterz.sentinelcore.core;

import net.minecraft.server.network.ServerPlayerEntity;

/** Lightweight role context; will expand when group system/mod-mode lands. */
public final class RoleContext {
  private RoleContext() {}

  public static boolean isOp(ServerPlayerEntity player) {
    return player.hasPermissionLevel(2);
  }

  public static boolean isStaff(ServerPlayerEntity player) {
    // placeholder: admin/op considered staff for now; later include moderator
    return isOp(player);
  }

  public static boolean isInModMode(ServerPlayerEntity player) {
    // stub — real state will live in a capability/datatracker
    return false;
  }
}
