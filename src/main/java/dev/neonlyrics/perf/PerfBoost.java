package dev.neonlyrics.perf;

import dev.neonlyrics.NeonLyrics;
import dev.neonlyrics.config.Config;
import dev.neonlyrics.hud.Island;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;

/** Entry point for the aggressive FPS mode. Safe to call from any thread. */
public final class PerfBoost {
   private static boolean dirty;

   private PerfBoost() {
   }

   public static boolean active() {
      return NeonLyrics.cfg().fpsBoost;
   }

   public static void setBoost(boolean on) {
      Config c = NeonLyrics.cfg();
      if (on == c.fpsBoost) {
         if (on) {
            PerfHooks.apply();
         }
         return;
      }

      c.fpsBoost = on;
      c.boostBefore = "";
      if (on) {
         PerfHooks.apply();
      } else {
         PerfHooks.restore();
      }

      NeonLyrics.saveConfig();
   }

   /** Re-applies the current perf settings while the boost is on. */
   public static void refresh() {
      PerfSettings.save();
      dirty = false;
      if (active()) {
         PerfHooks.apply();
      }
   }

   /** Queues a cheap settings change (used while dragging sliders). */
   public static void markDirty() {
      dirty = true;
   }

   /** Called once after the client finished loading its config. */
   public static void onInit() {
      PerfSettings.save();
      if (NeonLyrics.cfg().fpsBoost) {
         PerfHooks.apply();
      } else if (PerfState.get().applied) {
         PerfHooks.restore();
         NeonLyrics.saveConfig();
      }
   }

   public static void tick(MinecraftClient mc) {
      PerfHooks.tick(mc);
      if (dirty && mc != null && mc.getWindow() != null) {
         boolean held = GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
         if (!held) {
            refresh();
         }
      }
   }

   public static void presetMax() {
      PerfSettings s = PerfSettings.get();
      s.applyVanillaOptions = true;
      s.reduceRenderDistance = true;
      s.renderDistance = 6;
      s.reduceSimulationDistance = true;
      s.simulationDistance = 5;
      s.reduceEntityDistance = true;
      s.entityDistanceScaling = 0.5D;
      s.disableEntityShadows = true;
      s.reduceParticles = true;
      s.particles = "MINIMAL";
      s.disableBiomeBlend = true;
      s.disableAo = true;
      s.fastGraphics = true;
      s.disableClouds = true;
      s.disableWeather = true;
      s.raiseFpsCap = true;
      s.maxFps = 260;
      s.disableVsync = true;
      s.lowLatency = true;
      s.disableBobView = true;
      s.disableMenuBlur = true;
      s.disableVignette = true;
      s.disableImprovedTransparency = true;
      s.disableCutoutLeaves = true;
      s.hideLightningFlashes = true;
      s.disableChunkFade = true;
      s.mipmapLevels = 0;
      s.cloudRenderDistance = 2;
      s.weatherRadius = 3;
      s.adaptiveViewDistance = true;
      s.adaptiveMinViewDistance = 4;
      s.cullEntities = true;
      s.entityCullDistance = 40;
      s.cullBlockEntities = true;
      s.blockEntityCullDistance = 28;
      s.nameTagDistance = 20;
      s.cullParticles = true;
      s.particleDistance = 20;
      s.skipVanillaParticles = false;
      s.skipAllBlockEntities = false;
      s.clamp();
      PerfSettings.save();
      if (!active()) {
         setBoost(true);
      } else {
         PerfHooks.apply();
      }
      NeonLyrics.saveConfig();
      Island.notify("Performance: maximum", true);
   }

   public static void presetBalanced() {
      PerfSettings s = PerfSettings.get();
      s.applyVanillaOptions = true;
      s.reduceRenderDistance = true;
      s.renderDistance = 10;
      s.reduceSimulationDistance = true;
      s.simulationDistance = 6;
      s.reduceEntityDistance = true;
      s.entityDistanceScaling = 0.75D;
      s.disableEntityShadows = true;
      s.reduceParticles = true;
      s.particles = "DECREASED";
      s.disableBiomeBlend = true;
      s.disableAo = false;
      s.fastGraphics = true;
      s.disableClouds = true;
      s.disableWeather = false;
      s.raiseFpsCap = true;
      s.maxFps = 144;
      s.disableVsync = true;
      s.lowLatency = true;
      s.disableBobView = true;
      s.disableMenuBlur = true;
      s.disableVignette = true;
      s.disableImprovedTransparency = true;
      s.disableCutoutLeaves = false;
      s.hideLightningFlashes = true;
      s.disableChunkFade = true;
      s.mipmapLevels = -1;
      s.cloudRenderDistance = 2;
      s.weatherRadius = 5;
      s.adaptiveViewDistance = true;
      s.adaptiveMinViewDistance = 6;
      s.cullEntities = true;
      s.entityCullDistance = 64;
      s.cullBlockEntities = true;
      s.blockEntityCullDistance = 48;
      s.nameTagDistance = 40;
      s.cullParticles = true;
      s.particleDistance = 32;
      s.skipVanillaParticles = false;
      s.skipAllBlockEntities = false;
      s.clamp();
      PerfSettings.save();
      if (!active()) {
         setBoost(true);
      } else {
         PerfHooks.apply();
      }
      NeonLyrics.saveConfig();
      Island.notify("Performance: balanced", true);
   }

   public static void presetOff() {
      setBoost(false);
   }
}
