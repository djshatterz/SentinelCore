package org.github.shatterz.sentinelcore.core;

import net.minecraft.server.network.ServerPlayerEntity;

public class RoleContext {

    public enum Role {
        DEFAULT, MODERATOR, ADMIN, DEVELOPER, OP
    }

    public static Role get(ServerPlayerEntity player) {
        if (player.hasPermissionLevel(4)) return Role.OP;
        if (PermissionMgr.has(player, "sentinelcore.developer")) return Role.DEVELOPER;
        if (PermissionMgr.has(player, "sentinelcore.admin")) return Role.ADMIN;
        if (PermissionMgr.has(player, "sentinelcore.moderator")) return Role.MODERATOR;
        return Role.DEFAULT;
    }
}
