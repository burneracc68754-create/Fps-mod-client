package dev.neonlyrics.hud;

import net.minecraft.client.gui.DrawContext;

public final class Icons {
   private static final int U = 4;
   private static DrawContext G;
   private static int C;
   private static float A;

   private Icons() {
   }

   public static void draw(DrawContext g, Icons.Icon icon, float cx, float cy, float size, int rgb, float a) {
      if (!(a * Gfx.alpha <= 0.02F)) {
         G = g;
         C = rgb;
         A = a;
         g.getMatrices().pushMatrix();
         g.getMatrices().translate(cx - size / 2.0F, cy - size / 2.0F);
         float s = size / 16.0F / 4.0F;
         g.getMatrices().scale(s, s);
         switch (icon) {
            case HOME:
               line(2.0F, 8.0F, 8.0F, 2.5F);
               line(8.0F, 2.5F, 14.0F, 8.0F);
               line(3.8F, 7.0F, 3.8F, 13.5F);
               line(12.2F, 7.0F, 12.2F, 13.5F);
               line(3.8F, 13.5F, 12.2F, 13.5F);
               break;
            case LYRICS:
               line(6.0F, 12.0F, 6.0F, 3.2F);
               line(13.0F, 10.5F, 13.0F, 2.0F);
               line(6.0F, 3.2F, 13.0F, 2.0F, 2.2F);
               dot(4.3F, 12.3F, 2.0F);
               dot(11.3F, 10.8F, 2.0F);
               break;
            case TEXT:
               line(2.5F, 4.0F, 13.5F, 4.0F, 1.7F);
               line(2.5F, 8.0F, 11.0F, 8.0F, 1.7F);
               line(2.5F, 12.0F, 8.5F, 12.0F, 1.7F);
               break;
            case SKY:
               ring(7.0F, 8.5F, 4.6F);
               dot(12.5F, 3.5F, 1.1F);
               dot(14.0F, 7.0F, 0.8F);
               dot(10.5F, 1.8F, 0.7F);
               break;
            case SPOTIFY:
               arc(8.0F, 13.0F, 9.0F, 1.5F);
               arc(8.0F, 13.0F, 6.0F, 1.5F);
               arc(8.0F, 13.0F, 3.0F, 1.6F);
               break;
            case DISPLAY:
               box(2.0F, 3.0F, 14.0F, 11.5F);
               line(6.0F, 14.0F, 10.0F, 14.0F);
               line(8.0F, 11.5F, 8.0F, 14.0F);
               break;
            case PERF:
               line(9.5F, 1.8F, 4.5F, 9.0F);
               line(4.5F, 9.0F, 11.5F, 7.0F);
               line(11.5F, 7.0F, 6.5F, 14.2F);
               break;
            case FAV:
               line(4.0F, 2.0F, 12.0F, 2.0F);
               line(4.0F, 2.0F, 4.0F, 14.0F);
               line(12.0F, 2.0F, 12.0F, 14.0F);
               line(4.0F, 14.0F, 8.0F, 10.5F);
               line(12.0F, 14.0F, 8.0F, 10.5F);
               break;
            case SETTINGS:
               line(2.0F, 4.5F, 14.0F, 4.5F);
               line(2.0F, 8.0F, 14.0F, 8.0F);
               line(2.0F, 11.5F, 14.0F, 11.5F);
               dot(5.0F, 4.5F, 1.6F);
               dot(11.0F, 8.0F, 1.6F);
               dot(7.0F, 11.5F, 1.6F);
               break;
            case PLAY:
               tri(4.5F, 2.5F, 13.0F, 8.0F, 4.5F, 13.5F);
               break;
            case PAUSE:
               fill(4.0F, 3.0F, 6.6F, 13.0F);
               fill(9.4F, 3.0F, 12.0F, 13.0F);
               break;
            case NEXT:
               tri(3.0F, 3.0F, 10.5F, 8.0F, 3.0F, 13.0F);
               fill(11.0F, 3.0F, 13.0F, 13.0F);
               break;
            case PREV:
               tri(13.0F, 3.0F, 5.5F, 8.0F, 13.0F, 13.0F);
               fill(3.0F, 3.0F, 5.0F, 13.0F);
               break;
            case CLOSE:
               line(4.0F, 4.0F, 12.0F, 12.0F, 1.7F);
               line(12.0F, 4.0F, 4.0F, 12.0F, 1.7F);
               break;
            case BACK:
               line(10.0F, 3.0F, 5.0F, 8.0F, 1.8F);
               line(5.0F, 8.0F, 10.0F, 13.0F, 1.8F);
               break;
            case CHEVRON:
               line(6.0F, 3.0F, 11.0F, 8.0F, 1.8F);
               line(11.0F, 8.0F, 6.0F, 13.0F, 1.8F);
               break;
            case CHECK:
               line(3.0F, 8.5F, 6.5F, 12.0F, 2.0F);
               line(6.5F, 12.0F, 13.0F, 4.5F, 2.0F);
               break;
            case SYNC:
               arc(8.0F, 8.0F, 5.5F, 1.6F);
               line(13.5F, 8.0F, 11.6F, 6.0F, 1.6F);
               line(13.5F, 8.0F, 15.2F, 6.0F, 1.6F);
               break;
            case HAT:
               line(1.5F, 11.5F, 14.5F, 11.5F, 1.8F);
               line(4.5F, 11.5F, 5.2F, 4.5F);
               line(11.5F, 11.5F, 10.8F, 4.5F);
               line(5.2F, 4.5F, 10.8F, 4.5F);
               line(4.9F, 9.0F, 11.1F, 9.0F, 1.2F);
         }

         g.getMatrices().popMatrix();
      }
   }

