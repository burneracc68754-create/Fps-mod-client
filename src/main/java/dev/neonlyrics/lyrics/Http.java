package dev.neonlyrics.lyrics;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Map.Entry;

final class Http {
   static final String UA = "NeonLyricsHUD/1.1 (Minecraft Fabric mod)";

   private Http() {
   }

   static Http.Response get(String url, Map<String, String> headers) throws Exception {
      return send("GET", url, headers, null, null);
   }

   static Http.Response postForm(String url, Map<String, String> headers, String form) throws Exception {
      return send("POST", url, headers, form, "application/x-www-form-urlencoded");
   }

   static byte[] getBytes(String url) throws Exception {
      HttpURLConnection c = (HttpURLConnection)URI.create(url).toURL().openConnection();
      c.setConnectTimeout(6000);
      c.setReadTimeout(8000);
      c.setRequestProperty("User-Agent", "NeonLyricsHUD/1.1 (Minecraft Fabric mod)");

      try (InputStream in = c.getInputStream()) {
         return in.readAllBytes();
      } finally {
         c.disconnect();
      }
   }

   static Http.Response empty(String method, String url, Map<String, String> headers) throws Exception {
      return send(method, url, headers, "", "application/json");
   }

   private static Http.Response send(String method, String url, Map<String, String> headers, String body, String contentType) throws Exception {
      HttpURLConnection c = (HttpURLConnection)URI.create(url).toURL().openConnection();
      c.setRequestMethod(method);
      c.setConnectTimeout(6000);
      c.setReadTimeout(8000);
      c.setRequestProperty("User-Agent", "NeonLyricsHUD/1.1 (Minecraft Fabric mod)");
      c.setRequestProperty("Accept", "application/json");

      for (Entry<String, String> h : headers.entrySet()) {
         c.setRequestProperty(h.getKey(), h.getValue());
      }

      if (body != null) {
         c.setDoOutput(true);
         c.setRequestProperty("Content-Type", contentType);

         try (OutputStream os = c.getOutputStream()) {
            os.write(body.getBytes(StandardCharsets.UTF_8));
         }
      }

      int code = c.getResponseCode();
      InputStream in = code >= 400 ? c.getErrorStream() : c.getInputStream();
      String text = "";
      if (in != null) {
         InputStream var9 = in;

         try {
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            in.transferTo(bo);
            text = bo.toString(StandardCharsets.UTF_8);
         } catch (Throwable var14) {
            if (in != null) {
               try {
                  var9.close();
               } catch (Throwable var12) {
                  var14.addSuppressed(var12);
               }
            }

            throw var14;
         }

         if (in != null) {
            in.close();
         }
      }

      c.disconnect();
      return new Http.Response(code, text);
   }

   record Response(int code, String body) {
      Response {
      }
   }
}
