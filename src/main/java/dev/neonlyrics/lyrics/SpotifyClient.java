package dev.neonlyrics.lyrics;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.neonlyrics.NeonLyrics;
import dev.neonlyrics.config.Config;
import dev.neonlyrics.config.Storage;
import dev.neonlyrics.hud.Cover;
import dev.neonlyrics.hud.Island;
import dev.neonlyrics.sky.SkyPreset;
import dev.neonlyrics.util.Fx;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import net.minecraft.util.Util;
import net.minecraft.client.MinecraftClient;

public final class SpotifyClient {
   public static final int PORT = 8789;
   public static final String REDIRECT = "http://127.0.0.1:8789/callback";
   public static final String DASHBOARD = "https://developer.spotify.com/dashboard";
   private static final String SCOPES = "user-read-currently-playing user-read-playback-state user-modify-playback-state";
   private static SpotifyClient.Tokens tokens = new SpotifyClient.Tokens();
   private static volatile SpotifyClient.Status status = SpotifyClient.Status.DISCONNECTED;
   private static volatile String lastError = "";
   private static volatile NowPlaying now = NowPlaying.EMPTY;
   private static final ArrayDeque<SpotifyClient.Played> HISTORY = new ArrayDeque<>();
   private static volatile long backoffUntil = 0L;
   private static String coverKey = "";
   private static ScheduledExecutorService exec;
   private static volatile ServerSocket loginServer;
   private static long demoStart = 0L;
   private static List<NowPlaying.Line> demoLines;

   private SpotifyClient() {
   }

   private static void remember(String title, String artist) {
      if (title != null && !title.isBlank()) {
         SpotifyClient.Played first = HISTORY.peekFirst();
         if (first == null || !first.title().equals(title) || !first.artist().equals(String.valueOf(artist))) {
            HISTORY.addFirst(new SpotifyClient.Played(title, String.valueOf(artist), System.currentTimeMillis()));

            while (HISTORY.size() > 12) {
               HISTORY.removeLast();
            }
         }
      }
   }

   private static void coverFallback(String title, String artist) {
      long songAt = System.currentTimeMillis();
      if (exec != null) {
         exec.schedule(() -> {
            try {
               if (Cover.loadedAt() >= songAt) {
                  return;
               }

               NowPlaying cur = now;
               if (cur.title == null || !cur.title.equals(title)) {
                  return;
               }

               String clean = title.replaceAll("\\s*[\\(\\[](feat|ft|with)[^\\)\\]]*[\\)\\]]", "").trim();
               String term = URLEncoder.encode(artist + " " + clean, StandardCharsets.UTF_8);
               Http.Response r = Http.get("https://itunes.apple.com/search?media=music&entity=song&limit=1&term=" + term, Map.of());
               if (r.code() != 200) {
                  return;
               }

               JsonObject o = JsonParser.parseString(r.body()).getAsJsonObject();
               JsonArray res = o.getAsJsonArray("results");
               if (res == null || res.isEmpty()) {
                  return;
               }

               JsonObject first = res.get(0).getAsJsonObject();
               if (!first.has("artworkUrl100")) {
                  return;
               }

               String url = first.get("artworkUrl100").getAsString().replace("100x100bb", "600x600bb");
               if (Cover.loadedAt() >= songAt) {
                  return;
               }

               Cover.load(Http.getBytes(url));
            } catch (Throwable var12) {
            }
         }, 2500L, TimeUnit.MILLISECONDS);
      }
   }

   public static List<SpotifyClient.Played> history() {
      return new ArrayList<>(HISTORY);
   }

   public static void init() {
      tokens = Storage.load("spotify.json", SpotifyClient.Tokens.class, SpotifyClient.Tokens::new);
      status = tokens.refresh != null && !tokens.refresh.isBlank() ? SpotifyClient.Status.CONNECTED : SpotifyClient.Status.DISCONNECTED;
      exec = Executors.newSingleThreadScheduledExecutor(r -> {
         Thread t = new Thread(r, "NeonLyrics-Spotify");
         t.setDaemon(true);
         return t;
      });
      exec.scheduleWithFixedDelay(SpotifyClient::poll, 2L, 2L, TimeUnit.SECONDS);
      Runtime.getRuntime().addShutdownHook(new Thread(LocalMedia::stop, "NeonLyrics-MediaStop"));
   }

   public static SpotifyClient.Status status() {
      return NeonLyrics.cfg().spotifyClientId.isBlank() && status != SpotifyClient.Status.CONNECTED ? SpotifyClient.Status.NOT_CONFIGURED : status;
   }

