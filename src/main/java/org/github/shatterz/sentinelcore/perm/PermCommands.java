package org.github.shatterz.sentinelcore.perm;

import static com.mojang.brigadier.arguments.StringArgumentType.greedyString;
import static com.mojang.brigadier.arguments.StringArgumentType.word;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.github.shatterz.sentinelcore.config.ConfigManager;

public final class PermCommands {
  private PermCommands() {}

  /** Register commands via Fabric’s v2 command callback. */
  public static void register() {
    CommandRegistrationCallback.EVENT.register(
        (dispatcher, registryAccess, environment) -> register(dispatcher));
  }

  private static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
    // /sccore perm test <node>
    LiteralArgumentBuilder<ServerCommandSource> testCmd =
        literal("test")
            .then(
                argument("node", greedyString())
                    .executes(
                        ctx -> {
                          ServerCommandSource src = ctx.getSource();
                          ServerPlayerEntity player = src.getPlayer();
                          if (player == null) {
                            src.sendFeedback(() -> Text.literal("Player-only for now."), false);
                            return 1;
                          }
                          String node = ctx.getArgument("node", String.class);
                          UUID uuid = player.getUuid();
                          boolean ok = Perms.check(uuid, node, Map.of());
                          src.sendFeedback(() -> Text.literal("perm(" + node + ") = " + ok), false);
                          return ok ? 1 : 0;
                        }));

    // /sccore perm reload
    LiteralArgumentBuilder<ServerCommandSource> reloadCmd =
        literal("reload")
            .requires(src -> src.hasPermissionLevel(3))
            .executes(
                ctx -> {
                  boolean ok = ConfigManager.reloadNow();
                  if (ok) {
                    ctx.getSource()
                        .sendFeedback(() -> Text.literal("SentinelCore config reloaded."), true);
                    return 1;
                  } else {
                    ctx.getSource()
                        .sendError(Text.literal("Reload failed. Check console for details."));
                    return 0;
                  }
                });

    // /sccore perm role <player> <role>
    LiteralArgumentBuilder<ServerCommandSource> roleCmd =
        literal("role")
            .requires(src -> src.hasPermissionLevel(3))
            .then(
                argument("player", EntityArgumentType.player())
                    .then(
                        argument("role", word())
                            .executes(
                                ctx -> {
                                  ServerCommandSource src = ctx.getSource();
                                  ServerPlayerEntity target =
                                      EntityArgumentType.getPlayer(ctx, "player");
                                  String role = ctx.getArgument("role", String.class);
                                  boolean ok = ConfigManager.setUserRole(target.getUuid(), role);
                                  if (ok) {
                                    src.sendFeedback(
                                        () ->
                                            Text.literal(
                                                "Assigned role '"
                                                    + role
                                                    + "' to "
                                                    + target.getName().getString()
                                                    + "."),
                                        true);
                                    return 1;
                                  } else {
                                    src.sendError(
                                        Text.literal("Failed to assign role. See console."));
                                    return 0;
                                  }
                                })));

    // /sccore perm ...
    LiteralArgumentBuilder<ServerCommandSource> permRoot =
        literal("perm").then(testCmd).then(reloadCmd).then(roleCmd);

    // /sccore ...
    LiteralArgumentBuilder<ServerCommandSource> root = literal("sccore").then(permRoot);

    dispatcher.register(root);
  }
}
