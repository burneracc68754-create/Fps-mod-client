package dev.neonlyrics.mixin;

import dev.neonlyrics.cosmetic.PhoneMenu;
import net.minecraft.client.render.GameRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class HandMixin {
   public HandMixin() {
   }

   @Inject(method = "renderHand(FZLorg/joml/Matrix4f;)V", at = @At("HEAD"), cancellable = true, require = 0)
   private void neonlyrics$hideHand(float partialTick, boolean sleeping, Matrix4f projection, CallbackInfo ci) {
      if (PhoneMenu.hidesHand()) {
         ci.cancel();
      }
   }
}
