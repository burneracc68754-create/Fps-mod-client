package dev.neonlyrics.cosmetic;

import dev.neonlyrics.NeonLyrics;
import dev.neonlyrics.config.Config;
import dev.neonlyrics.gui.LyricsScreen;
import dev.neonlyrics.hud.Gfx;
import dev.neonlyrics.hud.SkyThumbs;
import dev.neonlyrics.lyrics.NowPlaying;
import dev.neonlyrics.lyrics.SpotifyClient;
import dev.neonlyrics.sky.SkyPreset;
import dev.neonlyrics.util.Fx;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.render.RenderSetup;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.math.Vec3d;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.font.TextRenderer.TextLayerType;
import net.minecraft.client.render.VertexConsumerProvider.Immediate;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector3fc;

final class WorldPanel {
   static final int W = 380;
   static final int H = 222;
   static final float K = 0.0105F;
   private static final float RCX = 98.0F;
   private static final float RCY = 104.0F;
   private static final float RR = 78.0F;
   private static final float BX = 62.0F;
   private static final float CX0 = 206.0F;
   private static final float CX1 = 366.0F;
   private static final int ACCENT = 10321407;
   private static final int ACCENT2 = 14471167;
   private static final int DEEP = 5914576;
   private static final int TOP = 1709620;
   private static final int BOTTOM = 723479;
   private static final int LIGHT = 15728880;
   private static RenderLayer flat;
   private final Vec3d pos;
   private final float rx;
   private final float rz;
   private final float nx;
   private final float nz;
   private final List<WorldPanel.Hit> hits = new ArrayList<>();
   private final Map<String, Float> hov = new HashMap<>();
   private int page;
   private int skyPage;
   private int hatPage;
   private final long born = System.currentTimeMillis();
   private long closing = -1L;
   private long pageTime = 0L;
   private long lastNs = System.nanoTime();
   private float dt;
   private float spin;
   private boolean hovered;
   private boolean sndLand;
   private boolean sndClose;
   private float hx;
   private float hy;
   private static final String[] TABS = new String[]{"sky", "lyrics", "style", "more"};
   private Matrix4f m;
   private float ox;
   private float oy;
   private VertexConsumer q;
   private final List<Runnable> late = new ArrayList<>();

   private static RenderLayer flat() {
      if (flat == null) {
         flat = RenderLayer.of("neonlyrics_panel", RenderSetup.builder(RenderPipelines.DEBUG_QUADS).build());
      }

      return flat;
   }

   private static String tr(String key) {
      return Text.translatable("neonlyrics.wp." + key).getString();
   }

   WorldPanel(MinecraftClient mc) {
      Camera cam = mc.gameRenderer.getCamera();
      Vector3fc f = cam.getHorizontalPlane();
      double fl = Math.sqrt(f.x() * f.x() + f.z() * f.z());
      float fx = fl < 1.0E-4 ? 0.0F : (float)(f.x() / fl);
      float fz = fl < 1.0E-4 ? 1.0F : (float)(f.z() / fl);
      Vec3d c = cam.getCameraPos();
      this.pos = new Vec3d(c.x + fx * 3.2, c.y - 0.05, c.z + fz * 3.2);
      this.nx = -fx;
      this.nz = -fz;
      this.rx = -fz;
      this.rz = fx;
      play(mc, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 1.3F, 0.8F);
   }

   Vec3d pos() {
      return this.pos;
   }

   boolean hovered() {
      return this.hovered;
   }

   boolean closing() {
      return this.closing >= 0L;
   }

   void close() {
      if (this.closing < 0L) {
         this.closing = System.currentTimeMillis();
      }
   }

   boolean finished() {
      return this.closing >= 0L && System.currentTimeMillis() - this.closing > 900L;
   }

   private float age() {
      return (float)(System.currentTimeMillis() - this.born);
   }

   private float closeAge() {
      return this.closing < 0L ? -1.0F : (float)(System.currentTimeMillis() - this.closing);
   }

   void tick(MinecraftClient mc) {
      if (!this.sndLand && this.age() > 860.0F) {
         this.sndLand = true;
         play(mc, SoundEvents.BLOCK_BEACON_POWER_SELECT, 1.8F, 0.3F);
      }

      if (!this.sndClose && this.closing >= 0L && this.closeAge() > 360.0F) {
         this.sndClose = true;
         play(mc, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 1.0F, 0.7F);
      }
   }

   private static void play(MinecraftClient mc, SoundEvent ev, float pitch, float vol) {
      try {
         mc.getSoundManager().play(PositionedSoundInstance.ui(ev, pitch, vol));
      } catch (Throwable var5) {
      }
   }

