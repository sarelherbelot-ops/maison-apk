package fr.atelier.maison;

import android.content.Context;
import android.content.SharedPreferences;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/** Petit client HTTP pour l'API REST de Home Assistant. */
final class Ha {
    private Ha() {}

    static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences("maison", Context.MODE_PRIVATE);
    }

    static String token(Context c) {
        return prefs(c).getString("token", "").trim();
    }

    /** Adresse maison si joignable, sinon adresse à distance. */
    static String base(Context c) {
        SharedPreferences sp = prefs(c);
        String local = sp.getString("local", "");
        String remote = sp.getString("remote", "");
        if (!local.isEmpty() && reachable(local)) return strip(local);
        if (!remote.isEmpty()) return strip(remote);
        return strip(local);
    }

    private static String strip(String u) {
        while (u.endsWith("/")) u = u.substring(0, u.length() - 1);
        return u;
    }

    static boolean reachable(String u) {
        try {
            HttpURLConnection c = (HttpURLConnection) new URL(u).openConnection();
            c.setConnectTimeout(1500);
            c.setReadTimeout(1500);
            c.getResponseCode();
            c.disconnect();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    static String get(String url, String token) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(6000);
        c.setReadTimeout(10000);
        if (token != null && !token.isEmpty()) c.setRequestProperty("Authorization", "Bearer " + token);
        int code = c.getResponseCode();
        if (code != 200) throw new Exception("HTTP " + code);
        InputStream in = c.getInputStream();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        in.close();
        c.disconnect();
        return out.toString("UTF-8");
    }

    static byte[] getBytes(String url) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(6000);
        c.setReadTimeout(15000);
        if (c.getResponseCode() != 200) throw new Exception("HTTP " + c.getResponseCode());
        InputStream in = c.getInputStream();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        in.close();
        c.disconnect();
        return out.toByteArray();
    }

    static void post(String url, String token, String json) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(6000);
        c.setReadTimeout(10000);
        c.setRequestMethod("POST");
        c.setDoOutput(true);
        c.setRequestProperty("Authorization", "Bearer " + token);
        c.setRequestProperty("Content-Type", "application/json");
        OutputStream o = c.getOutputStream();
        o.write(json.getBytes("UTF-8"));
        o.close();
        int code = c.getResponseCode();
        c.disconnect();
        if (code >= 300) throw new Exception("HTTP " + code);
    }
}
