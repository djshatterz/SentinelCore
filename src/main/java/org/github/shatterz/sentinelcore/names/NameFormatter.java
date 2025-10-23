package org.github.shatterz.sentinelcore.names;

import java.util.UUID;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.github.shatterz.sentinelcore.config.ConfigManager;
import org.github.shatterz.sentinelcore.config.CoreConfig;
import org.github.shatterz.sentinelcore.log.SentinelLogger;
import org.github.shatterz.sentinelcore.perm.RoleContext;
import org.github.shatterz.sentinelcore.perm.RoleContextManager;
import org.slf4j.Logger;

/**
 * NameFormatter combines community and team prefixes with mod-mode color overrides. Creates display
 * names in format: [Community] [Team] PlayerName
 *
 * <p>Colors:
 *
 * <ul>
 *   <li>Community prefix: per-prefix color from config
 *   <li>Team prefix: gold (default, configurable)
 *   <li>Player name: mod=blue, admin=red, default=white
 * </ul>
 *
 * <p>Triggered on join and context change.
 */
public final class NameFormatter {
  private static final Logger LOG = SentinelLogger.root();

  private NameFormatter() {}

  /**
   * Updates the display name for a player based on their community prefix, team status, and role
   * context.
   */
  public static void updateDisplayName(ServerPlayerEntity player) {
    if (player == null) {
      return;
    }

    CoreConfig config = ConfigManager.get();
    if (config.community == null || !config.community.enabled) {
      // Reset to default if disabled
      player.setCustomName(null);
      return;
    }

    UUID uuid = player.getUuid();
    RoleContext ctx = RoleContextManager.get(uuid);

    MutableText displayName = Text.empty();
    MutableText prefixOnly = Text.empty();
    boolean hasAnyPrefix = false;

    // 1. Community prefix (if selected)
    String communityId = CommunityPrefixManager.getSelectedPrefix(uuid);
    LOG.info("Player {} community prefix: {}", player.getName().getString(), communityId);

    if (communityId != null && config.community.prefixes.containsKey(communityId)) {
      CoreConfig.Community.PrefixConfig prefixCfg = config.community.prefixes.get(communityId);
      MutableText communityText = formatPrefix(communityId, prefixCfg, config.community.formatting);
      displayName.append(communityText);
      prefixOnly.append(communityText);
      hasAnyPrefix = true;
      LOG.info("Added community prefix [{}] for {}", communityId, player.getName().getString());
    }

    // 2. Team prefix (if eligible)
    if (config.community.teamPrefix != null
        && config.community.teamPrefix.enabled
        && isEligibleForTeamPrefix(ctx.getGroup(), config.community.teamPrefix)) {
      MutableText teamText =
          formatTeamPrefix(config.community.teamPrefix, config.community.formatting);
      displayName.append(teamText);
      prefixOnly.append(teamText);
      hasAnyPrefix = true;
      LOG.info(
          "Added team prefix [Team] for {} (group: {})",
          player.getName().getString(),
          ctx.getGroup());
    } else {
      // Helpful diagnostics to understand why team prefix didn't render
      String g = ctx != null ? ctx.getGroup() : "<null>";
      boolean enabled = config.community.teamPrefix != null && config.community.teamPrefix.enabled;
      LOG.info(
          "Team prefix not applied for {} (group='{}', enabled={}, appliesTo={})",
          player.getName().getString(),
          g,
          enabled,
          (config.community.teamPrefix != null
              ? config.community.teamPrefix.appliesTo
              : java.util.List.of()));
    }

    // 3. Player name color: keep default (white) for now
    //    Note: We'll later evolve this to color only the [Team] prefix based on role
    //    (e.g., blue for moderators, red for administrators), keeping the name white.
    Formatting nameColor = Formatting.WHITE;
    displayName.append(Text.literal(player.getName().getString()).formatted(nameColor));

    // Do not set CustomName to avoid affecting system messages (death/join/advancement)
    player.setCustomName(null);
    player.setCustomNameVisible(false); // Don't show nameplate above head

    // CRITICAL: Send player list update to all clients so they see the new display name
    net.minecraft.server.MinecraftServer server = player.getEntityWorld().getServer();
    if (server != null) {
      server
          .getPlayerManager()
          .sendToAll(
              new net.minecraft.network.packet.s2c.play.PlayerListS2CPacket(
                  net.minecraft.network.packet.s2c.play.PlayerListS2CPacket.Action
                      .UPDATE_DISPLAY_NAME,
                  player));
      LOG.info("Sent player list update packet for {}", player.getName().getString());
    }

    // Apply scoreboard team prefix (colored) + name color so chat/tab reflect prefixes
    applyScoreboardDecoration(player, prefixOnly, nameColor, hasAnyPrefix);

    LOG.debug("Updated display name for {}: {}", player.getName().getString(), displayName);
  }

