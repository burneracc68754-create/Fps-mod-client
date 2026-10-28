package dev.neonlyrics.gui;

import dev.neonlyrics.NeonLyrics;
import dev.neonlyrics.config.Config;
import dev.neonlyrics.hud.Anim;
import dev.neonlyrics.hud.Gfx;
import dev.neonlyrics.hud.Island;
import dev.neonlyrics.hud.Theme;
import dev.neonlyrics.lyrics.LocalMedia;
import dev.neonlyrics.lyrics.NowPlaying;
import dev.neonlyrics.lyrics.SpotifyClient;
import dev.neonlyrics.util.Fx;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.gui.Click;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.screen.Screen;

public class SpotifyTutorialScreen extends Screen {
   private static final int STEPS = 6;
   private final Screen parent;
   private final long opened = System.currentTimeMillis();
   private long stepTime = System.currentTimeMillis();
   private int step = 0;
   private int prevStep = -1;
   private int dir = 1;
   private final List<SpotifyTutorialScreen.Hit> hits = new ArrayList<>();
   private final Anim.Pool pool = new Anim.Pool(14.0);
   private final Anim dots = new Anim(0.0, 14.0);
   private int mx;
   private int my;
   private int px;
   private int py;
   private int pw;
   private int ph;
   private TextFieldWidget idBox;

   public SpotifyTutorialScreen(Screen parent) {
      super(Text.literal("Spotify Tutorial"));
      this.parent = parent;
   }

   protected void init() {
      this.pw = Math.min(430, this.width - 16);
      this.ph = Math.min(250, this.height - 16);
      this.px = (this.width - this.pw) / 2;
      this.py = (this.height - this.ph) / 2;
      this.idBox = new TextFieldWidget(this.textRenderer, this.px + 22, this.py + 118, 170, 12, Text.literal(""));
      this.idBox.setDrawsBackground(false);
      this.idBox.setMaxLength(64);
      this.idBox.setEditableColor(-790529);
      this.idBox.setText(NeonLyrics.cfg().spotifyClientId);
      this.idBox.setPlaceholder(Text.literal(Fx.t("neonlyrics.hint.clientid")).formatted(Formatting.DARK_GRAY));
      this.addDrawableChild(this.idBox);
   }

   private void go(int s) {
      s = Math.max(0, Math.min(5, s));
      if (s != this.step) {
         this.dir = s > this.step ? 1 : -1;
         this.prevStep = this.step;
         this.step = s;
         this.stepTime = System.currentTimeMillis();
         this.setFocused(null);
         Fx.sound(Fx.Sfx.CLICK);
      }
   }

   public void renderBackground(DrawContext g, int mouseX, int mouseY, float delta) {
      if (NeonLyrics.cfg().blur && this.client != null && this.client.world != null) {
         super.renderBackground(g, mouseX, mouseY, delta);
      }

      float a = Anim.outCubic(Anim.since(this.opened, 250L));
      Gfx.alpha = 1.0F;
      Gfx.vGradient(g, 0, 0, this.width, this.height, 460044, 986135, 0.55F * a, 0.75F * a);
   }

