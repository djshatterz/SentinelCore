package org.github.shatterz.sentinelcore.command;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import org.github.shatterz.sentinelcore.SentinelCore;
import org.github.shatterz.sentinelcore.core.ConfigMgr;
import org.github.shatterz.sentinelcore.core.PermissionMgr;

public final class CoreCommands {
  private CoreCommands() {}

  public static void register() {
    CommandRegistrationCallback.EVENT.register(
        (dispatcher, registryAccess, environment) -> {
          register(dispatcher);
        });
  }

  private static void register(CommandDispatcher<ServerCommandSource> d) {
    d.register(
        CommandManager.literal("sccore")
            .then(
                CommandManager.literal("info")
                    .executes(
                        ctx -> {
                          ctx.getSource()
                              .sendFeedback(
                                  () ->
                                      Text.literal(
                                          "SentinelCore (modid: " + SentinelCore.MOD_ID + ")"),
                                  false);
                          return 1;
                        }))
            .then(
                CommandManager.literal("reload")
                    .requires(
                        src -> {
                          if (src.getEntity()
                              instanceof net.minecraft.server.network.ServerPlayerEntity p) {
                            return PermissionMgr.isAdmin(p);
                          }
                          return true; // console allowed
                        })
                    .executes(
                        ctx -> {
                          ConfigMgr.load();
                          ctx.getSource()
                              .sendFeedback(
                                  () -> Text.literal("[SentinelCore] Config reloaded."), true);
                          return 1;
                        })));
  }
}
