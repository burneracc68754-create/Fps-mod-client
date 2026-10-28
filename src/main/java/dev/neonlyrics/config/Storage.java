package dev.neonlyrics.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.neonlyrics.NeonLyrics;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.function.Supplier;
import net.fabricmc.loader.api.FabricLoader;

public final class Storage {
   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

   private Storage() {
   }

   public static Path dir() {
      return FabricLoader.getInstance().getConfigDir().resolve("neonlyrics");
   }

   public static void migrateFromAuction() {
      try {
         Path old = FabricLoader.getInstance().getConfigDir().resolve("neonauction");
         if (!Files.isDirectory(old) || Files.exists(dir().resolve("config.json"))) {
            return;
         }

         Files.createDirectories(dir());
         Path oldCfg = Files.exists(old.resolve("config-v13-backup.json")) ? old.resolve("config-v13-backup.json") : old.resolve("config.json");
         if (Files.exists(oldCfg)) {
            Files.copy(oldCfg, dir().resolve("config.json"));
         }

         for (String f : new String[]{"spotify.json", "favorites.json"}) {
            Path src = old.resolve(f);
            if (Files.exists(src) && !Files.exists(dir().resolve(f))) {
               Files.copy(src, dir().resolve(f));
            }
         }

         NeonLyrics.LOG.info("Einstellungen aus config/neonauction uebernommen");
      } catch (Exception var7) {
         NeonLyrics.LOG.warn("Konnte alte Einstellungen nicht uebernehmen", var7);
      }
   }

   public static <T> T load(String file, Type type, Supplier<T> fallback) {
      Path p = dir().resolve(file);
      if (!Files.exists(p)) {
         return fallback.get();
      }

      try (Reader r = Files.newBufferedReader(p, StandardCharsets.UTF_8)) {
         T t = (T)GSON.fromJson(r, type);
         return t != null ? t : fallback.get();
      } catch (Exception var9) {
         NeonLyrics.LOG.warn("Konnte {} nicht laden, nutze Standardwerte", file, var9);
         return fallback.get();
      }
   }

   public static void save(String file, Object data) {
      try {
         Files.createDirectories(dir());
         Path p = dir().resolve(file);
         Path tmp = dir().resolve(file + ".tmp");

         try (Writer w = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
            GSON.toJson(data, w);
         }

         Files.move(tmp, p, StandardCopyOption.REPLACE_EXISTING);
      } catch (Exception var9) {
         NeonLyrics.LOG.warn("Konnte {} nicht speichern", file, var9);
      }
   }
}