   public void render(DrawContext g, int mouseX, int mouseY, float delta) {
      this.mx = mouseX;
      this.my = mouseY;
      this.hits.clear();
      Config c = NeonLyrics.cfg();
      int acc = Theme.accent();
      float open = Anim.outCubic(Anim.since(this.opened, 300L));
      Gfx.alpha = open;
      g.getMatrices().pushMatrix();
      float sc = 0.94F + 0.06F * Anim.outBack(Anim.since(this.opened, 380L));
      g.getMatrices().translate(this.px + this.pw / 2.0F, this.py + this.ph / 2.0F);
      g.getMatrices().scale(sc, sc);
      g.getMatrices().translate(-(this.px + this.pw / 2.0F), -(this.py + this.ph / 2.0F));
      Gfx.glow(g, this.px, this.py, this.pw, this.ph, 10, acc, 0.7F, 10);
      Gfx.round(g, this.px, this.py, this.pw, this.ph, 10, 986135, 0.97F);
      Gfx.vGradient(g, this.px + 1, this.py + 1, this.pw - 2, 50, acc, 986135, 0.08F, 0.0F);
      Gfx.flowBorder(g, this.px + 4, this.py, this.pw - 8, this.ph, acc, Theme.accent2(), 0.8F, 0.0F, Anim.outCubic(Anim.since(this.opened, 700L)));
      g.getMatrices().pushMatrix();
      g.getMatrices().translate(this.px + 22, this.py + 18);
      g.getMatrices().scale(1.2F, 1.2F);
      g.drawItem(new ItemStack(Items.JUKEBOX), -8, -8);
      g.getMatrices().popMatrix();
      Gfx.text(g, Fx.t("neonlyrics.tut.title"), this.px + 36, this.py + 10, 16777215, 1.0F, true);
      Gfx.text(g, Fx.t("neonlyrics.tut.stepof", this.step + 1, 6), this.px + 36, this.py + 21, 10326968, 1.0F, false);
      float d = this.dots.target(this.step).get();
      int dw = 72;
      int dx = this.px + this.pw - 16 - dw;

      for (int i = 0; i < 6; i++) {
         boolean done = i < this.step;
         Gfx.round(g, dx + i * 12, this.py + 15, 8, 4, 2, done ? acc : 2761272, done ? 0.7F : 1.0F);
         this.hit(dx + i * 12 - 2, this.py + 11, 12, 12, () -> {}, i);
      }

      Gfx.round(g, dx + Math.round(d * 12.0F), this.py + 14, 8, 6, 3, acc, 1.0F);
      Gfx.rect(g, this.px + 14, this.py + 34, this.pw - 28, 1, 3813456, 0.8F);
      float e = Anim.outCubic(Anim.since(this.stepTime, 300L));
      g.enableScissor(this.px + 4, this.py + 36, this.px + this.pw - 4, this.py + this.ph - 30);
      if (this.prevStep >= 0 && e < 1.0F) {
         float oa = Gfx.alpha;
         Gfx.alpha = open * (1.0F - e);
         g.getMatrices().pushMatrix();
         g.getMatrices().translate(-this.dir * e * 30.0F, 0.0F);
         this.content(g, this.prevStep, false);
         g.getMatrices().popMatrix();
         Gfx.alpha = oa;
      }

      float oa = Gfx.alpha;
      Gfx.alpha = open * e;
      g.getMatrices().pushMatrix();
      g.getMatrices().translate(this.prevStep >= 0 ? this.dir * (1.0F - e) * 30.0F : 0.0F, this.prevStep < 0 ? (1.0F - e) * 8.0F : 0.0F);
      this.content(g, this.step, true);
      g.getMatrices().popMatrix();
      Gfx.alpha = oa;
      g.disableScissor();
      int by = this.py + this.ph - 24;
      Gfx.rect(g, this.px + 14, by - 6, this.pw - 28, 1, 3813456, 0.8F);
      this.button(g, "close", this.px + 14, by, 80, 16, Fx.t("neonlyrics.tut.close"), 10326968, false, this::close);
      if (this.step > 0) {
         this.button(g, "back", this.px + this.pw - 14 - 80 - 6 - 80, by, 80, 16, Fx.t("neonlyrics.tut.back"), 10326968, false, () -> this.go(this.step - 1));
      }

      boolean last = this.step == 5;
      this.button(g, "next", this.px + this.pw - 14 - 80, by, 80, 16, last ? Fx.t("neonlyrics.tut.finish") : Fx.t("neonlyrics.tut.next"), acc, true, () -> {
         if (last) {
            this.close();
         } else {
            this.go(this.step + 1);
         }
      });
      g.getMatrices().popMatrix();
      this.idBox.visible = this.step == 4 && e > 0.9F;
      this.idBox.active = this.idBox.visible;
      if (!this.idBox.visible && this.idBox.isFocused()) {
         this.setFocused(null);
      }

      if (this.step == 4) {
         c.spotifyClientId = this.idBox.getText().trim();
      }

      Gfx.alpha = 1.0F;
      super.render(g, mouseX, mouseY, delta);
   }

