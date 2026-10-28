package dev.neonlyrics.gui;

import dev.neonlyrics.License;
import dev.neonlyrics.NeonLyrics;
import dev.neonlyrics.config.Config;
import dev.neonlyrics.config.Storage;
import dev.neonlyrics.hud.Gfx;
import java.nio.file.Files;
import java.time.LocalDateTime;
import net.minecraft.client.gui.Click;
import net.minecraft.text.Text;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.screen.Screen;

public class LicenseScreen extends Screen {
   private static final int ACCENT = 10321407;
   private TextFieldWidget box;
   private String status = "";
   private int statusCol = 16777215;
   private long okTime = -1L;
   private int bx;
   private int by;
   private int bw;
   private int bh;
   private int px;
   private int py;
   private int pw;
   private int ph;

   public LicenseScreen() {
      super(Text.literal("Lyrics Cord"));
   }

   private static String tr(String key) {
      return Text.translatable("neonlyrics.lic." + key).getString();
   }

   protected void init() {
      int w = Math.min(320, this.width - 40);
      this.box = new TextFieldWidget(this.textRenderer, this.width / 2 - w / 2 + 12, this.height / 2 + 4, w - 24, 16, Text.literal(""));
      this.box.setMaxLength(200);
      this.box.setPlaceholder(Text.literal("LCORD-XXXX-XXXX-XXXX"));
      String old = NeonLyrics.cfg().licenseCode;
      this.box.setText(old.startsWith("R1:") ? "" : old);
      this.addDrawableChild(this.box);
      this.setInitialFocus(this.box);
   }

   public void render(DrawContext g, int mouseX, int mouseY, float delta) {
      int w = Math.min(320, this.width - 40);
      int h = 150;
      int x = this.width / 2 - w / 2;
      int y = this.height / 2 - 70;
      Gfx.aaRound(g, x, y, w, h, 12.0F, 1380895, 0.96F);
      Gfx.aaRound(g, x, y, w, 2.0F, 1.0F, 10321407, 0.9F);
      Gfx.center(g, tr("title"), this.width / 2, y + 14, 16777215, 1.0F, true);
      Gfx.center(g, tr("account") + " " + License.name(), this.width / 2, y + 32, 13219327, 1.0F, false);
      Gfx.center(g, tr("hint"), this.width / 2, y + 48, 11184821, 1.0F, false);
      Gfx.aaRound(g, x + 10, this.height / 2, w - 20, 24.0F, 6.0F, 0, 0.5F);
      super.render(g, mouseX, mouseY, delta);
      this.ph = 18;
      this.pw = (w - 30) / 2;
      this.px = x + 10;
      this.py = y + h - 34;
      this.bx = this.px + this.pw + 10;
      this.by = this.py;
      this.bw = this.pw;
      this.bh = this.ph;
      boolean ovP = mouseX >= this.px && mouseY >= this.py && mouseX < this.px + this.pw && mouseY < this.py + this.ph;
      boolean ovB = mouseX >= this.bx && mouseY >= this.by && mouseX < this.bx + this.bw && mouseY < this.by + this.bh;
      Gfx.aaRound(g, this.px, this.py, this.pw, this.ph, 9.0F, 16777215, ovP ? 0.18F : 0.09F);
      Gfx.center(g, tr("paste"), this.px + this.pw / 2, this.py + 5, 16777215, 1.0F, false);
      Gfx.aaRound(g, this.bx, this.by, this.bw, this.bh, 9.0F, 10321407, ovB ? 1.0F : 0.8F);
      Gfx.center(g, tr("unlock"), this.bx + this.bw / 2, this.by + 5, 16777215, 1.0F, true);
      if (!this.status.isEmpty()) {
         Gfx.center(g, this.status, this.width / 2, y + h + 8, this.statusCol, 1.0F, true);
      }

      if (this.okTime > 0L && System.currentTimeMillis() - this.okTime > 900L) {
         this.close();
      }
   }

   public boolean mouseClicked(Click event, boolean doubleClick) {
      if (event.button() == 0) {
         double mx = event.x();
         double my = event.y();
         if (mx >= this.px && my >= this.py && mx < this.px + this.pw && my < this.py + this.ph) {
            this.box.setText(this.client.keyboard.getClipboard().trim());
            return true;
         }

         if (mx >= this.bx && my >= this.by && mx < this.bx + this.bw && my < this.by + this.bh) {
            this.tryUnlock();
            return true;
         }
      }

      return super.mouseClicked(event, doubleClick);
   }

   private void unlocked() {
      NeonLyrics.saveConfig();
      this.status = tr("ok");
      this.statusCol = 8257434;
      this.okTime = System.currentTimeMillis();
   }

   private void tryUnlock() {
      String code = this.box.getText().trim();
      String u = License.uuid();
      Config cf = NeonLyrics.cfg();
      if (License.isRedeemCode(code)) {
         String n = License.normalize(code);
         String owner = cf.usedCodes.get(n);
         if (owner != null && !owner.equals(u)) {
            this.status = tr("used");
            this.statusCol = 16738922;
            return;
         }

         cf.usedCodes.put(n, u);
         cf.licenseCode = "R1:" + n + ":" + u;
         this.unlocked();
      } else if (License.check(u, code)) {
         cf.licenseCode = code;
         this.unlocked();
      } else {
         String n = License.normalize(code);
         this.status = tr("bad") + (n.isEmpty() ? "" : "  (" + (n.length() > 22 ? n.substring(0, 22) + "..." : n) + ")");
         this.statusCol = 16738922;
      }

      try {
         Files.writeString(
            Storage.dir().resolve("license-status.txt"),
            LocalDateTime.now()
               + "\nAccount: "
               + License.name()
               + " "
               + u
               + "\nEingabe-Laenge: "
               + code.length()
               + "\nGelesen: "
               + License.normalize(code)
               + "\nErgebnis: "
               + this.status
               + "\n"
         );
      } catch (Throwable var6) {
      }
   }
}
