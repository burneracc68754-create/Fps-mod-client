package dev.neonlyrics.lyrics;

import dev.neonlyrics.NeonLyrics;
import dev.neonlyrics.config.Storage;
import dev.neonlyrics.hud.Island;
import dev.neonlyrics.util.Fx;
import java.util.ArrayList;
import java.util.List;

public final class Favorites {
   private static Favorites.Data data;

   private Favorites() {
   }

   private static Favorites.Data data() {
      if (data == null) {
         data = Storage.load("favorites.json", Favorites.Data.class, Favorites.Data::new);
         if (data.list == null) {
            data.list = new ArrayList<>();
         }
      }

      return data;
   }

   public static List<Favorites.Entry> all() {
      return data().list;
   }

   public static void saveCurrent() {
      NowPlaying np = SpotifyClient.now();
      if (np.hasTrack() && !np.lines.isEmpty()) {
         long p = np.progressNow(NeonLyrics.cfg().lyricsOffsetMs);
         int li = np.lineIndex(p);
         if (li >= 0 && !np.lines.get(li).text().isBlank()) {
            String text = np.lines.get(li).text();

            for (Favorites.Entry e : data().list) {
               if (e.line.equals(text) && e.title.equals(np.title)) {
                  Island.notify(Fx.t("neonlyrics.fav.already"), true);
                  return;
               }
            }

            Favorites.Entry ex = new Favorites.Entry();
            ex.line = text;
            ex.title = np.title;
            ex.artist = np.artist;
            ex.time = System.currentTimeMillis();
            data().list.add(0, ex);

            while (data().list.size() > 200) {
               data().list.remove(data().list.size() - 1);
            }

            Storage.save("favorites.json", data());
            Island.notify(Fx.t("neonlyrics.fav.saved"), true);
         } else {
            Island.notify(Fx.t("neonlyrics.fav.noline"), false);
         }
      } else {
         Island.notify(Fx.t("neonlyrics.media.nolyrics"), false);
      }
   }

   public static void remove(Favorites.Entry e) {
      data().list.remove(e);
      Storage.save("favorites.json", data());
   }

   private static final class Data {
      List<Favorites.Entry> list = new ArrayList<>();

      private Data() {
      }
   }

   public static final class Entry {
      public String line = "";
      public String title = "";
      public String artist = "";
      public long time;

      public Entry() {
      }
   }
}
