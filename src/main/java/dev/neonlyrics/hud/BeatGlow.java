package dev.neonlyrics.hud;

import dev.neonlyrics.License;
import dev.neonlyrics.NeonLyrics;
import dev.neonlyrics.config.Config;
import dev.neonlyrics.lyrics.NowPlaying;
import dev.neonlyrics.lyrics.SpotifyClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

public final class BeatGlow {
   private static float level = 0.0F;
   private static long last = System.nanoTime();

   private BeatGlow() {
   }

   public static void render(DrawContext g, RenderTickCounter delta) {
      if (License.ok()) {
         Config c = NeonLyrics.cfg();
         MinecraftClient mc = MinecraftClient.getInstance();
         long now = System.nanoTime();
         float dt = Math.min(0.1F, (float)(now - last) / 1.0E9F);
         last = now;
         NowPlaying np = SpotifyClient.now();
         float beat = c.beatGlow && np.playing && !mc.options.hudHidden ? np.beatPulse(np.progressNow(c.lyricsOffsetMs)) : 0.0F;
         level = beat > level ? beat : level + (beat - level) * (1.0F - (float)Math.exp(-5.0F * dt));
         if (!(level < 0.02F)) {
            float old = Gfx.alpha;
            Gfx.alpha = 1.0F;
            int w = g.getScaledWindowWidth();
            int h = g.getScaledWindowHeight();
            int acc = Theme.accent();
            int size = Math.max(24, Math.min(w, h) / 7);
            float a = 0.28F * level;
            Gfx.vGradient(g, 0, 0, w, size, acc, acc, a, 0.0F);
            Gfx.vGradient(g, 0, h - size, w, size, acc, acc, 0.0F, a);
            int steps = 12;

            for (int i = 0; i < steps; i++) {
               float k = 1.0F - (float)i / steps;
               int x0 = Math.round((float)(size * i) / steps);
               int x1 = Math.round((float)(size * (i + 1)) / steps);
               Gfx.rect(g, x0, 0, x1 - x0, h, acc, a * k * k / 2.2F);
               Gfx.rect(g, w - x1, 0, x1 - x0, h, acc, a * k * k / 2.2F);
            }

            Gfx.alpha = old;
         }
      }
   }
}
