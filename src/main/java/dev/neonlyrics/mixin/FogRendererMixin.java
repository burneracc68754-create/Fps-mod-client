package dev.neonlyrics.mixin;

import dev.neonlyrics.sky.WorldSky;
import net.minecraft.block.enums.CameraSubmersionType;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.fog.FogRenderer;
import net.minecraft.client.world.ClientWorld;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FogRenderer.class)
public abstract class FogRendererMixin {
   public FogRendererMixin() {
   }

   @Inject(
      method = "applyFog(Lnet/minecraft/client/render/Camera;ILnet/minecraft/client/render/RenderTickCounter;FLnet/minecraft/client/world/ClientWorld;)Lorg/joml/Vector4f;",
      at = @At("RETURN"),
      cancellable = true,
      require = 0
   )
   private void neonlyrics$skyFog(
      Camera camera, int renderDistance, RenderTickCounter ticks, float tickDelta, ClientWorld level, CallbackInfoReturnable<Vector4f> cir
   ) {
      if (camera.getSubmersionType() == CameraSubmersionType.NONE) {
         Vector4f v = cir.getReturnValue();
         if (v != null && WorldSky.tintFog(v)) {
            cir.setReturnValue(v);
         }
      }
   }
}
