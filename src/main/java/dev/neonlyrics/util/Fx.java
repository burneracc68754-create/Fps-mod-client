package dev.neonlyrics.util;

import dev.neonlyrics.NeonLyrics;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.util.Formatting;
import net.minecraft.text.Text;
import net.minecraft.client.MinecraftClient;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;

public final class Fx {
   private Fx() {
   }

   public static String t(String key, Object... args) {
      return I18n.translate(key, args);
   }

   public static void sound(Fx.Sfx s) {
      if (NeonLyrics.cfg().sounds) {
         MinecraftClient mc = MinecraftClient.getInstance();
         if (mc.getSoundManager() != null) {
            switch (s) {
               case CLICK:
                  mc.getSoundManager().play(PositionedSoundInstance.ui((SoundEvent)SoundEvents.UI_BUTTON_CLICK.value(), 1.4F, 0.35F));
                  break;
               case WIN:
                  mc.getSoundManager().play(PositionedSoundInstance.ui(SoundEvents.ENTITY_PLAYER_LEVELUP, 1.0F, 0.6F));
                  break;
               case ERROR:
                  mc.getSoundManager().play(PositionedSoundInstance.ui((SoundEvent)SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(), 0.6F, 0.7F));
                  break;
               case TICK:
                  mc.getSoundManager().play(PositionedSoundInstance.ui((SoundEvent)SoundEvents.BLOCK_NOTE_BLOCK_HAT.value(), 1.8F, 0.4F));
            }
         }
      }
   }

   public static void info(String text) {
      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc.inGameHud != null) {
         mc.inGameHud
            .getChatHud()
            .addMessage(
               Text.literal("[Lyrics Cord] ")
                  .formatted(Formatting.AQUA)
                  .append(Text.literal(text).formatted(Formatting.WHITE))
            );
      }
   }

   public static void error(String text) {
      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc.inGameHud != null) {
         mc.inGameHud
            .getChatHud()
            .addMessage(
               Text.literal("[Lyrics Cord] ")
                  .formatted(Formatting.LIGHT_PURPLE)
                  .append(Text.literal(text).formatted(Formatting.RED))
            );
         sound(Fx.Sfx.ERROR);
      }
   }

   public static void clipboard(String s) {
      MinecraftClient.getInstance().keyboard.setClipboard(s);
   }

   public static String myName() {
      return MinecraftClient.getInstance().getSession().getUsername();
   }

   public enum Sfx {
      CLICK,
      WIN,
      ERROR,
      TICK;

      Sfx() {
      }
   }
}
