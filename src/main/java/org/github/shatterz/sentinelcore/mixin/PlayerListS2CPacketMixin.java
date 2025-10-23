package org.github.shatterz.sentinelcore.mixin;

import com.mojang.authlib.GameProfile;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.text.Text;
import net.minecraft.world.GameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Mixin to modify player list packet to use custom display names in tab list. This hooks into the
 * packet creation to inject our formatted names.
 */
@Mixin(PlayerListS2CPacket.Entry.class)
public class PlayerListS2CPacketMixin {

  /**
   * Modify the display name parameter when creating a PlayerListS2CPacket.Entry. If the player has
   * a custom name set, use that for the tab list.
   */
  @ModifyVariable(
      method =
          "<init>(Lcom/mojang/authlib/GameProfile;ILnet/minecraft/world/GameMode;Lnet/minecraft/text/Text;Lnet/minecraft/server/network/ServerPlayerEntity$PublicKey;)V",
      at = @At("HEAD"),
      argsOnly = true,
      ordinal = 0)
  private static Text modifyDisplayName(
      Text displayName, GameProfile profile, int latency, GameMode gameMode) {
    // The display name comes from ServerPlayerEntity.getPlayerListName()
    // which uses getCustomName() if present, otherwise returns null
    // So we just pass through - the custom name set by NameFormatter will be used
    return displayName;
  }
}
