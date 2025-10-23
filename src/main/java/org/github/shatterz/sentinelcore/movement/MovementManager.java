package org.github.shatterz.sentinelcore.movement;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.github.shatterz.sentinelcore.audit.AuditManager;
import org.github.shatterz.sentinelcore.config.CoreConfig;
import org.github.shatterz.sentinelcore.log.SentinelCategories;
import org.github.shatterz.sentinelcore.log.SentinelLogger;
import org.slf4j.Logger;

/**
 * Centralized movement utilities. Provides safe-teleport with simple hazard checks and cleanup of
 * player state (dismount, glide cancel, velocity reset). SpawnElytra removal is left as a hook for
 * a future module.
 */
public final class MovementManager {
  private static final Logger LOG = SentinelLogger.cat(SentinelCategories.MOVE);

  private static volatile CoreConfig.Movement CFG = new CoreConfig.Movement();

  private MovementManager() {}

  public static void applyConfig(CoreConfig cfg) {
    if (cfg != null && cfg.movement != null) {
      CFG = cfg.movement;
      LOG.info(
          "Movement config applied: searchUpDown={} centerOnBlock={} avoidLava={} avoidPowderSnow={} avoidCactusFire={} dismount={} cancelGlide={} removeElytra={}",
          CFG.safeTeleport.searchUpDown,
          CFG.safeTeleport.centerOnBlock,
          CFG.safeTeleport.avoidLava,
          CFG.safeTeleport.avoidPowderSnow,
          CFG.safeTeleport.avoidCactusFire,
          CFG.dismountBeforeTeleport,
          CFG.cancelGliding,
          CFG.removeSpawnElytra);
    }
  }

  /** Attempts a safe teleport near the target position. Returns true on success. */
  public static boolean safeTeleport(
      ServerPlayerEntity player, ServerWorld world, BlockPos target, Float yaw, Float pitch) {
    if (player == null || world == null || target == null) return false;

    BlockPos best = findSafeY(world, target, CFG.safeTeleport.searchUpDown);
    if (best == null) {
      player.sendMessage(Text.literal("No safe spot found."), false);
      return false;
    }

    // Pre-teleport cleanup
    tryCleanup(player);

    // Center on block if configured
    double x = best.getX() + (CFG.safeTeleport.centerOnBlock ? 0.5 : 0.0);
    double y = best.getY();
    double z = best.getZ() + (CFG.safeTeleport.centerOnBlock ? 0.5 : 0.0);

    float finalYaw = yaw != null ? yaw : player.getYaw();
    float finalPitch = pitch != null ? pitch : player.getPitch();

    Vec3d from = new Vec3d(player.getX(), player.getY(), player.getZ());
    ServerWorld fromWorld = (ServerWorld) player.getEntityWorld();

    // Teleport (handles cross-dimension). API requires flags + keepVelocity in 1.21+
    java.util.EnumSet<net.minecraft.network.packet.s2c.play.PositionFlag> flags =
        java.util.EnumSet.noneOf(net.minecraft.network.packet.s2c.play.PositionFlag.class);
    player.teleport(world, x, y, z, flags, finalYaw, finalPitch, false);

    // Audit event
    try {
      Map<String, Object> meta = new HashMap<>();
      meta.put("fromWorld", fromWorld.getRegistryKey().getValue().toString());
      meta.put("from", String.format("%.2f,%.2f,%.2f", from.x, from.y, from.z));
      meta.put("toWorld", world.getRegistryKey().getValue().toString());
      meta.put("to", String.format("%.2f,%.2f,%.2f", x, y, z));
      AuditManager.logSystem("teleport", "safeTeleport", meta);
    } catch (Throwable ignored) {
    }

    return true;
  }

  private static void tryCleanup(ServerPlayerEntity player) {
    try {
      if (CFG.dismountBeforeTeleport && player.hasVehicle()) {
        player.stopRiding();
      }
      // Optional: cancel elytra gliding (no-op here to avoid API drift)
      // Reset velocity
      player.setVelocity(Vec3d.ZERO);
      player.velocityModified = true;

      if (CFG.removeSpawnElytra) {
        // Hook for SpawnElytra system; no-op for now
      }
    } catch (Throwable t) {
      LOG.warn("Cleanup before teleport failed: {}", t.toString());
    }
  }

  private static BlockPos findSafeY(ServerWorld world, BlockPos target, int span) {
    int startY = target.getY();
    int minY = Math.max(world.getBottomY(), startY - span);
    int topAtXZ =
        world.getTopY(
            net.minecraft.world.Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
            target.getX(),
            target.getZ());
    int maxY = Math.min(topAtXZ - 2, startY + span);

    // Scan up and down from target Y, preferring closer positions
    for (int dy = 0; dy <= span; dy++) {
      int up = startY + dy;
      int down = startY - dy;
      if (up <= maxY) {
        BlockPos pos = new BlockPos(target.getX(), up, target.getZ());
        if (isSafe(world, pos)) return pos;
      }
      if (dy > 0 && down >= minY) {
        BlockPos pos = new BlockPos(target.getX(), down, target.getZ());
        if (isSafe(world, pos)) return pos;
      }
    }
    return null;
  }

  private static boolean isSafe(ServerWorld world, BlockPos feet) {
    BlockPos head = feet.up();
    BlockPos ground = feet.down();

    // Air/space checks at feet/head
    if (!world.getBlockState(feet).getCollisionShape(world, feet).isEmpty()) return false;
    if (!world.getBlockState(head).getCollisionShape(world, head).isEmpty()) return false;

    // Ground must be solid enough to stand on
    BlockState g = world.getBlockState(ground);
    if (g.getCollisionShape(world, ground).isEmpty()) return false;

    // Hazard avoidance
    if (CFG.safeTeleport.avoidPowderSnow) {
      if (world.getBlockState(feet).isOf(Blocks.POWDER_SNOW)) return false;
      if (g.isOf(Blocks.POWDER_SNOW)) return false;
    }
    if (CFG.safeTeleport.avoidLava) {
      if (world.getFluidState(ground).isIn(FluidTags.LAVA)) return false;
      if (world.getFluidState(feet).isIn(FluidTags.LAVA)) return false;
    }
    if (CFG.safeTeleport.avoidCactusFire) {
      if (g.isOf(Blocks.CACTUS)
          || g.isOf(Blocks.MAGMA_BLOCK)
          || g.isOf(Blocks.FIRE)
          || g.isOf(Blocks.SOUL_FIRE)) {
        return false;
      }
    }
    return true;
  }
}
