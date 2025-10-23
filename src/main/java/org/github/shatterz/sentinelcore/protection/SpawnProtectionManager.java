package org.github.shatterz.sentinelcore.protection;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.github.shatterz.sentinelcore.config.CoreConfig;
import org.github.shatterz.sentinelcore.log.SentinelCategories;
import org.github.shatterz.sentinelcore.log.SentinelLogger;
import org.github.shatterz.sentinelcore.perm.PermissionManager;
import org.slf4j.Logger;

/**
 * Protects spawn region from building, explosions, and grief. Allows creative-mode bypass and
 * whitelisted interactions.
 */
public final class SpawnProtectionManager {
  private static final Logger LOG = SentinelLogger.cat(SentinelCategories.SPAWN);
  private static final Pattern CENTER_PATTERN =
      Pattern.compile("([^:]+:[^:]+):(-?\\d+),(-?\\d+),(-?\\d+)");

  private static volatile CoreConfig.SpawnProtection CFG = new CoreConfig.SpawnProtection();
  private static volatile Vec3d CENTER = new Vec3d(0, 100, 0);
  private static volatile String WORLD_ID = "world";

  private SpawnProtectionManager() {}

  public static void applyConfig(CoreConfig cfg) {
    if (cfg != null && cfg.spawnProtection != null) {
      CFG = cfg.spawnProtection;
      parseCenter(CFG.center);
      LOG.info(
          "SpawnProtection config applied: enabled={} shape={} radius={} bottomY={} center={} world={} permBypass={}",
          CFG.enabled,
          CFG.shape,
          CFG.radius,
          CFG.bottomY,
          CENTER,
          WORLD_ID,
          CFG.allowPermissionBypass);
    }
  }

  public static void registerListeners() {
    // Block break prevention
    PlayerBlockBreakEvents.BEFORE.register(
        (world, player, pos, state, blockEntity) -> {
          if (isProtected(world, pos, player)) {
            if (player instanceof ServerPlayerEntity sp) {
              sp.sendMessage(Text.literal("Spawn is protected."), true);
            }
            return false;
          }
          return true;
        });

    // Block place prevention (handled via AttackBlock for quick-break and UseBlock for place)
    AttackBlockCallback.EVENT.register(
        (player, world, hand, pos, direction) -> {
          if (isProtected(world, pos, player)) {
            if (player instanceof ServerPlayerEntity sp) {
              sp.sendMessage(Text.literal("Spawn is protected."), true);
            }
            return ActionResult.FAIL;
          }
          return ActionResult.PASS;
        });

    // Interact whitelist and block placement prevention
    UseBlockCallback.EVENT.register(
        (player, world, hand, hitResult) -> {
          BlockPos clicked = hitResult.getBlockPos();
          BlockPos target = clicked.offset(hitResult.getSide());
          Item held = player.getStackInHand(hand).getItem();

          // If attempting to place a block, check target placement position
          if (held instanceof BlockItem) {
            if (isProtected(world, target, player) || isProtected(world, clicked, player)) {
              if (player instanceof ServerPlayerEntity sp) {
                sp.sendMessage(Text.literal("Spawn is protected."), true);
              }
              return ActionResult.FAIL;
            }
          }

          // Otherwise, this is an interaction with an existing block; apply whitelist
          if (isProtected(world, clicked, player)) {
            BlockState state = world.getBlockState(clicked);
            String blockId = Registries.BLOCK.getId(state.getBlock()).toString();
            if (!CFG.whitelist.interact.contains(blockId)) {
              if (player instanceof ServerPlayerEntity sp) {
                sp.sendMessage(Text.literal("Spawn is protected."), true);
              }
              return ActionResult.FAIL;
            }
          }
          return ActionResult.PASS;
        });

    LOG.info("SpawnProtection event listeners registered.");
  }

  private static boolean isProtected(World world, BlockPos pos, PlayerEntity player) {
    if (!CFG.enabled) return false;
    if (!(world instanceof ServerWorld sw)) return false;

    // Check world match
    String wid = sw.getRegistryKey().getValue().toString();
    if (!wid.equals(WORLD_ID)) {
      LOG.debug("World mismatch: {} != {}", wid, WORLD_ID);
      return false;
    }

    // Creative or admin bypass
    if (player.isCreative()) {
      LOG.debug("Bypass (creative) at {} for player {}", pos, player.getName().getString());
      return false;
    }
    if (CFG.allowPermissionBypass && player instanceof ServerPlayerEntity sp) {
      if (PermissionManager.has(sp, "sentinelcore.spawnprot.bypass")) {
        LOG.debug("Bypass (permission) at {} for player {}", pos, player.getName().getString());
        return false;
      }
    }

    // Shape check
    if ("cylinder".equalsIgnoreCase(CFG.shape)) {
      double dx = pos.getX() + 0.5 - CENTER.x;
      double dz = pos.getZ() + 0.5 - CENTER.z;
      double distSq = dx * dx + dz * dz;
      double maxDistSq = CFG.radius * CFG.radius;
      if (distSq > maxDistSq) {
        LOG.debug("Outside radius: dist²={} > max²={}", distSq, maxDistSq);
        return false;
      }
      // Bottom Y check
      if (pos.getY() < CFG.bottomY) {
        LOG.debug("Below bottom Y: {} < {}", pos.getY(), CFG.bottomY);
        return false;
      }
      LOG.debug("Protection active at {} for player {}", pos, player.getName().getString());
    }
    // Add sphere support if needed

    return true;
  }

  private static void parseCenter(String s) {
    Matcher m = CENTER_PATTERN.matcher(s);
    if (m.matches()) {
      WORLD_ID = m.group(1);
      double x = Double.parseDouble(m.group(2));
      double y = Double.parseDouble(m.group(3));
      double z = Double.parseDouble(m.group(4));
      CENTER = new Vec3d(x, y, z);
    } else {
      LOG.warn("Invalid spawn protection center format: {}", s);
      WORLD_ID = "minecraft:overworld";
      CENTER = new Vec3d(0, 100, 0);
    }
  }

  public static CoreConfig.SpawnProtection getConfig() {
    return CFG;
  }

  public static Vec3d getCenter() {
    return CENTER;
  }

  public static String getWorldId() {
    return WORLD_ID;
  }
}
