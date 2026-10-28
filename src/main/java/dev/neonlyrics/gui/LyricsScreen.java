package dev.neonlyrics.gui;

import dev.neonlyrics.NeonLyrics;
import dev.neonlyrics.config.Config;
import dev.neonlyrics.cosmetic.Accessories;
import dev.neonlyrics.cosmetic.DeathFx;
import dev.neonlyrics.cosmetic.PhoneMenu;
import dev.neonlyrics.hud.Anim;
import dev.neonlyrics.hud.Cover;
import dev.neonlyrics.hud.Gfx;
import dev.neonlyrics.hud.Icons;
import dev.neonlyrics.hud.Island;
import dev.neonlyrics.hud.SkyThumbs;
import dev.neonlyrics.hud.Theme;
import dev.neonlyrics.lyrics.Favorites;
import dev.neonlyrics.lyrics.LocalMedia;
import dev.neonlyrics.lyrics.NowPlaying;
import dev.neonlyrics.lyrics.SpotifyClient;
import dev.neonlyrics.sky.SkyPreset;
import dev.neonlyrics.sky.WorldSky;
import dev.neonlyrics.util.AutoPerf;
import dev.neonlyrics.perf.PerfBoost;
import dev.neonlyrics.perf.PerfSettings;
import dev.neonlyrics.util.Fx;
import dev.neonlyrics.util.Profiles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.gui.Click;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;
import net.minecraft.text.Text;
import net.minecraft.text.Style;
import net.minecraft.util.Identifier;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.StyleSpriteSource.Font;
import org.joml.Matrix3x2f;
import org.joml.Vector2f;

public class LyricsScreen extends Screen {
   private static final int COL_BG = 1118484;
   private static final int GROUP = 1710623;
   private static final int GROUP_HI = 2236968;
   private static final int LINE = 2500140;
   private static final int TEXT = 15658737;
   private static final int SUB = 8618892;
   private static final int DIM = 5000276;
   private static final int TRACK = 3421243;
   private static final int KNOB_OFF = 9079442;
   private static final int INK = 1118484;
   private static final Map<LyricsScreen.T, StyleSpriteSource> FONTS = new EnumMap<>(LyricsScreen.T.class);
   private final Map<LyricsScreen.T, Map<String, Text>> comps = new EnumMap<>(LyricsScreen.T.class);
   private final Map<LyricsScreen.T, Map<String, Integer>> widths = new EnumMap<>(LyricsScreen.T.class);
   private static final SkyPreset[] SKY_ORDER = new SkyPreset[]{
      SkyPreset.INFERNO,
      SkyPreset.MATRIX,
      SkyPreset.PRISM,
      SkyPreset.PULSAR,
      SkyPreset.AQUARELL,
      SkyPreset.SYNTHWAVE,
      SkyPreset.AURORA,
      SkyPreset.NEBULA,
      SkyPreset.BLACK_HOLE,
      SkyPreset.COSMOS,
      SkyPreset.NEON_CITY,
      SkyPreset.GOLDEN_HOUR,
      SkyPreset.COTTON_CANDY,
      SkyPreset.SAKURA,
      SkyPreset.FROST,
      SkyPreset.MIDNIGHT,
      SkyPreset.DEEP_SEA,
      SkyPreset.BLOOD_MOON,
      SkyPreset.STORM,
      SkyPreset.CUSTOM
   };
   private static final int[] DARK_COLS = new int[]{131848, 460058, 656920, 1378328, 201240, 1053982, 1839672, 2758216, 4068404, 668480, 1784412, 4852242};
   private static final int[] BRIGHT = new int[]{
      10719743, 13716479, 16743112, 16739950, 16757596, 16769658, 4652976, 3073750, 8177919, 4942847, 16184063, 16777215
   };
   private static final String[] LANGS = new String[]{"de", "en", "es", "fr", "tr", "pl", "it", "ru"};
   private static final int[] TARGETS = new int[]{0, 60, 90, 120, 144, 240};
   private static final int RAIL = 56;
   private static final int ROW = 38;
   private static LyricsScreen.Page lastPage = LyricsScreen.Page.SKY;
   private LyricsScreen.Page page;
   private long pageTime;
   private final long openTime = System.currentTimeMillis();
   private long closeTime = -1L;
   private int dir = 1;
   private final Anim.Pool pool = new Anim.Pool(12.0);
   private final Map<String, Long> pressed = new HashMap<>();
   private final List<LyricsScreen.Hit> hits = new ArrayList<>();
   private final List<LyricsScreen.Slider> sliders = new ArrayList<>();
   private LyricsScreen.Slider activeSlider;
   private int mx;
   private int my;
   private float S = 1.0F;
   private int W;
   private int H;
   private int colX;
   private int colY;
   private int colW;
   private int colH;
   private int vx;
   private int vy;
   private int vw;
   private int vh;
   private float stageK = 0.0F;
   private float scroll = 0.0F;
   private float scrollTarget = 0.0F;
   private int contentH = 0;
   private int ly;
   private int rowIdx;
   private float rowA = 1.0F;
   private boolean inContent;
   private boolean firstInGroup;
   private long lastNs = System.nanoTime();
   private DrawContext gfx;
   private TextFieldWidget idBox;
   private int idX;
   private int idY;
   private int idW;
   private boolean idWanted;
   private String lastTrack = "";
   private long trackAt = 0L;
   private float spin = 0.0F;
   private float vizLevel = 0.0F;
   private float frameDt = 0.016F;
   private final Map<String, float[]> springs = new HashMap<>();
   private final Map<String, LyricsScreen.Ripple> ripples = new HashMap<>();
   private final Map<Integer, Long> seen = new HashMap<>();
   private String curRid;
   private boolean floating;
   private final Matrix3x2f floatInv = new Matrix3x2f();

   private static StyleSpriteSource fd(String name) {
      return new Font(Identifier.of("neonlyrics", name));
   }

   public static SkyPreset[] skyOrder() {
      return (SkyPreset[])SKY_ORDER.clone();
   }

   public LyricsScreen() {
      this(lastPage);
   }

   public LyricsScreen(boolean floating) {
      this(lastPage);
      this.floating = floating;
   }

   public LyricsScreen(LyricsScreen.Page page) {
      super(Text.literal("Lyrics Cord"));
      this.page = page;
      this.pageTime = System.currentTimeMillis() + 520L;
   }

   private static Config c() {
      return NeonLyrics.cfg();
   }

   private static String t(String key, Object... args) {
      return Fx.t("neonlyrics." + key, args);
   }

   protected void init() {
      this.layout(SpotifyClient.now().hasTrack() ? 1.0F : 0.0F);
      this.stageK = SpotifyClient.now().hasTrack() ? 1.0F : 0.0F;
      this.idBox = new TextFieldWidget(this.textRenderer, 0, -200, 100, 12, Text.literal(""));
      this.idBox.setDrawsBackground(false);
      this.idBox.setMaxLength(64);
      this.idBox.setEditableColor(-1118479);
      this.idBox.setText(c().spotifyClientId);
      this.idBox.setPlaceholder(Text.literal(t("hint.clientid")).formatted(Formatting.DARK_GRAY));
      this.addDrawableChild(this.idBox);
      show(this.idBox, false);
   }

   private void layout(float k) {
      this.S = Math.min(1.0F, Math.min(this.width / 640.0F, this.height / 350.0F));
      this.W = Math.round(this.width / this.S);
      this.H = Math.round(this.height / this.S);
      int wMusic = Math.max(330, Math.min(470, Math.round(this.W * 0.46F)));
      int wAlone = Math.min(this.W - 60, 640);
      this.colW = Math.round(wAlone + (wMusic - wAlone) * k);
      int xMusic = this.W - wMusic - 16;
      int xAlone = (this.W - wAlone) / 2;
      this.colX = Math.round(xAlone + (xMusic - xAlone) * k);
      this.colY = 12;
      this.colH = this.H - 24;
      this.vx = this.colX + 56 + 18;
      this.vw = this.colW - 56 - 18 - 16;
      this.vy = this.colY + 50;
      this.vh = this.colH - 50 - 10;
   }

   private static void show(TextFieldWidget b, boolean v) {
      b.visible = v;
      b.active = v;
      if (!v && b.isFocused()) {
         b.setFocused(false);
      }
   }

   private void go(LyricsScreen.Page p) {
      if (p != this.page) {
         this.dir = p.ordinal() >= this.page.ordinal() ? 1 : -1;
         this.page = p;
         lastPage = p;
         this.pageTime = System.currentTimeMillis();
         this.scroll = this.scrollTarget = 0.0F;
         this.seen.clear();
         this.setFocused(null);
         Fx.sound(Fx.Sfx.CLICK);
      }
   }

   private boolean anim() {
      return c().fxMenuAnim;
   }

   private float since() {
      return this.anim() ? (float)(System.currentTimeMillis() - this.openTime) / 1000.0F : 9.0F;
   }

   private float closeK() {
      if (this.closeTime < 0L) {
         return 1.0F;
      } else {
         return this.anim() ? 1.0F - Anim.outCubic(Anim.since(this.closeTime, 160L)) : 0.0F;
      }
   }

   private static float oc(float x) {
      return Anim.outCubic(Anim.clamp(x));
   }

   private static float ob(float x) {
      return Anim.outBack(Anim.clamp(x));
   }

   public void renderBackground(DrawContext g, int mouseX, int mouseY, float delta) {
      if (!this.floating) {
         float a = oc(this.since() / 0.3F) * this.closeK();
         boolean blur = c().blur;
         if (blur && this.client != null && this.client.world != null) {
            super.renderBackground(g, mouseX, mouseY, delta);
         }

         Gfx.alpha = 1.0F;
         int steps = 24;

         for (int i = 0; i < steps; i++) {
            float f = i / (steps - 1.0F);
            float al = f < 0.55F ? 0.86F + -0.31F * (f / 0.55F) : 0.55F + 0.25F * ((f - 0.55F) / 0.45F);
            if (this.stageK < 0.99F) {
               al += (0.62F - al) * (1.0F - this.stageK);
            }

            int x0 = Math.round((float)(this.width * i) / steps);
            int x1 = Math.round((float)(this.width * (i + 1)) / steps);
            Gfx.rect(g, x0, 0, x1 - x0, this.height, 263176, al * a);
         }

         float fl = Math.max(0.0F, 1.0F - Math.abs(this.since() - 0.12F) / 0.12F);
         if (fl > 0.01F) {
            Gfx.rect(g, 0, 0, this.width, this.height, 16777215, 0.1F * fl);
         }
      }
   }

   public void render(DrawContext g, int mouseX, int mouseY, float delta) {
      if (!this.floating) {
         this.renderBody(g, mouseX, mouseY, delta);
      } else {
         float t = this.since();
         float ck = this.closeK();
         float wk = this.anim() ? oc(t / 0.22F) : 1.0F;
         float hk = this.anim() ? oc((t - 0.12F) / 0.38F) : 1.0F;
         float sx = 0.08F + 0.92F * wk;
         float sy = (0.03F + 0.97F * hk) * (0.03F + 0.97F * ck);
         float s0 = 0.84F;
         float bob = (float)Math.sin(System.currentTimeMillis() / 900.0) * this.height * 0.005F;
         Matrix3x2f m = new Matrix3x2f()
            .translate(this.width / 2.0F, this.height / 2.0F + bob)
            .mul(new Matrix3x2f(1.0F, -0.035F, 0.0F, 1.0F, 0.0F, 0.0F))
            .scale(s0 * sx, s0 * sy)
            .translate(-this.width / 2.0F, -this.height / 2.0F);
         m.invert(this.floatInv);
         Vector2f mp = this.floatInv.transformPosition(mouseX, mouseY, new Vector2f());
         g.getMatrices().pushMatrix();
         g.getMatrices().mul(m);
         float a = Math.min(1.0F, t / 0.15F) * ck;
         Gfx.glow(g, 0, 0, this.width, this.height, 22, 10321407, 0.55F * a, 18);
         Gfx.aaRound(g, 0.0F, 0.0F, this.width, this.height, 22.0F, 920352, 0.86F * a);
         Gfx.aaRound(g, 1.0F, 1.0F, this.width - 2, this.height / 2.0F, 21.0F, 16777215, 0.025F * a);
         this.renderBody(g, Math.round(mp.x), Math.round(mp.y), delta);
         g.getMatrices().popMatrix();
      }
   }

   private Click fm(Click e) {
      if (!this.floating) {
         return e;
      }

      Vector2f p = this.floatInv.transformPosition((float)e.x(), (float)e.y(), new Vector2f());
      return new Click(p.x, p.y, e.buttonInfo());
   }

   public void removed() {
      super.removed();
      if (this.floating) {
         PhoneMenu.menuClosed();
      }
   }

