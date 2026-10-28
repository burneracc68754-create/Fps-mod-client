package dev.neonlyrics.cosmetic;

import dev.neonlyrics.NeonLyrics;
import dev.neonlyrics.gui.LyricsScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import org.joml.Matrix4f;

public final class PhoneMenu {
   private static WorldPanel panel;

   private PhoneMenu() {
   }

   public static boolean interactive() {
      return NeonLyrics.cfg().menuMode == 1;
   }

   public static void open(MinecraftClient mc) {
      if (panel != null) {
         if (!panel.closing()) {
            panel.close();
         }
      } else if (interactive() && mc.world != null && mc.player != null) {
         panel = new WorldPanel(mc);
      } else {
         mc.setScreen(new LyricsScreen());
      }
   }

   public static void closeNow() {
      panel = null;
   }

   public static void menuClosed() {
   }

   public static boolean hidesHand() {
      return false;
   }

   public static void tick(MinecraftClient mc) {
      if (panel != null) {
         if (mc.world != null && mc.player != null && !panel.finished() && !(mc.player.getEntityPos().distanceTo(panel.pos()) > 32.0)) {
            panel.tick(mc);
         } else {
            panel = null;
         }
      }
   }

   public static void startTick(MinecraftClient mc) {
      if (panel != null && mc.currentScreen == null && panel.hovered()) {
         while (mc.options.attackKey.wasPressed()) {
            panel.click();
         }

         while (mc.options.useKey.wasPressed()) {
            panel.click();
         }
      }
   }

   public static void render(MinecraftClient mc, VertexConsumerProvider buffers, Matrix4f base, float pt) {
      if (panel != null && mc.player != null) {
         panel.render(mc, buffers, base);
      }
   }
}
