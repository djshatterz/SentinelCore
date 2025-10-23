package org.github.shatterz.sentinelcore.movement;

import static com.mojang.brigadier.arguments.DoubleArgumentType.doubleArg;
import static com.mojang.brigadier.arguments.DoubleArgumentType.getDouble;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import org.github.shatterz.sentinelcore.perm.PermissionManager;

/** Simple test commands for MovementManager: /scmove safe <x> <y> <z> */
public final class MoveCommands {
  private MoveCommands() {}

  public static void register() {
    CommandRegistrationCallback.EVENT.register(
        (dispatcher, registryAccess, environment) -> register(dispatcher));
  }

  private static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
    dispatcher.register(
        literal("scmove")
            .requires(src -> has(src, "sentinelcore.movement.teleport.safe"))
            .then(
                literal("safe")
                    .then(
                        argument("x", doubleArg())
                            .then(
                                argument("y", doubleArg())
                                    .then(
                                        argument("z", doubleArg())
                                            .executes(
                                                ctx -> {
                                                  ServerPlayerEntity p =
                                                      ctx.getSource().getPlayer();
                                                  if (p == null) {
                                                    ctx.getSource()
                                                        .sendError(Text.literal("Players only."));
                                                    return 0;
                                                  }
                                                  ServerWorld world =
                                                      (ServerWorld) p.getEntityWorld();
                                                  double x = getDouble(ctx, "x");
                                                  double y = getDouble(ctx, "y");
                                                  double z = getDouble(ctx, "z");
                                                  BlockPos pos = BlockPos.ofFloored(x, y, z);
                                                  boolean ok =
                                                      MovementManager.safeTeleport(
                                                          p, world, pos, null, null);
                                                  if (ok) {
                                                    ctx.getSource()
                                                        .sendFeedback(
                                                            () ->
                                                                Text.literal(
                                                                    "Teleported safely to nearest spot."),
                                                            false);
                                                    return 1;
                                                  } else {
                                                    return 0;
                                                  }
                                                }))))));
  }

  private static boolean has(ServerCommandSource src, String node) {
    if (src.getPlayer() == null) return src.hasPermissionLevel(3);
    return PermissionManager.has(src.getPlayer(), node) || src.hasPermissionLevel(3);
  }
}
