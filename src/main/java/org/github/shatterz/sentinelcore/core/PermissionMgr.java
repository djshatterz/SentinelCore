package org.github.shatterz.sentinelcore.core;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import org.github.shatterz.sentinelcore.SentinelCore;
import net.minecraft.server.network.ServerPlayerEntity;

public class PermissionMgr {
    private static LuckPerms luckPerms;

    public static void init() {
        try {
            luckPerms = LuckPermsProvider.get();
            SentinelCore.LOGGER.info("[PermissionMgr] LuckPerms hook initialized.");
        } catch (IllegalStateException e) {
            SentinelCore.LOGGER.warn("[PermissionMgr] LuckPerms not found — fallback to default perms.");
            luckPerms = null;
        }
    }

    public static boolean has(ServerPlayerEntity player, String node) {
        if (luckPerms == null) return player.hasPermissionLevel(4); // op fallback
        User user = luckPerms.getPlayerAdapter(ServerPlayerEntity.class).getUser(player);
        return user != null && user.getCachedData().getPermissionData().checkPermission(node).asBoolean();
    }

    public static boolean isAdmin(ServerPlayerEntity player) {
        return has(player, "sentinelcore.admin") || player.hasPermissionLevel(4);
    }
}