   private void aim(Camera cam) {
      this.hovered = false;
      if (!(this.age() < 1200.0F) && this.closing < 0L) {
         Vec3d o = cam.getCameraPos();
         Vector3fc d = cam.getHorizontalPlane();
         double denom = d.x() * this.nx + d.z() * this.nz;
         if (!(denom > -1.0E-4)) {
            double t = ((this.pos.x - o.x) * this.nx + (this.pos.z - o.z) * this.nz) / denom;
            if (!(t <= 0.0) && !(t > 14.0)) {
               double px = o.x + d.x() * t - this.pos.x;
               double py = o.y + d.y() * t - this.pos.y;
               double pz = o.z + d.z() * t - this.pos.z;
               float u = (float)((px * this.rx + pz * this.rz) / 0.0105F) + 190.0F;
               float v = 111.0F - (float)(py / 0.0105F);
               if (!(u < 14.0F) && !(v < 0.0F) && !(u > 380.0F) && !(v > 222.0F)) {
                  this.hovered = true;
                  this.hx = u;
                  this.hy = v;
               }
            }
         }
      }
   }

   private boolean over(float x, float y, float w, float h) {
      return this.hovered && this.hx >= x && this.hy >= y && this.hx < x + w && this.hy < y + h;
   }

   private float hv(String id, boolean on) {
      float v = this.hov.getOrDefault(id, 0.0F);
      v += ((on ? 1.0F : 0.0F) - v) * Math.min(1.0F, this.dt * 14.0F);
      this.hov.put(id, v);
      return v;
   }

   void click() {
      if (this.hovered && this.closing < 0L) {
         for (int i = this.hits.size() - 1; i >= 0; i--) {
            WorldPanel.Hit h = this.hits.get(i);
            if (this.hx >= h.x() && this.hy >= h.y() && this.hx < h.x() + h.w() && this.hy < h.y() + h.h()) {
               Fx.sound(Fx.Sfx.CLICK);
               h.run().run();
               NeonLyrics.saveConfig();
               return;
            }
         }
      }
   }

   private void go(int p) {
      if (p != this.page) {
         this.page = p;
         this.pageTime = System.currentTimeMillis();
      }
   }

   void render(MinecraftClient mc, VertexConsumerProvider buffers, Matrix4f base) {
      Camera cam = mc.gameRenderer.getCamera();
      this.aim(cam);
      long ns = System.nanoTime();
      this.dt = Math.min(0.1F, (float)(ns - this.lastNs) / 1.0E9F);
      this.lastNs = ns;
      float t = this.age();
      float c = this.closeAge();
      float fly;
      float slide;
      float content;
      float arm;
      if (c < 0.0F) {
         fly = inOut(clamp(t / 900.0F));
         slide = outCubic(clamp((t - 820.0F) / 460.0F));
         content = outCubic(clamp((t - 1100.0F) / 300.0F));
         arm = inOut(clamp((t - 1150.0F) / 400.0F));
      } else {
         arm = 1.0F - inOut(clamp(c / 220.0F));
         content = 1.0F - clamp(c / 160.0F);
         slide = 1.0F - inOut(clamp((c - 120.0F) / 300.0F));
         fly = 1.0F - inOut(clamp((c - 380.0F) / 480.0F));
      }

      float w = 3.5F + 16.5F * (float)Math.exp(-t / 420.0F) + (c >= 0.0F ? 10.0F * clamp(c / 600.0F) : 0.0F);
      this.spin = this.spin + w * this.dt;
      Vec3d cp = cam.getCameraPos();
      float bob = (float)Math.sin(System.currentTimeMillis() / 1200.0) * 0.012F * slide;
      Matrix4f rot = new Matrix4f(this.rx, 0.0F, this.rz, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, this.nx, 0.0F, this.nz, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F);
      Matrix4f panel = new Matrix4f(base)
         .translate(
            (float)(this.pos.x - cp.x), (float)(this.pos.y - cp.y) + bob, (float)(this.pos.z - cp.z)
         )
         .mul(rot);
      this.q = buffers.getBuffer(flat());
      this.hits.clear();
      this.late.clear();
      Config cf = NeonLyrics.cfg();
      NowPlaying np = SpotifyClient.now();
      if (slide > 0.01F) {
         float anchor = -0.96599996F;
         this.m = new Matrix4f(panel).translate(anchor, 0.0F, 0.0F).scale(slide, 1.0F, 1.0F).translate(-anchor, 0.0F, 0.0F);
         this.ox = 190.0F;
         this.oy = 111.0F;
         float a = Math.min(1.0F, slide * 1.5F);

         for (int i = 4; i >= 1; i--) {
            this.round(62.0F - i * 2.5F, -i * 2.5F, 318.0F + i * 5.0F, 222.0F + i * 5.0F, 14.0F + i * 2.5F, 10321407, 10321407, 0.05F * a, 0.0F);
         }

         this.round(61.2F, -0.8F, 319.6F, 223.6F, 14.8F, 12167664, 7231688, 0.35F * a, 0.0F);
         this.round(62.0F, 0.0F, 318.0F, 222.0F, 14.0F, 1709620, 723479, 0.96F * a, 0.0F);
         this.round(63.0F, 0.6F, 316.0F, 1.2F, 1.0F, 16777215, 16777215, 0.1F * a, 0.0F);
         if (content > 0.01F) {
            this.body(mc, cf, np, content);
         }
      }

      float recX = -0.96599996F;
      float recY = 0.0735F;
      Vector3f end = new Vector3f(this.rx * recX, recY + bob, this.rz * recX)
         .add((float)(this.pos.x - cp.x), (float)(this.pos.y - cp.y), (float)(this.pos.z - cp.z));
      Vector3fc f = cam.getHorizontalPlane();
      Vector3fc u = cam.getVerticalPlane();
      Vector3f start = new Vector3f(f).mul(0.9F).add(new Vector3f(u).mul(-0.3F));
      Vector3f p = new Vector3f(start).lerp(end, fly).add(0.0F, (float)Math.sin(Math.PI * fly) * 0.45F, 0.0F);
      float scale = 0.12F + 0.88F * outCubic(fly);
      float tilt = (1.0F - fly) * 70.0F;
      float ra = Math.min(1.0F, (c < 0.0F ? t : 900.0F - c) / 150.0F);
      if (ra > 0.01F) {
         this.m = new Matrix4f(base).translate(p).mul(rot).rotateX((float)Math.toRadians(-tilt)).scale(scale).translate(0.0F, 0.0F, 0.006F);
         this.ox = 0.0F;
         this.oy = 0.0F;
         this.record(np, ra, arm);
      }

      if (this.hovered) {
         this.m = new Matrix4f(panel);
         this.ox = 190.0F;
         this.oy = 111.0F;
         this.ring(this.hx, this.hy, 0.0F, 2.2F, 0.0F, 360.0F, 16777215, 0.95F, 16777215, 0.95F, 0.02F, 12);
         this.ring(this.hx, this.hy, 2.2F, 5.5F, 0.0F, 360.0F, 10321407, 0.35F, 10321407, 0.0F, 0.02F, 12);
      }

      if (buffers instanceof Immediate bs) {
         bs.draw(flat());
      }

      for (Runnable r : this.late) {
         r.run();
      }
   }