   private void renderBody(DrawContext g, int mouseX, int mouseY, float delta) {
      if (this.closeTime < 0L || this.anim() && System.currentTimeMillis() - this.closeTime <= 170L) {
         long ns = System.nanoTime();
         float dt = Math.min(0.1F, (float)(ns - this.lastNs) / 1.0E9F);
         this.lastNs = ns;
         this.frameDt = dt;
         NowPlaying np = SpotifyClient.now();
         this.stageK = this.pool.to("stage", np.hasTrack() ? 1.0 : 0.0);
         if (!this.anim()) {
            this.stageK = np.hasTrack() ? 1.0F : 0.0F;
         }

         this.layout(this.stageK);
         this.mx = Math.round(mouseX / this.S);
         this.my = Math.round(mouseY / this.S);
         this.gfx = g;
         Gfx.revealOn = false;
         if (this.activeSlider != null) {
            if (!this.client.mouse.wasLeftButtonClicked()) {
               this.activeSlider = null;
               NeonLyrics.saveConfig();
            } else {
               this.activeSlider.set().accept(Anim.clamp((float)(this.mx - this.activeSlider.x()) / this.activeSlider.w()));
            }
         }

         this.hits.clear();
         this.sliders.clear();
         this.idWanted = false;
         float t = this.since();
         float ck = this.closeK();
         g.getMatrices().pushMatrix();
         g.getMatrices().scale(this.S, this.S);
         Gfx.alpha = ck;
         if (this.stageK > 0.01F) {
            this.stage(g, np, t, dt);
         }

         float co = this.anim() ? ob((t - 0.3F) / 0.5F) : 1.0F;
         float slide = (1.0F - co) * (this.W - this.colX + 20) + (1.0F - ck) * 30.0F;
         g.getMatrices().pushMatrix();
         g.getMatrices().translate(slide, 0.0F);
         if (co < 0.999F || ck < 0.999F) {
            float sc = 0.92F + 0.08F * Math.min(co, 1.02F) - 0.04F * (1.0F - ck);
            float ccx = this.colX + this.colW / 2.0F;
            float ccy = this.colY + this.colH / 2.0F;
            g.getMatrices().translate(ccx, ccy);
            g.getMatrices().rotate((1.0F - Anim.clamp(co)) * 0.06F);
            g.getMatrices().scale(sc, sc);
            g.getMatrices().translate(-ccx, -ccy);
         }

         this.column(g, dt);
         g.getMatrices().popMatrix();
         g.getMatrices().popMatrix();
         Gfx.alpha = 1.0F;
         float pa = this.anim() ? Anim.outCubic(Anim.since(this.pageTime, 260L)) : 1.0F;
         boolean idVis = this.idWanted && this.closeTime < 0L && co > 0.98F && pa > 0.95F && this.idY >= this.vy && this.idY + 12 <= this.vy + this.vh;
         show(this.idBox, idVis);
         if (idVis) {
            this.idBox.setX(Math.round(this.idX * this.S));
            this.idBox.setY(Math.round(this.idY * this.S));
            this.idBox.setWidth(Math.max(20, Math.round(this.idW * this.S)));
            c().spotifyClientId = this.idBox.getText().trim();
         }

         super.render(g, mouseX, mouseY, delta);
      } else {
         super.close();
      }
   }

   private void stage(DrawContext g, NowPlaying np, float t, float dt) {
      Config c = c();
      long now = System.currentTimeMillis();
      String track = String.valueOf(np.trackId);
      if (!track.equals(this.lastTrack)) {
         this.lastTrack = track;
         this.trackAt = now;
      }

      float base = Gfx.alpha;
      float sk = this.stageK;
      int shift = Math.round((1.0F - sk) * -40.0F);
      int x0 = 40 + shift;
      int x1 = this.colX - 34;
      int areaW = Math.max(120, x1 - x0);
      int cs = Math.round(Math.min(150.0F, Math.min(this.H * 0.38F, areaW * 0.46F)));
      int cy = Math.round(this.H * 0.12F);
      if (np.playing) {
         this.spin += dt * 70.0F;
      }

      float vin = this.anim() ? oc((t - 0.4F) / 0.5F) : 1.0F;
      float vr = cs * 0.47F;
      float vcx = x0 + cs / 2.0F + vin * cs * 0.6F;
      float vcy = cy + cs / 2.0F;
      if (vin > 0.01F) {
         Gfx.alpha = base * sk * Anim.clamp((t - 0.4F) / 0.1F);
         Gfx.aaCircle(g, vcx, vcy, vr, 789518, 1.0F);

         for (int i = 1; i <= 7; i++) {
            Gfx.aaCircle(g, vcx, vcy, vr * (1.0F - 0.075F * i), (i & 1) == 1 ? 1513243 : 789518, 1.0F);
         }

         float ls = vr * 0.62F;
         g.getMatrices().pushMatrix();
         g.getMatrices().translate(vcx, vcy);
         g.getMatrices().rotate((float)Math.toRadians(this.spin));
         Gfx.aaRound(g, -vr * 0.92F, -0.75F, vr * 1.84F, 1.5F, 0.75F, 16777215, 0.05F);
         Gfx.aaRound(g, -0.75F, -vr * 0.92F, 1.5F, vr * 1.84F, 0.75F, 16777215, 0.03F);
         if (!Cover.draw(g, Math.round(-ls / 2.0F), Math.round(-ls / 2.0F), Math.round(ls), 1.0F)) {
            Gfx.aaCircle(g, 0.0F, 0.0F, ls / 2.0F, 2763315, 1.0F);
            Gfx.aaCircle(g, ls * 0.18F, -ls * 0.1F, 2.0F, 16777215, 0.6F);
         }

         Gfx.aaCircle(g, 0.0F, 0.0F, 2.2F, 789518, 1.0F);
         g.getMatrices().popMatrix();
      }

      float sl = this.anim() ? ob((t - 0.05F) / 0.45F) : 1.0F;
      float trackIn = this.anim() ? oc((float)(now - this.trackAt) / 450.0F) : 1.0F;
      Gfx.alpha = base * sk * Anim.clamp((t - 0.05F) / 0.2F);
      g.getMatrices().pushMatrix();
      g.getMatrices().translate(x0 + cs / 2.0F, cy + cs / 2.0F + (1.0F - sl) * 60.0F);
      g.getMatrices().rotate((float)Math.toRadians((1.0F - sl) * -14.0F));
      float pop = (0.94F + 0.06F * trackIn) * (0.8F + 0.2F * sl);
      g.getMatrices().scale(pop, pop);
      Gfx.aaRound(g, -cs / 2.0F - 2.0F, -cs / 2.0F + 4.0F, cs + 4, cs + 4, 10.0F, 0, 0.35F);
      if (!Cover.drawSquare(g, -cs / 2, -cs / 2, cs, 1.0F)) {
         Gfx.aaRound(g, -cs / 2.0F, -cs / 2.0F, cs, cs, 8.0F, 1973796, 1.0F);
         Icons.draw(g, Icons.Icon.LYRICS, 0.0F, 0.0F, cs * 0.26F, 4539725, 1.0F);
      }

      g.getMatrices().popMatrix();
      int ty = cy + cs + 20;
      float meta = this.anim() ? oc((t - 0.55F) / 0.4F) : 1.0F;
      Gfx.alpha = base * sk * meta * trackIn;
      int lift = Math.round((1.0F - meta * trackIn) * 14.0F);
      float twipe = this.anim() ? Anim.outCubic(Anim.clamp((t - 0.55F) / 0.5F)) * trackIn : 1.0F;
      String ttl = this.ell(np.title, areaW, LyricsScreen.T.XL);
      if (twipe < 0.999F) {
         g.enableScissor(x0 - 2, ty - 4, x0 + Math.round((this.tw(ttl, LyricsScreen.T.XL) + 6) * twipe), ty + 22);
      }

      this.txt(ttl, x0, ty + lift, 15658737, 1.0F, LyricsScreen.T.XL);
      if (twipe < 0.999F) {
         g.disableScissor();
      }

      this.txt(this.ell(np.artist, areaW, LyricsScreen.T.M), x0, ty + 24 + lift, 8618892, 1.0F, LyricsScreen.T.M);
      float lyA = this.anim() ? oc((t - 0.7F) / 0.4F) : 1.0F;
      Gfx.alpha = base * sk * lyA;
      int lyY = ty + 46 + Math.round((1.0F - lyA) * 14.0F);
      if (np.lyricsState == NowPlaying.LyricsState.SYNCED) {
         long prog = np.progressNow(c.lyricsOffsetMs);
         int li = np.lineIndex(prog);
         if (li >= 0 && li < np.lines.size() && !np.lines.get(li).text().isBlank()) {
            NowPlaying.Line ln = np.lines.get(li);
            long end = li + 1 < np.lines.size() ? np.lines.get(li + 1).timeMs() : ln.timeMs() + 4000L;
            float frac = Anim.clamp((float)(prog - ln.timeMs()) / (float)Math.max(400L, Math.min(end - ln.timeMs(), 6000L) - 200L));
            String[] words = ln.text().split(" ");
            int sung = (int)Math.ceil(frac * words.length);
            if (ln.wordTimes() != null && ln.wordTimes().length == words.length) {
               sung = 0;

               for (long wt : ln.wordTimes()) {
                  if (wt <= prog) {
                     sung++;
                  }
               }
            }

            float la = Anim.clamp(this.pool.toFrom("ln" + li + track, 0.0, 1.0));
            int yy = lyY + Math.round((1.0F - la) * 8.0F);
            float xx = x0;
            int lines = 0;
            int space = this.tw(" ", LyricsScreen.T.TITLE);

            for (int k = 0; k < words.length && lines < 2; k++) {
               int ww = this.tw(words[k], LyricsScreen.T.TITLE);
               if (xx + ww > x0 + areaW && xx > x0) {
                  xx = x0;
                  yy += 15;
                  if (++lines >= 2) {
                     break;
                  }
               }

               this.txt(words[k], xx, yy, k < sung ? 15658737 : 5921378, la, LyricsScreen.T.TITLE);
               xx += ww + space;
            }

            if (li + 1 < np.lines.size()) {
               this.txt(this.ell(np.lines.get(li + 1).text(), areaW, LyricsScreen.T.M), x0, yy + 19, 5921378, la, LyricsScreen.T.M);
            }
         }
      } else {
         String st = switch (np.lyricsState) {
            case LOADING -> t("spotify.ly.loading");
            case NOT_FOUND -> t("spotify.ly.none");
            case ERROR -> t("spotify.ly.error");
            default -> "";
         };
         this.txt(this.ell(st, areaW, LyricsScreen.T.M), x0, lyY, 8618892, 1.0F, LyricsScreen.T.M);
      }

      int bars = 28;
      int bw = 3;
      int gap = 3;
      int bh = 34;
      int by = this.H - 26;
      long prog = np.progressNow(c.lyricsOffsetMs);
      float beat = np.playing ? np.beatPulse(prog) : 0.0F;
      this.vizLevel = this.vizLevel + ((np.playing ? 1.0F : 0.12F) - this.vizLevel) * (1.0F - (float)Math.exp(-6.0F * dt));
      double tt = now / 1000.0;

      for (int i = 0; i < bars; i++) {
         float k = this.anim() ? oc((t - 0.5F - i * 0.012F) / 0.3F) : 1.0F;
         double wave = Math.abs(Math.sin(i * 0.7 + tt * 6.0) * Math.sin(i * 0.23 + tt * 3.1));
         float h = 3.0F + (bh - 3) * (float)wave * this.vizLevel * (0.55F + 0.6F * beat) * k;
         Gfx.alpha = base * sk * (0.3F + 0.6F * k);
         Gfx.aaRound(g, x0 + i * (bw + gap), by - h, bw, h, 1.5F, 16777215, 0.85F);
      }

      Gfx.alpha = base * sk * (this.anim() ? oc((t - 0.75F) / 0.3F) : 1.0F);
      int cx = x0 + bars * (bw + gap) + 24;
      int ccy = by - 14;
      this.iconBtn(g, "prev", cx, ccy - 10, 20, Icons.Icon.PREV, 8.0F, () -> SpotifyClient.control(SpotifyClient.Cmd.PREVIOUS));
      float hv = this.hover(cx + 28, ccy - 14, 28, 28, "play");
      float press = 1.0F - Anim.outCubic(Anim.since(this.pressed.getOrDefault("play", 0L), 220L));
      Gfx.aaCircle(g, cx + 42, ccy, 14.0F * (1.0F + 0.05F * hv) * (1.0F - 0.08F * press), 16777215, 1.0F);
      Icons.draw(g, np.playing ? Icons.Icon.PAUSE : Icons.Icon.PLAY, cx + 42 + (np.playing ? 0.0F : 0.8F), ccy, 10.0F, 1118484, 1.0F);
      this.hit(cx + 28, ccy - 14, 28, 28, () -> {
         this.pressed.put("play", System.currentTimeMillis());
         SpotifyClient.control(SpotifyClient.Cmd.PLAYPAUSE);
      });
      this.iconBtn(g, "next", cx + 64, ccy - 10, 20, Icons.Icon.NEXT, 8.0F, () -> SpotifyClient.control(SpotifyClient.Cmd.NEXT));
      this.iconBtn(g, "fav", cx + 92, ccy - 10, 20, Icons.Icon.FAV, 8.0F, Favorites::saveCurrent);
      Gfx.alpha = base;
   }

   private Icons.Icon icon(LyricsScreen.Page p) {
      return switch (p) {
         case SKY -> Icons.Icon.SKY;
         case LYRICS -> Icons.Icon.TEXT;
         case STYLE -> Icons.Icon.HAT;
         case SPOTIFY -> Icons.Icon.SPOTIFY;
         case DISPLAY -> Icons.Icon.DISPLAY;
         case MENU -> Icons.Icon.SETTINGS;
         case PERF -> Icons.Icon.PERF;
         case FAV -> Icons.Icon.FAV;
      };
   }

   private String title(LyricsScreen.Page p) {
      return t("ui.page." + p.name().toLowerCase(Locale.ROOT));
   }

   private String subtitle(LyricsScreen.Page p) {
      return p == LyricsScreen.Page.SKY ? t("ui.page.sky.sub", SKY_ORDER.length) : t("ui.page." + p.name().toLowerCase(Locale.ROOT) + ".sub");
   }

