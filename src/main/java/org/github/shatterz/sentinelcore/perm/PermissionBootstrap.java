package org.github.shatterz.sentinelcore.perm;

import org.github.shatterz.sentinelcore.config.ConfigManager;
import org.github.shatterz.sentinelcore.config.CoreConfig;
import org.github.shatterz.sentinelcore.log.SentinelCategories;
import org.github.shatterz.sentinelcore.log.SentinelLogger;
import org.github.shatterz.sentinelcore.perm.luckperms.LuckPermsService;
import org.github.shatterz.sentinelcore.perm.memory.MemoryPermissionService;
import org.slf4j.Logger;

public final class PermissionBootstrap {
  private static final Logger LOG = SentinelLogger.cat(SentinelCategories.PERM);
  private static PermissionService current;

  private PermissionBootstrap() {}

  public static void init() {
    installFor(ConfigManager.get());
    // re-install service when config changes
    ConfigManager.init(cfg -> installFor(cfg));
  }

  private static void installFor(CoreConfig cfg) {
    String backend =
        (cfg.permissions != null && cfg.permissions.backend != null)
            ? cfg.permissions.backend
            : "memory";

    if ("memory".equalsIgnoreCase(backend)) {
      MemoryPermissionService mem =
          (current instanceof MemoryPermissionService m) ? m : new MemoryPermissionService(cfg);
      if (mem != current) {
        current = mem;
        Perms.install(current);
      } else {
        mem.reload(cfg);
      }
      LOG.info("Using permission backend: memory");

    } else if ("luckperms".equalsIgnoreCase(backend)
        || "luckperms-bridge".equalsIgnoreCase(backend)) {
      // Bridge mode - query LuckPerms directly
      LuckPermsService lp = new LuckPermsService(LuckPermsService.Mode.BRIDGE);
      if (lp.isAvailable()) {
        current = lp;
        Perms.install(current);
        LOG.info("Using permission backend: luckperms (bridge mode)");
      } else {
        LOG.warn("LuckPerms not available, falling back to memory backend");
        fallbackToMemory(cfg);
      }

    } else if ("luckperms-mirror".equalsIgnoreCase(backend)) {
      // Mirror mode - cache LuckPerms data
      LuckPermsService lp = new LuckPermsService(LuckPermsService.Mode.MIRROR);
      if (lp.isAvailable()) {
        current = lp;
        Perms.install(current);
        LOG.info("Using permission backend: luckperms (mirror mode)");
      } else {
        LOG.warn("LuckPerms not available, falling back to memory backend");
        fallbackToMemory(cfg);
      }

    } else {
      // Unknown backend, fallback to memory
      LOG.warn("Unknown permission backend '{}', using memory.", backend);
      fallbackToMemory(cfg);
    }
  }

  private static void fallbackToMemory(CoreConfig cfg) {
    MemoryPermissionService mem = new MemoryPermissionService(cfg);
    current = mem;
    Perms.install(current);
  }
}
