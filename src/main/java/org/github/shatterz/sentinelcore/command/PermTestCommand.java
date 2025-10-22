package org.github.shatterz.sentinelcore.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import org.github.shatterz.sentinelcore.core.PermissionMgr;

public final class PermTestCommand {
  private PermTestCommand() {}

  public static void register() {
    CommandRegistrationCallback.EVENT.register(
        (dispatcher, registryAccess, env) -> register(dispatcher));
  }

  private static void register(CommandDispatcher<ServerCommandSource> d) {
    d.register(
        CommandManager.literal("sccore")
            .then(
                CommandManager.literal("perm")
                    .then(
                        CommandManager.argument("node", StringArgumentType.greedyString())
                            .executes(
                                ctx -> {
                                  var src = ctx.getSource();
                                  var ent = src.getEntity();
                                  if (ent
                                      instanceof
                                      net.minecraft.server.network.ServerPlayerEntity
                                      p) {
                                    String node = StringArgumentType.getString(ctx, "node");
                                    boolean ok = PermissionMgr.has(p, node);
                                    src.sendFeedback(
                                        () -> Text.literal("[SentinelCore] " + node + " = " + ok),
                                        false);
                                    return ok ? 1 : 0;
                                  } else {
                                    src.sendFeedback(
                                        () -> Text.literal("[SentinelCore] Must be a player."),
                                        false);
                                    return 0;
                                  }
                                }))));
  }
}