   private void record(NowPlaying np, float a, float arm) {
      float R = 78.0F;
      this.ring(0.0F, 0.0F, R, R + 14.0F, 0.0F, 360.0F, 10321407, 0.32F * a, 10321407, 0.0F, 0.0F, 48);
      int bands = 10;

      for (int i = 0; i < bands; i++) {
         float r0 = R * (0.38F + 0.62F * i / bands);
         float r1 = R * (0.38F + 0.62F * (i + 1) / bands);
         int col = i % 2 == 0 ? 986900 : 1513246;
         this.ring(0.0F, 0.0F, r0, r1, 0.0F, 360.0F, col, a, col, a, 0.001F, 48);
      }

      this.ring(0.0F, 0.0F, R - 1.4F, R, 0.0F, 360.0F, 3421247, a, 3421247, a, 0.002F, 48);
      float deg = (float)Math.toDegrees(this.spin);

      for (int k = 0; k < 4; k++) {
         float r0 = R * (0.48F + 0.12F * k);
         float s0 = deg + k * 97;
         this.ring(0.0F, 0.0F, r0, r0 + 1.1F, s0, s0 + 38.0F, 4539738, a, 4539738, a, 0.002F, 8);
      }

      this.ring(0.0F, 0.0F, R * 0.4F, R * 0.98F, 300.0F, 330.0F, 16777215, 0.02F * a, 16777215, 0.11F * a, 0.003F, 6);
      this.ring(0.0F, 0.0F, R * 0.4F, R * 0.98F, 120.0F, 150.0F, 16777215, 0.02F * a, 16777215, 0.08F * a, 0.003F, 6);
      this.ring(0.0F, 0.0F, 0.0F, R * 0.36F, 0.0F, 360.0F, 14471167, a, 5914576, a, 0.004F, 40);
      this.ring(0.0F, 0.0F, R * 0.33F, R * 0.36F, 0.0F, 360.0F, 5914576, a, 5914576, a, 0.005F, 40);
      this.ring(0.0F, 0.0F, 0.0F, R * 0.04F, 0.0F, 360.0F, 526348, a, 526348, a, 0.006F, 16);
      Matrix4f lm = new Matrix4f(this.m).rotateZ(-this.spin);
      this.textAt(lm, "LYRICS CORD", 0.0F, -R * 0.2F, 0.5F, 16777215, a, true);
      String sub = np.hasTrack() ? np.artist : tr("nomusic");
      this.textAt(lm, ellip(sub, 70.0F, 0.42F), 0.0F, R * 0.1F, 0.42F, 16777215, 0.85F * a, true);
      float px = R * 0.98F;
      float py = -R * 0.92F;
      float th = (float)Math.toRadians(88.0F + 15.0F * arm);
      float L = R * 1.05F;
      float dx = (float)Math.cos(th);
      float dy = (float)Math.sin(th);
      float nx2 = -dy * 1.7F;
      float ny2 = dx * 1.7F;
      float ex = px + dx * L;
      float ey = py + dy * L;
      this.poly(px + nx2, py + ny2, ex + nx2, ey + ny2, ex - nx2, ey - ny2, px - nx2, py - ny2, 14211298, a, 0.008F);
      this.poly(
         ex - 4.0F * dx + 3.5F * -dy,
         ey - 4.0F * dy + 3.5F * dx,
         ex + 5.0F * dx + 3.5F * -dy,
         ey + 5.0F * dy + 3.5F * dx,
         ex + 5.0F * dx - 3.5F * -dy,
         ey + 5.0F * dy - 3.5F * dx,
         ex - 4.0F * dx - 3.5F * -dy,
         ey - 4.0F * dy - 3.5F * dx,
         11053238,
         a,
         0.009F
      );
      this.ring(px, py, 0.0F, 8.0F, 0.0F, 360.0F, 5921384, a, 2894902, a, 0.009F, 20);
      this.ring(px, py, 0.0F, 3.0F, 0.0F, 360.0F, 14737642, a, 14737642, a, 0.01F, 12);
   }