   public static String lastError() {
      return lastError;
   }

   public static NowPlaying now() {
      return NeonLyrics.cfg().demoLyrics ? demo() : now;
   }

   private static NowPlaying demo() {
      if (demoLines == null) {
         demoLines = LyricsProvider.demo();
      }

      long len = demoLines.get(demoLines.size() - 1).timeMs() + 1500L;
      if (demoStart == 0L) {
         demoStart = System.currentTimeMillis();
      }

      long p = (System.currentTimeMillis() - demoStart) % len;
      return new NowPlaying("demo", "Neon Nights (Demo)", "Lyrics Cord", len, true, p, System.currentTimeMillis(), demoLines, NowPlaying.LyricsState.SYNCED);
   }

   public static void restartDemo() {
      demoStart = System.currentTimeMillis();
   }

   public static void login() {
      String clientId = NeonLyrics.cfg().spotifyClientId.trim();
      if (clientId.isEmpty()) {
         Fx.error(Fx.t("neonlyrics.spotify.needid"));
      } else {
         closeLoginServer();

         try {
            byte[] rnd = new byte[48];
            new SecureRandom().nextBytes(rnd);
            String verifier = Base64.getUrlEncoder().withoutPadding().encodeToString(rnd);
            String challenge = Base64.getUrlEncoder()
               .withoutPadding()
               .encodeToString(MessageDigest.getInstance("SHA-256").digest(verifier.getBytes(StandardCharsets.US_ASCII)));
            byte[] st = new byte[12];
            new SecureRandom().nextBytes(st);
            String state = Base64.getUrlEncoder().withoutPadding().encodeToString(st);
            ServerSocket ss = new ServerSocket(8789, 4, InetAddress.getByName("127.0.0.1"));
            ss.setSoTimeout(180000);
            loginServer = ss;
            status = SpotifyClient.Status.WAITING_LOGIN;
            String url = "https://accounts.spotify.com/authorize?response_type=code&client_id="
               + LyricsProvider.enc(clientId)
               + "&scope="
               + LyricsProvider.enc("user-read-currently-playing user-read-playback-state user-modify-playback-state")
               + "&redirect_uri="
               + LyricsProvider.enc("http://127.0.0.1:8789/callback")
               + "&code_challenge_method=S256&code_challenge="
               + challenge
               + "&state="
               + state;
            Util.getOperatingSystem().open(url);
            Thread t = new Thread(() -> waitForCallback(ss, clientId, verifier, state), "NeonLyrics-SpotifyLogin");
            t.setDaemon(true);
            t.start();
         } catch (Exception var9) {
            fail(var9);
         }
      }
   }

   private static void waitForCallback(ServerSocket ss, String clientId, String verifier, String state) {
      try {
         ServerSocket e = ss;

         try {
            while (true) {
               try (Socket s = ss.accept()) {
                  s.setSoTimeout(5000);
                  BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
                  String first = in.readLine();
                  if (first != null && first.startsWith("GET /callback")) {
                     String path = first.split(" ")[1];
                     Map<String, String> q = query(path.contains("?") ? path.substring(path.indexOf(63) + 1) : "");
                     if (q.containsKey("error") || !state.equals(q.get("state")) || !q.containsKey("code")) {
                        respond(s, 400, page(false));
                        throw new IllegalStateException("Spotify: " + q.getOrDefault("error", "ungueltige Antwort"));
                     }

                     respond(s, 200, page(true));
                     exchange(clientId, verifier, q.get("code"));
                     break;
                  }

                  respond(s, 404, "Not found");
               }
            }
         } catch (Throwable var21) {
            if (ss != null) {
               try {
                  e.close();
               } catch (Throwable var18) {
                  var21.addSuppressed(var18);
               }
            }

            throw var21;
         }

         if (ss != null) {
            ss.close();
         }

         return;
      } catch (SocketTimeoutException var22) {
         status = SpotifyClient.Status.DISCONNECTED;
         return;
      } catch (Exception var23) {
         if (status == SpotifyClient.Status.WAITING_LOGIN) {
            fail(var23);
         }
      } finally {
         loginServer = null;
      }
   }

