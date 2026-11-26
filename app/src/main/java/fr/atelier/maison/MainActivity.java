package fr.atelier.maison;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.webkit.*;
import android.widget.EditText;
import android.widget.LinearLayout;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends Activity {
    WebView web;
    SharedPreferences sp;
    boolean errShown = false;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        sp = getSharedPreferences("maison", MODE_PRIVATE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        web = new WebView(this);
        setContentView(web);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        CookieManager.getInstance().setAcceptCookie(true);
        web.setWebViewClient(new WebViewClient() {
            @Override public void onReceivedError(WebView v, WebResourceRequest r, WebResourceError e) {
                if (r.isForMainFrame() && !errShown) { errShown = true; showSettings("Connexion impossible. Vérifie les adresses :"); }
            }
        });
        web.setWebChromeClient(new WebChromeClient());
        if (sp.getString("local", "").isEmpty() && sp.getString("remote", "").isEmpty()) showSettings("Adresses de Home Assistant :");
        else start();
    }

    void start() {
        errShown = false;
        final String local = sp.getString("local", ""), remote = sp.getString("remote", "");
        new Thread(() -> {
            String target = remote.isEmpty() ? local : remote;
            if (!local.isEmpty() && reachable(local)) target = local;
            final String t = target;
            runOnUiThread(() -> web.loadUrl(t));
        }).start();
    }

    boolean reachable(String u) {
        try {
            HttpURLConnection c = (HttpURLConnection) new URL(u).openConnection();
            c.setConnectTimeout(1500); c.setReadTimeout(1500);
            c.getResponseCode(); c.disconnect(); return true;
        } catch (Exception e) { return false; }
    }

    void showSettings(String title) {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(48, 24, 48, 0);
        final EditText a = new EditText(this), r = new EditText(this);
        a.setHint("Adresse maison (ex: http://192.168.86.160:8123)");
        r.setHint("Adresse à distance (ex: http://100.x.x.x:8123)");
        a.setInputType(InputType.TYPE_TEXT_VARIATION_URI); r.setInputType(InputType.TYPE_TEXT_VARIATION_URI);
        a.setText(sp.getString("local", "http://192.168.86.160:8123"));
        r.setText(sp.getString("remote", ""));
        l.addView(a); l.addView(r);
        new AlertDialog.Builder(this).setTitle(title).setView(l).setCancelable(false)
            .setPositiveButton("Valider", (d, w) -> {
                sp.edit().putString("local", fix(a.getText().toString())).putString("remote", fix(r.getText().toString())).apply();
                start();
            }).show();
    }

    String fix(String u) {
        u = u.trim();
        if (u.isEmpty()) return u;
        if (!u.startsWith("http")) u = "http://" + u;
        return u;
    }

    @Override public boolean onKeyLongPress(int k, KeyEvent e) {
        if (k == KeyEvent.KEYCODE_VOLUME_UP) { showSettings("Réglages :"); return true; }
        return super.onKeyLongPress(k, e);
    }
    @Override public boolean onKeyDown(int k, KeyEvent e) {
        if (k == KeyEvent.KEYCODE_VOLUME_UP) { e.startTracking(); return super.onKeyDown(k, e); }
        if (k == KeyEvent.KEYCODE_BACK && web.canGoBack()) { web.goBack(); return true; }
        return super.onKeyDown(k, e);
    }
}