   private void body(MinecraftClient mc, Config cf, NowPlaying np, float a) {
      float by = 197.0F;
      this.roundBtn("prev", 72.0F, by, 9.0F, "⏮", a, () -> SpotifyClient.control(SpotifyClient.Cmd.PREVIOUS));
      this.roundBtn("play", 98.0F, by, 10.5F, np.playing ? "⏸" : "▶", a, () -> SpotifyClient.control(SpotifyClient.Cmd.PLAYPAUSE));
      this.roundBtn("next", 124.0F, by, 9.0F, "⏭", a, () -> SpotifyClient.control(SpotifyClient.Cmd.NEXT));
      this.hits.add(new WorldPanel.Hit(43.4F, 49.4F, 109.2F, 109.2F, () -> SpotifyClient.control(SpotifyClient.Cmd.PLAYPAUSE)));
      TextRenderer font = MinecraftClient.getInstance().textRenderer;
      this.text("Lyrics Cord", 206.0F, 10.0F, 0.85F, 16777215, a);
      float xh = this.hv("x", this.over(351.0F, 6.0F, 15.0F, 15.0F));
      this.ring(
         358.5F,
         13.5F,
         0.0F,
         7.5F,
         0.0F,
         360.0F,
         Gfx.lerpColor(16777215, 16734842, xh),
         (0.08F + 0.55F * xh) * a,
         Gfx.lerpColor(16777215, 16734842, xh),
         (0.08F + 0.55F * xh) * a,
         0.002F,
         16
      );
      this.text("✕", 355.5F, 10.5F, 0.7F, 16777215, a);
      this.hits.add(new WorldPanel.Hit(351.0F, 6.0F, 15.0F, 15.0F, this::close));
      String nl = tr("normal");
      float nw = font.getWidth(nl) * 0.62F + 12.0F;
      float nhv = this.hv("nrm", this.over(346.0F - nw, 6.0F, nw, 15.0F));
      this.round(346.0F - nw, 6.0F, nw, 15.0F, 7.5F, 16777215, 16777215, (0.07F + 0.12F * nhv) * a, 0.002F);
      this.text(nl, 352.0F - nw, 10.5F, 0.62F, 16777215, (0.8F + 0.2F * nhv) * a);
      this.hits.add(new WorldPanel.Hit(346.0F - nw, 6.0F, nw, 15.0F, () -> {
         cf.menuMode = 0;
         PhoneMenu.closeNow();
         mc.setScreen(new LyricsScreen());
      }));
      float tabW = 37.0F;

      for (int i = 0; i < TABS.length; i++) {
         float x = 206.0F + i * (tabW + 4.0F);
         boolean on = this.page == i;
         float h = this.hv("tab" + i, this.over(x, 26.0F, tabW, 14.0F));
         int k = i;
         if (on) {
            this.round(x, 26.0F, tabW, 14.0F, 7.0F, 10321407, 8018664, 0.9F * a, 0.002F);
         } else {
            this.round(x, 26.0F, tabW, 14.0F, 7.0F, 16777215, 16777215, (0.05F + 0.08F * h) * a, 0.002F);
         }

         String lbl = ellip(tr(TABS[i]), tabW - 6.0F, 0.62F);
         this.text(lbl, x + (tabW - font.getWidth(lbl) * 0.62F) / 2.0F, 30.0F, 0.62F, 16777215, (on ? 1.0F : 0.7F + 0.3F * h) * a);
         this.hits.add(new WorldPanel.Hit(x, 26.0F, tabW, 14.0F, () -> this.go(k)));
      }

      String song = np.hasTrack() ? "♪  " + np.title + "  ·  " + np.artist : "♪  " + tr("nomusic");
      this.text(ellip(song, 160.0F, 0.62F), 206.0F, 208.0F, 0.62F, 14471167, 0.75F * a);
      float pa = a * outCubic(clamp((float)(System.currentTimeMillis() - this.pageTime) / 220.0F));
      switch (this.page) {
         case 0:
            this.skies(cf, pa);
            break;
         case 1:
            this.lyrics(cf, pa);
            break;
         case 2:
            this.style(cf, pa);
            break;
         default:
            this.more(mc, cf, pa);
      }
   }