   private void content(DrawContext g, int s, boolean live) {
      int acc = Theme.accent();
      int lx = this.px + 18;
      int ly = this.py + 44;
      int lw = s == 5 ? this.pw - 36 : 196;
      int rx = this.px + 224;
      int ry = this.py + 44;
      int rw = this.pw - 224 - 16;
      int rh = this.ph - 44 - 34;
      String title = Fx.t("neonlyrics.tut.s" + s + ".title");
      Gfx.text(g, title, lx, ly, 16777215, 1.0F, true);
      int ty = ly + 14;

      for (String line : Gfx.wrap(Fx.t("neonlyrics.tut.s" + s + ".text"), lw)) {
         Gfx.text(g, line, lx, ty, 15986687, 0.88F, false);
         ty += 10;
      }

      ty += 6;
      switch (s) {
         case 0:
            String[] need = new String[]{Fx.t("neonlyrics.tut.need1"), Fx.t("neonlyrics.tut.need2"), Fx.t("neonlyrics.tut.need3")};

            for (String n : need) {
               Gfx.circle(g, lx + 3, ty + 3, 2.0F, acc, 1.0F);
               Gfx.text(g, n, lx + 10, ty, 15986687, 0.9F, false);
               ty += 11;
            }

            if (LocalMedia.WINDOWS) {
               this.button(g, "useauto" + live, lx, ty + 6, 150, 16, Fx.t("neonlyrics.tut.useauto"), 6022299, true, () -> {
                  SpotifyClient.setMode(0);
                  this.close();
               }, live);
            }

            this.drawWelcome(g, rx, ry, rw, rh);
            break;
         case 1:
            this.button(
               g,
               "dash" + live,
               lx,
               ty,
               120,
               16,
               Fx.t("neonlyrics.spotify.dashboard"),
               6022299,
               true,
               () -> Util.getOperatingSystem().open("https://developer.spotify.com/dashboard"),
               live
            );
            this.drawBrowser(g, rx, ry, rw, rh, 1);
            break;
         case 2:
            Gfx.round(g, lx, ty - 2, lw, 13, 4, 1972265, 0.95F);
            Gfx.text(g, "http://127.0.0.1:8789/callback", lx + 5, ty + 1, 16762460, 1.0F, false);
            ty += 16;
            this.button(g, "copy" + live, lx, ty, 120, 16, Fx.t("neonlyrics.tut.copyuri"), 6022299, true, () -> {
               Fx.clipboard("http://127.0.0.1:8789/callback");
               Island.notify(Fx.t("neonlyrics.toast.copiedres"), true);
            }, live);
            this.drawBrowser(g, rx, ry, rw, rh, 2);
            break;
         case 3:
            this.drawBrowser(g, rx, ry, rw, rh, 3);
            break;
         case 4:
            int fy = this.py + 114;
            Gfx.text(g, Fx.t("neonlyrics.spotify.clientid").toUpperCase(), lx, fy - 12, 10326968, 1.0F, false);
            boolean f = this.idBox.isFocused();
            Gfx.glow(g, lx, fy, 180, 18, 5, acc, f ? 0.8F : 0.0F, 4);
            Gfx.round(g, lx, fy, 180, 18, 5, f ? 2761272 : 1972265, 0.95F);
            SpotifyClient.Status st = SpotifyClient.status();
            boolean ok = st == SpotifyClient.Status.CONNECTED;
            this.button(
               g,
               "connect" + live,
               lx,
               fy + 24,
               120,
               16,
               ok ? Fx.t("neonlyrics.spotify.st.on") : Fx.t("neonlyrics.spotify.connect"),
               ok ? 6022299 : acc,
               !ok,
               () -> {
                  NeonLyrics.cfg().spotifyClientId = this.idBox.getText().trim();
                  NeonLyrics.saveConfig();
                  SpotifyClient.setMode(1);
                  if (SpotifyClient.status() != SpotifyClient.Status.CONNECTED) {
                     SpotifyClient.login();
                  }
               },
               live
            );
            this.drawStatus(g, rx, ry, rw, rh);
            break;
         default:
            this.drawHelp(g, lx, ty, this.pw - 36, this.py + this.ph - 34 - ty);
      }
   }

   private void drawWelcome(DrawContext g, int x, int y, int w, int h) {
      int acc = Theme.accent();
      Gfx.round(g, x, y, w, h, 8, 722962, 0.95F);
      Gfx.vGradient(g, x + 6, y + 6, w - 12, h - 12, 788246, 2758213, 1.0F, 1.0F);
      Random r = new Random(4L);
      double t = System.currentTimeMillis() / 1000.0;

      for (int i = 0; i < 40; i++) {
         float tw = (float)(0.5 + 0.5 * Math.sin(t * 2.0 + i));
         Gfx.rect(g, x + 8 + r.nextInt(w - 16), y + 8 + r.nextInt(h - 40), 1, 1, 16777215, 0.3F + 0.6F * tw);
      }

      for (int i = 0; i < w - 12; i += 2) {
         float hh = (float)(14.0 + 8.0 * Math.sin(i * 0.12 + t));
         int col = Gfx.lerpColor(10976255, 15129855, (float)(0.5 + 0.5 * Math.sin(i * 0.1 + t)));
         g.fillGradient(x + 6 + i, y + 26, x + 8 + i, (int)(y + 26 + hh), Gfx.argb(col, 0.0F), Gfx.argb(col, 0.4F));
      }

      Gfx.round(g, x + w / 2 - 44, y + 10, 88, 11, 5, 1183260, 0.9F);
      Gfx.round(g, x + w / 2 - 20, y + 12, 22, 7, 2, acc, 0.6F);
      g.getMatrices().pushMatrix();
      g.getMatrices().translate(x + w / 2.0F, y + h / 2.0F + 8.0F);
      g.getMatrices().rotate(-0.08F);
      g.getMatrices().scale(1.8F, 1.8F);
      String demo = Fx.t("neonlyrics.tut.preview");
      Gfx.text(g, demo, -Gfx.w(demo) / 2, 0, 13215999, 0.95F, true);
      g.getMatrices().popMatrix();
      Gfx.rect(g, x + 6, y + h - 14, w - 12, 8, 3037738, 0.95F);
   }

