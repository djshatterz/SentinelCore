package org.github.shatterz.sentinelcore;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.github.shatterz.sentinelcore.core.PermissionMgr;
import org.github.shatterz.sentinelcore.commands.SCCoreCommand;

public class SentinelCore implements ModInitializer {
    public static final String MOD_ID = "sentinelcore";
    public static final Logger LOGGER = LoggerFactory.getLogger("SentinelCore");
    public static final String VERSION = "1.0-SNAPSHOT";

    @Override
    public void onInitialize() {
        LOGGER.info("[SentinelCore] Initializing base systems...");
        PermissionMgr.init();

        // register /sccore
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
            SCCoreCommand.register(dispatcher)
        );

        LOGGER.info("[SentinelCore] Commands registered.");
    }
}