   private void column(DrawContext g, float dt) {
      Gfx.aaRound(g, this.colX - 1, this.colY - 1, this.colW + 2, this.colH + 2, 15.0F, 16777215, 0.1F);
      Gfx.aaRound(g, this.colX, this.colY, this.colW, this.colH, 14.0F, 1118484, 0.94F);
      Gfx.vGradient(g, this.colX + 10, this.colY + 1, this.colW - 20, 40, 16777215, 16777215, 0.05F, 0.0F);
      Gfx.rect(g, this.colX + 56, this.colY + 12, 1, this.colH - 24, 2500140, 1.0F);
      int ry = this.colY + 10;
      int rh = Math.min(44, (this.colH - 20) / LyricsScreen.Page.values().length - 2);
      float sel = this.spring("railsel", this.page.ordinal());
      float vel = this.springs.get("railsel")[1];
      float stretch = Math.min(10.0F, Math.abs(vel) * 0.9F);
      Gfx.aaRound(g, this.colX + 6, ry + sel * (rh + 2) - stretch / 2.0F, 44.0F, rh + stretch, 10.0F, 16777215, 1.0F);

      for (LyricsScreen.Page p : LyricsScreen.Page.values()) {
         int y = ry + p.ordinal() * (rh + 2);
         float on = Anim.clamp(1.0F - Math.abs(sel - p.ordinal()));
         float hv = this.hover(this.colX + 6, y, 44, rh, "rail" + p.name());
         if (p != this.page && hv > 0.01F) {
            Gfx.aaRound(g, this.colX + 6, y, 44.0F, rh, 10.0F, 16777215, 0.05F * hv);
         }

         int fg = Gfx.lerpColor(Gfx.lerpColor(8618892, 13158606, hv), 1118484, on);
         float intro = this.anim() ? Anim.outBack(Anim.clamp((this.since() - 0.55F - p.ordinal() * 0.06F) / 0.35F)) : 1.0F;
         float bounce = (1.0F + 0.18F * (1.0F - Anim.outCubic(Anim.since(this.pressed.getOrDefault("rail" + p.name(), 0L), 380L))) + 0.08F * hv)
            * Math.max(0.01F, intro);
         Icons.draw(g, this.icon(p), this.colX + 28.0F, y + rh / 2.0F - 5.0F - hv, 10.0F * bounce, fg, 1.0F);
         this.txtC(this.ell(this.title(p), 42, LyricsScreen.T.S), this.colX + 28.0F, y + rh / 2.0F + 5.0F, fg, 1.0F, LyricsScreen.T.S);
         this.hit(this.colX + 6, y, 44, rh, () -> {
            this.pressed.put("rail" + p.name(), System.currentTimeMillis());
            this.go(p);
         });
      }

      int xx = this.colX + this.colW - 28;
      int xy = this.colY + 12;
      float hvx = this.hover(xx, xy, 18, 18, "close");
      Gfx.aaCircle(g, xx + 9, xy + 9, 9.0F, 16777215, 0.05F + 0.07F * hvx);
      g.getMatrices().pushMatrix();
      g.getMatrices().translate(xx + 9, xy + 9);
      g.getMatrices().rotate((float)(hvx * Math.PI / 2.0));
      Icons.draw(g, Icons.Icon.CLOSE, 0.0F, 0.0F, 6.5F, Gfx.lerpColor(8618892, 15658737, hvx), 1.0F);
      g.getMatrices().popMatrix();
      this.hit(xx, xy, 18, 18, this::close);
      float pa = this.anim() ? Anim.outCubic(Anim.since(this.pageTime, 260L)) : 1.0F;
      int hx = this.vx + Math.round((1.0F - pa) * 8.0F * this.dir);
      float wipe = this.anim() ? Anim.outCubic(Anim.since(this.pageTime, 420L)) : 1.0F;
      int tw0 = this.tw(this.title(this.page), LyricsScreen.T.TITLE);
      if (wipe < 0.999F) {
         g.enableScissor(hx - 2, this.colY + 8, hx + Math.round((tw0 + 6) * wipe), this.colY + 30);
      }

      this.txt(this.title(this.page), hx, this.colY + 15, 15658737, pa, LyricsScreen.T.TITLE);
      if (wipe < 0.999F) {
         g.disableScissor();
         Gfx.aaRound(g, hx + (tw0 + 4) * wipe, this.colY + 13, 1.5F, 12.0F, 0.75F, 16777215, 0.8F * (1.0F - wipe));
      }

      float sa = this.anim() ? Anim.outCubic(Anim.since(this.pageTime + 120L, 300L)) : 1.0F;
      this.txt(
         this.ell(this.subtitle(this.page), this.vw - 30, LyricsScreen.T.S), hx, this.colY + 31 + Math.round((1.0F - sa) * 5.0F), 8618892, sa, LyricsScreen.T.S
      );
      float maxScroll = Math.max(0, this.contentH - this.vh + 6);
      float over = 36.0F;
      this.scrollTarget = Math.max(-over, Math.min(maxScroll + over, this.scrollTarget));
      if (this.scrollTarget < 0.0F) {
         this.scrollTarget = this.scrollTarget + (0.0F - this.scrollTarget) * (1.0F - (float)Math.exp(-9.0F * dt));
      }

      if (this.scrollTarget > maxScroll) {
         this.scrollTarget = this.scrollTarget + (maxScroll - this.scrollTarget) * (1.0F - (float)Math.exp(-9.0F * dt));
      }

      this.scroll = this.scroll + (this.scrollTarget - this.scroll) * (1.0F - (float)Math.exp(-16.0F * dt));
      g.enableScissor(this.vx - 4, this.vy, this.vx + this.vw + 4, this.vy + this.vh);
      this.ly = 0;
      this.rowIdx = 0;
      float baseA = Gfx.alpha;
      Gfx.alpha = baseA * Math.max(0.0F, pa);
      this.inContent = true;
      switch (this.page) {
         case SKY:
            this.sky();
            break;
         case LYRICS:
            this.lyrics();
            break;
         case STYLE:
            this.style();
            break;
         case SPOTIFY:
            this.spotify();
            break;
         case DISPLAY:
            this.display();
            break;
         case MENU:
            this.menuPage();
            break;
         case PERF:
            this.perf();
            break;
         case FAV:
            this.fav();
      }

      this.inContent = false;
      this.contentH = this.ly + 4;
      Gfx.alpha = baseA;
      g.disableScissor();
      if (this.scroll > 1.0F) {
         Gfx.vGradient(g, this.vx - 4, this.vy, this.vw + 8, 10, 1118484, 1118484, 0.94F * baseA, 0.0F);
      }

      if (this.scroll < maxScroll - 1.0F) {
         Gfx.vGradient(g, this.vx - 4, this.vy + this.vh - 10, this.vw + 8, 10, 1118484, 1118484, 0.0F, 0.94F * baseA);
      }

      if (maxScroll > 0.0F) {
         int bh = Math.max(16, Math.round(this.vh * ((float)this.vh / this.contentH)));
         int by = this.vy + Math.round((this.vh - bh) * (this.scroll / maxScroll));
         float act = this.pool.to("scrollact", Math.abs(this.scrollTarget - this.scroll) > 0.5F ? 1.0 : 0.0);
         Gfx.aaRound(g, this.colX + this.colW - 7 - act, by, 2.0F + act * 1.5F, bh, 1.0F + act, 16777215, 0.12F + 0.25F * act);
      }
   }

   private void setSky(SkyPreset p) {
      c().sky = p.ordinal();
      c().lastSky = p.ordinal();
   }

   private void sky() {
      Config c = c();
      this.gStart(t("ui.group.general"), null);
      this.toggleRow("skyon", t("ui.sky.enabled"), t("ui.sky.enabled.sub"), c.sky >= 0, v -> c.sky = v ? Math.max(0, c.lastSky) : -1);
      this.toggleRow("shuf", t("ui.sky.shuffle"), t("ui.sky.shuffle.sub"), c.skyShuffle, v -> c.skyShuffle = v);
      this.linkRow(t("ui.sky.random"), null, SkyPreset::shuffle);
      this.gEnd();
      this.label(t("ui.sky.style"), t("ui.sky.count", SKY_ORDER.length));
      this.skyGrid();
      SkyPreset cur = SkyPreset.get(c.sky >= 0 ? c.sky : c.lastSky);
      if (cur != null) {
         this.note(t("skydesc." + cur.name().toLowerCase(Locale.ROOT)));
      }

      if (cur == SkyPreset.CUSTOM && c.sky >= 0) {
         this.gStart(t("skyed.title"), null);
         this.swatchRow("ctop", t("skyed.top"), DARK_COLS, c.customTop, false, v -> {
            c.customTop = v;
            SkyPreset.applyCustom();
         });
         this.swatchRow("chor", t("skyed.horizon"), DARK_COLS, c.customHorizon, false, v -> {
            c.customHorizon = v;
            SkyPreset.applyCustom();
         });
         this.swatchRow("ca1", t("skyed.aurora1"), BRIGHT, c.customAurora1, false, v -> {
            c.customAurora1 = v;
            SkyPreset.applyCustom();
         });
         this.swatchRow("ca2", t("skyed.aurora2"), BRIGHT, c.customAurora2, false, v -> {
            c.customAurora2 = v;
            SkyPreset.applyCustom();
         });
         this.toggleRow("cau", t("feat.aurora"), null, c.customAurora, v -> {
            c.customAurora = v;
            SkyPreset.applyCustom();
         });
         this.toggleRow("cst", t("feat.stars"), null, c.customStars, v -> {
            c.customStars = v;
            SkyPreset.applyCustom();
         });
         this.toggleRow("cmo", t("feat.moon"), null, c.customMoon, v -> {
            c.customMoon = v;
            SkyPreset.applyCustom();
         });
         this.toggleRow("csh", t("feat.shooting"), null, c.customShooting, v -> {
            c.customShooting = v;
            SkyPreset.applyCustom();
         });
         SkyPreset.Weather[] ws = SkyPreset.Weather.values();
         SkyPreset.Weather w = ws[Math.floorMod(c.customWeather, ws.length)];
         this.pickerRow(
            "cwe", t("skyed.weather"), null, w == SkyPreset.Weather.NONE ? t("skyed.noweather") : t("feat." + w.name().toLowerCase(Locale.ROOT)), -1, () -> {
               c.customWeather = Math.floorMod(c.customWeather - 1, ws.length);
               SkyPreset.applyCustom();
            }, () -> {
               c.customWeather = Math.floorMod(c.customWeather + 1, ws.length);
               SkyPreset.applyCustom();
            }
         );
         this.gEnd();
      }

      this.gStart(t("ui.sky.tune"), null);
      String pal = c.skyPalette < 0 ? t("ui.palette.original") : t("ui.palette." + WorldSky.PALETTE_KEYS[c.skyPalette]);
      int np = WorldSky.PALETTES.length + 1;
      this.pickerRow(
         "pal",
         t("ui.sky.palette"),
         t("ui.sky.palette.sub"),
         pal,
         -1,
         () -> c.skyPalette = Math.floorMod(c.skyPalette, np) - 1,
         () -> c.skyPalette = Math.floorMod(c.skyPalette + 2, np) - 1
      );
      this.sliderRow(
         "spd", t("ui.sky.speed"), (c.skySpeed - 0.25F) / 1.75F, String.format(Locale.ROOT, "%.1f×", c.skySpeed), v -> c.skySpeed = 0.25F + v * 1.75F
      );
      this.sliderRow("liv", t("ui.sky.lively"), c.skyLively, Math.round(c.skyLively * 100.0F) + " %", v -> c.skyLively = v);
      this.sliderRow(
         "brt",
         t("ui.sky.breath"),
         c.skyBreath / 2.0F,
         c.skyBreath < 0.02F ? t("ui.off") : String.format(Locale.ROOT, "%.1f×", c.skyBreath),
         v -> c.skyBreath = v < 0.03F ? 0.0F : v * 2.0F
      );
      this.sliderRow("pat", t("ui.sky.pattern"), (c.skyPattern - 0.2F) / 1.3F, Math.round(c.skyPattern * 100.0F) + " %", v -> c.skyPattern = 0.2F + v * 1.3F);
      this.sliderRow(
         "str", t("opt.skystrength"), (c.skyStrength - 0.1F) / 0.9F, Math.round(c.skyStrength * 100.0F) + " %", v -> c.skyStrength = 0.1F + v * 0.9F
      );
      this.sliderRow("wea", t("opt.weatheramount"), c.weatherAmount / 2.0F, Math.round(c.weatherAmount * 100.0F) + " %", v -> c.weatherAmount = v * 2.0F);
      this.gEnd();
      this.gStart(t("ui.sky.behaviour"), null);
      this.toggleRow("rw", t("opt.realweather"), t("ui.sky.realweather.sub"), c.realWeather, v -> c.realWeather = v);
      this.toggleRow("hc", t("opt.hideclouds"), t("ui.sky.hideclouds.sub"), c.hideClouds, v -> c.hideClouds = v);
      this.toggleRow("sd", t("extras.skyday"), t("ui.sky.skyday.sub"), c.skyDay, v -> {
         c.skyDay = v;
         if (v) {
            c.fxDayNight = false;
         }
      });
      this.toggleRow("dn", t("extras.daynight"), t("ui.sky.daynight.sub"), c.fxDayNight, v -> {
         c.fxDayNight = v;
         if (v) {
            c.skyDay = false;
         }
      });
      this.toggleRow("con", t("extras.const"), t("ui.sky.const.sub"), c.fxConstellations, v -> c.fxConstellations = v);
      this.toggleRow("skm", t("extras.skymusic"), t("ui.sky.music.sub"), c.fxSkyMusic, v -> c.fxSkyMusic = v);
      this.gEnd();
   }