   private static void exchange(String clientId, String verifier, String code) throws Exception {
      String form = "grant_type=authorization_code&code="
         + LyricsProvider.enc(code)
         + "&redirect_uri="
         + LyricsProvider.enc("http://127.0.0.1:8789/callback")
         + "&client_id="
         + LyricsProvider.enc(clientId)
         + "&code_verifier="
         + LyricsProvider.enc(verifier);
      Http.Response r = Http.postForm("https://accounts.spotify.com/api/token", Map.of(), form);
      if (r.code() != 200) {
         throw new IllegalStateException("Token " + r.code() + ": " + r.body());
      }

      readTokens(r.body());
      status = SpotifyClient.Status.CONNECTED;
      lastError = "";
      MinecraftClient.getInstance().execute(() -> {
         Island.notify(Fx.t("neonlyrics.spotify.connected"), true);
         Fx.sound(Fx.Sfx.WIN);
      });
   }

   private static void readTokens(String json) {
      JsonObject o = JsonParser.parseString(json).getAsJsonObject();
      tokens.access = o.get("access_token").getAsString();
      if (o.has("refresh_token") && !o.get("refresh_token").isJsonNull()) {
         tokens.refresh = o.get("refresh_token").getAsString();
      }

      tokens.expiresAt = System.currentTimeMillis() + (o.has("expires_in") ? o.get("expires_in").getAsLong() : 3600L) * 1000L - 30000L;
      Storage.save("spotify.json", tokens);
   }

   private static boolean refresh() {
      String clientId = NeonLyrics.cfg().spotifyClientId.trim();
      if (tokens.refresh != null && !tokens.refresh.isBlank() && !clientId.isEmpty()) {
         try {
            String form = "grant_type=refresh_token&refresh_token=" + LyricsProvider.enc(tokens.refresh) + "&client_id=" + LyricsProvider.enc(clientId);
            Http.Response r = Http.postForm("https://accounts.spotify.com/api/token", Map.of(), form);
            if (r.code() != 200) {
               if (r.code() == 400) {
                  logout();
               }

               lastError = "Refresh " + r.code();
               return false;
            } else {
               readTokens(r.body());
               return true;
            }
         } catch (Exception var3) {
            lastError = String.valueOf(var3.getMessage());
            return false;
         }
      } else {
         return false;
      }
   }

   public static void logout() {
      tokens = new SpotifyClient.Tokens();
      Storage.save("spotify.json", tokens);
      now = NowPlaying.EMPTY;
      status = SpotifyClient.Status.DISCONNECTED;
   }

   private static void closeLoginServer() {
      ServerSocket s = loginServer;
      if (s != null) {
         try {
            s.close();
         } catch (Exception var2) {
         }
      }
   }

   private static void poll() {
      try {
         if (NeonLyrics.cfg().demoLyrics) {
            return;
         }

         if (autoMode()) {
            LocalMedia.ensureRunning();
            return;
         }

         LocalMedia.stop();
         if (status != SpotifyClient.Status.CONNECTED) {
            return;
         }

         if (System.currentTimeMillis() < backoffUntil) {
            return;
         }

         if (MinecraftClient.getInstance().world == null) {
            return;
         }

         if (System.currentTimeMillis() > tokens.expiresAt && !refresh()) {
            return;
         }

         Http.Response r = Http.get("https://api.spotify.com/v1/me/player/currently-playing", Map.of("Authorization", "Bearer " + tokens.access));
         if (r.code() == 401) {
            if (refresh()) {
               poll();
            }

            return;
         }

         if (r.code() == 429) {
            backoffUntil = System.currentTimeMillis() + 15000L;
            return;
         }

         if (r.code() == 204 || r.body().isBlank()) {
            now = NowPlaying.EMPTY;
            return;
         }

         if (r.code() != 200) {
            lastError = "Spotify " + r.code();
            return;
         }

         JsonObject o = JsonParser.parseString(r.body()).getAsJsonObject();
         if (!o.has("item") || o.get("item").isJsonNull()) {
            now = NowPlaying.EMPTY;
            return;
         }

         JsonObject item = o.getAsJsonObject("item");
         String id = item.has("id") && !item.get("id").isJsonNull() ? item.get("id").getAsString() : item.get("name").getAsString();
         String title = item.get("name").getAsString();
         StringBuilder artists = new StringBuilder();
         if (item.has("artists")) {
            JsonArray arr = item.getAsJsonArray("artists");

            for (int i = 0; i < arr.size(); i++) {
               if (i > 0) {
                  artists.append(", ");
               }

               artists.append(arr.get(i).getAsJsonObject().get("name").getAsString());
            }
         }

         String firstArtist = artists.toString().split(", ")[0];
         String album = item.has("album") ? item.getAsJsonObject("album").get("name").getAsString() : "";
         long duration = item.has("duration_ms") ? item.get("duration_ms").getAsLong() : 0L;
         long progress = o.has("progress_ms") && !o.get("progress_ms").isJsonNull() ? o.get("progress_ms").getAsLong() : 0L;
         boolean playing = o.has("is_playing") && o.get("is_playing").getAsBoolean();
         publish(id, title, artists.toString(), firstArtist, album, duration, progress, playing);
         if (!id.equals(coverKey)) {
            coverKey = id;
            String url = null;
            if (item.has("album") && item.getAsJsonObject("album").has("images")) {
               JsonArray imgs = item.getAsJsonObject("album").getAsJsonArray("images");
               if (!imgs.isEmpty()) {
                  url = imgs.get(imgs.size() - 1).getAsJsonObject().get("url").getAsString();
               }
            }

            if (url == null) {
               Cover.clear();
            } else {
               String u = url;
               exec.execute(() -> {
                  try {
                     Cover.load(Http.getBytes(u));
                  } catch (Exception var2x) {
                  }
               });
            }
         }
      } catch (Exception var15) {
         lastError = String.valueOf(var15.getMessage());
      }
   }

