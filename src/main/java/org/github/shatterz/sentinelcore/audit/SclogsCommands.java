package org.github.shatterz.sentinelcore.audit;

import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import org.github.shatterz.sentinelcore.perm.PermissionManager;

public final class SclogsCommands {
  private SclogsCommands() {}

  public static void register() {
    CommandRegistrationCallback.EVENT.register(
        (dispatcher, registryAccess, environment) -> register(dispatcher));
  }

  private static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
    // /sclogs toggle <on|off>
    LiteralArgumentBuilder<ServerCommandSource> toggle =
        literal("toggle")
            .requires(src -> has(src, "sentinelcore.audit.toggle"))
            .then(
                literal("on")
                    .executes(
                        ctx -> {
                          AuditManager.toggle(true);
                          ctx.getSource()
                              .sendFeedback(() -> Text.literal("Audit logging enabled."), true);
                          return 1;
                        }))
            .then(
                literal("off")
                    .executes(
                        ctx -> {
                          AuditManager.toggle(false);
                          ctx.getSource()
                              .sendFeedback(() -> Text.literal("Audit logging disabled."), true);
                          return 1;
                        }));

    // /sclogs tail [n]
    LiteralArgumentBuilder<ServerCommandSource> tail =
        literal("tail")
            .requires(src -> has(src, "sentinelcore.audit.tail"))
            .executes(ctx -> doTail(ctx.getSource(), 20))
            .then(
                argument("n", integer(1, 200))
                    .executes(ctx -> doTail(ctx.getSource(), getInteger(ctx, "n"))));

    LiteralArgumentBuilder<ServerCommandSource> root = literal("sclogs").then(toggle).then(tail);

    dispatcher.register(root);
  }

  private static boolean has(ServerCommandSource src, String node) {
    if (src.getPlayer() == null) return src.hasPermissionLevel(3);
    return PermissionManager.has(src.getPlayer(), node) || src.hasPermissionLevel(3);
  }

  private static int doTail(ServerCommandSource src, int n) {
    Path dir = JsonlFileAuditSink.defaultBaseDir("sentinelcore");
    try {
      if (!Files.exists(dir)) {
        src.sendFeedback(() -> Text.literal("No audit logs yet."), false);
        return 1;
      }
      Path latest =
          Files.list(dir)
              .filter(
                  p ->
                      p.getFileName().toString().startsWith("audit-")
                          && p.toString().endsWith(".jsonl"))
              .max(java.util.Comparator.naturalOrder())
              .orElse(null);
      if (latest == null) {
        src.sendFeedback(() -> Text.literal("No audit files found."), false);
        return 1;
      }
      Deque<String> ring = new ArrayDeque<>(n);
      try (java.io.BufferedReader r = Files.newBufferedReader(latest, StandardCharsets.UTF_8)) {
        String line;
        while ((line = r.readLine()) != null) {
          if (ring.size() == n) ring.removeFirst();
          ring.addLast(line);
        }
      }
      int count = 0;
      for (String s : ring) {
        count++;
        if (count > 50) break; // hard cap per chat spam safety
        String out = s.length() > 240 ? s.substring(0, 240) + "..." : s;
        final String fOut = out;
        src.sendFeedback(() -> Text.literal(fOut), false);
      }
      final int shown = Math.min(count, n);
      final String summary = "Shown " + shown + " lines from " + latest.getFileName();
      src.sendFeedback(() -> Text.literal(summary), false);
      return 1;
    } catch (IOException e) {
      src.sendError(Text.literal("Failed to read audit logs: " + e.getMessage()));
      return 0;
    }
  }
}