   private void lyrics() {
      Config c = c();
      this.gStart(t("ui.group.general"), null);
      this.toggleRow("lyron", t("ui.lyrics.show"), t("ui.lyrics.show.sub"), c.lyricsSky, v -> c.lyricsSky = v);
      this.segRow("kind", t("ui.lyrics.kind"), null, new String[]{t("ui.style.sky"), t("ui.style.float")}, c.lyricsStyle, i -> c.lyricsStyle = i);
      this.toggleRow("fol", t("opt.lyricsfollow"), t("ui.lyrics.follow.sub"), c.lyricsFollow, v -> c.lyricsFollow = v);
      this.toggleRow("lock", t("ui.lyrics.lock"), t("ui.lyrics.lock.sub"), c.lyricsViewLock, v -> c.lyricsViewLock = v);
      this.toggleRow("und", t("style.underground"), t("ui.lyrics.under.sub"), c.lyricsUnderground, v -> c.lyricsUnderground = v);
      this.gEnd();
      this.gStart(t("ui.lyrics.look"), null);
      this.swatchRow(
         "lcol",
         t("ui.lyrics.color"),
         WorldSky.LYRIC_COLORS,
         c.lyricsColor < 0 ? -1 : WorldSky.LYRIC_COLORS[Math.floorMod(c.lyricsColor, WorldSky.LYRIC_COLORS.length)],
         true,
         v -> c.lyricsColor = indexOf(WorldSky.LYRIC_COLORS, v)
      );
      this.glowRow();
      this.pairRow(
         "look1",
         34,
         this.sliderCell(
            "size", t("opt.lyricssize"), (c.lyricsSize - 0.6F) / 3.4F, String.format(Locale.ROOT, "%.1f×", c.lyricsSize), v -> c.lyricsSize = 0.6F + v * 3.4F
         ),
         this.sliderCell(
            "opa", t("opt.lyricsopacity"), (c.lyricsOpacity - 0.2F) / 0.8F, Math.round(c.lyricsOpacity * 100.0F) + " %", v -> c.lyricsOpacity = 0.2F + v * 0.8F
         )
      );
      String[] anims = new String[]{t("style.anim0"), t("style.anim1"), t("style.anim2")};
      this.pairRow(
         "look2",
         34,
         c.lyricsGlow
            ? this.sliderCell(
               "gstr",
               t("style.glowstrength"),
               (c.lyricsGlowStrength - 0.2F) / 2.3F,
               Math.round(c.lyricsGlowStrength * 100.0F) + " %",
               v -> c.lyricsGlowStrength = 0.2F + v * 2.3F
            )
            : null,
         this.pickerCell(
            "anim",
            t("style.anim"),
            anims[Math.floorMod(c.lyricsAnim, 3)],
            -1,
            () -> c.lyricsAnim = Math.floorMod(c.lyricsAnim - 1, 3),
            () -> c.lyricsAnim = Math.floorMod(c.lyricsAnim + 1, 3)
         )
      );
      int nf = Gfx.FONT_IDS.length;
      this.pickerRow("font", t("opt.lyricsfont"), null, Gfx.FONT_NAMES[c.lyricsFont], c.lyricsFont, () -> {
         c.lyricsFont = Math.floorMod(c.lyricsFont - 1, nf);
         Gfx.clearWidths();
      }, () -> {
         c.lyricsFont = Math.floorMod(c.lyricsFont + 1, nf);
         Gfx.clearWidths();
      });
      this.gEnd();
      this.gStart(t("ui.lyrics.timing"), null);
      this.pickerRow(
         "off",
         t("opt.lyricsoffset"),
         t("ui.lyrics.offset.sub"),
         (c.lyricsOffsetMs > 0 ? "+" : "") + c.lyricsOffsetMs + " ms",
         -1,
         () -> c.lyricsOffsetMs = Math.max(-5000, c.lyricsOffsetMs - 100),
         () -> c.lyricsOffsetMs = Math.min(5000, c.lyricsOffsetMs + 100)
      );
      this.toggleRow("beat", t("extras.beat"), t("ui.lyrics.beat.sub"), c.fxBeatPulse, v -> c.fxBeatPulse = v);
      this.toggleRow("intro", t("extras.intro"), t("ui.lyrics.intro.sub"), c.fxSongIntro, v -> c.fxSongIntro = v);
      this.gEnd();
      this.gStart(t("ui.lyrics.translate"), null);
      this.toggleRow("tr", t("extras.translate"), t("ui.lyrics.translate.sub"), c.translate, v -> c.translate = v);
      int li = Math.max(0, Arrays.asList(LANGS).indexOf(c.translateTo));
      this.pickerRow(
         "lang",
         t("ui.lyrics.lang"),
         null,
         LANGS[li].toUpperCase(Locale.ROOT),
         -1,
         () -> c.translateTo = LANGS[Math.floorMod(li - 1, LANGS.length)],
         () -> c.translateTo = LANGS[Math.floorMod(li + 1, LANGS.length)]
      );
      this.gEnd();
   }

   private void style() {
      Config c = c();
      this.label(t("ui.style.hat"), null);
      this.hatGrid();
      this.note(t("ui.style.hat.note"));
      this.gStart(t("ui.style.hatopts"), null);
      this.toggleRow("hglow", t("ui.style.hatglow"), t("ui.style.hatglow.sub"), c.hatGlow, v -> c.hatGlow = v);
      this.gEnd();
      this.gStart(t("ui.style.crosshair"), null);
      String[] cr = new String[]{t("ui.cross.0"), t("ui.cross.1"), t("ui.cross.2"), t("ui.cross.3"), t("ui.cross.4")};
      this.pickerRow(
         "cross",
         t("ui.style.crosshair"),
         t("ui.style.crosshair.sub"),
         cr[Math.floorMod(c.crosshair, cr.length)],
         -1,
         () -> c.crosshair = Math.floorMod(c.crosshair - 1, cr.length),
         () -> c.crosshair = Math.floorMod(c.crosshair + 1, cr.length)
      );
      this.gEnd();
      this.gStart(t("ui.style.effects"), null);
      String[] tr = new String[]{t("ui.trail.0"), t("ui.trail.1"), t("ui.trail.2"), t("ui.trail.3")};
      this.pickerRow(
         "trail",
         t("ui.style.trail"),
         t("ui.style.trail.sub"),
         tr[Math.floorMod(c.trail, tr.length)],
         -1,
         () -> c.trail = Math.floorMod(c.trail - 1, tr.length),
         () -> c.trail = Math.floorMod(c.trail + 1, tr.length)
      );
      this.toggleRow("aura", t("ui.style.aura"), t("ui.style.aura.sub"), c.aura, v -> c.aura = v);
      this.toggleRow("bglow", t("ui.style.beatglow"), t("ui.style.beatglow.sub"), c.beatGlow, v -> c.beatGlow = v);
      this.gEnd();
      this.gStart(t("ui.style.death"), null);
      String[] de = new String[4];

      for (int i = 0; i < de.length; i++) {
         de[i] = t("ui.death." + i);
      }

      this.pickerRow(
         "death",
         t("ui.style.death"),
         t("ui.style.death.sub"),
         de[Math.floorMod(c.deathFx, de.length)],
         -1,
         () -> c.deathFx = Math.floorMod(c.deathFx - 1, de.length),
         () -> c.deathFx = Math.floorMod(c.deathFx + 1, de.length)
      );
      if (c.deathFx > 0) {
         this.toggleRow("dothers", t("ui.style.deathothers"), t("ui.style.deathothers.sub"), c.deathOthers, v -> c.deathOthers = v);
         this.linkRow(t("ui.style.deathtest"), t("ui.style.deathtest.sub"), () -> {
            this.close();
            DeathFx.test();
         });
      }

      this.gEnd();
   }

   private void hatGrid() {
      Config c = c();
      String[] hats = Accessories.HATS;
      int n = 3;
      int gap = 8;
      int cw = (this.vw - gap * (n - 1)) / n;
      int ch = Math.round(cw * 0.625F);
      int rows = (hats.length + n - 1) / n;

      for (int r = 0; r < rows; r++) {
         int y0 = this.rowStart(ch + 24);
         if (y0 != Integer.MIN_VALUE) {
            for (int k = 0; k < n; k++) {
               int i = r * n + k;
               if (i >= hats.length) {
                  break;
               }

               int x = this.vx + k * (cw + gap);
               int y = y0 + 2;
               boolean on = c.hat == i;
               float hv = this.hover(x, y, cw, ch + 16, "hat" + i);
               float sv = this.pool.to("hatsel" + i, on ? 1.0 : 0.0);
               float clickPop = 1.0F - Anim.outCubic(Anim.since(this.pressed.getOrDefault("hat" + i, 0L), 300L));
               float sc = this.spring("hatsc" + i, 1.0F + 0.05F * hv) - 0.05F * clickPop;
               this.gfx.getMatrices().pushMatrix();
               this.gfx.getMatrices().translate(x + cw / 2.0F, y + ch / 2.0F);
               this.gfx.getMatrices().scale(sc, sc);
               this.gfx.getMatrices().translate(-(x + cw / 2.0F), -(y + ch / 2.0F));
               if (sv > 0.01F) {
                  float ring = 2.0F + 2.0F * (1.0F - Anim.outBack(sv));
                  Gfx.aaRound(this.gfx, x - ring, y - ring, cw + ring * 2.0F, ch + ring * 2.0F, 9.0F + ring, 16777215, sv);
               }

               Gfx.aaRound(this.gfx, x, y, cw, ch, 9.0F, Gfx.lerpColor(1710623, 2236968, hv), 1.0F);
               if (i == 0) {
                  Icons.draw(this.gfx, Icons.Icon.CLOSE, x + cw / 2.0F, y + ch / 2.0F, 10.0F, Gfx.lerpColor(5000276, 15658737, Math.max(hv, sv)), 1.0F);
               } else {
                  float bob = (float)Math.sin(System.currentTimeMillis() / 400.0 + i) * 1.5F * Math.max(hv, sv);
                  Identifier tex = Identifier.of("neonlyrics", "textures/gui/hats/" + hats[i] + ".png");

                  try {
                     this.gfx
                        .drawTexture(
                           RenderPipelines.GUI_TEXTURED, tex, x + 2, Math.round(y + 2 + bob), 0.0F, 0.0F, cw - 4, ch - 4, cw - 4, ch - 4, Gfx.argb(16777215, 1.0F)
                        );
                  } catch (Throwable var22) {
                  }
               }

               this.gfx.getMatrices().popMatrix();
               this.txt(
                  this.ell(t("ui.hat." + hats[i]), cw, LyricsScreen.T.S),
                  x,
                  y + ch + 6,
                  Gfx.lerpColor(Gfx.lerpColor(8618892, 13158606, hv), 15658737, sv),
                  1.0F,
                  on ? LyricsScreen.T.B : LyricsScreen.T.S
               );
               this.hit(x, y, cw, ch + 16, () -> {
                  this.pressed.put("hat" + i, System.currentTimeMillis());
                  c.hat = i;
                  Fx.sound(Fx.Sfx.CLICK);
                  NeonLyrics.saveConfig();
               });
            }
         }

         this.rowEnd();
      }
   }

   private void spotify() {
      Config c = c();
      this.gStart(t("ui.group.status"), null);
      this.statusRow();
      this.gEnd();
      this.gStart(t("ui.spotify.connection"), null);
      boolean auto = SpotifyClient.autoMode();
      this.segRow("mode", t("ui.spotify.mode"), null, new String[]{t("spotify.mode.auto"), t("spotify.mode.api")}, auto ? 0 : 1, i -> {
         SpotifyClient.setMode(i);
         this.idBox.setText(c().spotifyClientId);
      });
      if (auto) {
         this.linkRow(t("spotify.auto.retry"), null, () -> {
            LocalMedia.restart();
            Island.notify(t("spotify.auto.retrying"), true);
         });
      } else {
         this.fieldRow(t("spotify.clientid"), t("ui.spotify.clientid.sub"));
         boolean connected = SpotifyClient.status() == SpotifyClient.Status.CONNECTED;
         this.linkRow(connected ? t("spotify.disconnect") : t("spotify.connect"), null, () -> {
            c.spotifyClientId = this.idBox.getText().trim();
            NeonLyrics.saveConfig();
            if (SpotifyClient.status() == SpotifyClient.Status.CONNECTED) {
               SpotifyClient.logout();
            } else {
               SpotifyClient.login();
            }
         });
         this.linkRow(t("tut.open"), null, this::openTutorial);
         this.copyRow(t("ui.spotify.redirect"), "http://127.0.0.1:8789/callback");
         this.linkRow(t("spotify.dashboard"), null, () -> Util.getOperatingSystem().open("https://developer.spotify.com/dashboard"));
      }

      this.gEnd();
      if (auto) {
         this.note(t("spotify.auto.p1"));
         this.note(t("spotify.auto.p2"));
         this.note(t("spotify.auto.p4"));
      }

      this.gStart(t("ui.spotify.playback"), null);
      this.toggleRow("demo", t("ui.spotify.demo"), t("ui.spotify.demo.sub"), c.demoLyrics, v -> {
         c.demoLyrics = v;
         SpotifyClient.restartDemo();
      });
      this.linkRow(t("ui.spotify.sync"), null, SpotifyClient::syncHere);
      this.gEnd();
   }

   private void display() {
      Config c = c();
      this.gStart(t("ui.display.bar"), null);
      this.toggleRow("isl", t("ui.display.island"), t("ui.display.island.sub"), c.islandEnabled, v -> c.islandEnabled = v);
      this.toggleRow("lisl", t("opt.lyricsisland"), t("ui.display.lyricsisland.sub"), c.lyricsIsland, v -> c.lyricsIsland = v);
      this.toggleRow("cov", t("extras.cover"), t("ui.display.cover.sub"), c.albumCover, v -> c.albumCover = v);
      this.toggleRow("clk", t("opt.clock"), null, c.showClock, v -> c.showClock = v);
      this.toggleRow("png", t("opt.ping"), null, c.showPing, v -> c.showPing = v);
      this.toggleRow("fps", t("opt.fps"), null, c.showFps, v -> c.showFps = v);
      int nf = Gfx.FONT_NAMES.length;
      this.pickerRow(
         "ufont", t("ui.display.barfont"), t("ui.display.barfont.sub"), Gfx.FONT_NAMES[c.uiFont], c.uiFont < Gfx.FONT_IDS.length ? c.uiFont : -1, () -> {
            c.uiFont = Math.floorMod(c.uiFont - 1, nf);
            Gfx.clearWidths();
         }, () -> {
            c.uiFont = Math.floorMod(c.uiFont + 1, nf);
            Gfx.clearWidths();
         }
      );
      this.gEnd();
      this.gStart(t("ui.display.hud"), null);
      this.toggleRow("hot", t("opt.glasshotbar"), t("ui.display.hotbar.sub"), c.glassHotbar, v -> c.glassHotbar = v);
      this.toggleRow("intro", t("extras.startup"), null, c.fxStartup, v -> c.fxStartup = v);
      this.gEnd();
      this.gStart(t("ui.display.stats"), null);
      this.toggleRow("stats", t("opt.statshud"), t("ui.display.stats.sub"), c.statsHud, v -> c.statsHud = v);
      this.toggleRow("statsmem", t("opt.statsmemory"), null, c.statsMemory, v -> c.statsMemory = v);
      this.toggleRow("statscrd", t("opt.statscoords"), null, c.statsCoords, v -> c.statsCoords = v);
      this.toggleRow("statspng", t("opt.statsping"), null, c.statsPing, v -> c.statsPing = v);
      this.gEnd();
   }