   private static void line(float x0, float y0, float x1, float y1) {
      line(x0, y0, x1, y1, 1.5F);
   }

   private static void line(float x0, float y0, float x1, float y1, float w) {
      float dx = (x1 - x0) * 4.0F;
      float dy = (y1 - y0) * 4.0F;
      float len = (float)Math.sqrt(dx * dx + dy * dy);
      int hw = Math.max(1, Math.round(w * 4.0F / 2.0F));
      G.getMatrices().pushMatrix();
      G.getMatrices().translate(x0 * 4.0F, y0 * 4.0F);
      G.getMatrices().rotate((float)Math.atan2(dy, dx));
      G.fill(-hw + 1, -hw, Math.round(len) + hw - 1, hw, Gfx.argb(C, A));
      G.getMatrices().popMatrix();
   }

   private static void box(float x0, float y0, float x1, float y1) {
      line(x0, y0, x1, y0);
      line(x1, y0, x1, y1);
      line(x1, y1, x0, y1);
      line(x0, y1, x0, y0);
   }

   private static void fill(float x0, float y0, float x1, float y1) {
      Gfx.round(G, Math.round(x0 * 4.0F), Math.round(y0 * 4.0F), Math.round((x1 - x0) * 4.0F), Math.round((y1 - y0) * 4.0F), 2, C, A);
   }

   private static void tri(float ax, float ay, float bx, float by, float cx, float cy) {
      float minY = Math.min(ay, Math.min(by, cy));
      float maxY = Math.max(ay, Math.max(by, cy));
      int col = Gfx.argb(C, A);

      for (int yy = Math.round(minY * 4.0F); yy < Math.round(maxY * 4.0F); yy++) {
         float y = (yy + 0.5F) / 4.0F;
         float lo = Float.MAX_VALUE;
         float hi = -Float.MAX_VALUE;
         float[][] e = new float[][]{{ax, ay, bx, by}, {bx, by, cx, cy}, {cx, cy, ax, ay}};

         for (float[] s : e) {
            if (y >= s[1] && y <= s[3] || y >= s[3] && y <= s[1]) {
               float t = s[3] == s[1] ? 0.0F : (y - s[1]) / (s[3] - s[1]);
               float x = s[0] + (s[2] - s[0]) * t;
               lo = Math.min(lo, x);
               hi = Math.max(hi, x);
            }
         }

         if (hi > lo) {
            G.fill(Math.round(lo * 4.0F), yy, Math.round(hi * 4.0F), yy + 1, col);
         }
      }
   }

   private static void dot(float x, float y, float r) {
      Gfx.circle(G, x * 4.0F, y * 4.0F, r * 4.0F, C, A);
   }

   private static void ring(float x, float y, float r) {
      int n = 14;

      for (int i = 0; i < n; i++) {
         double a0 = i * Math.PI * 2.0 / n;
         double a1 = (i + 1) * Math.PI * 2.0 / n;
         line(x + (float)Math.cos(a0) * r, y + (float)Math.sin(a0) * r, x + (float)Math.cos(a1) * r, y + (float)Math.sin(a1) * r);
      }
   }

   private static void arc(float x, float y, float r, float w) {
      int n = 8;

      for (int i = 0; i < n; i++) {
         double a0 = 3.6128315516282616 + i * Math.PI * 0.7 / n;
         double a1 = 3.6128315516282616 + (i + 1) * Math.PI * 0.7 / n;
         line(x + (float)Math.cos(a0) * r, y + (float)Math.sin(a0) * r, x + (float)Math.cos(a1) * r, y + (float)Math.sin(a1) * r, w);
      }
   }

   public enum Icon {
      HOME,
      LYRICS,
      TEXT,
      SKY,
      SPOTIFY,
      DISPLAY,
      PERF,
      FAV,
      SETTINGS,
      PLAY,
      PAUSE,
      NEXT,
      PREV,
      CLOSE,
      BACK,
      CHEVRON,
      CHECK,
      SYNC,
      HAT;

      Icon() {
      }
   }
}
