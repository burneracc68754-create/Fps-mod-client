package dev.neonlyrics.cosmetic;

import dev.neonlyrics.NeonLyrics;
import dev.neonlyrics.config.Config;
import dev.neonlyrics.lyrics.NowPlaying;
import dev.neonlyrics.lyrics.SpotifyClient;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import javax.imageio.ImageIO;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.world.LightType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.Identifier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.LightmapTextureManager;
import org.joml.Matrix4f;

public final class Accessories {
   public static final String[] HATS = new String[]{
      "none",
      "headphones",
      "cone",
      "crown",
      "halo",
      "pumpkin",
      "flame_crown",
      "saturn",
      "storm_cloud",
      "unicorn",
      "disco_ball",
      "rubber_duck",
      "octopus",
      "cat_ears",
      "devil_horns",
      "top_hat",
      "cap",
      "party_hat",
      "viking",
      "bunny_ears",
      "note_ring",
      "flower_crown",
      "beanie",
      "visor",
      "witch_hat",
      "antennae",
      "mushroom"
   };
   public static final Identifier WHITE = Identifier.of("neonlyrics", "cosmetic/white");
   private static boolean whiteReady;
   private static boolean whiteFailed;
   private static final float[][] MESH = new float[HATS.length][];
   private static final boolean[] TRIED = new boolean[HATS.length];
   private static final int STRIDE = 14;

   private Accessories() {
   }

   private static float[] mesh(int i) {
      if (TRIED[i]) {
         return MESH[i];
      }

      TRIED[i] = true;

      try (InputStream in = Accessories.class.getResourceAsStream("/assets/neonlyrics/models/" + HATS[i] + ".bin")) {
         if (in != null) {
            ByteBuffer b = ByteBuffer.wrap(in.readAllBytes()).order(ByteOrder.LITTLE_ENDIAN);
            if (b.getInt() != 827149390) {
               return null;
            }

            int n = b.getInt();
            float[] m = new float[n * 14];

            for (int t = 0; t < n; t++) {
               for (int k = 0; k < 12; k++) {
                  m[t * 14 + k] = b.getFloat();
               }

               m[t * 14 + 12] = Float.intBitsToFloat(b.getInt());
               m[t * 14 + 13] = b.getInt();
            }

            MESH[i] = m;
            return MESH[i];
         } else {
            Object var10 = null;
            return (float[])var10;
         }
      } catch (Throwable var9) {
         NeonLyrics.LOG.warn("Hut-Modell fehlt: " + HATS[i], var9);
         return MESH[i];
      }
   }

   public static boolean whiteTex() {
      return white();
   }

   private static boolean white() {
      if (whiteReady) {
         return true;
      }

      if (whiteFailed) {
         return false;
      }

      try {
         BufferedImage img = new BufferedImage(4, 4, 2);

         for (int y = 0; y < 4; y++) {
            for (int x = 0; x < 4; x++) {
               img.setRGB(x, y, -1);
            }
         }

         ByteArrayOutputStream out = new ByteArrayOutputStream();
         ImageIO.write(img, "png", out);
         MinecraftClient.getInstance().getTextureManager().registerTexture(WHITE, new NativeImageBackedTexture(() -> "neonlyrics_white", NativeImage.read(out.toByteArray())));
         whiteReady = true;
      } catch (Throwable var3) {
         whiteFailed = true;
      }

      return whiteReady;
   }

   public static void render(MinecraftClient mc, VertexConsumerProvider buffers, Matrix4f base, Vec3d cam, float pt) {
      Config c = NeonLyrics.cfg();
      int idx = c.hat;
      if (idx > 0 && idx < HATS.length) {
         ClientPlayerEntity pl = mc.player;
         if (pl != null && !pl.isInvisible() && !pl.isSleeping() && !pl.isSwimming() && !pl.isGliding() && !mc.options.getPerspective().isFirstPerson()) {
            float[] m = mesh(idx);
            if (m != null && white()) {
               Vec3d pos = pl.getLerpedPos(pt);
               double neckY = pos.y + pl.getStandingEyeHeight() - 0.22;
               float yaw = (float)Math.toRadians(MathHelper.lerpAngleDegrees(pt, pl.lastHeadYaw, pl.headYaw));
               float pitch = (float)Math.toRadians(pl.getPitch(pt));
               float cy = (float)Math.cos(-yaw);
               float sy = (float)Math.sin(-yaw);
               float cp = (float)Math.cos(pitch);
               float sp = (float)Math.sin(pitch);
               float s = 0.060937498F;
               double ox = pos.x - cam.x;
               double oy = neckY - cam.y;
               double oz = pos.z - cam.z;

               int light;
               try {
                  BlockPos bp = BlockPos.ofFloored(pos.x, neckY + 0.3, pos.z);
                  light = LightmapTextureManager.pack(mc.world.getLightLevel(LightType.BLOCK, bp), mc.world.getLightLevel(LightType.SKY, bp));
               } catch (Throwable var49) {
                  light = 15728880;
               }

               NowPlaying np = SpotifyClient.now();
               float beat = np.playing ? np.beatPulse(np.progressNow(c.lyricsOffsetMs)) : 0.0F;
               float glow = c.hatGlow ? 0.75F + 0.25F * beat : 0.55F;
               VertexConsumer vc = buffers.getBuffer(RenderLayers.entityCutoutNoCull(WHITE));
               int n = m.length / 14;
               float[] v = new float[9];

               for (int t = 0; t < n; t++) {
                  int o = t * 14;
                  float nx = m[o + 9];
                  float ny = m[o + 10];
                  float nz = m[o + 11];
                  float ny1 = ny * cp - nz * sp;
                  float nz1 = ny * sp + nz * cp;
                  float nx2 = nx * cy + nz1 * sy;
                  float nz2 = -nx * sy + nz1 * cy;

                  for (int k = 0; k < 3; k++) {
                     float x = m[o + k * 3] * s;
                     float y = (m[o + k * 3 + 1] + 4.0F) * s;
                     float z = m[o + k * 3 + 2] * s;
                     float y1 = y * cp - z * sp;
                     float z1 = y * sp + z * cp;
                     v[k * 3] = x * cy + z1 * sy;
                     v[k * 3 + 1] = y1;
                     v[k * 3 + 2] = -x * sy + z1 * cy;
                  }

                  int rgb = Float.floatToRawIntBits(m[o + 12]);
                  boolean em = m[o + 13] > 0.5F;
                  int li = light;
                  int col;
                  if (em) {
                     int r = Math.min(255, Math.round((rgb >> 16 & 0xFF) * (0.6F + 0.6F * glow)));
                     int g = Math.min(255, Math.round((rgb >> 8 & 0xFF) * (0.6F + 0.6F * glow)));
                     int bl = Math.min(255, Math.round((rgb & 0xFF) * (0.6F + 0.6F * glow)));
                     col = 0xFF000000 | r << 16 | g << 8 | bl;
                     li = 15728880;
                  } else {
                     col = 0xFF000000 | rgb;
                  }

                  for (int k = 0; k < 4; k++) {
                     int kk = Math.min(k, 2);
                     vc.vertex(base, (float)(ox + v[kk * 3]), (float)(oy + v[kk * 3 + 1]), (float)(oz + v[kk * 3 + 2]))
                        .color(col)
                        .texture(0.5F, 0.5F)
                        .overlay(OverlayTexture.DEFAULT_UV)
                        .light(li)
                        .normal(nx2, ny1, nz2);
                  }
               }
            }
         }
      }
   }
}
