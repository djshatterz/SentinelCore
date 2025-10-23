package org.github.shatterz.sentinelcore.perm;

import org.github.shatterz.sentinelcore.config.ConfigManager;
import org.github.shatterz.sentinelcore.config.CoreConfig;
import org.github.shatterz.sentinelcore.perm.memory.MemoryPermissionService;
import org.github.shatterz.sentinelcore.log.SentinelLogger;
import org.github.shatterz.sentinelcore.log.SentinelCategories;
import org.slf4j.Logger;

public final class PermissionBootstrap {
    private static final Logger LOG = SentinelLogger.cat(SentinelCategories.PERM);
    private static PermissionService current;

    private PermissionBootstrap(){}

    public static void init() {
        installFor(ConfigManager.get());
        // re-install service when config changes
        ConfigManager.init(cfg -> installFor(cfg));
    }

    private static void installFor(CoreConfig cfg) {
        String backend = (cfg.permissions != null && cfg.permissions.backend != null)
                ? cfg.permissions.backend : "memory";

        if ("memory".equalsIgnoreCase(backend)) {
            MemoryPermissionService mem = (current instanceof MemoryPermissionService m) ? m : new MemoryPermissionService(cfg);
            if (mem != current) {
                current = mem;
                Perms.install(current);
            } else {
                mem.reload(cfg);
            }
            LOG.info("Using permission backend: memory");
        } else {
            // fallback to memory for now; LuckPerms adapter can come later
            MemoryPermissionService mem = new MemoryPermissionService(cfg);
            current = mem;
            Perms.install(current);
            LOG.warn("Unknown permission backend '{}', using memory.", backend);
        }
    }
}
