package org.github.shatterz.sentinelcore;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.github.shatterz.sentinelcore.core.PermissionMgr;
import org.github.shatterz.sentinelcore.commands.SCCoreCommand;
import org.github.shatterz.sentinelcore.commands.admin.SCLogsCommand;
import org.github.shatterz.sentinelcore.core.audit.AuditManager;

import java.nio.file.Path;

public class SentinelCore implements ModInitializer {
    public static final String MOD_ID = "sentinelcore";
    public static final Logger LOGGER = LoggerFactory.getLogger("SentinelCore");
    public static final String VERSION = "1.0-SNAPSHOT";

    @Override
    public void onInitialize() {
        LOGGER.info("[SentinelCore] Initializing base systems...");

        // Init audit (config dir: config/sentinelcore/)
        Path cfgDir = Path.of("config").resolve("sentinelcore");
        AuditManager.init(cfgDir);

        PermissionMgr.init();

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            SCCoreCommand.register(dispatcher);
            SCLogsCommand.register(dispatcher);
        });

        LOGGER.info("[SentinelCore] Commands registered.");
    }
}