   static void publish(String id, String title, String artists, String firstArtist, String album, long duration, long progress, boolean playing) {
      NowPlaying prev = now;
      boolean changed = !id.equals(prev.trackId);
      if (!changed && playing && prev.playing) {
         long predicted = prev.progressNow(0);
         long diff = progress - predicted;
         if (Math.abs(diff) < 1500L) {
            progress = predicted + diff / 4L;
         }
      }

      List<NowPlaying.Line> lines = changed ? LyricsProvider.cached(id) : prev.lines;
      NowPlaying.LyricsState ls = changed
         ? (lines == null ? NowPlaying.LyricsState.LOADING : (lines.isEmpty() ? NowPlaying.LyricsState.NOT_FOUND : NowPlaying.LyricsState.SYNCED))
         : prev.lyricsState;
      now = new NowPlaying(id, title, artists, duration, playing, progress, System.currentTimeMillis(), lines, ls);
      if (changed) {
         coverFallback(title, firstArtist);
         MinecraftClient.getInstance().execute(() -> {
            Island.notify("♪ " + title, true);
            remember(title, artists);
            Config cf = NeonLyrics.cfg();
            if (cf.skyShuffle && cf.sky >= 0) {
               SkyPreset.shuffle();
            }
         });
         if (ls == NowPlaying.LyricsState.LOADING) {
            exec.execute(() -> {
               try {
                  List<NowPlaying.Line> fetched = LyricsProvider.fetch(id, title, firstArtist, album, duration);
                  NowPlaying cur = now;
                  if (id.equals(cur.trackId)) {
                     now = cur.withLyrics(fetched, fetched == null ? NowPlaying.LyricsState.NOT_FOUND : NowPlaying.LyricsState.SYNCED);
                  }
               } catch (Exception var8) {
                  NowPlaying curx = now;
                  if (id.equals(curx.trackId)) {
                     now = curx.withLyrics(null, NowPlaying.LyricsState.ERROR);
                  }
               }
            });
         }
      }
   }

   static void publishNothing() {
      now = NowPlaying.EMPTY;
   }

   public static void syncHere() {
      NowPlaying np = now();
      Config c = NeonLyrics.cfg();
      if (np.hasTrack() && !np.lines.isEmpty()) {
         long raw = np.progressNow(0);
         long cur = raw + c.lyricsOffsetMs;
         long best = Long.MAX_VALUE;
         long bestT = 0L;

         for (NowPlaying.Line l : np.lines) {
            if (!l.text().isBlank()) {
               long d = Math.abs(l.timeMs() - cur);
               if (d < best) {
                  best = d;
                  bestT = l.timeMs();
               }
            }
         }

         c.lyricsOffsetMs = (int)Math.max(-5000L, Math.min(5000L, bestT - raw));
         NeonLyrics.saveConfig();
         Island.notify(Fx.t("neonlyrics.media.synced", (c.lyricsOffsetMs > 0 ? "+" : "") + c.lyricsOffsetMs), true);
      } else {
         Island.notify(Fx.t("neonlyrics.media.nolyrics"), false);
      }
   }

