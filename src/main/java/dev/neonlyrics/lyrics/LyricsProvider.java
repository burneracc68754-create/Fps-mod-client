package dev.neonlyrics.lyrics;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class LyricsProvider {
   private static final String BASE = "https://lrclib.net/api";
   private static final Pattern LRC = Pattern.compile("\\[(\\d{1,3}):(\\d{1,2}(?:[.:]\\d{1,3})?)]");
   private static final Map<String, List<NowPlaying.Line>> CACHE = new HashMap<>();
   private static final Pattern WORD_TAG = Pattern.compile("<(\\d{1,3}):(\\d{1,2}(?:[.:]\\d{1,3})?)>");

   private LyricsProvider() {
   }

   public static synchronized List<NowPlaying.Line> cached(String trackId) {
      return CACHE.get(trackId);
   }

   public static List<NowPlaying.Line> fetch(String trackId, String title, String artist, String album, long durationMs) throws Exception {
      List<NowPlaying.Line> c = cached(trackId);
      if (c != null) {
         return c;
      }

      String synced = null;
      String url = "https://lrclib.net/api/get?track_name="
         + enc(title)
         + "&artist_name="
         + enc(artist)
         + (album != null && !album.isBlank() ? "&album_name=" + enc(album) : "")
         + (durationMs > 0L ? "&duration=" + Math.round(durationMs / 1000.0) : "");
      Http.Response r = Http.get(url, Map.of());
      if (r.code() == 200) {
         JsonObject o = JsonParser.parseString(r.body()).getAsJsonObject();
         synced = str(o, "syncedLyrics");
      }

      if (synced == null) {
         String clean = title.replaceAll("\\s*[-–]\\s.*$", "").replaceAll("\\s*[(\\[].*?[)\\]]", "").trim();
         Http.Response s = Http.get("https://lrclib.net/api/search?track_name=" + enc(clean) + "&artist_name=" + enc(artist), Map.of());
         if (s.code() == 200) {
            JsonArray arr = JsonParser.parseString(s.body()).getAsJsonArray();
            double best = Double.MAX_VALUE;

            for (JsonElement e : arr) {
               JsonObject o = e.getAsJsonObject();
               String sl = str(o, "syncedLyrics");
               if (sl != null) {
                  double d = durationMs <= 0L
                     ? 0.0
                     : (o.has("duration") && !o.get("duration").isJsonNull() ? Math.abs(o.get("duration").getAsDouble() - durationMs / 1000.0) : 30.0);
                  if (d < best && d < 8.0) {
                     best = d;
                     synced = sl;
                  }
               }
            }
         }
      }

      List<NowPlaying.Line> lines = synced == null ? null : parse(synced);
      if (lines != null && lines.isEmpty()) {
         lines = null;
      }

      synchronized (LyricsProvider.class) {
         CACHE.put(trackId, lines == null ? Collections.emptyList() : lines);
         if (CACHE.size() > 60) {
            CACHE.clear();
         }

         return lines;
      }
   }

   public static List<NowPlaying.Line> parse(String lrc) {
      List<NowPlaying.Line> out = new ArrayList<>();

      for (String raw : lrc.split("\\r?\\n")) {
         Matcher m = LRC.matcher(raw);
         List<Long> times = new ArrayList<>();

         int end;
         for (end = 0; m.find() && m.start() == end; end = m.end()) {
            long min = Long.parseLong(m.group(1));
            double sec = Double.parseDouble(m.group(2).replace(':', '.'));
            times.add(min * 60000L + Math.round(sec * 1000.0));
         }

         if (!times.isEmpty()) {
            String text = raw.substring(end).trim();

            for (long t : times) {
               out.add(text.indexOf(60) >= 0 ? enhancedLine(t, text) : line(t, text));
            }
         }
      }

      out.sort((a, b) -> Long.compare(a.timeMs(), b.timeMs()));
      return out;
   }

   static NowPlaying.Line enhancedLine(long t, String text) {
      Matcher m = WORD_TAG.matcher(text);
      List<String> words = new ArrayList<>();
      List<Long> times = new ArrayList<>();
      int last = -1;
      long lastT = t;

      while (m.find()) {
         if (last >= 0) {
            addWords(text.substring(last, m.start()), lastT, words, times);
         }

         lastT = Long.parseLong(m.group(1)) * 60000L + Math.round(Double.parseDouble(m.group(2).replace(':', '.')) * 1000.0);
         last = m.end();
      }

      if (last >= 0) {
         addWords(text.substring(last), lastT, words, times);
      }

      String clean = WORD_TAG.matcher(text).replaceAll("").replaceAll("\\s+", " ").trim();
      if (words.isEmpty()) {
         return line(t, clean);
      }

      long[] wt = new long[times.size()];

      for (int i = 0; i < wt.length; i++) {
         wt[i] = Math.max(t, times.get(i));
      }

      return new NowPlaying.Line(t, clean, words.toArray(new String[0]), wt);
   }

   private static void addWords(String chunk, long time, List<String> words, List<Long> times) {
      for (String w : chunk.trim().split("\\s+")) {
         if (!w.isBlank()) {
            words.add(w);
            times.add(time);
         }
      }
   }

   public static NowPlaying.Line line(long t, String text) {
      String[] words = text.isBlank() ? new String[0] : text.trim().split("\\s+");
      return new NowPlaying.Line(t, text, words);
   }

   private static String str(JsonObject o, String k) {
      if (o.has(k) && !o.get(k).isJsonNull()) {
         String v = o.get(k).getAsString();
         return v.isBlank() ? null : v;
      } else {
         return null;
      }
   }

   static String enc(String s) {
      return URLEncoder.encode(s == null ? "" : s, StandardCharsets.UTF_8);
   }

   public static List<NowPlaying.Line> demo() {
      String[] text = new String[]{
         "Neon lights across a frozen sky",
         "every block a little brighter tonight",
         "auction's open, put your money down",
         "highest bid takes the purple crown",
         "snow is falling on the spawner farm",
         "sixty four skellys safe from harm",
         "pay me once and I will pay you twice",
         "if you win the gamble, that's the price",
         "time is ticking, only ten to go",
         "sold to you in the falling snow"
      };
      List<NowPlaying.Line> l = new ArrayList<>();
      long t = 1500L;

      for (String s : text) {
         l.add(line(t, s));
         t += 3400L;
      }

      l.add(line(t, ""));
      return l;
   }
}
