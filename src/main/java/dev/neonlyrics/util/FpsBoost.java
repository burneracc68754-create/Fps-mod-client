package dev.neonlyrics.util;

import dev.neonlyrics.perf.PerfBoost;

/**
 * Legacy entry point kept for older call sites. All work now happens in
 * {@link PerfBoost}, which restores every touched vanilla option on disable.
 */
public final class FpsBoost {
   private FpsBoost() {
   }

   public static void apply(boolean on) {
      PerfBoost.setBoost(on);
   }
}