   private void menuPage() {
      Config c = c();
      this.gStart(t("ui.menu.mode"), null);
      this.segRow(
         "mmode",
         t("ui.menu.open"),
         t(c.menuMode == 1 ? "ui.menu.world.sub" : "ui.menu.normal.sub"),
         new String[]{t("ui.menu.normal"), t("ui.menu.world")},
         c.menuMode,
         i -> c.menuMode = i
      );
      this.linkRow(t("ui.menu.reset"), t("ui.menu.reset.sub"), () -> {
         c.menuMode = 0;
         c.accent = 0;
         c.blur = true;
         c.fxMenuAnim = true;
         c.sounds = true;
         PhoneMenu.closeNow();
         NeonLyrics.saveConfig();
      });
      this.gEnd();
      this.gStart(t("ui.display.menu"), null);
      this.accentRow();
      this.toggleRow("blur", t("opt.blur"), null, c.blur, v -> c.blur = v);
      this.toggleRow("anim", t("perf.menuanim"), null, c.fxMenuAnim, v -> c.fxMenuAnim = v);
      this.toggleRow("snd", t("opt.sounds"), null, c.sounds, v -> c.sounds = v);
      this.gEnd();
   }

   private void perf() {
      Config c = c();
      PerfSettings s = PerfSettings.get();
      this.gStart(t("ui.group.now"), null);
      this.fpsRow();
      this.gEnd();
      this.gStart(t("ui.perf.presets"), null);
      this.linkRow(t("ui.perf.preset.max"), t("ui.perf.preset.max.sub"), PerfBoost::presetMax);
      this.linkRow(t("ui.perf.preset.balanced"), t("ui.perf.preset.balanced.sub"), PerfBoost::presetBalanced);
      this.linkRow(t("ui.perf.preset.off"), t("ui.perf.preset.off.sub"), PerfBoost::presetOff);
      this.gEnd();
      this.gStart(t("ui.group.auto"), null);
      this.toggleRow("auto", t("perf.auto"), t("ui.perf.auto.sub"), c.autoPerf, v -> {
         c.autoPerf = v;
         AutoPerf.reset();
      });
      int ti = 0;

      for (int i = 0; i < TARGETS.length; i++) {
         if (TARGETS[i] == c.targetFps) {
            ti = i;
         }
      }

      int tIdx = ti;
      this.pickerRow(
         "target",
         t("perf.target"),
         t("ui.perf.target.sub"),
         c.targetFps <= 0 ? t("ui.perf.target.auto", AutoPerf.target()) : c.targetFps + " FPS",
         -1,
         () -> {
            c.targetFps = TARGETS[Math.floorMod(tIdx - 1, TARGETS.length)];
            AutoPerf.reset();
         },
         () -> {
            c.targetFps = TARGETS[Math.floorMod(tIdx + 1, TARGETS.length)];
            AutoPerf.reset();
         }
      );
      this.segRow("qual", t("quality"), null, new String[]{t("quality.0"), t("quality.1"), t("quality.2")}, c.quality, ix -> c.quality = ix);
      this.segRow("prof", t("ui.perf.profile"), null, new String[]{"Chill", "PvP"}, c.profile, Profiles::set);
      this.toggleRow("boost", t("opt.fpsboost"), t("ui.perf.boost.sub"), c.fpsBoost, PerfBoost::setBoost);
      this.linkRow(t("ui.perf.reapply"), t("ui.perf.reapply.sub"), PerfBoost::refresh);
      this.gEnd();
      this.gStart(t("ui.perf.vanilla"), null);
      this.toggleRow("p_van", t("ui.perf.vanillaopt"), t("ui.perf.vanillaopt.sub"), s.applyVanillaOptions, v -> {
         s.applyVanillaOptions = v;
         PerfBoost.refresh();
      });
      this.toggleRow("p_lat", t("ui.perf.lowlatency"), t("ui.perf.lowlatency.sub"), s.lowLatency, v -> {
         s.lowLatency = v;
         PerfBoost.refresh();
      });
      this.sliderRow(
         "p_rd",
         t("ui.perf.rd"),
         (s.renderDistance - 2) / 30.0F,
         s.renderDistance + " chunks",
         v -> {
            s.renderDistance = Math.round(2.0F + v * 30.0F);
            PerfBoost.markDirty();
         }
      );
      this.sliderRow(
         "p_sd",
         t("ui.perf.sd"),
         (s.simulationDistance - 5) / 27.0F,
         s.simulationDistance + " chunks",
         v -> {
            s.simulationDistance = Math.round(5.0F + v * 27.0F);
            PerfBoost.markDirty();
         }
      );
      this.sliderRow(
         "p_fps",
         t("ui.perf.maxfps"),
         (s.maxFps - 60) / 200.0F,
         s.maxFps + " FPS",
         v -> {
            s.maxFps = Math.round(60.0F + v * 200.0F);
            PerfBoost.markDirty();
         }
      );
      this.toggleRow("p_shadow", t("ui.perf.shadows"), null, s.disableEntityShadows, v -> {
         s.disableEntityShadows = v;
         PerfBoost.refresh();
      });
      this.toggleRow("p_clouds", t("ui.perf.clouds"), null, s.disableClouds, v -> {
         s.disableClouds = v;
         PerfBoost.refresh();
      });
      this.toggleRow("p_weather", t("ui.perf.weather"), null, s.disableWeather, v -> {
         s.disableWeather = v;
         PerfBoost.refresh();
      });
      this.toggleRow("p_ao", t("ui.perf.ao"), null, s.disableAo, v -> {
         s.disableAo = v;
         PerfBoost.refresh();
      });
      this.toggleRow("p_adapt", t("ui.perf.adaptive"), t("ui.perf.adaptive.sub"), s.adaptiveViewDistance, v -> {
         s.adaptiveViewDistance = v;
         PerfBoost.refresh();
      });
      this.gEnd();
      this.gStart(t("ui.perf.culling"), null);
      this.toggleRow("p_ent", t("ui.perf.cullent"), null, s.cullEntities, v -> {
         s.cullEntities = v;
         PerfBoost.refresh();
      });
      this.sliderRow(
         "p_entd",
         t("ui.perf.entdist"),
         (s.entityCullDistance - 8) / 120.0F,
         s.entityCullDistance + " m",
         v -> {
            s.entityCullDistance = Math.round(8.0F + v * 120.0F);
            PerfBoost.markDirty();
         }
      );
      this.toggleRow("p_be", t("ui.perf.cullbe"), null, s.cullBlockEntities, v -> {
         s.cullBlockEntities = v;
         PerfBoost.refresh();
      });
      this.sliderRow(
         "p_bed",
         t("ui.perf.bedist"),
         (s.blockEntityCullDistance - 8) / 120.0F,
         s.blockEntityCullDistance + " m",
         v -> {
            s.blockEntityCullDistance = Math.round(8.0F + v * 120.0F);
            PerfBoost.markDirty();
         }
      );
      this.toggleRow("p_part", t("ui.perf.cullpart"), null, s.cullParticles, v -> {
         s.cullParticles = v;
         PerfBoost.refresh();
      });
      this.sliderRow(
         "p_partd",
         t("ui.perf.partdist"),
         (s.particleDistance - 4) / 60.0F,
         s.particleDistance + " m",
         v -> {
            s.particleDistance = Math.round(4.0F + v * 60.0F);
            PerfBoost.markDirty();
         }
      );
      this.sliderRow(
         "p_tagd",
         t("ui.perf.tagdist"),
         (s.nameTagDistance - 4) / 60.0F,
         s.nameTagDistance + " m",
         v -> {
            s.nameTagDistance = Math.round(4.0F + v * 60.0F);
            PerfBoost.markDirty();
         }
      );
      this.toggleRow("p_nopart", t("ui.perf.skipallpart"), t("ui.perf.skipallpart.sub"), s.skipVanillaParticles, v -> {
         s.skipVanillaParticles = v;
         PerfBoost.refresh();
      });
      this.toggleRow("p_nobe", t("ui.perf.skipallbe"), t("ui.perf.skipallbe.sub"), s.skipAllBlockEntities, v -> {
         s.skipAllBlockEntities = v;
         PerfBoost.refresh();
      });
      this.gEnd();
      this.gStart(t("ui.perf.effects"), null);
      this.toggleRow("e_col", t("perf.skycolor"), null, c.fxSkyColor, v -> c.fxSkyColor = v);
      this.toggleRow("e_wea", t("perf.weather"), null, c.fxWeather, v -> c.fxWeather = v);
      this.toggleRow("e_aur", t("perf.aurora"), null, c.fxAurora, v -> c.fxAurora = v);
      this.toggleRow("e_sta", t("perf.stars"), null, c.fxStars, v -> c.fxStars = v);
      this.toggleRow("e_glo", t("style.glow"), t("ui.perf.glow.sub"), c.lyricsGlow, v -> c.lyricsGlow = v);
      this.linkRow(t("perf.alloff"), null, () -> this.setAllEffects(false));
      this.linkRow(t("perf.allon"), null, () -> this.setAllEffects(true));
      this.gEnd();
   }

   private void fav() {
      List<Favorites.Entry> list = Favorites.all();
      if (list.isEmpty()) {
         this.label(t("ui.fav.lines"), null);
         this.note(t("fav.empty"));
      } else {
         this.gStart(t("ui.fav.lines"), Integer.toString(list.size()));

         for (int i = 0; i < list.size(); i++) {
            Favorites.Entry e = list.get(i);
            int y = this.rowStart(38);
            if (y != Integer.MIN_VALUE) {
               int x = this.vx + 12;
               int w = this.vw - 24;
               this.rowBg(y, 38, "fav" + i);
               this.txt(this.ell("„" + e.line + "“", w - 26, LyricsScreen.T.B), x, y + 9, 15658737, 1.0F, LyricsScreen.T.B);
               this.txt(this.ell(e.title + (e.artist.isBlank() ? "" : " · " + e.artist), w - 26, LyricsScreen.T.S), x, y + 21, 8618892, 1.0F, LyricsScreen.T.S);
               this.iconBtn(this.gfx, "favdel" + i, x + w - 16, y + 10, 18, Icons.Icon.CLOSE, 6.0F, () -> Favorites.remove(e));
            }

            this.rowEnd();
         }

         this.gEnd();
      }

      List<SpotifyClient.Played> hist = SpotifyClient.history();
      if (hist.isEmpty()) {
         this.label(t("ui.fav.history"), null);
         this.note(t("ui.fav.history.empty"));
      } else {
         this.gStart(t("ui.fav.history"), Integer.toString(hist.size()));
         long now = System.currentTimeMillis();

         for (int i = 0; i < hist.size(); i++) {
            SpotifyClient.Played e = hist.get(i);
            int y = this.rowStart(38);
            if (y != Integer.MIN_VALUE) {
               int x = this.vx + 12;
               int w = this.vw - 24;
               this.rowBg(y, 38, "his" + i);
               String ago = ago(now - e.at());
               this.txt(this.ell(e.title(), w - 14 - this.tw(ago, LyricsScreen.T.S), LyricsScreen.T.B), x, y + 9, 15658737, 1.0F, LyricsScreen.T.B);
               this.txt(this.ell(e.artist(), w, LyricsScreen.T.S), x, y + 21, 8618892, 1.0F, LyricsScreen.T.S);
               this.txtR(ago, x + w, y + 10, 8618892, 1.0F, LyricsScreen.T.S);
            }

            this.rowEnd();
         }

         this.gEnd();
      }
   }

   private int rowStart(int h) {
      int sy = this.vy + this.ly - Math.round(this.scroll);
      int idx = this.rowIdx++;
      this.ly += h;
      this.rowA = Gfx.alpha;
      if (sy + h < this.vy - 2 || sy > this.vy + this.vh + 2) {
         return Integer.MIN_VALUE;
      }

      if (!this.anim()) {
         return sy;
      }

      long now = System.currentTimeMillis();
      long first = this.seen.computeIfAbsent(idx, kx -> Math.max(now, this.pageTime + Math.min(idx, 14) * 35L));
      float in = Anim.outCubic(Anim.since(first, 340L));
      float k = Anim.outBack(Anim.since(first, 420L));
      Gfx.alpha = this.rowA * in;
      return sy + Math.round((1.0F - k) * 14.0F);
   }

   private void rowEnd() {
      Gfx.alpha = this.rowA;
   }

   private void gStart(String title, String meta) {
      this.label(title, meta);
      int y = this.rowStart(5);
      if (y != Integer.MIN_VALUE) {
         Gfx.aaRound(this.gfx, this.vx, y, this.vw, 16.0F, 9.0F, 1710623, 1.0F);
      }

      this.rowEnd();
      this.firstInGroup = true;
   }

   private void gEnd() {
      int y = this.rowStart(13);
      if (y != Integer.MIN_VALUE) {
         Gfx.aaRound(this.gfx, this.vx, y - 5, this.vw, 16.0F, 9.0F, 1710623, 1.0F);
      }

      this.rowEnd();
      this.firstInGroup = false;
   }

   private void label(String title, String meta) {
      int y = this.rowStart(20);
      if (y != Integer.MIN_VALUE) {
         this.txt(title.toUpperCase(Locale.ROOT), this.vx + 3, y + 8, 8618892, 1.0F, LyricsScreen.T.S);
         if (meta != null) {
            this.txtR(meta, this.vx + this.vw - 3, y + 8, 8618892, 1.0F, LyricsScreen.T.S);
         }
      }

      this.rowEnd();
   }

   private void rowBg(int y, int h, String id) {
      float hv = this.hover(this.vx, y, this.vw, h, id);
      Gfx.rect(this.gfx, this.vx, y, this.vw, h, Gfx.lerpColor(1710623, 2236968, hv), 1.0F);
      if (hv > 0.01F) {
         Gfx.aaRound(this.gfx, this.vx + 3, y + h / 2.0F - (h - 14) * hv / 2.0F, 2.0F, (h - 14) * hv, 1.0F, 16777215, 0.5F * hv);
      }

      if (!this.firstInGroup) {
         Gfx.rect(this.gfx, this.vx + 12, y, this.vw - 24, 1, 2500140, 1.0F);
      }

      this.firstInGroup = false;
      this.ripple(id, this.vx, y, this.vw, h);
      this.curRid = id;
   }