   public static void control(SpotifyClient.Cmd cmd) {
      if (NeonLyrics.cfg().demoLyrics) {
         restartDemo();
      } else {
         String label = switch (cmd) {
            case NEXT -> Fx.t("neonlyrics.media.next");
            case PREVIOUS -> Fx.t("neonlyrics.media.prev");
            case PLAYPAUSE -> now.playing ? Fx.t("neonlyrics.media.pause") : Fx.t("neonlyrics.media.play");
         };
         Island.notify(label, true);
         if (autoMode()) {
            LocalMedia.command(cmd.name().toLowerCase(Locale.ROOT));
         } else {
            exec.execute(() -> {
               try {
                  if (status != SpotifyClient.Status.CONNECTED) {
                     return;
                  }

                  if (System.currentTimeMillis() > tokens.expiresAt && !refresh()) {
                     return;
                  }

                  Map<String, String> h = Map.of("Authorization", "Bearer " + tokens.access);

                  Http.Response r = switch (cmd) {
                     case NEXT -> Http.empty("POST", "https://api.spotify.com/v1/me/player/next", h);
                     case PREVIOUS -> Http.empty("POST", "https://api.spotify.com/v1/me/player/previous", h);
                     case PLAYPAUSE -> Http.empty("PUT", "https://api.spotify.com/v1/me/player/" + (now.playing ? "pause" : "play"), h);
                  };
                  if (r.code() == 401 || r.code() == 403) {
                     MinecraftClient.getInstance().execute(() -> Island.notify(Fx.t("neonlyrics.media.noperm"), false));
                  }

                  backoffUntil = 0L;
                  Thread.sleep(400L);
                  poll();
               } catch (Exception var3) {
                  lastError = String.valueOf(var3.getMessage());
               }
            });
         }
      }
   }

   public static boolean autoMode() {
      return NeonLyrics.cfg().spotifyMode == 0;
   }

   public static void setMode(int mode) {
      if (NeonLyrics.cfg().spotifyMode != mode) {
         NeonLyrics.cfg().spotifyMode = mode;
         NeonLyrics.saveConfig();
         now = NowPlaying.EMPTY;
         if (mode != 0) {
            LocalMedia.stop();
         }
      }
   }

   private static void fail(Exception e) {
      status = SpotifyClient.Status.ERROR;
      lastError = String.valueOf(e.getMessage());
      NeonLyrics.LOG.warn("Spotify-Login fehlgeschlagen", e);
      MinecraftClient.getInstance().execute(() -> Fx.error(Fx.t("neonlyrics.spotify.failed", lastError)));
   }

   private static Map<String, String> query(String q) {
      Map<String, String> m = new HashMap<>();

      for (String p : q.split("&")) {
         int i = p.indexOf(61);
         if (i > 0) {
            m.put(URLDecoder.decode(p.substring(0, i), StandardCharsets.UTF_8), URLDecoder.decode(p.substring(i + 1), StandardCharsets.UTF_8));
         }
      }

      return m;
   }

   private static void respond(Socket s, int code, String html) throws Exception {
      byte[] b = html.getBytes(StandardCharsets.UTF_8);
      OutputStream out = s.getOutputStream();
      out.write(
         ("HTTP/1.1 " + code + " OK\r\nContent-Type: text/html; charset=utf-8\r\nContent-Length: " + b.length + "\r\nConnection: close\r\n\r\n")
            .getBytes(StandardCharsets.US_ASCII)
      );
      out.write(b);
      out.flush();
   }

   private static String page(boolean ok) {
      return "<!doctype html><meta charset=utf-8><title>Lyrics Cord</title><body style=\"margin:0;height:100vh;display:grid;place-items:center;background:#0f0c17;color:#f3efff;font-family:system-ui,sans-serif\"><div style=\"text-align:center\"><div style=\"font-size:20px;font-weight:600\">"
         + (ok ? "Spotify ist verbunden" : "Verbindung fehlgeschlagen")
         + "</div><div style=\"margin-top:8px;color:#9d93b8\">"
         + (ok ? "Du kannst dieses Fenster schliessen und zurueck ins Spiel." : "Versuche es im Spiel noch einmal.")
         + "</div></div></body>";
   }

   public enum Cmd {
      NEXT,
      PREVIOUS,
      PLAYPAUSE;

      Cmd() {
      }
   }

   public record Played(String title, String artist, long at) {
      public Played {
      }
   }

   public enum Status {
      NOT_CONFIGURED,
      DISCONNECTED,
      WAITING_LOGIN,
      CONNECTED,
      ERROR;

      Status() {
      }
   }

   private static final class Tokens {
      String access = "";
      String refresh = "";
      long expiresAt;

      private Tokens() {
      }
   }
}
