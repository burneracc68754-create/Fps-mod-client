package dev.neonlyrics.perf;

import dev.neonlyrics.config.Storage;

/**
 * Advanced settings for the FPS mode, stored in config/neonlyrics/perf.json.
 *
 * <p>The defaults are deliberately aggressive: the FPS switch should actually
 * buy frames instead of only changing two vanilla sliders.</p>
 */
public final class PerfSettings {
   private static PerfSettings instance;

   public boolean applyVanillaOptions = true;

   public boolean reduceRenderDistance = true;
   public int renderDistance = 8;
   public boolean reduceSimulationDistance = true;
   public int simulationDistance = 5;
   public boolean reduceEntityDistance = true;
   public double entityDistanceScaling = 0.5D;

   public boolean disableEntityShadows = true;
   public boolean reduceParticles = true;
   public String particles = "MINIMAL";
   public boolean disableBiomeBlend = true;
   public boolean disableAo = true;
   public boolean fastGraphics = true;
   public boolean disableClouds = true;
   public boolean disableWeather = true;

   public boolean raiseFpsCap = true;
   public int maxFps = 260;
   public boolean disableVsync = true;
   public boolean lowLatency = true;
   public boolean disableBobView = true;
   public boolean disableMenuBlur = true;
   public boolean disableVignette = true;
   public boolean disableImprovedTransparency = true;
   public boolean disableCutoutLeaves = true;
   public boolean hideLightningFlashes = true;
   public boolean disableChunkFade = true;
   public int mipmapLevels = -1;
   public int cloudRenderDistance = 2;
   public int weatherRadius = 1;

   public boolean adaptiveViewDistance = true;
   public int adaptiveMinViewDistance = 4;
   public int adaptiveLowFps = 45;
   public int adaptiveHighFps = 75;

   public boolean cullEntities = true;
   public int entityCullDistance = 48;
   public boolean cullBlockEntities = true;
   public int blockEntityCullDistance = 32;
   public int nameTagDistance = 24;

   public boolean cullParticles = true;
   public int particleDistance = 24;
   public boolean skipVanillaParticles = false;
   public boolean skipAllBlockEntities = false;

   public boolean skipSkyEffects = true;
   public boolean skipPlayerFx = true;
   public boolean skipSongIntro = false;

   public boolean cheapHud = true;
   public boolean squareHud = true;
   public boolean skipHudGlow = true;
   public boolean disableBeatGlow = true;
   public boolean disableCosmetics = true;
   public boolean disableLightning = true;

   private PerfSettings() {
   }

   public static PerfSettings get() {
      if (instance == null) {
         instance = Storage.load("perf.json", PerfSettings.class, PerfSettings::new);
         if (instance == null) {
            instance = new PerfSettings();
         }
         instance.clamp();
      }
      return instance;
   }

   public static void save() {
      Storage.save("perf.json", get());
   }

   public void clamp() {
      this.renderDistance = clamp(this.renderDistance, 2, 32);
      this.simulationDistance = clamp(this.simulationDistance, 5, 32);
      this.entityDistanceScaling = Math.max(0.5D, Math.min(5.0D, this.entityDistanceScaling));
      if (this.particles == null
         || !(this.particles.equals("MINIMAL") || this.particles.equals("DECREASED") || this.particles.equals("ALL"))) {
         this.particles = "MINIMAL";
      }
      this.maxFps = clamp(this.maxFps, 60, 260);
      this.mipmapLevels = this.mipmapLevels < 0 ? -1 : clamp(this.mipmapLevels, 0, 4);
      this.cloudRenderDistance = clamp(this.cloudRenderDistance, 2, 32);
      this.weatherRadius = clamp(this.weatherRadius, 3, 10);
      this.adaptiveMinViewDistance = clamp(this.adaptiveMinViewDistance, 2, 32);
      this.adaptiveLowFps = clamp(this.adaptiveLowFps, 10, 240);
      this.adaptiveHighFps = clamp(this.adaptiveHighFps, this.adaptiveLowFps + 5, 500);
      this.entityCullDistance = clamp(this.entityCullDistance, 8, 256);
      this.blockEntityCullDistance = clamp(this.blockEntityCullDistance, 8, 256);
      this.nameTagDistance = clamp(this.nameTagDistance, 4, 128);
      this.particleDistance = clamp(this.particleDistance, 4, 128);
   }

   private static int clamp(int v, int lo, int hi) {
      return Math.max(lo, Math.min(hi, v));
   }
}
