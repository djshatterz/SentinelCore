package org.github.shatterz.sentinelcore.mixin;

import java.util.List;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;
import org.github.shatterz.sentinelcore.protection.SpawnProtectionManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Prevents explosions from damaging blocks inside the spawn protection zone. */
@Mixin(Explosion.class)
public class ExplosionMixin {
  @Shadow @Final private World world;

  @Shadow @Final private List<BlockPos> affectedBlocks;

  @Inject(method = "affectWorld", at = @At("HEAD"))
  private void sentinelcore$protectSpawnFromExplosions(CallbackInfo ci) {
    affectedBlocks.removeIf(pos -> SpawnProtectionManager.shouldProtectExplosion(world, pos));
  }
}