   private void labels(int y, int h, String title, String sub, int maxW) {
      int x = this.vx + 12;
      if (sub == null) {
         this.txt(this.ell(title, maxW, LyricsScreen.T.B), x, y + h / 2 - 4, 15658737, 1.0F, LyricsScreen.T.B);
      } else {
         this.txt(this.ell(title, maxW, LyricsScreen.T.B), x, y + h / 2 - 9, 15658737, 1.0F, LyricsScreen.T.B);
         this.txt(this.ell(sub, maxW, LyricsScreen.T.S), x, y + h / 2 + 3, 8618892, 1.0F, LyricsScreen.T.S);
      }
   }

   private int rowH(String sub) {
      return sub == null ? 30 : 38;
   }

   private void note(String text) {
      List<String> lines = this.wrap(text, this.vw - 6, LyricsScreen.T.S);
      int y = this.rowStart(6 + lines.size() * 9);
      if (y != Integer.MIN_VALUE) {
         for (int i = 0; i < lines.size(); i++) {
            this.txt(lines.get(i), this.vx + 3, y + 3 + i * 9, 8618892, 1.0F, LyricsScreen.T.S);
         }
      }

      this.rowEnd();
   }

   private void toggleRow(String id, String title, String sub, boolean value, Consumer<Boolean> set) {
      int h = this.rowH(sub);
      int y = this.rowStart(h);
      if (y != Integer.MIN_VALUE) {
         this.rowBg(y, h, "tr" + id);
         this.labels(y, h, title, sub, this.vw - 70);
         int tw0 = 24;
         int th = 14;
         int tx = this.vx + this.vw - 12 - tw0;
         int ty = y + h / 2 - th / 2;
         float ks = this.spring("tg" + id, value ? 1.0F : 0.0F);
         float k = Anim.clamp(ks);
         float flash = 1.0F - Anim.outCubic(Anim.since(this.pressed.getOrDefault("tg" + id, 0L), 450L));
         if (flash > 0.01F) {
            Gfx.aaRound(
               this.gfx,
               tx - 4.0F * (1.0F - flash) - 1.0F,
               ty - 4.0F * (1.0F - flash) - 1.0F,
               tw0 + 8.0F * (1.0F - flash) + 2.0F,
               th + 8.0F * (1.0F - flash) + 2.0F,
               th / 2.0F + 4.0F,
               16777215,
               0.25F * flash
            );
         }

         Gfx.aaRound(this.gfx, tx, ty, tw0, th, th / 2.0F, Gfx.lerpColor(3421243, 16777215, k), 1.0F);
         float stretch = Math.min(5.0F, Math.abs(this.springs.get("tg" + id)[1]) * 0.35F);
         Gfx.aaRound(this.gfx, tx + 2 + ks * 10.0F - (value ? stretch : 0.0F), ty + 2, 10.0F + stretch, 10.0F, 5.0F, Gfx.lerpColor(9079442, 1118484, k), 1.0F);
         this.hit(this.vx, y, this.vw, h, () -> {
            this.pressed.put("tg" + id, System.currentTimeMillis());
            set.accept(!value);
            Fx.sound(Fx.Sfx.CLICK);
            NeonLyrics.saveConfig();
         }, "tr" + id);
      }

      this.rowEnd();
   }

   private void sliderRow(String id, String title, float value, String valueText, Consumer<Float> set) {
      int h = 42;
      int y = this.rowStart(h);
      if (y != Integer.MIN_VALUE) {
         this.rowBg(y, h, "sr" + id);
         int x = this.vx + 12;
         int w = this.vw - 24;
         this.txt(this.ell(title, w - 50, LyricsScreen.T.B), x, y + 9, 15658737, 1.0F, LyricsScreen.T.B);
         value = Anim.clamp(value);
         boolean drag = this.activeSlider != null && this.activeSlider.id().equals(id);
         float dk = this.pool.to("sd" + id, drag ? 1.0 : 0.0);
         this.txtR(valueText, x + w, y + 9 - Math.round(dk * 1.5F), Gfx.lerpColor(12105920, 16777215, dk), 1.0F, drag ? LyricsScreen.T.B : LyricsScreen.T.S);
         float v = drag ? value : this.spring("sv" + id, value);
         int cy = y + 29;
         float hv = this.pool.to("sh" + id, !drag && !this.in(x - 6, cy - 8, w + 12, 16) ? 0.0 : 1.0);
         Gfx.aaRound(this.gfx, x, cy - 1.5F, w, 3.0F, 1.5F, 3421243, 1.0F);
         Gfx.aaRound(this.gfx, x, cy - 1.5F, Math.max(3.0F, w * Anim.clamp(v)), 3.0F, 1.5F, 16777215, 1.0F);
         if (hv > 0.01F) {
            Gfx.aaCircle(this.gfx, x + w * Anim.clamp(v), cy, 8.0F + 3.0F * dk, 16777215, 0.12F * hv);
         }

         Gfx.aaCircle(this.gfx, x + w * Anim.clamp(v), cy, 4.5F + 0.8F * hv + 1.5F * dk, 16777215, 1.0F);
         if (this.closeTime < 0L) {
            int sy0 = Math.max(cy - 9, this.vy);
            int sy1 = Math.min(cy + 9, this.vy + this.vh);
            this.sliders.add(new LyricsScreen.Slider(id, x, sy0, w, sy1 - sy0, set));
         }
      }

      this.rowEnd();
   }

   private void pickerRow(String id, String title, String sub, String value, int fontIdx, Runnable prev, Runnable next) {
      int h = this.rowH(sub);
      int y = this.rowStart(h);
      if (y != Integer.MIN_VALUE) {
         this.rowBg(y, h, "pr" + id);
         int area = Math.min(150, this.vw / 2);
         int ax = this.vx + this.vw - 12 - area;
         this.labels(y, h, title, sub, ax - this.vx - 20);
         int cy = y + h / 2;
         boolean hl = this.in(ax, y, area / 2, h);
         boolean hr = this.in(ax + area / 2, y, area - area / 2, h);
         float pulse = 1.0F - Anim.outCubic(Anim.since(this.pressed.getOrDefault("pk" + id, 0L), 240L));
         int right = this.vx + this.vw - 24;
         int vw0;
         if (fontIdx >= 0) {
            vw0 = Math.min(area - 30, Gfx.wIn(value, fontIdx, false));
            Gfx.textIn(this.gfx, value, right - vw0, cy - 4 - Math.round(pulse * 1.5F), 13684950, 1.0F, fontIdx, false);
         } else {
            String vv = this.ell(value, area - 30, LyricsScreen.T.M);
            vw0 = this.tw(vv, LyricsScreen.T.M);
            this.txtR(vv, right, cy - 4 - pulse * 1.5F, 13684950, 1.0F, LyricsScreen.T.M);
         }

         float hlk = this.pool.to("hl" + id, hl ? 1.0 : 0.0);
         float hrk = this.pool.to("hr" + id, hr ? 1.0 : 0.0);
         Icons.draw(this.gfx, Icons.Icon.BACK, right - vw0 - 10 - hlk * 2.0F, cy, 6.0F + hlk, Gfx.lerpColor(5000276, 15658737, hlk), 1.0F);
         Icons.draw(this.gfx, Icons.Icon.CHEVRON, this.vx + this.vw - 15 + hrk * 2.0F, cy, 6.0F + hrk, Gfx.lerpColor(5000276, 15658737, hrk), 1.0F);
         this.hit(ax, y, area / 2, h, () -> {
            this.pressed.put("pk" + id, System.currentTimeMillis());
            prev.run();
            Fx.sound(Fx.Sfx.CLICK);
            NeonLyrics.saveConfig();
         }, "pr" + id);
         this.hit(ax + area / 2, y, area - area / 2, h, () -> {
            this.pressed.put("pk" + id, System.currentTimeMillis());
            next.run();
            Fx.sound(Fx.Sfx.CLICK);
            NeonLyrics.saveConfig();
         }, "pr" + id);
      }

      this.rowEnd();
   }

   private void segRow(String id, String title, String sub, String[] opts, int sel, IntConsumer set) {
      int h = this.rowH(sub);
      int y = this.rowStart(h);
      if (y != Integer.MIN_VALUE) {
         this.rowBg(y, h, "sg" + id);
         int n = opts.length;
         int ow = 0;

         for (String o : opts) {
            ow = Math.max(ow, Math.max(this.tw(o, LyricsScreen.T.S), this.tw(o, LyricsScreen.T.B)) + 18);
         }

         int sw0 = ow * n + 4;
         int sh = 18;
         int sx = this.vx + this.vw - 12 - sw0;
         int sy = y + h / 2 - sh / 2;
         this.labels(y, h, title, sub, sx - this.vx - 20);
         Gfx.aaRound(this.gfx, sx, sy, sw0, sh, 9.0F, 921105, 1.0F);
         float pos = this.spring("seg" + id, sel);
         Gfx.aaRound(this.gfx, sx + 2 + pos * ow, sy + 2, ow, sh - 4, 7.0F, 16777215, 1.0F);

         for (int i = 0; i < n; i++) {
            float on = Anim.clamp(1.0F - Math.abs(pos - i));
            this.txtC(opts[i], sx + 2 + i * ow + ow / 2.0F, sy + 5, Gfx.lerpColor(8618892, 1118484, on), 1.0F, on > 0.5F ? LyricsScreen.T.B : LyricsScreen.T.S);
            int idx = i;
            this.hit(sx + 2 + i * ow, sy, ow, sh, () -> {
               if (idx != sel) {
                  set.accept(idx);
                  Fx.sound(Fx.Sfx.CLICK);
                  NeonLyrics.saveConfig();
               }
            });
         }
      }

      this.rowEnd();
   }

   private void swatchRow(String id, String title, int[] cols, int value, boolean auto, IntConsumer set) {
      int h = 30;
      int y = this.rowStart(h);
      if (y != Integer.MIN_VALUE) {
         this.rowBg(y, h, "sw" + id);
         int cy = y + h / 2;
         int n = cols.length + (auto ? 1 : 0);
         int step = 14;
         int startX = this.vx + this.vw - 17 - (n - 1) * step;
         this.labels(y, h, title, null, startX - this.vx - 24);

         for (int i = 0; i < n; i++) {
            boolean isAuto = auto && i == 0;
            int col = isAuto ? 0 : cols[i - (auto ? 1 : 0)];
            boolean sel = isAuto ? value < 0 : value == col;
            float cx = startX + i * step;
            float sv = this.pool.to(id + "s" + i, sel ? 1.0 : 0.0);
            if (sv > 0.01F) {
               Gfx.aaCircle(this.gfx, cx, cy, 6.5F, 16777215, sv);
               Gfx.aaCircle(this.gfx, cx, cy, 5.2F, 1710623, 1.0F);
            }

            if (isAuto) {
               Gfx.aaCircle(this.gfx, cx, cy, 4.2F, 3421243, 1.0F);
               this.txtC("A", cx, cy - 3, 8618892, 1.0F, LyricsScreen.T.S);
            } else {
               Gfx.aaCircle(this.gfx, cx, cy, 4.2F, col, 1.0F);
            }

            int v = isAuto ? -1 : col;
            this.hit(Math.round(cx - 7.0F), y, 14, h, () -> {
               set.accept(v);
               Fx.sound(Fx.Sfx.CLICK);
               NeonLyrics.saveConfig();
            });
         }
      }

      this.rowEnd();
   }

   private void glowRow() {
      Config c = c();
      int h = 30;
      int y = this.rowStart(h);
      if (y != Integer.MIN_VALUE) {
         this.rowBg(y, h, "glowrow");
         int cy = y + h / 2;
         int[] cols = WorldSky.LYRIC_COLORS;
         int value = c.lyricsGlowColor < 0 ? -1 : cols[Math.floorMod(c.lyricsGlowColor, cols.length)];
         int tw0 = 24;
         int th = 14;
         int tx = this.vx + this.vw - 12 - tw0;
         int ty = cy - th / 2;
         int n = cols.length + 1;
         int step = 14;
         int startX = tx - 14 - (n - 1) * step;
         this.labels(y, h, t("style.glow"), null, startX - this.vx - 24);
         float on = this.pool.to("glowon", c.lyricsGlow ? 1.0 : 0.0);
         float old = Gfx.alpha;
         Gfx.alpha = old * (0.35F + 0.65F * on);

         for (int i = 0; i < n; i++) {
            boolean isAuto = i == 0;
            int col = isAuto ? 0 : cols[i - 1];
            boolean sel = isAuto ? value < 0 : value == col;
            float cx = startX + i * step;
            float sv = this.pool.to("gcs" + i, sel ? 1.0 : 0.0);
            if (sv > 0.01F) {
               Gfx.aaCircle(this.gfx, cx, cy, 6.5F, 16777215, sv);
               Gfx.aaCircle(this.gfx, cx, cy, 5.2F, 1710623, 1.0F);
            }

            if (isAuto) {
               Gfx.aaCircle(this.gfx, cx, cy, 4.2F, 3421243, 1.0F);
               this.txtC("A", cx, cy - 3, 8618892, 1.0F, LyricsScreen.T.S);
            } else {
               Gfx.aaCircle(this.gfx, cx, cy, 4.2F, col, 1.0F);
            }

            int v = isAuto ? -1 : col;
            this.hit(Math.round(cx - 7.0F), y, 14, h, () -> {
               c.lyricsGlowColor = indexOf(cols, v);
               c.lyricsGlow = true;
               Fx.sound(Fx.Sfx.CLICK);
               NeonLyrics.saveConfig();
            });
         }

         Gfx.alpha = old;
         float ks = this.spring("tgglow", c.lyricsGlow ? 1.0F : 0.0F);
         float k = Anim.clamp(ks);
         Gfx.aaRound(this.gfx, tx, ty, tw0, th, th / 2.0F, Gfx.lerpColor(3421243, 16777215, k), 1.0F);
         Gfx.aaRound(this.gfx, tx + 2 + ks * 10.0F, ty + 2, 10.0F, 10.0F, 5.0F, Gfx.lerpColor(9079442, 1118484, k), 1.0F);
         this.hit(tx - 6, y, tw0 + 18, h, () -> {
            c.lyricsGlow = !c.lyricsGlow;
            Fx.sound(Fx.Sfx.CLICK);
            NeonLyrics.saveConfig();
         });
      }

      this.rowEnd();
   }

