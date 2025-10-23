package org.github.shatterz.sentinelcore.perm;
import org.github.shatterz.sentinelcore.config.ConfigManager;


import static com.mojang.brigadier.arguments.StringArgumentType.string;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

import com.mojang.brigadier.CommandDispatcher;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public final class PermCommands {
  private PermCommands() {}

  public static void register() {
    CommandRegistrationCallback.EVENT.register(
        (dispatcher, registryAccess, env) -> register(dispatcher));
  }

    private static void register(CommandDispatcher<ServerCommandSource> d) {
        d.register(literal("sccore")
                .then(literal("perm")
                        // /sccore perm test <node>  (existing)
                        .then(literal("test")
                                .then(argument("node", string())
                                        .executes(ctx -> {
                                            ServerCommandSource src = ctx.getSource();
                                            ServerPlayerEntity player = src.getPlayer();
                                            if (player == null) {
                                                src.sendFeedback(() -> Text.literal("Player-only for now."), false);
                                                return 1;
                                            }
                                            String node = ctx.getArgument("node", String.class);
                                            boolean ok = Perms.check(player.getUuid(), node, Map.of());
                                            src.sendFeedback(() -> Text.literal("perm(" + node + ") = " + ok), false);
                                            return ok ? 1 : 0;
                                        })))
                        // /sccore perm reload  (new)
                        .then(literal("reload")
                                .requires(src -> src.hasPermissionLevel(3))
                                .executes(ctx -> {
                                    boolean ok = ConfigManager.reloadNow();
                                    if (ok) {
                                        ctx.getSource().sendFeedback(() -> Text.literal("SentinelCore config reloaded."), true);
                                        return 1;
                                    } else {
                                        ctx.getSource().sendError(Text.literal("Reload failed. Check console for details."));
                                        return 0;
                                    }
                                }))
                )
        );
    }
