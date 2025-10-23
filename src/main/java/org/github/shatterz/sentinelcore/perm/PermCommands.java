package org.github.shatterz.sentinelcore.perm;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

import java.util.Map;
import java.util.UUID;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;
import static net.minecraft.command.argument.IdentifierArgumentType.identifier; // or StringArgumentType if you prefer
import static com.mojang.brigadier.arguments.StringArgumentType.string;

public final class PermCommands {
    private PermCommands(){}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, env) -> register(dispatcher));
    }

    private static void register(CommandDispatcher<ServerCommandSource> d) {
        d.register(literal("sccore")
                .then(literal("perm")
                        .then(literal("test")
                                .then(argument("node", string())
                                        .executes(ctx -> {
                                            ServerCommandSource src = ctx.getSource();
                                            ServerPlayerEntity player = src.getPlayer();
                                            if (player == null) {
                                                src.sendFeedback(() -> Text.literal("Player-only for now."), false);
                                                return 1;
                                            }
                                            String node = string().parse(ctx.getArguments().get("node").toString());
                                            UUID uuid = player.getUuid();
                                            boolean ok = Perms.check(uuid, node, Map.of());
                                            src.sendFeedback(() -> Text.literal("perm(" + node + ") = " + ok), false);
                                            return ok ? 1 : 0;
                                        }))))); // keep tiny on purpose
    }
}
