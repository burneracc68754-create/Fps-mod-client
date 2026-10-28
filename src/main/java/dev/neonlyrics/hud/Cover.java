package dev.neonlyrics.hud;

import dev.neonlyrics.NeonLyrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.util.Identifier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public final class Cover {
   private static final Identifier ID = Identifier.of("neonlyrics", "cover");
   private static final Identifier ID_SQ = Identifier.of("neonlyrics", "cover_sq");
   private static final int SIZE = 64;
   private static final int SQ = 256;
   private static volatile Cover.Pending pending;
   private static volatile boolean clearPending;
   private static volatile long loadedAt = 0L;
   private static NativeImageBackedTexture tex;
   private static NativeImageBackedTexture texSq;
   private static boolean ready;
   private static long shownAt;
   private static int col1 = -1;
   private static int col2 = -1;

   private Cover() {
   }

   public static void load(byte[] imageBytes) {
      try {
         BufferedImage src = ImageIO.read(new ByteArrayInputStream(imageBytes));
         if (src == null) {
            return;
         }

         BufferedImage round = scale(src, 64);
         float r = 32.0F;

         for (int y = 0; y < 64; y++) {
            for (int x = 0; x < 64; x++) {
               float dx = x + 0.5F - r;
               float dy = y + 0.5F - r;
               float a = Math.max(0.0F, Math.min(1.0F, r - (float)Math.sqrt(dx * dx + dy * dy)));
               int argb = round.getRGB(x, y);
               round.setRGB(x, y, Math.round((argb >>> 24 & 0xFF) * a) << 24 | argb & 16777215);
            }
         }

         BufferedImage sq = scale(src, 256);
         float cr = 11.52F;

         for (int y = 0; y < 256; y++) {
            for (int x = 0; x < 256; x++) {
               float a = cornerA(x, y, 256, cr);
               if (!(a >= 1.0F)) {
                  int argb = sq.getRGB(x, y);
                  sq.setRGB(x, y, Math.round((argb >>> 24 & 0xFF) * a) << 24 | argb & 16777215);
               }
            }
         }

         int c1 = avg(sq, 0, 128);
         int c2 = avg(sq, 128, 256);
         pending = new Cover.Pending(png(round), png(sq), c1, c2);
         loadedAt = System.currentTimeMillis();
      } catch (Throwable var10) {
         NeonLyrics.LOG.debug("Cover konnte nicht geladen werden", var10);
      }
   }

   private static BufferedImage scale(BufferedImage src, int size) {
      BufferedImage img = new BufferedImage(size, size, 2);
      Graphics2D g2 = img.createGraphics();
      g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
      g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
      int s = Math.min(src.getWidth(), src.getHeight());
      int ox = (src.getWidth() - s) / 2;
      int oy = (src.getHeight() - s) / 2;
      g2.drawImage(src, 0, 0, size, size, ox, oy, ox + s, oy + s, null);
      g2.dispose();
      return img;
   }

   private static float cornerA(int x, int y, int n, float r) {
      float cx = x + 0.5F;
      float cy = y + 0.5F;
      float qx = Math.max(0.0F, Math.max(r - cx, cx - (n - r)));
      float qy = Math.max(0.0F, Math.max(r - cy, cy - (n - r)));
      if (qx != 0.0F && qy != 0.0F) {
         float d = (float)Math.sqrt(qx * qx + qy * qy);
         return Math.max(0.0F, Math.min(1.0F, r - d + 0.5F));
      } else {
         return 1.0F;
      }
   }

   private static int avg(BufferedImage img, int y0, int y1) {
      long r = 0L;
      long g = 0L;
      long b = 0L;
      long n = 0L;

      for (int y = y0; y < y1; y += 4) {
         for (int x = 0; x < img.getWidth(); x += 4) {
            int c = img.getRGB(x, y);
            r += c >> 16 & 0xFF;
            g += c >> 8 & 0xFF;
            b += c & 0xFF;
            n++;
         }
      }

      return n == 0L ? 2105380 : (int)(r / n) << 16 | (int)(g / n) << 8 | (int)(b / n);
   }

   private static byte[] png(BufferedImage img) throws Exception {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      ImageIO.write(img, "png", out);
      return out.toByteArray();
   }

   public static void clear() {
      clearPending = true;
   }

   public static long loadedAt() {
      return loadedAt;
   }

   private static void sync() {
      if (clearPending) {
         clearPending = false;
         ready = false;
         col2 = -1;
         col1 = -1;
      }

      Cover.Pending p = pending;
      if (p != null) {
         pending = null;

         try {
            NativeImage a = NativeImage.read(p.round());
            NativeImage b = NativeImage.read(p.square());
            TextureManager tm = MinecraftClient.getInstance().getTextureManager();
            if (tex == null) {
               tex = new NativeImageBackedTexture(() -> "neonlyrics_cover", a);
               tm.registerTexture(ID, tex);
            } else {
               tex.setImage(a);
               tex.upload();
            }

            if (texSq == null) {
               texSq = new NativeImageBackedTexture(() -> "neonlyrics_cover_sq", b);
               tm.registerTexture(ID_SQ, texSq);
            } else {
               texSq.setImage(b);
               texSq.upload();
            }

            col1 = p.c1();
            col2 = p.c2();
            ready = true;
            shownAt = System.currentTimeMillis();
         } catch (Throwable var4) {
            ready = false;
         }
      }
   }

   public static boolean draw(DrawContext g, int x, int y, int size, float alpha) {
      if (!NeonLyrics.cfg().albumCover) {
         return false;
      }

      try {
         sync();
         if (!ready) {
            return false;
         }

         float in = Anim.outCubic(Anim.since(shownAt, 350L));
         g.drawTexture(RenderPipelines.GUI_TEXTURED, ID, x, y, 0.0F, 0.0F, size, size, size, size, Gfx.argb(16777215, alpha * in));
         return in > 0.5F;
      } catch (Throwable var6) {
         ready = false;
         return false;
      }
   }

   public static boolean drawSquare(DrawContext g, int x, int y, int size, float alpha) {
      try {
         sync();
         if (!ready) {
            return false;
         }

         float in = Anim.outCubic(Anim.since(shownAt, 450L));
         g.drawTexture(RenderPipelines.GUI_TEXTURED, ID_SQ, x, y, 0.0F, 0.0F, size, size, size, size, Gfx.argb(16777215, alpha * in));
         return true;
      } catch (Throwable var6) {
         ready = false;
         return false;
      }
   }

   public static int[] colors() {
      sync();
      return ready && col1 >= 0 ? new int[]{col1, col2} : null;
   }

   public static float fade() {
      return ready ? Anim.outCubic(Anim.since(shownAt, 600L)) : 0.0F;
   }

   private record Pending(byte[] round, byte[] square, int c1, int c2) {
      private Pending {
      }
   }
}