   private void skies(Config cf, float a) {
      SkyPreset[] all = LyricsScreen.skyOrder();
      int per = 6;
      int pages = (all.length + per - 1) / per;
      this.skyPage = Math.floorMod(this.skyPage, pages);
      float tw = 48.0F;
      float th = 27.0F;

      for (int i = 0; i < per; i++) {
         int idx = this.skyPage * per + i;
         if (idx >= all.length) {
            break;
         }

         SkyPreset p = all[idx];
         float x = 206.0F + i % 3 * (tw + 8.0F);
         float y = 47.0F + i / 3 * (th + 14.0F);
         boolean on = cf.sky == p.ordinal();
         float h = this.hv("sky" + idx, this.over(x - 1.0F, y - 1.0F, tw + 2.0F, th + 12.0F));
         float lift = h * 1.2F;
         if (on) {
            this.round(x - 2.0F, y - 2.0F - lift, tw + 4.0F, th + 4.0F, 6.0F, 10321407, 14471167, a, 0.002F);
         } else if (h > 0.01F) {
            this.round(x - 1.5F, y - 1.5F - lift, tw + 3.0F, th + 3.0F, 5.5F, 16777215, 16777215, 0.5F * h * a, 0.002F);
         }

         Identifier img = SkyThumbs.texture(p);
         if (img != null) {
            this.image(img, x, y - lift, tw, th, a);
         } else {
            this.round(x, y - lift, tw, th, 4.0F, p.top, p.horizon, a, 0.003F);
         }

         this.text(
            ellip(Text.translatable(p.key()).getString(), tw, 0.56F), x + 1.0F, y + th + 3.0F, 0.56F, 16777215, a * (on ? 1.0F : 0.75F + 0.25F * h)
         );
         this.hits.add(new WorldPanel.Hit(x - 1.0F, y - 1.0F, tw + 2.0F, th + 12.0F, () -> {
            cf.sky = p.ordinal();
            cf.lastSky = cf.sky;
         }));
      }

      this.pager("sk", 128.0F, this.skyPage, pages, d -> this.skyPage += d, a);
      this.slider("str", tr("strength"), 148.0F, (cf.skyStrength - 0.2F) / 1.3F, v -> cf.skyStrength = 0.2F + 1.3F * v, a);
      this.slider("spd", tr("speed"), 167.0F, (cf.skySpeed - 0.25F) / 1.75F, v -> cf.skySpeed = 0.25F + 1.75F * v, a);
      this.slider("pat", tr("pattern"), 186.0F, (cf.skyPattern - 0.2F) / 1.3F, v -> cf.skyPattern = 0.2F + 1.3F * v, a);
   }

   private void lyrics(Config cf, float a) {
      this.toggle(0, tr("lyricssky"), cf.lyricsSky, () -> cf.lyricsSky = !cf.lyricsSky, a);
      this.toggle(1, tr("floating"), cf.lyricsStyle == 1, () -> cf.lyricsStyle = cf.lyricsStyle == 1 ? 0 : 1, a);
      this.toggle(2, tr("songintro"), cf.fxSongIntro, () -> cf.fxSongIntro = !cf.fxSongIntro, a);
      this.toggle(3, tr("beat"), cf.fxBeatPulse, () -> cf.fxBeatPulse = !cf.fxBeatPulse, a);
      this.toggle(4, tr("skymusic"), cf.fxSkyMusic, () -> cf.fxSkyMusic = !cf.fxSkyMusic, a);
      this.slider("lsz", tr("size"), 160.0F, (cf.lyricsSize - 0.8F) / 1.8F, v -> cf.lyricsSize = 0.8F + 1.8F * v, a);
   }

