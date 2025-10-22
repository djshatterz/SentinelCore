package org.github.shatterz.sentinelcore.commands.admin;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import org.github.shatterz.sentinelcore.core.PermissionMgr;
import org.github.shatterz.sentinelcore.core.audit.AuditManager;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Deque;
import java.util.LinkedList;
import java.util.stream.Stream;

public class SCLogsCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("sclogs")
            .requires(src -> {
                try { return PermissionMgr.isAdmin(src.getPlayer()); } catch (Exception e) { return true; }
            })
            .then(CommandManager.literal("info")
                .executes(ctx -> {
                    var file = AuditManager.todayFile();
                    ctx.getSource().sendFeedback(() -> Text.literal("[SentinelCore] Audit file: " + file.toAbsolutePath()), false);
                    return 1;
                })
            )
            .then(CommandManager.literal("tail")
                .then(CommandManager.argument("lines", IntegerArgumentType.integer(1, 2000))
                    .executes(ctx -> {
                        int n = IntegerArgumentType.getInteger(ctx, "lines");
                        var file = AuditManager.todayFile();
                        if (!Files.exists(file)) {
                            ctx.getSource().sendFeedback(() -> Text.literal("[SentinelCore] No audit entries for today."), false);
                            return 1;
                        }
                        try (Stream<String> s = Files.lines(file, StandardCharsets.UTF_8)) {
                            Deque<String> ring = new LinkedList<>();
                            s.forEach(line -> {
                                if (ring.size() == n) ring.removeFirst();
                                ring.addLast(line);
                            });
                            ring.forEach(line ->
                                ctx.getSource().sendFeedback(() -> Text.literal(line), false)
                            );
                        } catch (IOException e) {
                            ctx.getSource().sendError(Text.literal("[SentinelCore] Failed to read audit file: " + e.getMessage()));
                        }
                        return 1;
                    })
                )
            )
        );
    }
}
