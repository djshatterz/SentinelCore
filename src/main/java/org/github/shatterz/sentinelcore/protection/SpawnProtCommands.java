package org.github.shatterz.sentinelcore.protection;

import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;
import org.github.shatterz.sentinelcore.config.ConfigManager;
import org.github.shatterz.sentinelcore.config.CoreConfig;
import org.github.shatterz.sentinelcore.perm.PermissionManager;

/**
 * Admin commands for spawn protection: /spawnprot
 * info|enable|disable|setcenter|setradius|setbottomy
 */
public final class SpawnProtCommands {
  private SpawnProtCommands() {}

  public static void register() {
    CommandRegistrationCallback.EVENT.register(
        (dispatcher, registryAccess, environment) -> register(dispatcher));
  }

  private static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
    dispatcher.register(
        literal("spawnprot")
            .then(
                literal("info")
                    .requires(src -> has(src, "sentinelcore.spawnprot.info"))
                    .executes(
                        ctx -> {
                          CoreConfig.SpawnProtection cfg = SpawnProtectionManager.getConfig();
                          Vec3d center = SpawnProtectionManager.getCenter();
                          String world = SpawnProtectionManager.getWorldId();
                          ctx.getSource()
                              .sendFeedback(
                                  () -> Text.literal("Spawn Protection").formatted(Formatting.AQUA),
                                  false);
                          ctx.getSource()
                              .sendFeedback(
                                  () -> Text.literal(String.format("  Enabled: %s", cfg.enabled)),
                                  false);
                          ctx.getSource()
                              .sendFeedback(
                                  () -> Text.literal(String.format("  Shape: %s", cfg.shape)),
                                  false);
                          ctx.getSource()
                              .sendFeedback(
                                  () ->
                                      Text.literal(
                                          String.format(
                                              "  Center: %s @ %.1f,%.1f,%.1f",
                                              world, center.x, center.y, center.z)),
                                  false);
                          ctx.getSource()
                              .sendFeedback(
                                  () -> Text.literal(String.format("  Radius: %d", cfg.radius)),
                                  false);
                          ctx.getSource()
                              .sendFeedback(
                                  () -> Text.literal(String.format("  Bottom Y: %d", cfg.bottomY)),
                                  false);
                          return 1;
                        }))
            .then(
                literal("enable")
                    .requires(src -> has(src, "sentinelcore.spawnprot.admin"))
                    .executes(
                        ctx -> {
                          CoreConfig cfg = ConfigManager.get();
                          cfg.spawnProtection.enabled = true;
                          try {
                            ConfigManager.saveYAML(cfg);
                            SpawnProtectionManager.applyConfig(cfg);
                            ctx.getSource()
                                .sendFeedback(
                                    () ->
                                        Text.literal("Spawn protection enabled.")
                                            .formatted(Formatting.GREEN),
                                    true);
                            return 1;
                          } catch (Exception e) {
                            ctx.getSource().sendError(Text.literal("Failed to save config."));
                            return 0;
                          }
                        }))
            .then(
                literal("disable")
                    .requires(src -> has(src, "sentinelcore.spawnprot.admin"))
                    .executes(
                        ctx -> {
                          CoreConfig cfg = ConfigManager.get();
                          cfg.spawnProtection.enabled = false;
                          try {
                            ConfigManager.saveYAML(cfg);
                            SpawnProtectionManager.applyConfig(cfg);
                            ctx.getSource()
                                .sendFeedback(
                                    () ->
                                        Text.literal("Spawn protection disabled.")
                                            .formatted(Formatting.YELLOW),
                                    true);
                            return 1;
                          } catch (Exception e) {
                            ctx.getSource().sendError(Text.literal("Failed to save config."));
                            return 0;
                          }
                        }))
            .then(
                literal("setcenter")
                    .requires(src -> has(src, "sentinelcore.spawnprot.admin"))
                    .executes(
                        ctx -> {
                          ServerPlayerEntity p = ctx.getSource().getPlayer();
                          if (p == null) {
                            ctx.getSource().sendError(Text.literal("Players only."));
                            return 0;
                          }
                          CoreConfig cfg = ConfigManager.get();
                          String world = p.getEntityWorld().getRegistryKey().getValue().toString();
                          String centerStr =
                              String.format(
                                  "%s:%d,%d,%d",
                                  world, (int) p.getX(), (int) p.getY(), (int) p.getZ());
                          cfg.spawnProtection.center = centerStr;
                          try {
                            ConfigManager.saveYAML(cfg);
                            SpawnProtectionManager.applyConfig(cfg);
                            ctx.getSource()
                                .sendFeedback(
                                    () ->
                                        Text.literal("Spawn protection center set to: " + centerStr)
                                            .formatted(Formatting.GREEN),
                                    true);
                            return 1;
                          } catch (Exception e) {
                            ctx.getSource().sendError(Text.literal("Failed to save config."));
                            return 0;
                          }
                        }))
            .then(
                literal("setradius")
                    .requires(src -> has(src, "sentinelcore.spawnprot.admin"))
                    .then(
                        argument("blocks", integer(1, 500))
                            .executes(
                                ctx -> {
                                  int r = getInteger(ctx, "blocks");
                                  CoreConfig cfg = ConfigManager.get();
                                  cfg.spawnProtection.radius = r;
                                  try {
                                    ConfigManager.saveYAML(cfg);
                                    SpawnProtectionManager.applyConfig(cfg);
                                    ctx.getSource()
                                        .sendFeedback(
                                            () ->
                                                Text.literal("Spawn protection radius set to: " + r)
                                                    .formatted(Formatting.GREEN),
                                            true);
                                    return 1;
                                  } catch (Exception e) {
                                    ctx.getSource()
                                        .sendError(Text.literal("Failed to save config."));
                                    return 0;
                                  }
                                })))
            .then(
                literal("setbottomy")
                    .requires(src -> has(src, "sentinelcore.spawnprot.admin"))
                    .then(
                        argument("y", integer(-64, 320))
                            .executes(
                                ctx -> {
                                  int y = getInteger(ctx, "y");
                                  CoreConfig cfg = ConfigManager.get();
                                  cfg.spawnProtection.bottomY = y;
                                  try {
                                    ConfigManager.saveYAML(cfg);
                                    SpawnProtectionManager.applyConfig(cfg);
                                    ctx.getSource()
                                        .sendFeedback(
                                            () ->
                                                Text.literal(
                                                        "Spawn protection bottom Y set to: " + y)
                                                    .formatted(Formatting.GREEN),
                                            true);
                                    return 1;
                                  } catch (Exception e) {
                                    ctx.getSource()
                                        .sendError(Text.literal("Failed to save config."));
                                    return 0;
                                  }
                                }))));
  }

  private static boolean has(ServerCommandSource src, String node) {
    if (src.getPlayer() == null) return src.hasPermissionLevel(3);
    return PermissionManager.has(src.getPlayer(), node) || src.hasPermissionLevel(3);
  }
}