   private void drawBrowser(DrawContext g, int x, int y, int w, int h, int which) {
      int acc = Theme.accent();
      float pulse = Anim.pulse(1100.0);
      Gfx.round(g, x, y, w, h, 8, 15592946, 0.98F);
      Gfx.round(g, x, y, w, 14, 8, 14079710, 1.0F);
      Gfx.rect(g, x, y + 8, w, 6, 14079710, 1.0F);

      for (int i = 0; i < 3; i++) {
         Gfx.circle(g, x + 8 + i * 6, y + 7, 2.0F, new int[]{16736087, 16694318, 2672704}[i], 1.0F);
      }

      Gfx.round(g, x + 30, y + 3, w - 38, 8, 3, 16777215, 1.0F);
      Gfx.text(g, Gfx.ellipsize("developer.spotify.com/dashboard", w - 44), x + 34, y + 3, 5592422, 0.9F, false);
      int cy = y + 20;
      int dark = 1710626;
      int gray = 9079449;
      int green = 1947988;
      switch (which) {
         case 1:
            Gfx.text(g, "Dashboard", x + 8, cy, dark, 1.0F, false);
            cy += 16;

            for (int i = 0; i < 3; i++) {
               Gfx.round(g, x + 8, cy + i * 14, w - 16, 10, 3, 14474468, 1.0F);
            }

            int bx = x + w - 66;
            int by = y + 18;
            Gfx.glow(g, bx, by, 58, 13, 6, green, 0.6F + 0.6F * pulse, 5);
            Gfx.round(g, bx, by, 58, 13, 6, green, 1.0F);
            Gfx.center(g, "Create app", bx + 29, by + 3, 16777215, 1.0F, false);
            this.arrow(g, bx - 14, by + 6, pulse);
            break;
         case 2:
            Gfx.text(g, "Create app", x + 8, cy, dark, 1.0F, false);
            cy += 13;
            String[][] rows = new String[][]{
               {"App name", "Minecraft Lyrics"}, {"App description", "Lyrics im Spiel"}, {"Redirect URIs", "http://127.0.0.1:8789/…"}
            };

            for (int i = 0; i < rows.length; i++) {
               boolean hl = i == 2;
               Gfx.text(g, rows[i][0], x + 8, cy, gray, 1.0F, false);
               cy += 9;
               if (hl) {
                  Gfx.glow(g, x + 8, cy, w - 16, 11, 3, acc, 0.6F + 0.6F * pulse, 4);
               }

               Gfx.round(g, x + 8, cy, w - 16, 11, 3, 16777215, 1.0F);
               Gfx.round(g, x + 8, cy, w - 16, 1, 1, hl ? acc : 13158610, 1.0F);
               Gfx.text(g, Gfx.ellipsize(rows[i][1], w - 24), x + 11, cy + 2, hl ? acc : dark, 1.0F, false);
               if (hl) {
                  Gfx.round(g, x + w - 30, cy + 1, 20, 9, 3, 15000812, 1.0F);
               }

               if (hl) {
                  Gfx.center(g, "Add", x + w - 20, cy + 2, dark, 0.8F, false);
               }

               cy += 14;
            }

            Gfx.round(g, x + 8, cy, 8, 8, 2, green, 1.0F);
            Gfx.rect(g, x + 10, cy + 4, 2, 1, 16777215, 1.0F);
            Gfx.rect(g, x + 11, cy + 5, 1, 1, 16777215, 1.0F);
            Gfx.rect(g, x + 12, cy + 3, 1, 2, 16777215, 1.0F);
            Gfx.rect(g, x + 13, cy + 2, 1, 1, 16777215, 1.0F);
            Gfx.text(g, "Web API", x + 20, cy, dark, 1.0F, false);
            Gfx.round(g, x + w - 44, cy - 2, 36, 12, 6, green, 1.0F);
            Gfx.center(g, "Save", x + w - 26, cy + 1, 16777215, 1.0F, false);
            break;
         case 3:
            Gfx.text(g, "Minecraft Lyrics", x + 8, cy, dark, 1.0F, false);
            Gfx.round(g, x + w - 52, cy - 2, 44, 12, 6, 14474468, 1.0F);
            Gfx.center(g, "Settings", x + w - 30, cy + 1, dark, 1.0F, false);
            cy += 16;
            Gfx.text(g, "Basic Information", x + 8, cy, gray, 1.0F, false);
            cy += 12;
            Gfx.text(g, "Client ID", x + 8, cy, dark, 1.0F, false);
            cy += 10;
            Gfx.glow(g, x + 8, cy, w - 16, 12, 3, acc, 0.6F + 0.6F * pulse, 4);
            Gfx.round(g, x + 8, cy, w - 16, 12, 3, 16777215, 1.0F);
            Gfx.text(g, Gfx.ellipsize("3f9a1c7e2b8d4e60a5c1…", w - 40), x + 11, cy + 2, dark, 1.0F, false);
            Gfx.round(g, x + w - 20, cy + 2, 8, 8, 2, acc, 1.0F);
            Gfx.round(g, x + w - 18, cy + 4, 4, 4, 1, 16777215, 1.0F);
            cy += 18;
            Gfx.text(g, "Client secret", x + 8, cy, gray, 1.0F, false);
            Gfx.text(g, Fx.t("neonlyrics.tut.nosecret"), x + 8, cy + 10, 12597547, 1.0F, false);
      }
   }

