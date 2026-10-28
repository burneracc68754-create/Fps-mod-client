package dev.neonlyrics.perf;

import dev.neonlyrics.config.Storage;
import java.util.LinkedHashMap;
import java.util.Map;

/** Persisted copy of the vanilla options changed by the FPS mode. */
public final class PerfState {
   private static PerfState instance;

   public boolean applied = false;
   public int qualityBefore = 1;
   public Map<String, String> previous = new LinkedHashMap<>();

   private PerfState() {
   }

   public static PerfState get() {
      if (instance == null) {
         instance = Storage.load("perf-state.json", PerfState.class, PerfState::new);
         if (instance == null) {
            instance = new PerfState();
         }
         if (instance.previous == null) {
            instance.previous = new LinkedHashMap<>();
         }
      }
      return instance;
   }

   public static void save() {
      Storage.save("perf-state.json", get());
   }
}
