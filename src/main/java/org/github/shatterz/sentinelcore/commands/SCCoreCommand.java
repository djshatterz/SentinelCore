package org.github.shatterz.sentinelcore.commands;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import org.github.shatterz.sentinelcore.SentinelCore;
import org.github.shatterz.sentinelcore.core.PermissionMgr;

public class SCCoreCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("sccore")
            .then(CommandManager.literal("info")
                .executes(ctx -> {
                    ctx.getSource().sendFeedback(() -> Text.literal("SentinelCore v" + SentinelCore.VERSION), false);
                    return 1;
                })
            )
            .then(CommandManager.literal("reload")
                .requires(src -> PermissionMgr.isAdmin(src.getPlayer()))
                .executes(ctx -> {
                    ctx.getSource().sendFeedback(() -> Text.literal("[SentinelCore] Reloading configuration..."), true);
                    SentinelCore.LOGGER.info("[SentinelCore] Reload triggered by " + ctx.getSource().getName());
                    // TODO: reload configs in future phase
                    return 1;
                })
            )
        );
    }
}