   private void arrow(DrawContext g, int x, int y, float p) {
      int o = (int)(p * 3.0F);
      int col = Theme.accent();
      Gfx.rect(g, x - 8 + o, y, 8, 2, col, 1.0F);
      Gfx.rect(g, x - 2 + o, y - 2, 2, 6, col, 1.0F);
      Gfx.rect(g, x + o, y - 1, 2, 4, col, 1.0F);
   }

   private void drawStatus(DrawContext g, int x, int y, int w, int h) {
      int acc = Theme.accent();
      SpotifyClient.Status st = SpotifyClient.status();
      NowPlaying np = SpotifyClient.now();
      Gfx.round(g, x, y, w, h, 8, 1972265, 0.95F);

      int col = switch (st) {
         case CONNECTED -> 6022299;
         case ERROR -> 16735354;
         case WAITING_LOGIN -> 16762460;
         default -> 10326968;
      };
      Gfx.text(g, Fx.t("neonlyrics.tut.status"), x + 10, y + 8, 10326968, 1.0F, false);
      float p = st == SpotifyClient.Status.WAITING_LOGIN ? Anim.pulse(700.0) : 1.0F;
      Gfx.circle(g, x + 13, y + 26, 4.0F, col, 0.4F + 0.6F * p);

      String s = switch (st) {
         case CONNECTED -> Fx.t("neonlyrics.spotify.st.on");
         case ERROR -> Fx.t("neonlyrics.spotify.st.error", SpotifyClient.lastError());
         case WAITING_LOGIN -> Fx.t("neonlyrics.spotify.st.wait");
         case NOT_CONFIGURED -> Fx.t("neonlyrics.spotify.st.setup");
         case DISCONNECTED -> Fx.t("neonlyrics.spotify.st.off");
      };
      int ty = y + 22;

      for (String line : Gfx.wrap(s, w - 30)) {
         Gfx.text(g, line, x + 22, ty, col, 1.0F, false);
         ty += 10;
      }

      ty += 6;
      if (st == SpotifyClient.Status.CONNECTED) {
         String song = np.hasTrack() ? "♪ " + np.title : Fx.t("neonlyrics.tut.playsong");

         for (String line : Gfx.wrap(song, w - 20)) {
            Gfx.text(g, line, x + 10, ty, 15986687, 0.9F, false);
            ty += 10;
         }
      } else if (st == SpotifyClient.Status.WAITING_LOGIN) {
         for (String line : Gfx.wrap(Fx.t("neonlyrics.tut.browserhint"), w - 20)) {
            Gfx.text(g, line, x + 10, ty, 10326968, 0.9F, false);
            ty += 10;
         }
      }

      Gfx.rect(g, x + 10, y + h - 20, w - 20, 1, 3813456, 0.8F);
      Gfx.text(g, Gfx.ellipsize(Fx.t("neonlyrics.tut.local"), w - 20), x + 10, y + h - 14, 10326968, 0.85F, false);
      if (st == SpotifyClient.Status.CONNECTED) {
         Gfx.round(g, x, y, 3, h, 2, acc, 1.0F);
      }
   }

