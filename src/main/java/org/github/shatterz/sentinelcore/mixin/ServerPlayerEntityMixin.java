package org.github.shatterz.sentinelcore.mixin;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.github.shatterz.sentinelcore.log.SentinelLogger;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin to modify the player list name to use custom display names. This ensures player names in
 * tab list and chat show with community/team prefixes.
 */
@Mixin(ServerPlayerEntity.class)
public class ServerPlayerEntityMixin {
  private static final Logger LOG = SentinelLogger.root();

  /** Inject into getPlayerListName to return custom name if set. This affects tab list display. */
  @Inject(method = "getPlayerListName", at = @At("RETURN"), cancellable = true)
  private void injectCustomPlayerListName(CallbackInfoReturnable<Text> cir) {
    ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
    Text customName = player.getCustomName();

    // If player has a custom name set by NameFormatter, use it instead
    if (customName != null) {
      LOG.info(
          "[Mixin] getPlayerListName override for {} -> {}",
          player.getName().getString(),
          customName.getString());
      cir.setReturnValue(customName);
    }
  }
}
