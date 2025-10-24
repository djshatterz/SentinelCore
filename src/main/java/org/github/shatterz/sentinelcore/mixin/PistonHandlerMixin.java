package org.github.shatterz.sentinelcore.mixin;

import net.minecraft.block.piston.PistonHandler;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.github.shatterz.sentinelcore.protection.SpawnProtectionManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Prevents pistons from moving blocks into or out of the spawn protection zone. */
@Mixin(PistonHandler.class)
public class PistonHandlerMixin {
  @Shadow @Final private World world;

  @Shadow @Final private BlockPos posFrom;

  @Shadow @Final private Direction motionDirection;

  @Inject(method = "tryMove", at = @At("HEAD"), cancellable = true)
  private void sentinelcore$protectSpawnFromPistons(CallbackInfoReturnable<Boolean> cir) {
    if (SpawnProtectionManager.shouldProtectPiston(world, posFrom, motionDirection)) {
      cir.setReturnValue(false);
    }
  }
}
