package dev.neonlyrics;

import dev.neonlyrics.config.Config;
import dev.neonlyrics.config.Storage;
import dev.neonlyrics.cosmetic.DeathFx;
import dev.neonlyrics.cosmetic.PhoneMenu;
import dev.neonlyrics.gui.LicenseScreen;
import dev.neonlyrics.hud.BeatGlow;
import dev.neonlyrics.hud.Crosshair;
import dev.neonlyrics.hud.GlassHotbar;
import dev.neonlyrics.hud.Island;
import dev.neonlyrics.hud.StartupIntro;
import dev.neonlyrics.lyrics.Favorites;
import dev.neonlyrics.lyrics.SpotifyClient;
import dev.neonlyrics.perf.PerfBoost;
import dev.neonlyrics.sky.SkyRenderer;
import dev.neonlyrics.sky.WorldSky;
import dev.neonlyrics.hud.Stats;
import dev.neonlyrics.util.AutoPerf;
import dev.neonlyrics.util.Fx;
import dev.neonlyrics.util.Profiles;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.util.ActionResult;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding.Category;
import net.minecraft.client.util.InputUtil.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class NeonLyrics implements ClientModInitializer {
   public static final String MOD_ID = "neonlyrics";
   public static final Logger LOG = LoggerFactory.getLogger("NeonLyrics");
   private static Config config = new Config();
   public static KeyBinding keyMenu;
   public static KeyBinding keySkip;
   public static KeyBinding keyPrev;
   public static KeyBinding keyPause;
   public static KeyBinding keyLyrics;
   public static KeyBinding keySync;
   public static KeyBinding keyFav;
   public static KeyBinding keyClip;
   public static KeyBinding keyProfile;
   private static boolean licenseHint;
   private static boolean perfInit;

   public NeonLyrics() {
   }

   public static Config cfg() {
      return config;
   }

   public static void saveConfig() {
      config.clamp();
      Storage.save("config.json", config);
   }

   public void onInitializeClient() {
      Storage.migrateFromAuction();
      config = Storage.load("config.json", Config.class, Config::new);
      config.clamp();
      saveConfig();
      Category cat = Category.create(Identifier.of("neonlyrics", "main"));
      keyMenu = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.neonlyrics.openmenu", Type.KEYSYM, 74, cat));
      keySkip = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.neonlyrics.skip", Type.KEYSYM, 298, cat));
      keyPrev = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.neonlyrics.prev", Type.KEYSYM, -1, cat));
      keyPause = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.neonlyrics.pause", Type.KEYSYM, -1, cat));
      keyFav = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.neonlyrics.fav", Type.KEYSYM, 78, cat));
      keyClip = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.neonlyrics.clip", Type.KEYSYM, 299, cat));
      keyProfile = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.neonlyrics.profile", Type.KEYSYM, 297, cat));
      keySync = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.neonlyrics.sync", Type.KEYSYM, -1, cat));
      keyLyrics = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.neonlyrics.lyrics", Type.KEYSYM, -1, cat));
      HudElementRegistry.addFirst(Identifier.of("neonlyrics", "sky"), SkyRenderer::render);
      HudElementRegistry.addLast(Identifier.of("neonlyrics", "island"), Island::render);
      HudElementRegistry.addLast(Identifier.of("neonlyrics", "stats"), Stats::render);
      HudElementRegistry.replaceElement(VanillaHudElements.HOTBAR, GlassHotbar::wrap);
      HudElementRegistry.replaceElement(VanillaHudElements.CROSSHAIR, Crosshair::wrap);
      HudElementRegistry.addLast(Identifier.of("neonlyrics", "beatglow"), BeatGlow::render);
      WorldRenderEvents.AFTER_ENTITIES.register(WorldSky::render);
      SpotifyClient.init();
      StartupIntro.register();
      AttackEntityCallback.EVENT.register((AttackEntityCallback)(player, level, hand, entity, hit) -> {
         DeathFx.onAttack(entity);
         return ActionResult.PASS;
      });
      ClientTickEvents.END_CLIENT_TICK.register(this::tick);
      ClientTickEvents.START_CLIENT_TICK.register(PhoneMenu::startTick);
      LOG.info("Lyrics Cord geladen");
   }

   private void tick(MinecraftClient mc) {
      if (License.ok()) {
         if (!perfInit) {
            perfInit = true;
            PerfBoost.onInit();
         }
         AutoPerf.tick(mc);
         PerfBoost.tick(mc);
         PhoneMenu.tick(mc);
         DeathFx.tick(mc);
         SkyRenderer.tick(mc);

         while (keyMenu.wasPressed()) {
            if (mc.currentScreen == null) {
               PhoneMenu.open(mc);
            }
         }

         while (keySkip.wasPressed()) {
            SpotifyClient.control(SpotifyClient.Cmd.NEXT);
         }

         while (keyPrev.wasPressed()) {
            SpotifyClient.control(SpotifyClient.Cmd.PREVIOUS);
         }

         while (keyPause.wasPressed()) {
            SpotifyClient.control(SpotifyClient.Cmd.PLAYPAUSE);
         }

         while (keySync.wasPressed()) {
            SpotifyClient.syncHere();
         }

         while (keyFav.wasPressed()) {
            Favorites.saveCurrent();
         }

         while (keyClip.wasPressed()) {
            mc.options.hudHidden = !mc.options.hudHidden;
         }

         while (keyProfile.wasPressed()) {
            Profiles.toggle();
         }

         while (keyLyrics.wasPressed()) {
            config.lyricsSky = !config.lyricsSky;
            saveConfig();
            Island.notify(Fx.t(config.lyricsSky ? "neonlyrics.media.lyricson" : "neonlyrics.media.lyricsoff"), config.lyricsSky);
         }
      } else {
         while (keyMenu.wasPressed()) {
            if (mc.currentScreen == null) {
               mc.setScreen(new LicenseScreen());
            }
         }

         for (KeyBinding k : new KeyBinding[]{keySkip, keyPrev, keyPause, keySync, keyFav, keyClip, keyProfile, keyLyrics}) {
            while (k.wasPressed()) {
            }
         }

         if (!licenseHint && mc.player != null) {
            licenseHint = true;
            mc.inGameHud.getChatHud().addMessage(Text.translatable("neonlyrics.lic.chat"));
         }
      }
   }
}