   private void pairRow(String id, int h, LyricsScreen.Cell left, LyricsScreen.Cell right) {
      int y = this.rowStart(h);
      if (y != Integer.MIN_VALUE) {
         this.rowBg(y, h, "pair" + id);
         int half = this.vw / 2;
         Gfx.rect(this.gfx, this.vx + half, y + 7, 1, h - 14, 2500140, 1.0F);
         if (left != null) {
            left.draw(this.vx, y, half, h);
         }

         if (right != null) {
            right.draw(this.vx + half, y, this.vw - half, h);
         }
      }

      this.rowEnd();
   }

   private LyricsScreen.Cell sliderCell(String id, String title, float value0, String valueText, Consumer<Float> set) {
      return (x0, y, w0, h) -> {
         int x = x0 + 12;
         int w = w0 - 24;
         float value = Anim.clamp(value0);
         boolean drag = this.activeSlider != null && this.activeSlider.id().equals(id);
         float dk = this.pool.to("sd" + id, drag ? 1.0 : 0.0);
         this.txt(this.ell(title, w - this.tw(valueText, LyricsScreen.T.S) - 6, LyricsScreen.T.S), x, y + 7, 15658737, 1.0F, LyricsScreen.T.S);
         this.txtR(valueText, x + w, y + 7, Gfx.lerpColor(12105920, 16777215, dk), 1.0F, drag ? LyricsScreen.T.B : LyricsScreen.T.S);
         float v = drag ? value : this.spring("sv" + id, value);
         int cy = y + h - 10;
         float hv = this.pool.to("sh" + id, !drag && !this.in(x - 6, cy - 8, w + 12, 16) ? 0.0 : 1.0);
         Gfx.aaRound(this.gfx, x, cy - 1.5F, w, 3.0F, 1.5F, 3421243, 1.0F);
         Gfx.aaRound(this.gfx, x, cy - 1.5F, Math.max(3.0F, w * Anim.clamp(v)), 3.0F, 1.5F, 16777215, 1.0F);
         if (hv > 0.01F) {
            Gfx.aaCircle(this.gfx, x + w * Anim.clamp(v), cy, 7.0F + 2.0F * dk, 16777215, 0.12F * hv);
         }

         Gfx.aaCircle(this.gfx, x + w * Anim.clamp(v), cy, 4.0F + 0.7F * hv + 1.2F * dk, 16777215, 1.0F);
         if (this.closeTime < 0L) {
            int sy0 = Math.max(cy - 9, this.vy);
            int sy1 = Math.min(cy + 9, this.vy + this.vh);
            this.sliders.add(new LyricsScreen.Slider(id, x, sy0, w, sy1 - sy0, set));
         }
      };
   }

   private LyricsScreen.Cell pickerCell(String id, String title, String value, int fontIdx, Runnable prev, Runnable next) {
      return (x0, y, w0, h) -> {
         this.txt(this.ell(title, w0 - 24, LyricsScreen.T.S), x0 + 12, y + 7, 15658737, 1.0F, LyricsScreen.T.S);
         int cy = y + h - 10;
         float pulse = 1.0F - Anim.outCubic(Anim.since(this.pressed.getOrDefault("pk" + id, 0L), 240L));
         boolean hl = this.in(x0, y, w0 / 2, h);
         boolean hr = this.in(x0 + w0 / 2, y, w0 - w0 / 2, h);
         float hlk = this.pool.to("hl" + id, hl ? 1.0 : 0.0);
         float hrk = this.pool.to("hr" + id, hr ? 1.0 : 0.0);
         String vv = this.ell(value, w0 - 44, LyricsScreen.T.S);
         this.txtC(vv, x0 + w0 / 2.0F, cy - 4 - pulse * 1.5F, 13684950, 1.0F, LyricsScreen.T.S);
         Icons.draw(this.gfx, Icons.Icon.BACK, x0 + 16 - hlk * 2.0F, cy, 5.0F + hlk, Gfx.lerpColor(5000276, 15658737, hlk), 1.0F);
         Icons.draw(this.gfx, Icons.Icon.CHEVRON, x0 + w0 - 16 + hrk * 2.0F, cy, 5.0F + hrk, Gfx.lerpColor(5000276, 15658737, hrk), 1.0F);
         this.hit(x0, y, w0 / 2, h, () -> {
            this.pressed.put("pk" + id, System.currentTimeMillis());
            prev.run();
            Fx.sound(Fx.Sfx.CLICK);
            NeonLyrics.saveConfig();
         }, "pair" + id);
         this.hit(x0 + w0 / 2, y, w0 - w0 / 2, h, () -> {
            this.pressed.put("pk" + id, System.currentTimeMillis());
            next.run();
            Fx.sound(Fx.Sfx.CLICK);
            NeonLyrics.saveConfig();
         }, "pair" + id);
      };
   }

   private void accentRow() {
      Config c = c();
      int h = 30;
      int y = this.rowStart(h);
      if (y != Integer.MIN_VALUE) {
         this.rowBg(y, h, "accent");
         int cy = y + h / 2;
         int n = Theme.ACCENTS.length;
         int step = 14;
         int startX = this.vx + this.vw - 17 - (n - 1) * step;
         this.labels(y, h, t("ui.display.accent"), null, startX - this.vx - 24);

         for (int i = 0; i < n; i++) {
            float cx = startX + i * step;
            float sv = this.pool.to("accs" + i, c.accent == i ? 1.0 : 0.0);
            if (sv > 0.01F) {
               Gfx.aaCircle(this.gfx, cx, cy, 6.5F, 16777215, sv);
               Gfx.aaCircle(this.gfx, cx, cy, 5.2F, 1710623, 1.0F);
            }

            Gfx.aaCircle(this.gfx, cx, cy, 4.2F, Theme.ACCENTS[i], 1.0F);
            int idx = i;
            this.hit(Math.round(cx - 7.0F), y, 14, h, () -> {
               c.accent = idx;
               c.rgbAccent = false;
               Fx.sound(Fx.Sfx.CLICK);
               NeonLyrics.saveConfig();
            });
         }
      }

      this.rowEnd();
   }

   private void linkRow(String title, String sub, Runnable action) {
      int h = this.rowH(sub);
      int y = this.rowStart(h);
      if (y != Integer.MIN_VALUE) {
         String id = "lk" + title;
         this.rowBg(y, h, id);
         float hv = this.hover(this.vx, y, this.vw, h, id);
         float press = 1.0F - Anim.outCubic(Anim.since(this.pressed.getOrDefault(id, 0L), 260L));
         this.labels(y + Math.round(press), h, title, sub, this.vw - 40);
         Icons.draw(
            this.gfx,
            Icons.Icon.CHEVRON,
            this.vx + this.vw - 15 + hv * 3.0F + press * 3.0F,
            y + h / 2.0F,
            6.0F + hv,
            Gfx.lerpColor(5000276, 15658737, hv),
            1.0F
         );
         this.hit(this.vx, y, this.vw, h, () -> {
            this.pressed.put(id, System.currentTimeMillis());
            Fx.sound(Fx.Sfx.CLICK);
            action.run();
         }, id);
      }

      this.rowEnd();
   }

   private void fieldRow(String title, String sub) {
      int h = 58;
      int y = this.rowStart(h);
      if (y != Integer.MIN_VALUE) {
         this.rowBg(y, h, "field");
         int x = this.vx + 12;
         int w = this.vw - 24;
         this.txt(title, x, y + 8, 15658737, 1.0F, LyricsScreen.T.B);
         this.txt(this.ell(sub, w, LyricsScreen.T.S), x, y + 19, 8618892, 1.0F, LyricsScreen.T.S);
         float fk = this.pool.to("idfocus", this.idBox.isFocused() ? 1.0 : 0.0);
         Gfx.aaRound(this.gfx, x, y + 31, w, 20.0F, 6.0F, Gfx.lerpColor(2500140, 4868690, fk), 1.0F);
         Gfx.aaRound(this.gfx, x + 1, y + 32, w - 2, 18.0F, 5.0F, 921105, 1.0F);
         this.idWanted = true;
         this.idX = x + 7;
         this.idY = y + 37;
         this.idW = w - 14;
         this.hit(x, y + 31, w, 20, () -> this.setFocused(this.idBox));
      }

      this.rowEnd();
   }

   private void copyRow(String title, String value) {
      int h = 38;
      int y = this.rowStart(h);
      if (y != Integer.MIN_VALUE) {
         this.rowBg(y, h, "copy");
         String btn = t("btn.copy");
         int bw = this.tw(btn, LyricsScreen.T.S) + 16;
         this.labels(y, h, title, value, this.vw - bw - 40);
         int bx = this.vx + this.vw - 12 - bw;
         int by = y + h / 2 - 9;
         float hv = this.hover(bx, by, bw, 18, "copyb");
         Gfx.aaRound(this.gfx, bx, by, bw, 18.0F, 9.0F, Gfx.lerpColor(2763313, 3816002, hv), 1.0F);
         this.txtC(btn, bx + bw / 2.0F, by + 5, 15658737, 1.0F, LyricsScreen.T.S);
         this.hit(bx, by, bw, 18, () -> {
            Fx.clipboard(value);
            Fx.sound(Fx.Sfx.CLICK);
            Island.notify(t("toast.copiedres"), true);
         });
      }

      this.rowEnd();
   }

   private void statusRow() {
      Config c = c();
      int h = 38;
      int y = this.rowStart(h);
      if (y != Integer.MIN_VALUE) {
         this.rowBg(y, h, "status");
         NowPlaying np = SpotifyClient.now();
         boolean ok = c.demoLyrics
            || (SpotifyClient.autoMode() ? LocalMedia.status() == LocalMedia.Status.PLAYING : SpotifyClient.status() == SpotifyClient.Status.CONNECTED);
         boolean bad = SpotifyClient.autoMode()
            ? LocalMedia.status() == LocalMedia.Status.FAILED || LocalMedia.status() == LocalMedia.Status.UNSUPPORTED
            : SpotifyClient.status() == SpotifyClient.Status.ERROR;
         int col = ok ? 6022299 : (bad ? 16735354 : 16762460);
         float pulse = ok ? 0.5F + 0.5F * Anim.pulse(1600.0) : 1.0F;
         Gfx.aaCircle(this.gfx, this.vx + this.vw - 20, y + h / 2.0F, 7.0F, col, 0.15F * pulse);
         Gfx.aaCircle(this.gfx, this.vx + this.vw - 20, y + h / 2.0F, 3.0F, col, 1.0F);
         String song = np.hasTrack() ? np.title + (np.artist.isBlank() ? "" : " · " + np.artist) : t("spotify.nothing");
         this.labels(y, h, this.spotifyStatus(), song, this.vw - 50);
      }

      this.rowEnd();
   }

   private String spotifyStatus() {
      Config c = c();
      if (c.demoLyrics) {
         return t("spotify.st.demo");
      }

      if (SpotifyClient.autoMode()) {
         return switch (LocalMedia.status()) {
            case UNSUPPORTED -> t("spotify.auto.unsupported");
            case STARTING -> t("spotify.auto.starting");
            case NO_PLAYER -> t("spotify.auto.noplayer");
            case PLAYING -> t("spotify.auto.on", LocalMedia.app());
            case FAILED -> t("spotify.auto.failed");
         };
      } else {
         return switch (SpotifyClient.status()) {
            case NOT_CONFIGURED -> t("spotify.st.setup");
            case DISCONNECTED -> t("spotify.st.off");
            case WAITING_LOGIN -> t("spotify.st.wait");
            case CONNECTED -> t("spotify.st.on");
            case ERROR -> t("spotify.st.error", SpotifyClient.lastError());
         };
      }
   }

   private void fpsRow() {
      Config c = c();
      int h = 50;
      int y = this.rowStart(h);
      if (y != Integer.MIN_VALUE) {
         this.rowBg(y, h, "fpsrow");
         int x = this.vx + 12;
         int w = this.vw - 24;
         String num = Integer.toString(Math.round(this.pool.to("fpsnum", AutoPerf.fps())));
         this.txt(num, x, y + 12, 15658737, 1.0F, LyricsScreen.T.XL);
         this.txt("FPS", x + this.tw(num, LyricsScreen.T.XL) + 5, y + 24, 8618892, 1.0F, LyricsScreen.T.M);
         int lv = AutoPerf.level();
         String st = !c.autoPerf ? t("perf.auto.off") : (lv == 0 ? t("perf.auto.full") : t("perf.auto.level", lv, 6));
         this.txtR(t("ui.perf.cost", String.format(Locale.ROOT, "%.1f", AutoPerf.modMs())), x + w, y + 15, 8618892, 1.0F, LyricsScreen.T.S);
         this.txtR(st, x + w, y + 26, 8618892, 1.0F, LyricsScreen.T.S);
      }

      this.rowEnd();
   }