   private void style(Config cf, float a) {
      String[] hats = Accessories.HATS;
      int per = 6;
      int pages = (hats.length + per - 1) / per;
      this.hatPage = Math.floorMod(this.hatPage, pages);
      this.text(tr("hat"), 206.0F, 45.0F, 0.62F, 16777215, 0.55F * a);

      for (int i = 0; i < per; i++) {
         int idx = this.hatPage * per + i;
         if (idx >= hats.length) {
            break;
         }

         this.option(
            "hat" + idx,
            206.0F + i % 2 * 82,
            54 + i / 2 * 18,
            78.0F,
            15.0F,
            Text.translatable("neonlyrics.ui.hat." + hats[idx]).getString(),
            cf.hat == idx,
            () -> cf.hat = idx,
            a
         );
      }

      this.pager("ht", 108.0F, this.hatPage, pages, d -> this.hatPage += d, a);
      this.text(tr("death"), 206.0F, 125.0F, 0.62F, 16777215, 0.55F * a);

      for (int i = 0; i < 4; i++) {
         int k = i;
         this.option(
            "de" + i,
            206.0F + i % 2 * 82,
            134 + i / 2 * 18,
            78.0F,
            15.0F,
            Text.translatable("neonlyrics.ui.death." + i).getString(),
            cf.deathFx == i,
            () -> cf.deathFx = k,
            a
         );
      }

      this.toggle2("aur", 206.0F, 174.0F, 78.0F, tr("aura"), cf.aura, () -> cf.aura = !cf.aura, a);
      this.toggle2("hgl", 288.0F, 174.0F, 78.0F, tr("hatglow"), cf.hatGlow, () -> cf.hatGlow = !cf.hatGlow, a);
   }

   private void more(MinecraftClient mc, Config cf, float a) {
      this.toggle(0, tr("hotbar"), cf.glassHotbar, () -> cf.glassHotbar = !cf.glassHotbar, a);
      this.toggle(1, tr("island"), cf.islandEnabled, () -> cf.islandEnabled = !cf.islandEnabled, a);
      this.toggle(2, tr("clock"), cf.showClock, () -> cf.showClock = !cf.showClock, a);
      this.option("m0", 206.0F, 114.0F, 160.0F, 15.0F, "↗  " + tr("bigmenu"), false, () -> {
         PhoneMenu.closeNow();
         mc.setScreen(new LyricsScreen());
      }, a);
      this.option("m1", 206.0F, 132.0F, 160.0F, 15.0F, "☄  " + tr("random"), false, SkyPreset::shuffle, a);
      this.option("m2", 206.0F, 150.0F, 160.0F, 15.0F, "↺  " + tr("backnormal"), false, () -> {
         cf.menuMode = 0;
         this.close();
      }, a);
      this.option("m3", 206.0F, 168.0F, 160.0F, 15.0F, "✕  " + tr("close"), false, this::close, a);
   }

   private void toggle(int row, String label, boolean on, Runnable flip, float a) {
      this.toggle2("tg" + row, 206.0F, 47 + row * 20, 160.0F, label, on, flip, a);
   }

   private void toggle2(String id, float x, float y, float w, String label, boolean on, Runnable flip, float a) {
      float h = 16.0F;
      float hh = this.hv(id, this.over(x, y, w, h));
      this.round(x, y, w, h, 6.0F, 16777215, 16777215, (0.035F + 0.05F * hh) * a, 0.002F);
      this.text(ellip(label, w - 34.0F, 0.68F), x + 7.0F, y + 5.0F, 0.68F, 16777215, 0.92F * a);
      float tx = x + w - 25.0F;
      float ty = y + 4.0F;
      this.round(tx, ty, 19.0F, 8.0F, 4.0F, on ? 10321407 : 16777215, on ? 8018664 : 16777215, (on ? 0.95F : 0.16F) * a, 0.003F);
      this.ring(on ? tx + 15.0F : tx + 4.0F, ty + 4.0F, 0.0F, 3.2F, 0.0F, 360.0F, 16777215, a, 15262966, a, 0.004F, 12);
      this.hits.add(new WorldPanel.Hit(x, y, w, h, flip));
   }

   private void option(String id, float x, float y, float w, float h, String label, boolean on, Runnable run, float a) {
      float hh = this.hv(id, this.over(x, y, w, h));
      if (on) {
         this.round(x, y, w, h, 6.0F, 10321407, 8018664, 0.88F * a, 0.002F);
      } else {
         this.round(x, y, w, h, 6.0F, 16777215, 16777215, (0.035F + 0.06F * hh) * a, 0.002F);
      }

      this.text(ellip(label, w - 12.0F, 0.66F), x + 7.0F, y + (h - 6.0F) / 2.0F, 0.66F, 16777215, (on ? 1.0F : 0.8F + 0.2F * hh) * a);
      this.hits.add(new WorldPanel.Hit(x, y, w, h, run));
   }

