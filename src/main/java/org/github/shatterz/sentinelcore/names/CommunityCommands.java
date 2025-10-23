package org.github.shatterz.sentinelcore.names;

import static com.mojang.brigadier.arguments.StringArgumentType.word;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import java.util.UUID;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.github.shatterz.sentinelcore.config.ConfigManager;
import org.github.shatterz.sentinelcore.config.CoreConfig;
import org.github.shatterz.sentinelcore.log.SentinelLogger;
import org.github.shatterz.sentinelcore.perm.PermissionManager;
import org.slf4j.Logger;

/** Commands for community prefix selection: /community list, select, none, admin reload. */
public final class CommunityCommands {
  private static final Logger LOG = SentinelLogger.root();

  private CommunityCommands() {}

  /** Register commands via Fabric's v2 command callback. */
  public static void register() {
    CommandRegistrationCallback.EVENT.register(
        (dispatcher, registryAccess, environment) -> register(dispatcher));
  }

  private static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
    dispatcher.register(
        literal("community")
            .requires(src -> hasPermission(src, "sentinelcore.community.use"))
            .then(literal("list").executes(CommunityCommands::listPrefixes))
            .then(
                literal("select")
                    .then(
                        argument("prefix", word())
                            .suggests(
                                (ctx, builder) -> {
                                  CoreConfig cfg = ConfigManager.get();
                                  if (cfg.community != null && cfg.community.prefixes != null) {
                                    for (String id : cfg.community.prefixes.keySet()) {
                                      builder.suggest(id);
                                    }
                                  }
                                  return builder.buildFuture();
                                })
                            .executes(CommunityCommands::selectPrefix)))
            .then(literal("none").executes(CommunityCommands::clearPrefix))
            .then(
                literal("admin")
                    .requires(src -> hasPermission(src, "sentinelcore.community.admin"))
                    .then(literal("reload").executes(CommunityCommands::adminReload))));

    LOG.info("Registered /community commands.");
  }

  /** Helper to check permission for command source. */
  private static boolean hasPermission(ServerCommandSource src, String node) {
    if (src.getPlayer() != null) {
      return PermissionManager.has(src.getPlayer(), node);
    }
    // Console/command blocks always allowed
    return true;
  }

  private static int listPrefixes(CommandContext<ServerCommandSource> ctx) {
    ServerCommandSource src = ctx.getSource();
    CoreConfig cfg = ConfigManager.get();

    if (cfg.community == null || !cfg.community.enabled) {
      src.sendFeedback(() -> Text.literal("Community prefixes are disabled."), false);
      return 0;
    }

    if (cfg.community.prefixes.isEmpty()) {
      src.sendFeedback(() -> Text.literal("No community prefixes available."), false);
      return 0;
    }

    src.sendFeedback(
        () -> Text.literal("Available community prefixes:").formatted(Formatting.AQUA), false);

    for (String id : cfg.community.prefixes.keySet()) {
      src.sendFeedback(
          () -> Text.literal("  - ").append(Text.literal(id).formatted(Formatting.YELLOW)), false);
    }

    return 1;
  }

  private static int selectPrefix(CommandContext<ServerCommandSource> ctx) {
    ServerCommandSource src = ctx.getSource();
    ServerPlayerEntity player = src.getPlayer();

    if (player == null) {
      src.sendError(Text.literal("Only players can select community prefixes."));
      return 0;
    }

    String prefixId = ctx.getArgument("prefix", String.class);
    CoreConfig cfg = ConfigManager.get();

    if (cfg.community == null || !cfg.community.enabled) {
      src.sendError(Text.literal("Community prefixes are disabled."));
      return 0;
    }

    if (!cfg.community.prefixes.containsKey(prefixId)) {
      src.sendError(Text.literal("Unknown prefix: " + prefixId));
      return 0;
    }

    UUID uuid = player.getUuid();
    CommunityPrefixManager.setSelectedPrefix(uuid, prefixId);
    NameFormatter.updateDisplayName(player);

    src.sendFeedback(
        () ->
            Text.literal("Community prefix set to: ")
                .formatted(Formatting.GREEN)
                .append(Text.literal(prefixId).formatted(Formatting.YELLOW)),
        false);

    return 1;
  }

  private static int clearPrefix(CommandContext<ServerCommandSource> ctx) {
    ServerCommandSource src = ctx.getSource();
    ServerPlayerEntity player = src.getPlayer();

    if (player == null) {
      src.sendError(Text.literal("Only players can clear community prefixes."));
      return 0;
    }

    UUID uuid = player.getUuid();
    CommunityPrefixManager.clearPrefix(uuid);
    NameFormatter.updateDisplayName(player);

    src.sendFeedback(
        () -> Text.literal("Community prefix cleared.").formatted(Formatting.GREEN), false);

    return 1;
  }

  private static int adminReload(CommandContext<ServerCommandSource> ctx) {
    ServerCommandSource src = ctx.getSource();

    try {
      CommunityPrefixManager.reload();
      ConfigManager.reloadNow();

      src.sendFeedback(
          () ->
              Text.literal("Community prefix configuration reloaded.").formatted(Formatting.GREEN),
          true);

      return 1;
    } catch (Exception e) {
      LOG.error("Failed to reload community prefix config", e);
      src.sendError(Text.literal("Reload failed: " + e.getMessage()));
      return 0;
    }
  }
}
