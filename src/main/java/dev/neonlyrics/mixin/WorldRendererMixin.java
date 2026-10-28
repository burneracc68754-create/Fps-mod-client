package dev.neonlyrics.mixin;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import dev.neonlyrics.perf.PerfBoost;
import dev.neonlyrics.perf.PerfSettings;
import net.minecraft.client.option.CloudRenderMode;
import net.minecraft.client.render.FrameGraphBuilder;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.command.OrderedRenderCommandQueueImpl;
import net.minecraft.client.render.state.WorldRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Removes the most expensive vanilla render passes while the FPS mode is on. */
@Mixin(WorldRenderer.class)
public abstract class WorldRendererMixin {
   @Inject(method = "renderWeather", at = @At("HEAD"), cancellable = true, require = 0)
   private void neonlyrics$skipWeather(FrameGraphBuilder graph, GpuBufferSlice buffer, CallbackInfo ci) {
      if (PerfBoost.active() && PerfSettings.get().disableWeather) {
         ci.cancel();
      }
   }

   @Inject(method = "renderClouds", at = @At("HEAD"), cancellable = true, require = 0)
   private void neonlyrics$skipClouds(
      FrameGraphBuilder graph,
      CloudRenderMode mode,
      Vec3d cameraPos,
      long time,
      float tickDelta,
      int color,
      float opacity,
      CallbackInfo ci
   ) {
      if (PerfBoost.active() && PerfSettings.get().disableClouds) {
         ci.cancel();
      }
   }

   @Inject(method = "renderBlockEntities", at = @At("HEAD"), cancellable = true, require = 0)
   private void neonlyrics$skipBlockEntities(
      MatrixStack matrices, WorldRenderState state, OrderedRenderCommandQueueImpl queue, CallbackInfo ci
   ) {
      if (PerfBoost.active() && PerfSettings.get().skipAllBlockEntities) {
         ci.cancel();
      }
   }

   @Inject(method = "renderParticles", at = @At("HEAD"), cancellable = true, require = 0)
   private void neonlyrics$skipParticles(FrameGraphBuilder graph, GpuBufferSlice buffer, CallbackInfo ci) {
      if (PerfBoost.active() && PerfSettings.get().skipVanillaParticles) {
         ci.cancel();
      }
   }
}