   private void slider(String id, String label, float y, float v, Consumer<Float> set, float a) {
      v = clamp(v);
      float tx = 258.0F;
      float tw = 366.0F - tx;
      float hh = this.hv(id, this.over(206.0F, y - 4.0F, 160.0F, 14.0F));
      this.text(label, 206.0F, y, 0.66F, 16777215, (0.7F + 0.3F * hh) * a);
      this.round(tx, y + 1.5F, tw, 4.0F, 2.0F, 16777215, 16777215, (0.12F + 0.06F * hh) * a, 0.002F);
      this.round(tx, y + 1.5F, Math.max(4.0F, tw * v), 4.0F, 2.0F, 14471167, 10321407, a, 0.003F);
      this.ring(tx + tw * v, y + 3.5F, 0.0F, 4.5F + hh, 0.0F, 360.0F, 16777215, a, 15262966, a, 0.004F, 14);
      this.hits.add(new WorldPanel.Hit(tx - 4.0F, y - 4.0F, tw + 8.0F, 14.0F, () -> set.accept(clamp((this.hx - tx) / tw))));
   }

   private void roundBtn(String id, float cx, float cy, float r, String icon, float a, Runnable run) {
      float hh = this.hv(id, this.over(cx - r, cy - r, 2.0F * r, 2.0F * r));
      this.ring(cx, cy, 0.0F, r, 0.0F, 360.0F, Gfx.lerpColor(2762309, 10321407, hh), a, Gfx.lerpColor(1841460, 8018664, hh), a, 0.002F, 20);
      float tw = MinecraftClient.getInstance().textRenderer.getWidth(icon) * 0.75F;
      this.text(icon, cx - tw / 2.0F, cy - 3.0F, 0.75F, 16777215, a);
      this.hits.add(new WorldPanel.Hit(cx - r, cy - r, 2.0F * r, 2.0F * r, run));
   }

   private void pager(String id, float y, int cur, int pages, IntConsumer move, float a) {
      if (pages > 1) {
         float h1 = this.hv(id + "l", this.over(320.0F, y, 14.0F, 12.0F));
         float h2 = this.hv(id + "r", this.over(352.0F, y, 14.0F, 12.0F));
         this.round(320.0F, y, 14.0F, 12.0F, 6.0F, 16777215, 16777215, (0.08F + 0.2F * h1) * a, 0.002F);
         this.round(352.0F, y, 14.0F, 12.0F, 6.0F, 16777215, 16777215, (0.08F + 0.2F * h2) * a, 0.002F);
         this.text("‹", 325.0F, y + 2.5F, 0.75F, 16777215, a);
         this.text("›", 357.0F, y + 2.5F, 0.75F, 16777215, a);
         this.text(cur + 1 + "/" + pages, 336.0F, y + 3.0F, 0.6F, 16777215, 0.7F * a);
         this.hits.add(new WorldPanel.Hit(320.0F, y, 14.0F, 12.0F, () -> move.accept(-1)));
         this.hits.add(new WorldPanel.Hit(352.0F, y, 14.0F, 12.0F, () -> move.accept(1)));
      }
   }

   private float lx(float x) {
      return (x - this.ox) * 0.0105F;
   }

   private float ly(float y) {
      return (this.oy - y) * 0.0105F;
   }

   private void vert(float x, float y, float z, int col) {
      this.q.vertex(this.m, this.lx(x), this.ly(y), z).color(col);
   }

   private void quad(float x, float y, float w, float h, int cTop, float aTop, int cBot, float aBot, float z) {
      int t = Gfx.argbRaw(cTop, aTop);
      int b = Gfx.argbRaw(cBot, aBot);
      this.vert(x, y, z, t);
      this.vert(x + w, y, z, t);
      this.vert(x + w, y + h, z, b);
      this.vert(x, y + h, z, b);
   }

   private void poly(float x0, float y0, float x1, float y1, float x2, float y2, float x3, float y3, int rgb, float a, float z) {
      int col = Gfx.argbRaw(rgb, a);
      this.vert(x0, y0, z, col);
      this.vert(x1, y1, z, col);
      this.vert(x2, y2, z, col);
      this.vert(x3, y3, z, col);
   }

   private void ring(float cx, float cy, float r0, float r1, float a0, float a1, int cIn, float aIn, int cOut, float aOut, float z, int segs) {
      if (!(aIn <= 0.003F) || !(aOut <= 0.003F)) {
         int ci = Gfx.argbRaw(cIn, aIn);
         int co = Gfx.argbRaw(cOut, aOut);

         for (int i = 0; i < segs; i++) {
            double t0 = Math.toRadians(a0 + (a1 - a0) * i / segs);
            double t1 = Math.toRadians(a0 + (a1 - a0) * (i + 1) / segs);
            float c0 = (float)Math.cos(t0);
            float s0 = (float)Math.sin(t0);
            float c1 = (float)Math.cos(t1);
            float s1 = (float)Math.sin(t1);
            this.vert(cx + c0 * r0, cy + s0 * r0, z, ci);
            this.vert(cx + c0 * r1, cy + s0 * r1, z, co);
            this.vert(cx + c1 * r1, cy + s1 * r1, z, co);
            this.vert(cx + c1 * r0, cy + s1 * r0, z, ci);
         }
      }
   }

