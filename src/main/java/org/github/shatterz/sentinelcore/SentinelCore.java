package org.github.shatterz.sentinelcore;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SentinelCore implements ModInitializer {
    public static final String MOD_ID = "sentinelcore";
    public static final Logger LOGGER = LoggerFactory.getLogger("SentinelCore");

    @Override
    public void onInitialize() {
        LOGGER.info("[SentinelCore] Initializing base systems...");
        // Phase 1+: PermissionMgr.init(), Config.load(), etc.
    }
}
