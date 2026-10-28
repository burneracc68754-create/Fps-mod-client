package dev.neonlyrics.mixin;

import dev.neonlyrics.perf.PerfBoost;
import dev.neonlyrics.perf.PerfSettings;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Stops far-away particles before they are even allocated. Vanilla callers of
 * the spawn factory simply discard the returned particle, so returning null is
 * safe here.
 */
@Mixin(ParticleManager.class)
public abstract class ParticleManagerMixin {
   @Inject(
      method = "addParticle(Lnet/minecraft/particle/ParticleEffect;DDDDDD)Lnet/minecraft/client/particle/Particle;",
      at = @At("HEAD"),
      cancellable = true,
      require = 0
   )
   private void neonlyrics$cullParticle(
      ParticleEffect effect,
      double x,
      double y,
      double z,
      double velocityX,
      double velocityY,
      double velocityZ,
      CallbackInfoReturnable<Particle> cir
   ) {
      if (!PerfBoost.active()) {
         return;
      }

      PerfSettings s = PerfSettings.get();
      if (!s.cullParticles) {
         return;
      }

      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc == null || mc.player == null) {
         return;
      }

      Vec3d pos = mc.player.getEntityPos();
      double max = s.particleDistance;
      if (pos.squaredDistanceTo(x, y, z) > max * max) {
         cir.setReturnValue(null);
      }
   }
}