   private void skyGrid() {
      Config c = c();
      int n = this.vw >= 330 ? 3 : 2;
      int gap = 8;
      int cw = (this.vw - gap * (n - 1)) / n;
      int ch = Math.round(cw * 0.5F);
      int rowH = ch + 22;
      int rows = (SKY_ORDER.length + n - 1) / n;
      int sel = c.sky;

      for (int r = 0; r < rows; r++) {
         int y0 = this.rowStart(rowH);
         if (y0 != Integer.MIN_VALUE) {
            for (int k = 0; k < n; k++) {
               int i = r * n + k;
               if (i >= SKY_ORDER.length) {
                  break;
               }

               SkyPreset p = SKY_ORDER[i];
               int x = this.vx + k * (cw + gap);
               int y = y0 + 2;
               boolean on = sel == p.ordinal();
               float hv = this.hover(x, y, cw, ch + 16, "sky" + i);
               float sv = this.pool.to("skysel" + i, on ? 1.0 : 0.0);
               float clickPop = 1.0F - Anim.outCubic(Anim.since(this.pressed.getOrDefault("sky" + i, 0L), 300L));
               float sc = this.spring("skysc" + i, 1.0F + 0.05F * hv) - 0.05F * clickPop;
               int yy = Math.round(y - hv * 2.0F);
               this.gfx.getMatrices().pushMatrix();
               this.gfx.getMatrices().translate(x + cw / 2.0F, yy + ch / 2.0F);
               this.gfx.getMatrices().scale(sc, sc);
               this.gfx.getMatrices().translate(-(x + cw / 2.0F), -(yy + ch / 2.0F));
               if (hv > 0.01F) {
                  Gfx.aaRound(this.gfx, x, yy + 5, cw, ch, 8.0F, 0, 0.4F * hv);
               }

               if (sv > 0.01F) {
                  float ring = 2.0F + 2.0F * (1.0F - Anim.outBack(sv));
                  Gfx.aaRound(this.gfx, x - ring, yy - ring, cw + ring * 2.0F, ch + ring * 2.0F, 7.0F + ring, 16777215, sv);
                  Gfx.aaRound(this.gfx, x - 0.75F, yy - 0.75F, cw + 1.5F, ch + 1.5F, 7.5F, 1118484, 1.0F);
               }

               float a = sel < 0 ? 0.45F : 0.8F + 0.2F * Math.max(hv, sv);
               if (!SkyThumbs.draw(this.gfx, p, x, yy, cw, ch, a)) {
                  Gfx.aaRound(this.gfx, x, yy, cw, ch, 7.0F, p.top, a);
               }

               this.gfx.getMatrices().popMatrix();
               this.txt(
                  this.ell(Fx.t(p.key()), cw, LyricsScreen.T.S),
                  x,
                  y + ch + 6,
                  Gfx.lerpColor(Gfx.lerpColor(8618892, 13158606, hv), 15658737, sv),
                  1.0F,
                  on ? LyricsScreen.T.B : LyricsScreen.T.S
               );
               this.hit(x, y, cw, ch + 16, () -> {
                  this.pressed.put("sky" + i, System.currentTimeMillis());
                  this.setSky(p);
                  Fx.sound(Fx.Sfx.CLICK);
                  NeonLyrics.saveConfig();
               });
            }
         }

         this.rowEnd();
      }
   }

   private Text comp(String s, LyricsScreen.T t) {
      Map<String, Text> m = this.comps.computeIfAbsent(t, k -> new HashMap<>());
      if (m.size() > 600) {
         m.clear();
      }

      return m.computeIfAbsent(s, k -> Text.literal(k).fillStyle(Style.EMPTY.withFont(FONTS.get(t))));
   }

   private int tw(String s, LyricsScreen.T t) {
      if (s != null && !s.isEmpty()) {
         Map<String, Integer> m = this.widths.computeIfAbsent(t, k -> new HashMap<>());
         if (m.size() > 600) {
            m.clear();
         }

         Integer v = m.get(s);
         if (v == null) {
            v = this.textRenderer.getWidth(this.comp(s, t));
            m.put(s, v);
         }

         return v;
      } else {
         return 0;
      }
   }

   private void txt(String s, float x, float y, int col, float a, LyricsScreen.T t) {
      if (s != null && !s.isEmpty() && !(a * Gfx.alpha < 0.02F)) {
         this.gfx.drawText(this.textRenderer, this.comp(s, t), Math.round(x), Math.round(y), Gfx.argb(col, a), false);
      }
   }

   private void txtR(String s, float rx, float y, int col, float a, LyricsScreen.T t) {
      this.txt(s, rx - this.tw(s, t), y, col, a, t);
   }

   private void txtC(String s, float cx, float y, int col, float a, LyricsScreen.T t) {
      this.txt(s, cx - this.tw(s, t) / 2.0F, y, col, a, t);
   }

   private String ell(String s, int maxW, LyricsScreen.T t) {
      if (s == null) {
         return "";
      }

      if (this.tw(s, t) <= maxW) {
         return s;
      }

      while (s.length() > 1 && this.tw(s + "…", t) > maxW) {
         s = s.substring(0, s.length() - 1);
      }

      return s + "…";
   }

   private List<String> wrap(String s, int maxW, LyricsScreen.T t) {
      List<String> out = new ArrayList<>();
      if (s != null && !s.isEmpty()) {
         StringBuilder line = new StringBuilder();

         for (String w : s.split(" ")) {
            String test = line.length() == 0 ? w : line + " " + w;
            if (this.tw(test, t) > maxW && line.length() > 0) {
               out.add(line.toString());
               line = new StringBuilder(w);
            } else {
               line = new StringBuilder(test);
            }
         }

         if (line.length() > 0) {
            out.add(line.toString());
         }

         return out;
      } else {
         return out;
      }
   }

   private float spring(String id, float target) {
      float[] st = this.springs.computeIfAbsent(id, k -> new float[]{target, 0.0F});
      if (!this.anim()) {
         st[0] = target;
         st[1] = 0.0F;
         return target;
      }

      float h = Math.min(this.frameDt, 0.05F) / 3.0F;

      for (int i = 0; i < 3; i++) {
         float acc = 320.0F * (target - st[0]) - 24.0F * st[1];
         st[1] += acc * h;
         st[0] += st[1] * h;
      }

      return st[0];
   }

   private void ripple(String id, int x, int y, int w, int h) {
      LyricsScreen.Ripple rp = this.ripples.get(id);
      if (rp != null) {
         float age = (float)(System.currentTimeMillis() - rp.at()) / 520.0F;
         if (age >= 1.0F) {
            this.ripples.remove(id);
         } else {
            float e = Anim.outCubic(age);
            float cx = Math.max(x, Math.min(x + w, rp.x()));
            float left = cx - (cx - x) * e;
            float right = cx + (x + w - cx) * e;
            float a = 0.16F * (1.0F - age);
            int y0 = this.inContent ? Math.max(y, this.vy) : y;
            int y1 = this.inContent ? Math.min(y + h, this.vy + this.vh) : y + h;
            if (y1 > y0 && !(a < 0.005F)) {
               Gfx.aaRound(this.gfx, left, y0, Math.max(2.0F, right - left), y1 - y0, Math.min(6.0F, (y1 - y0) / 2.0F), 16777215, a);
            }
         }
      }
   }

   private void iconBtn(DrawContext g, String id, int x, int y, int s, Icons.Icon icon, float iconSize, Runnable r) {
      float hv = this.hover(x, y, s, s, "ib" + id);
      float press = 1.0F - Anim.outCubic(Anim.since(this.pressed.getOrDefault(id, 0L), 220L));
      float cx = x + s / 2.0F;
      float cy = y + s / 2.0F;
      if (hv > 0.01F) {
         Gfx.aaCircle(g, cx, cy, s / 2.0F * (0.8F + 0.2F * hv), 16777215, 0.07F * hv);
      }

      if (press > 0.01F) {
         Gfx.aaCircle(g, cx, cy, s / 2.0F + 6.0F * (1.0F - press), 16777215, 0.14F * press);
      }

      Icons.draw(g, icon, cx, cy, iconSize * (1.0F - 0.15F * press + 0.08F * hv), Gfx.lerpColor(13158606, 16777215, hv), 1.0F);
      this.hit(x, y, s, s, () -> {
         this.pressed.put(id, System.currentTimeMillis());
         Fx.sound(Fx.Sfx.CLICK);
         r.run();
      });
   }

   private float hover(int x, int y, int w, int h, String id) {
      boolean on = this.closeTime < 0L && this.in(x, y, w, h) && (!this.inContent || this.my >= this.vy && this.my < this.vy + this.vh);
      return this.pool.to("hv" + id, on ? 1.0 : 0.0);
   }

   private boolean in(int x, int y, int w, int h) {
      return this.mx >= x && this.my >= y && this.mx < x + w && this.my < y + h;
   }

   private void hit(int x, int y, int w, int h, Runnable r) {
      this.hit(x, y, w, h, r, null);
   }

   private void hit(int x, int y, int w, int h, Runnable r, String rid) {
      if (this.closeTime < 0L) {
         if (this.inContent) {
            int y0 = Math.max(y, this.vy);
            int y1 = Math.min(y + h, this.vy + this.vh);
            if (y1 > y0) {
               this.hits.add(new LyricsScreen.Hit(x, y0, w, y1 - y0, r, rid, true));
            }
         } else {
            this.hits.add(new LyricsScreen.Hit(x, y, w, h, r, rid, false));
         }
      }
   }

   private static String ago(long ms) {
      long m = ms / 60000L;
      if (m < 1L) {
         return t("ui.ago.now");
      } else {
         return m < 60L ? t("ui.ago.min", m) : t("ui.ago.h", m / 60L);
      }
   }

   private static int indexOf(int[] a, int v) {
      for (int i = 0; i < a.length; i++) {
         if (a[i] == v) {
            return i;
         }
      }

      return -1;
   }

   private void setAllEffects(boolean on) {
      Config c = c();
      if (!on && c.sky >= 0) {
         c.lastSky = c.sky;
      }

      c.sky = on ? Math.max(0, c.lastSky) : -1;
      c.fxSkyColor = on;
      c.fxWeather = on;
      c.fxAurora = on;
      c.fxStars = on;
      c.fxBeams = on;
      c.lyricsGlow = on;
      c.glassHotbar = on;
      c.blur = on;
      c.fxMenuAnim = on;
      c.quality = on ? 1 : 0;
      if (!on && !c.fpsBoost) {
         PerfBoost.setBoost(true);
      }

      if (on && c.fpsBoost) {
         PerfBoost.setBoost(false);
      }

      NeonLyrics.saveConfig();
      Island.notify(t(on ? "perf.allon.done" : "perf.alloff.done"), true);
   }

   private void openTutorial() {
      c().spotifyClientId = this.idBox.getText().trim();
      NeonLyrics.saveConfig();
      this.client.setScreen(new SpotifyTutorialScreen(new LyricsScreen(LyricsScreen.Page.SPOTIFY)));
   }

   public boolean mouseClicked(Click event, boolean doubleClick) {
      event = this.fm(event);
      if (super.mouseClicked(event, doubleClick)) {
         return true;
      }

      if (this.closeTime < 0L && event.button() == 0) {
         double ex = event.x() / this.S;
         double ey = event.y() / this.S;

         for (LyricsScreen.Slider s : this.sliders) {
            if (s.h() > 0 && ex >= s.x() - 6 && ex <= s.x() + s.w() + 6 && ey >= s.y() && ey <= s.y() + s.h()) {
               this.activeSlider = s;
               s.set().accept(Anim.clamp((float)((ex - s.x()) / s.w())));
               return true;
            }
         }

         for (int i = this.hits.size() - 1; i >= 0; i--) {
            LyricsScreen.Hit h = this.hits.get(i);
            if (ex >= h.x() && ey >= h.y() && ex < h.x() + h.w() && ey < h.y() + h.h()) {
               this.setFocused(null);
               if (h.rid() != null) {
                  this.ripples
                     .put(
                        h.rid(),
                        new LyricsScreen.Ripple((float)ex, (float)(h.content() ? ey - this.vy + this.scroll : ey), h.content(), System.currentTimeMillis())
                     );
               }

               h.run().run();
               return true;
            }
         }

         this.setFocused(null);
         return false;
      } else {
         return false;
      }
   }

   public boolean mouseDragged(Click event, double dx, double dy) {
      event = this.fm(event);
      if (this.activeSlider != null) {
         this.activeSlider.set().accept(Anim.clamp((float)((event.x() / this.S - this.activeSlider.x()) / this.activeSlider.w())));
         return true;
      } else {
         return super.mouseDragged(event, dx, dy);
      }
   }

   public boolean mouseReleased(Click event) {
      event = this.fm(event);
      if (this.activeSlider != null) {
         this.activeSlider = null;
         NeonLyrics.saveConfig();
         return true;
      } else {
         return super.mouseReleased(event);
      }
   }

   public boolean mouseScrolled(double x, double y, double h, double v) {
      this.scrollTarget -= (float)v * 30.0F;
      return true;
   }

   public boolean keyPressed(KeyInput event) {
      int key = event.key();
      boolean typing = this.getFocused() == this.idBox;
      if (!typing && key >= 49 && key < 49 + LyricsScreen.Page.values().length) {
         this.go(LyricsScreen.Page.values()[key - 49]);
         return true;
      }

      if (!typing && key == 258) {
         this.go(LyricsScreen.Page.values()[(this.page.ordinal() + 1) % LyricsScreen.Page.values().length]);
         return true;
      }

      if (typing || key != 264 && key != 267) {
         if (typing || key != 265 && key != 266) {
            if (!typing && KeyBindingHelper.getBoundKeyOf(NeonLyrics.keyMenu).getCode() == key) {
               this.close();
               return true;
            } else {
               return super.keyPressed(event);
            }
         } else {
            this.scrollTarget = this.scrollTarget - (key == 265 ? 30.0F : this.vh * 0.8F);
            return true;
         }
      } else {
         this.scrollTarget = this.scrollTarget + (key == 264 ? 30.0F : this.vh * 0.8F);
         return true;
      }
   }

   public void close() {
      if (this.closeTime < 0L) {
         this.closeTime = System.currentTimeMillis();
         c().spotifyClientId = this.idBox.getText().trim();
         NeonLyrics.saveConfig();
         show(this.idBox, false);
      }
   }

   public boolean shouldPause() {
      return false;
   }

   static {
      FONTS.put(LyricsScreen.T.S, fd("menu_s"));
      FONTS.put(LyricsScreen.T.M, fd("menu_m"));
      FONTS.put(LyricsScreen.T.B, fd("menu_b"));
      FONTS.put(LyricsScreen.T.TITLE, fd("menu_t"));
      FONTS.put(LyricsScreen.T.XL, fd("menu_xl"));
   }

   private interface Cell {
      void draw(int var1, int var2, int var3, int var4);
   }

   private record Hit(int x, int y, int w, int h, Runnable run, String rid, boolean content) {
      private Hit {
      }
   }

   public enum Page {
      SKY,
      LYRICS,
      STYLE,
      SPOTIFY,
      DISPLAY,
      MENU,
      PERF,
      FAV;

      Page() {
      }
   }

   private record Ripple(float x, float y, boolean content, long at) {
      private Ripple {
      }
   }

   private record Slider(String id, int x, int y, int w, int h, Consumer<Float> set) {
      private Slider {
      }
   }

   private enum T {
      S,
      M,
      B,
      TITLE,
      XL;

      T() {
      }
   }
}
