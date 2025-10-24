package org.github.shatterz.sentinelcore.mixin;

import net.minecraft.entity.mob.EndermanEntity;
import net.minecraft.util.math.BlockPos;
import org.github.shatterz.sentinelcore.protection.SpawnProtectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Prevents Endermen from picking up or placing blocks inside the spawn protection zone. */
@Mixin(EndermanEntity.class)
public class EndermanEntityMixin {

  @Inject(method = "tickMovement", at = @At("HEAD"))
  private void sentinelcore$protectSpawnFromEnderman(CallbackInfo ci) {
    EndermanEntity self = (EndermanEntity) (Object) this;
    BlockPos pos = self.getBlockPos();

    // If Enderman is carrying a block and inside the zone, drop it
    if (self.getCarriedBlock() != null
        && SpawnProtectionManager.shouldProtectEnderman(self.getEntityWorld(), pos)) {
      self.setCarriedBlock(null);
    }
  }
}
