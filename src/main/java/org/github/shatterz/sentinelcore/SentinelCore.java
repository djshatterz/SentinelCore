package org.github.shatterz.sentinelcore;

import net.fabricmc.api.ModInitializer;
import org.github.shatterz.sentinelcore.audit.AuditManager;
import org.github.shatterz.sentinelcore.audit.SclogsCommands;
import org.github.shatterz.sentinelcore.flags.FeatureFlagRegistry;
import org.github.shatterz.sentinelcore.log.SentinelCategories;
import org.github.shatterz.sentinelcore.log.SentinelLogger;
import org.github.shatterz.sentinelcore.names.CommunityCommands;
import org.github.shatterz.sentinelcore.names.CommunityPrefixManager;
import org.github.shatterz.sentinelcore.names.NameUpdateListener;
import org.github.shatterz.sentinelcore.perm.PermCommands;
import org.github.shatterz.sentinelcore.perm.PermissionBootstrap;
import org.github.shatterz.sentinelcore.perm.events.PlayerConnectionListener;
import org.slf4j.Logger;

public final class SentinelCore implements ModInitializer {
  public static final String MOD_ID = "sentinelcore";

  @Override
  public void onInitialize() {
    Logger log = SentinelLogger.root();
    log.info("[SentinelCore] Initializing base systems...");

    // Initialize permission system
    PermissionBootstrap.init();
    PermCommands.register();
    PlayerConnectionListener.register();

    // Initialize community prefix system
    CommunityPrefixManager.init();
    CommunityCommands.register();
    NameUpdateListener.register();

    // config + flags (hot-reload is inside ConfigManager; FeatureFlagRegistry wires a callback)
    FeatureFlagRegistry.wireReload();

    // Audit system setup and commands
    AuditManager.applyConfig(org.github.shatterz.sentinelcore.config.ConfigManager.get());
    org.github.shatterz.sentinelcore.config.ConfigManager.addReloadListener(
        AuditManager::applyConfig);
    SclogsCommands.register();

    // Write a startup audit record so logs are always tail-able right after boot
    try {
      AuditManager.logSystem("startup", "server_boot", java.util.Map.of());
    } catch (Throwable ignored) {
      // non-fatal
    }

    // category demo logs (will respect on/off later if you add level filtering)
    SentinelLogger.cat(SentinelCategories.AUDIT).info("Audit logging ready.");
    SentinelLogger.cat(SentinelCategories.PERM).info("Permission logging ready.");
    SentinelLogger.cat(SentinelCategories.MOVE).info("Movement logging ready.");
    SentinelLogger.cat(SentinelCategories.MODMODE).info("ModMode logging ready.");
    SentinelLogger.cat(SentinelCategories.SPAWN).info("Spawn logging ready.");

    log.info("[SentinelCore] Initialization complete.");
  }
}
