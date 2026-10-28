package dev.neonlyrics.perf;

import dev.neonlyrics.NeonLyrics;
import dev.neonlyrics.config.Config;
import dev.neonlyrics.util.AutoPerf;
import java.util.Map;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.CloudRenderMode;
import net.minecraft.client.option.GraphicsMode;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.particle.ParticlesMode;

/**
 * Applies and restores the vanilla option side of the FPS mode.
 *
 * <p>Every option that is touched is captured first and written back on disable,
 * so the boost behaves like a reversible preset instead of destroying the
 * player's settings. Values are also persisted to perf-state.json so a crash or
 * a normal game exit while boosted can still be undone on the next launch.</p>
 */
public final class PerfHooks {
   private static long lastAdapt;
   private static long lastChange;
   private static int lowStreak;
   private static int highStreak;

   private PerfHooks() {
   }

   public static void apply() {
      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc == null || mc.options == null) {
         return;
      }

      PerfSettings s = PerfSettings.get();
      PerfState st = PerfState.get();
      Config c = NeonLyrics.cfg();
      if (!st.applied) {
         captureAll(mc, st);
         st.qualityBefore = c.quality;
         st.applied = true;
         migrateLegacy(c.boostBefore, st.previous);
         c.boostBefore = "";
      }

      if (s.applyVanillaOptions) {
         if (s.reduceRenderDistance) {
            setInt(mc.options.getViewDistance(), s.renderDistance);
         }
         if (s.reduceSimulationDistance) {
            setInt(mc.options.getSimulationDistance(), s.simulationDistance);
         }
         if (s.reduceEntityDistance) {
            setDouble(mc.options.getEntityDistanceScaling(), s.entityDistanceScaling);
         }
         if (s.disableEntityShadows) {
            setBool(mc.options.getEntityShadows(), false);
         }
         if (s.reduceParticles) {
            ParticlesMode mode = ParticlesMode.MINIMAL;
            try {
               mode = ParticlesMode.valueOf(s.particles);
            } catch (Throwable ignored) {
            }
            setRaw(mc.options.getParticles(), mode);
         }
         if (s.disableBiomeBlend) {
            setInt(mc.options.getBiomeBlendRadius(), 0);
         }
         if (s.disableAo) {
            setBool(mc.options.getAo(), false);
         }
         if (s.fastGraphics) {
            setRaw(mc.options.getPreset(), GraphicsMode.FAST);
         }
         if (s.disableClouds) {
            setRaw(mc.options.getCloudRenderMode(), CloudRenderMode.OFF);
            setInt(mc.options.getCloudRenderDistance(), s.cloudRenderDistance);
         }
         if (s.disableWeather) {
            setInt(mc.options.getWeatherRadius(), s.weatherRadius);
         }
         if (s.raiseFpsCap) {
            setInt(mc.options.getMaxFps(), s.maxFps);
         }
         if (s.disableVsync) {
            setBool(mc.options.getEnableVsync(), false);
         }
         if (s.lowLatency) {
            setBool(mc.options.getEnableVsync(), false);
            setBool(mc.options.getRawMouseInput(), true);
            setInt(mc.options.getMaxFps(), 260);
         }
         if (s.disableBobView) {
            setBool(mc.options.getBobView(), false);
         }
         if (s.disableMenuBlur) {
            setInt(mc.options.getMenuBackgroundBlurriness(), 0);
         }
         if (s.disableVignette) {
            setBool(mc.options.getVignette(), false);
         }
         if (s.disableImprovedTransparency) {
            setBool(mc.options.getImprovedTransparency(), false);
         }
         if (s.disableCutoutLeaves) {
            setBool(mc.options.getCutoutLeaves(), false);
         }
         if (s.hideLightningFlashes) {
            setBool(mc.options.getHideLightningFlashes(), true);
         }
         if (s.disableChunkFade) {
            setDouble(mc.options.getChunkFade(), 0.0D);
         }
         if (s.mipmapLevels >= 0) {
            setInt(mc.options.getMipmapLevels(), s.mipmapLevels);
         }
      }

