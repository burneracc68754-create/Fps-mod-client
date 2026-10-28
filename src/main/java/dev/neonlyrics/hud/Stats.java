package dev.neonlyrics.hud;

import dev.neonlyrics.License;
import dev.neonlyrics.NeonLyrics;
import dev.neonlyrics.config.Config;
import dev.neonlyrics.gui.LyricsScreen;
import java.util.Locale;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.RenderTickCounter;

/**
 * Small, cheap performance overlay. Values are refreshed four times per second
 * and all strings are reused, so the overlay itself does not allocate per frame.
 */
public final class Stats {
   private static final int BG = 0x16161E;
   private static long lastUpdate;
   private static String fps = "--";
   private static String frame = "--";
   private static String memory = "--";
   private static String coords = "";
   private static String ping = "";
   private static int width;
   private static int height;

   private Stats() {
   }

   public static void render(DrawContext g, RenderTickCounter delta) {
      if (!License.ok()) {
         return;
      }

      MinecraftClient mc = MinecraftClient.getInstance();
      Config c = NeonLyrics.cfg();
      if (!c.statsHud || mc.player == null || mc.world == null || mc.options.hudHidden || mc.currentScreen instanceof LyricsScreen) {
         return;
      }

      long now = System.currentTimeMillis();
      if (now - lastUpdate >= 250L) {
         lastUpdate = now;
         update(mc, c);
      }

      if (height <= 0) {
         return;
      }

      int x = 6;
      int y = 6;
      int w = width + 16;
      int h = height + 10;
      Gfx.aaRound(g, x, y, w, h, 7.0F, BG, 0.72F);
      Gfx.aaRound(g, x, y, 2.0F, h, 1.0F, Theme.accent(), 0.95F);

      int ty = y + 5;
      int tx = x + 9;
      Gfx.textBold(g, fps, tx, ty, 0xFFFFFF, 1.0F, false);
      Gfx.text(g, frame, tx + Gfx.w(fps, Gfx.BOLD) + 5, ty + 1, 0xA0A4B4, 1.0F, false);
      ty += 10;
      if (c.statsMemory) {
         Gfx.text(g, memory, tx, ty, 0xA0A4B4, 1.0F, false);
         ty += 9;
      }
      if (c.statsCoords) {
         Gfx.text(g, coords, tx, ty, 0xA0A4B4, 1.0F, false);
         ty += 9;
      }
      if (c.statsPing && !ping.isEmpty()) {
         Gfx.text(g, ping, tx, ty, 0xA0A4B4, 1.0F, false);
      }
   }

   private static void update(MinecraftClient mc, Config c) {
      int f = mc.getCurrentFps();
      fps = f + " FPS";
      frame = String.format(Locale.ROOT, "%.1f ms", f > 0 ? 1000.0F / f : 0.0F);
      Runtime rt = Runtime.getRuntime();
      long used = (rt.totalMemory() - rt.freeMemory()) / 1048576L;
      long max = rt.maxMemory() / 1048576L;
      memory = "RAM " + used + " / " + max + " MB";
      coords = "XYZ " + mc.player.getBlockX() + " " + mc.player.getBlockY() + " " + mc.player.getBlockZ();
      ping = "";
      if (mc.getNetworkHandler() != null) {
         PlayerListEntry entry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
         if (entry != null) {
            ping = mc.isInSingleplayer() ? "Ping 0 ms (LAN)" : "Ping " + entry.getLatency() + " ms";
         }
      }

      width = Math.max(Gfx.w(fps, Gfx.BOLD) + 5 + Gfx.w(frame), Gfx.w(memory, Gfx.UI));
      if (c.statsCoords) {
         width = Math.max(width, Gfx.w(coords, Gfx.UI));
      }
      if (!ping.isEmpty()) {
         width = Math.max(width, Gfx.w(ping, Gfx.UI));
      }

      height = 11 + (c.statsMemory ? 9 : 0) + (c.statsCoords ? 9 : 0) + (c.statsPing && !ping.isEmpty() ? 9 : 0);
   }
}
