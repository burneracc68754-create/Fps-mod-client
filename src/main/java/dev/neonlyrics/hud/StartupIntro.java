package dev.neonlyrics.hud;

import dev.neonlyrics.NeonLyrics;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents.AfterInit;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents.AfterRender;
import net.minecraft.client.gui.screen.TitleScreen;

public final class StartupIntro {
   private static long start = -1L;
   private static boolean done = false;

   private StartupIntro() {
   }

   public static void register() {
      ScreenEvents.AFTER_INIT.register((AfterInit)(client, screen, w, h) -> {
         if (!done && screen instanceof TitleScreen && NeonLyrics.cfg().fxStartup) {
            if (start < 0L) {
               start = System.currentTimeMillis();
            }

            ScreenEvents.afterRender(screen).register((AfterRender)(scr, g, mx, my, delta) -> {
               if (!done) {
                  float t = (float)(System.currentTimeMillis() - start) / 1000.0F;
                  if (t > 2.4F) {
                     done = true;
                  } else {
                     int sw = g.getScaledWindowWidth();
                     int sh = g.getScaledWindowHeight();
                     float old = Gfx.alpha;
                     float out = t > 1.7F ? 1.0F - (t - 1.7F) / 0.7F : 1.0F;
                     Gfx.alpha = 1.0F;
                     Gfx.rect(g, 0, 0, sw, sh, 460046, 0.96F * out);
                     float in = Anim.outBack(Math.min(1.0F, t / 0.6F));
                     float glow = (float)(0.5 + 0.5 * Math.sin(t * 4.0F));
                     int acc = Theme.accent();
                     g.getMatrices().pushMatrix();
                     g.getMatrices().translate(sw / 2.0F, sh / 2.0F - 6.0F);
                     float sc = 2.6F * (0.7F + 0.3F * in);
                     g.getMatrices().scale(sc, sc);
                     String logo = "NEON";
                     int lw = Gfx.w(logo, Gfx.BOLD);
                     Gfx.alpha = out * Math.min(1.0F, t / 0.3F);
                     Gfx.aaRound(g, -lw / 2.0F - 8.0F, -7.0F, lw + 16, 17.0F, 8.5F, acc, 0.18F + 0.12F * glow);
                     Gfx.textBold(g, logo, -lw / 2, -3, 16777215, 1.0F, false);
                     g.getMatrices().popMatrix();
                     String sub = "Lyrics";
                     int n = Math.max(0, Math.min(sub.length(), (int)((t - 0.5F) / 0.05F)));
                     Gfx.center(g, sub.substring(0, n), sw / 2, sh / 2 + 22, 10326968, out, false);
                     float pw = Math.min(1.0F, t / 1.6F);
                     Gfx.aaRound(g, sw / 2.0F - 40.0F, sh / 2.0F + 36.0F, 80.0F, 2.0F, 1.0F, 16777215, 0.12F * out);
                     Gfx.aaRound(g, sw / 2.0F - 40.0F, sh / 2.0F + 36.0F, 80.0F * pw, 2.0F, 1.0F, acc, out);
                     Gfx.alpha = old;
                  }
               }
            });
         }
      });
   }
}