      c.quality = 0;
      PerfState.save();
      writeOptions(mc);
   }

   public static void restore() {
      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc == null || mc.options == null) {
         return;
      }

      PerfState st = PerfState.get();
      Config c = NeonLyrics.cfg();
      if (st.applied) {
         SimpleOption<?>[] options = allOptions(mc);
         String[] keys = keys();
         for (int i = 0; i < keys.length && i < options.length; i++) {
            String value = st.previous.get(keys[i]);
            if (value != null) {
               applyValue(options[i], value);
            }
         }

         if (c.quality <= 0) {
            c.quality = st.qualityBefore;
         }
         st.applied = false;
         st.previous.clear();
         PerfState.save();
      } else {
         restoreLegacy(c.boostBefore, mc);
      }

      c.boostBefore = "";
      writeOptions(mc);
   }

   public static void tick(MinecraftClient mc) {
      if (!PerfBoost.active()) {
         lowStreak = 0;
         highStreak = 0;
         return;
      }

      if (!PerfState.get().applied) {
         apply();
      }

      PerfSettings s = PerfSettings.get();
      if (!s.adaptiveViewDistance || mc == null || mc.world == null || mc.player == null) {
         return;
      }

      long now = System.currentTimeMillis();
      if (now - lastAdapt < 2000L) {
         return;
      }

      lastAdapt = now;
      int fps = AutoPerf.fps();
      if (fps <= 0) {
         return;
      }

      if (fps < s.adaptiveLowFps) {
         lowStreak++;
         highStreak = 0;
      } else if (fps > s.adaptiveHighFps) {
         highStreak++;
         lowStreak = 0;
      } else {
         lowStreak = 0;
         highStreak = 0;
      }

      SimpleOption<Integer> option = mc.options.getViewDistance();
      int current = option.getValue();
      if (lowStreak >= 2 && current > s.adaptiveMinViewDistance && now - lastChange >= 3000L) {
         setInt(option, current - 1);
         lowStreak = 0;
         lastChange = now;
         writeOptions(mc);
      } else if (highStreak >= 2 && current < s.renderDistance && now - lastChange >= 3000L) {
         setInt(option, current + 1);
         highStreak = 0;
         lastChange = now;
         writeOptions(mc);
      }
   }

   private static void captureAll(MinecraftClient mc, PerfState st) {
      SimpleOption<?>[] options = allOptions(mc);
      String[] keys = keys();
      for (int i = 0; i < keys.length && i < options.length; i++) {
         st.previous.put(keys[i], save(options[i]));
      }
   }

   private static SimpleOption<?>[] allOptions(MinecraftClient mc) {
      return new SimpleOption<?>[]{
         mc.options.getViewDistance(),
         mc.options.getSimulationDistance(),
         mc.options.getEntityDistanceScaling(),
         mc.options.getEntityShadows(),
         mc.options.getParticles(),
         mc.options.getBiomeBlendRadius(),
         mc.options.getAo(),
         mc.options.getPreset(),
         mc.options.getCloudRenderMode(),
         mc.options.getCloudRenderDistance(),
         mc.options.getMaxFps(),
         mc.options.getEnableVsync(),
         mc.options.getBobView(),
         mc.options.getMenuBackgroundBlurriness(),
         mc.options.getVignette(),
         mc.options.getImprovedTransparency(),
         mc.options.getCutoutLeaves(),
         mc.options.getHideLightningFlashes(),
         mc.options.getChunkFade(),
         mc.options.getWeatherRadius(),
         mc.options.getMipmapLevels(),
         mc.options.getRawMouseInput()
      };
   }

   private static String[] keys() {
      return new String[]{
         "viewDistance",
         "simulationDistance",
         "entityDistanceScaling",
         "entityShadows",
         "particles",
         "biomeBlendRadius",
         "ao",
         "graphicsMode",
         "cloudRenderMode",
         "cloudRenderDistance",
         "maxFps",
         "enableVsync",
         "bobView",
         "menuBackgroundBlurriness",
         "vignette",
         "improvedTransparency",
         "cutoutLeaves",
         "hideLightningFlashes",
         "chunkFade",
         "weatherRadius",
         "mipmapLevels",
         "rawMouseInput"
      };
   }

   private static void migrateLegacy(String legacy, Map<String, String> previous) {
      if (legacy == null || legacy.isEmpty()) {
         return;
      }

      String[] parts = legacy.split(";", -1);
      if (parts.length >= 3) {
         if (!parts[0].isEmpty()) {
            previous.put("entityShadows", parts[0]);
         }
         if (!parts[1].isEmpty()) {
            previous.put("particles", parts[1]);
         }
         if (!parts[2].isEmpty()) {
            previous.put("biomeBlendRadius", parts[2]);
         }
      }
   }

   private static void restoreLegacy(String legacy, MinecraftClient mc) {
      if (legacy == null || legacy.isEmpty()) {
         return;
      }

      String[] parts = legacy.split(";", -1);
      if (parts.length >= 3) {
         if (!parts[0].isEmpty()) {
            applyValue(mc.options.getEntityShadows(), parts[0]);
         }
         if (!parts[1].isEmpty()) {
            applyValue(mc.options.getParticles(), parts[1]);
         }
         if (!parts[2].isEmpty()) {
            applyValue(mc.options.getBiomeBlendRadius(), parts[2]);
         }
      }
   }

   private static String save(SimpleOption<?> option) {
      Object value = option.getValue();
      return value instanceof Enum<?> e ? e.name() : String.valueOf(value);
   }

   private static void applyValue(SimpleOption<?> option, String value) {
      Object current = option.getValue();
      Object next;
      if (current instanceof Boolean) {
         next = Boolean.parseBoolean(value);
      } else if (current instanceof Integer) {
         next = Integer.parseInt(value);
      } else if (current instanceof Double) {
         next = Double.parseDouble(value);
      } else if (current instanceof Float) {
         next = Float.parseFloat(value);
      } else {
         if (!(current instanceof Enum<?> e)) {
            return;
         }
         next = Enum.valueOf((Class<? extends Enum>)e.getDeclaringClass(), value);
      }

      setRaw(option, next);
   }

   private static void setRaw(SimpleOption<?> option, Object value) {
      ((SimpleOption)option).setValue(value);
   }

   private static void setInt(SimpleOption<?> option, int value) {
      setRaw(option, value);
   }

   private static void setBool(SimpleOption<?> option, boolean value) {
      setRaw(option, value);
   }

   private static void setDouble(SimpleOption<?> option, double value) {
      setRaw(option, value);
   }

   private static void writeOptions(MinecraftClient mc) {
      try {
         mc.options.write();
      } catch (Throwable t) {
         NeonLyrics.LOG.warn("FPS boost could not save options", t);
      }
   }
}