  /**
   * Apply or clear scoreboard-based name decoration for a player. This affects tab list and chat
   * name decoration in modern Minecraft.
   */
  private static void applyScoreboardDecoration(
      ServerPlayerEntity player, Text prefixText, Formatting nameColor, boolean enabled) {
    try {
      var server = player.getEntityWorld().getServer();
      if (server == null) return;

      var scoreboard = server.getScoreboard();
      String teamId = "sccore_" + player.getUuid().toString().substring(0, 8);

      net.minecraft.scoreboard.Team team = scoreboard.getTeam(teamId);
      if (!enabled) {
        // Remove holder from team and optionally delete the team
        if (team != null) {
          scoreboard.removeScoreHolderFromTeam(player.getName().getString(), team);
          scoreboard.removeTeam(team);
          LOG.info("Cleared scoreboard decoration for {}", player.getName().getString());
        }
        return;
      }

      if (team == null) {
        team = scoreboard.addTeam(teamId);
      }

      // Use the already formatted Text for the prefix to preserve colors
      team.setPrefix(prefixText);
      team.setColor(nameColor);
      scoreboard.addScoreHolderToTeam(player.getName().getString(), team);
      LOG.info(
          "Applied scoreboard decoration for {} with prefix '{}'",
          player.getName().getString(),
          prefixText.getString());
    } catch (Throwable t) {
      LOG.warn("Scoreboard decoration failed: {}", t.toString());
    }
  }

  /** Formats a community prefix with brackets and color. Example: "[Clymunity] " in dark green */
  private static MutableText formatPrefix(
      String prefixId,
      CoreConfig.Community.PrefixConfig prefixCfg,
      CoreConfig.Community.Formatting formatting) {
    MutableText text = Text.empty();

    // Left bracket
    text.append(Text.literal(formatting.brackets.left));

    // Prefix text with color
    Formatting color = parseColor(prefixCfg.color);
    text.append(Text.literal(prefixId).formatted(color));

    // Right bracket
    text.append(Text.literal(formatting.brackets.right));

    // Space after if configured
    if (formatting.brackets.spaceAfter) {
      text.append(Text.literal(" "));
    }

    return text;
  }

  /** Formats the team prefix with brackets and color. Example: "[Team] " in gold */
  private static MutableText formatTeamPrefix(
      CoreConfig.Community.TeamPrefix teamCfg, CoreConfig.Community.Formatting formatting) {
    MutableText text = Text.empty();

    // Left bracket
    text.append(Text.literal(formatting.brackets.left));

    // "Team" label with color
    Formatting color = parseColor(teamCfg.color);
    MutableText label = Text.literal(teamCfg.label).formatted(color);

    // Apply styles if configured
    if (teamCfg.style != null) {
      if (teamCfg.style.bold) {
        label = label.formatted(Formatting.BOLD);
      }
      if (teamCfg.style.italic) {
        label = label.formatted(Formatting.ITALIC);
      }
    }

    text.append(label);

    // Right bracket
    text.append(Text.literal(formatting.brackets.right));

    // Space after if configured
    if (formatting.brackets.spaceAfter) {
      text.append(Text.literal(" "));
    }

    return text;
  }

  /** Checks if a group is eligible for the team prefix. */
  private static boolean isEligibleForTeamPrefix(
      String group, CoreConfig.Community.TeamPrefix teamCfg) {
    if (group == null || teamCfg.appliesTo == null) {
      return false;
    }
    return teamCfg.appliesTo.contains(group);
  }

  /**
   * Gets the player name color based on role and mod-mode.
   *
   * <p>Priority: modmode > admin > default
   */
  private static Formatting getPlayerNameColor(RoleContext ctx) {
    // Mod-mode override: blue
    if (ctx.isModMode()) {
      return Formatting.BLUE;
    }

    // Admin/developer/op: red
    String group = ctx.getGroup();
    if ("admin".equalsIgnoreCase(group)
        || "developer".equalsIgnoreCase(group)
        || "op".equalsIgnoreCase(group)) {
      return Formatting.RED;
    }

    // Default: white
    return Formatting.WHITE;
  }

  /** Parses a color string to Minecraft Formatting. Supports named colors and hex (future). */
  private static Formatting parseColor(String colorStr) {
    if (colorStr == null) {
      return Formatting.WHITE;
    }

    // Try named colors first
    try {
      String lowered = colorStr.toLowerCase();
      Formatting f = Formatting.byName(lowered);
      // Accept both "gray" and common misspelling "grey"
      if (f == null && "grey".equals(lowered)) {
        f = Formatting.GRAY;
      }
      return f != null ? f : Formatting.WHITE;
    } catch (Exception ignored) {
      // Fallback to white if unknown
    }

    // Future: support hex colors via TextColor (requires additional processing)
    return Formatting.WHITE;
  }

  /** Refreshes display name for all online players. Called on config reload. */
  public static void refreshAllDisplayNames() {
    // Implement when we have server access - will be called from config reload listener
    LOG.info("Display name refresh triggered (placeholder - needs server instance)");
  }
}
