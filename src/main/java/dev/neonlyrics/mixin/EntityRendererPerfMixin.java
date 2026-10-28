package dev.neonlyrics.mixin;

import dev.neonlyrics.perf.PerfBoost;
import dev.neonlyrics.perf.PerfSettings;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Distance-culls entities and their name tags while the FPS mode is active. */
@Mixin(EntityRenderer.class)
public abstract class EntityRendererPerfMixin {
   @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true, require = 0)
   private void neonlyrics$cullEntity(
      Entity entity, Frustum frustum, double x, double y, double z, CallbackInfoReturnable<Boolean> cir
   ) {
      if (!PerfBoost.active()) {
         return;
      }

      PerfSettings s = PerfSettings.get();
      if (!s.cullEntities) {
         return;
      }

      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc == null || mc.player == entity) {
         return;
      }

      double max = s.entityCullDistance;
      if (x * x + y * y + z * z > max * max) {
         cir.setReturnValue(false);
      }
   }

   @Inject(method = "renderLabelIfPresent", at = @At("HEAD"), cancellable = true, require = 0)
   private void neonlyrics$cullNameTag(
      EntityRenderState state, MatrixStack matrices, OrderedRenderCommandQueue queue, CameraRenderState cameraState, CallbackInfo ci
   ) {
      if (!PerfBoost.active()) {
         return;
      }

      PerfSettings s = PerfSettings.get();
      if (!s.cullEntities) {
         return;
      }

      double max = s.nameTagDistance;
      if (max > 0.0D && state.squaredDistanceToCamera > max * max) {
         ci.cancel();
      }
   }
}
