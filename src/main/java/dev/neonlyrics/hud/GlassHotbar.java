package dev.neonlyrics.hud;

import dev.neonlyrics.License;
import dev.neonlyrics.NeonLyrics;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public final class GlassHotbar {
   private static final Anim sel = new Anim(0.0, 22.0);

   private GlassHotbar() {
   }

   public static HudElement wrap(HudElement original) {
      return (g, delta) -> {
         MinecraftClient mc = MinecraftClient.getInstance();
         if (NeonLyrics.cfg().glassHotbar && License.ok() && mc.player != null && !mc.player.isSpectator()) {
            render(g, mc);
         } else {
            original.render(g, delta);
         }
      };
   }

   private static void render(DrawContext g, MinecraftClient mc) {
      float old = Gfx.alpha;
      Gfx.alpha = 1.0F;
      int acc = Theme.accent();
      int w = 182;
      int h = 22;
      int x = g.getScaledWindowWidth() / 2 - w / 2;
      int y = g.getScaledWindowHeight() - h - 1;
      PlayerInventory inv = mc.player.getInventory();
      Gfx.round(g, x, y, w, h, 6, 1183260, 0.72F);
      Gfx.round(g, x, y, w, 1, 1, 16777215, 0.07F);
      int selected = inv.getSelectedSlot();
      float sx = sel.target(selected).get();
      int slotX = x + 1 + Math.round(sx * 20.0F);
      Gfx.glow(g, slotX, y + 1, 20, 20, 5, acc, 0.9F, 4);
      Gfx.round(g, slotX, y + 1, 20, 20, 5, acc, 0.22F);
      outline(g, slotX, y + 1, 20, 20, acc);

      for (int i = 0; i < 9; i++) {
         ItemStack st = inv.getStack(i);
         if (!st.isEmpty()) {
            int ix = x + 3 + i * 20;
            int iy = y + 3;
            g.drawItem(st, ix, iy);
            g.drawStackOverlay(mc.textRenderer, st, ix, iy);
         }
      }

      ItemStack off = mc.player.getOffHandStack();
      if (!off.isEmpty()) {
         int ox = x - 26;
         Gfx.round(g, ox, y, 22, 22, 6, 1183260, 0.72F);
         g.drawItem(off, ox + 3, y + 3);
         g.drawStackOverlay(mc.textRenderer, off, ox + 3, y + 3);
      }

      Gfx.alpha = old;
   }

   private static void outline(DrawContext g, int x, int y, int w, int h, int col) {
      Gfx.rect(g, x + 3, y, w - 6, 1, col, 0.95F);
      Gfx.rect(g, x + 3, y + h - 1, w - 6, 1, col, 0.95F);
      Gfx.rect(g, x, y + 3, 1, h - 6, col, 0.95F);
      Gfx.rect(g, x + w - 1, y + 3, 1, h - 6, col, 0.95F);
      Gfx.rect(g, x + 1, y + 1, 2, 1, col, 0.95F);
      Gfx.rect(g, x + 1, y + 2, 1, 1, col, 0.95F);
      Gfx.rect(g, x + w - 3, y + 1, 2, 1, col, 0.95F);
      Gfx.rect(g, x + w - 2, y + 2, 1, 1, col, 0.95F);
      Gfx.rect(g, x + 1, y + h - 2, 2, 1, col, 0.95F);
      Gfx.rect(g, x + 1, y + h - 3, 1, 1, col, 0.95F);
      Gfx.rect(g, x + w - 3, y + h - 2, 2, 1, col, 0.95F);
      Gfx.rect(g, x + w - 2, y + h - 3, 1, 1, col, 0.95F);
   }
}