   private void round(float x, float y, float w, float h, float r, int top, int bot, float a, float z) {
      if (!(a <= 0.003F)) {
         r = Math.min(r, Math.min(w, h) / 2.0F);
         int n = r > 3.0F ? 5 : 2;
         float step = r / n;

         for (int j = 0; j < n; j++) {
            float yc = r - (j + 0.5F) * step;
            float in = r - (float)Math.sqrt(Math.max(0.0F, r * r - yc * yc));
            this.band(x + in, y + j * step, w - 2.0F * in, step, y, h, top, bot, a, z);
            this.band(x + in, y + h - (j + 1) * step, w - 2.0F * in, step, y, h, top, bot, a, z);
         }

         this.band(x, y + r, w, h - 2.0F * r, y, h, top, bot, a, z);
      }
   }

   private void band(float x, float y, float w, float h, float oy0, float oh, int top, int bot, float a, float z) {
      if (!(h <= 0.0F) && !(w <= 0.0F)) {
         this.quad(x, y, w, h, Gfx.lerpColor(top, bot, (y - oy0) / oh), a, Gfx.lerpColor(top, bot, (y + h - oy0) / oh), a, z);
      }
   }

   private void image(Identifier tex, float x, float y, float w, float h, float a) {
      Matrix4f mm = new Matrix4f(this.m);
      float x0 = this.lx(x);
      float x1 = this.lx(x + w);
      float y0 = this.ly(y);
      float y1 = this.ly(y + h);
      float z = 0.005F;
      int col = Gfx.argbRaw(16777215, a);
      this.late.add(() -> {
         VertexConsumer v = MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers().getBuffer(RenderLayers.text(tex));
         v.vertex(mm, x0, y0, z).color(col).texture(0.0F, 0.0F).light(15728880);
         v.vertex(mm, x0, y1, z).color(col).texture(0.0F, 1.0F).light(15728880);
         v.vertex(mm, x1, y1, z).color(col).texture(1.0F, 1.0F).light(15728880);
         v.vertex(mm, x1, y0, z).color(col).texture(1.0F, 0.0F).light(15728880);
      });
   }

   private void text(String s, float x, float y, float scale, int rgb, float a) {
      this.textAt(this.m, s, this.lx(x) / 0.0105F, -this.ly(y) / 0.0105F, scale, rgb, a, false);
   }

   private void textAt(Matrix4f mat, String s, float px, float py, float scale, int rgb, float a, boolean centered) {
      if (!(a <= 0.01F) && !s.isEmpty()) {
         TextRenderer f = MinecraftClient.getInstance().textRenderer;
         float x = centered ? px - f.getWidth(s) * scale / 2.0F : px;
         Matrix4f tm = new Matrix4f(mat).translate(x * 0.0105F, -py * 0.0105F, 0.012F).scale(0.0105F * scale, -0.0105F * scale, 0.0105F);
         int col = Gfx.argbRaw(rgb, a);
         this.late
            .add(
               () -> {
                  MinecraftClient mc = MinecraftClient.getInstance();
                  mc.textRenderer
                     .draw(
                        Text.literal(s), 0.0F, 0.0F, col, false, tm, mc.getBufferBuilders().getEntityVertexConsumers(), TextLayerType.NORMAL, 0, 15728880
                     );
               }
            );
      }
   }

   private static String ellip(String s, float maxPx, float scale) {
      TextRenderer f = MinecraftClient.getInstance().textRenderer;
      if (f.getWidth(s) * scale <= maxPx) {
         return s;
      }

      String t = s;

      while (t.length() > 1 && f.getWidth(t + "...") * scale > maxPx) {
         t = t.substring(0, t.length() - 1);
      }

      return t + "...";
   }

   private static float clamp(float x) {
      return Math.max(0.0F, Math.min(1.0F, x));
   }

   private static float outCubic(float t) {
      float u = 1.0F - t;
      return 1.0F - u * u * u;
   }

   private static float inOut(float t) {
      return t < 0.5F ? 4.0F * t * t * t : 1.0F - (float)Math.pow(-2.0F * t + 2.0F, 3.0) / 2.0F;
   }

   private record Hit(float x, float y, float w, float h, Runnable run) {
      private Hit {
      }
   }
}
