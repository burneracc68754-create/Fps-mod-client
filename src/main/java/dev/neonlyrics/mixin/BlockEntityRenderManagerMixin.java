package dev.neonlyrics.mixin;

import dev.neonlyrics.perf.PerfBoost;
import dev.neonlyrics.perf.PerfSettings;
import net.minecraft.client.render.block.entity.BlockEntityRenderManager;
import net.minecraft.client.render.block.entity.state.BlockEntityRenderState;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Skips block entity renderers that are far from the camera. */
@Mixin(BlockEntityRenderManager.class)
public abstract class BlockEntityRenderManagerMixin {
   @Shadow
   private Vec3d cameraPos;

   @Inject(method = "render", at = @At("HEAD"), cancellable = true, require = 0)
   private void neonlyrics$cullBlockEntity(
      BlockEntityRenderState state, MatrixStack matrices, OrderedRenderCommandQueue queue, CameraRenderState cameraState, CallbackInfo ci
   ) {
      if (!PerfBoost.active()) {
         return;
      }

      PerfSettings s = PerfSettings.get();
      if (s.skipAllBlockEntities) {
         ci.cancel();
         return;
      }

      if (!s.cullBlockEntities || state == null || this.cameraPos == null) {
         return;
      }

      BlockPos pos = state.pos;
      if (pos == null) {
         return;
      }

      double max = s.blockEntityCullDistance;
      double dx = pos.getX() - this.cameraPos.x;
      double dy = pos.getY() - this.cameraPos.y;
      double dz = pos.getZ() - this.cameraPos.z;
      if (dx * dx + dy * dy + dz * dz > max * max) {
         ci.cancel();
      }
   }
}