   private void drawHelp(DrawContext g, int x, int y, int w, int h) {
      int cw = (w - 6) / 2;
      int chh = (h - 6) / 2;

      for (int i = 0; i < 4; i++) {
         int bx = x + i % 2 * (cw + 6);
         int by = y + i / 2 * (chh + 6);
         Gfx.round(g, bx, by, cw, chh, 6, 1972265, 0.95F);
         Gfx.round(g, bx, by + 5, 2, chh - 10, 1, 16762460, 1.0F);
         Gfx.text(g, Gfx.ellipsize(Fx.t("neonlyrics.tut.err" + (i + 1)), cw - 14), bx + 8, by + 5, 16762460, 1.0F, false);
         int ty = by + 16;

         for (String line : Gfx.wrap(Fx.t("neonlyrics.tut.fix" + (i + 1)), cw - 14)) {
            if (ty > by + chh - 9) {
               break;
            }

            Gfx.text(g, line, bx + 8, ty, 15986687, 0.85F, false);
            ty += 9;
         }
      }
   }

   private void button(DrawContext g, String id, int x, int y, int w, int h, String label, int color, boolean filled, Runnable r) {
      this.button(g, id, x, y, w, h, label, color, filled, r, true);
   }

   private void button(DrawContext g, String id, int x, int y, int w, int h, String label, int color, boolean filled, Runnable r, boolean clickable) {
      boolean hover = clickable && this.mx >= x && this.my >= y && this.mx < x + w && this.my < y + h;
      float hv = this.pool.to("b" + id, hover ? 1.0 : 0.0);
      Gfx.glow(g, x, y, w, h, 5, color, hv * 0.8F, 4);
      Gfx.round(g, x, y, w, h, 5, filled ? color : Gfx.lerpColor(1972265, color, 0.12F + 0.2F * hv), filled ? 0.3F + 0.25F * hv : 0.95F);
      Gfx.center(
         g, Gfx.ellipsize(label, w - 6), x + w / 2, y + (h - 8) / 2, Gfx.lerpColor(Gfx.lerpColor(15986687, color, 0.3F), 16777215, hv * 0.6F), 1.0F, false
      );
      if (clickable) {
         this.hits.add(new SpotifyTutorialScreen.Hit(x, y, w, h, () -> {
            Fx.sound(Fx.Sfx.CLICK);
            r.run();
         }));
      }
   }

   private void hit(int x, int y, int w, int h, Runnable r, int stepIndex) {
      this.hits.add(new SpotifyTutorialScreen.Hit(x, y, w, h, () -> this.go(stepIndex)));
   }

   public boolean mouseClicked(Click event, boolean doubleClick) {
      if (super.mouseClicked(event, doubleClick)) {
         return true;
      }

      if (event.button() != 0) {
         return false;
      }

      for (int i = this.hits.size() - 1; i >= 0; i--) {
         SpotifyTutorialScreen.Hit h = this.hits.get(i);
         if (event.x() >= h.x() && event.y() >= h.y() && event.x() < h.x() + h.w() && event.y() < h.y() + h.h()) {
            this.setFocused(null);
            h.r().run();
            return true;
         }
      }

      this.setFocused(null);
      return false;
   }

   public boolean keyPressed(KeyInput event) {
      int k = event.key();
      if (this.getFocused() == null) {
         if (k == 262 || k == 257 || k == 335) {
            if (this.step == 5) {
               this.close();
            } else {
               this.go(this.step + 1);
            }

            return true;
         }

         if (k == 263) {
            this.go(this.step - 1);
            return true;
         }
      }

      return super.keyPressed(event);
   }

   public void close() {
      Config c = NeonLyrics.cfg();
      c.spotifyClientId = this.idBox.getText().trim();
      c.spotifyTutorialSeen = true;
      NeonLyrics.saveConfig();
      this.client.setScreen(this.parent);
   }

   public boolean shouldPause() {
      return false;
   }

   private record Hit(int x, int y, int w, int h, Runnable r) {
      private Hit {
      }
   }
}
