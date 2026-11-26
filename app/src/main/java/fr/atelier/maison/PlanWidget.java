package fr.atelier.maison;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.View;
import android.widget.RemoteViews;
import com.caverock.androidsvg.SVG;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;

/** Widget : plan 3D du rez-de-chaussée avec halos de la vraie couleur des ampoules + boutons par pièce. */
public class PlanWidget extends AppWidgetProvider {
    static final String ACT_TOGGLE = "fr.atelier.maison.TOGGLE";
    static final String ACT_REFRESH = "fr.atelier.maison.REFRESH";

    static final int[] CHIPS = {
        R.id.c0, R.id.c1, R.id.c2, R.id.c3, R.id.c4, R.id.c5, R.id.c6, R.id.c7
    };

    @Override
    public void onUpdate(final Context c, AppWidgetManager m, int[] ids) {
        final PendingResult pr = goAsync();
        new Thread(new Runnable() {
            @Override public void run() {
                try { update(c); } catch (Throwable ignored) { }
                pr.finish();
            }
        }).start();
    }

    @Override
    public void onReceive(final Context c, Intent i) {
        String a = i.getAction();
        if (ACT_TOGGLE.equals(a)) {
            final int idx = i.getIntExtra("idx", -1);
            final PendingResult pr = goAsync();
            new Thread(new Runnable() {
                @Override public void run() {
                    try { toggle(c, idx); } catch (Throwable ignored) { }
                    try { Thread.sleep(1200); } catch (Exception ignored) { }
                    try { update(c); } catch (Throwable ignored) { }
                    pr.finish();
                }
            }).start();
        } else if (ACT_REFRESH.equals(a)) {
            final PendingResult pr = goAsync();
            new Thread(new Runnable() {
                @Override public void run() {
                    try { update(c); } catch (Throwable ignored) { }
                    pr.finish();
                }
            }).start();
        } else {
            super.onReceive(c, i);
        }
    }

    static void toggle(Context c, int idx) throws Exception {
        if (idx < 0 || idx >= Room.ALL.length) return;
        Room r = Room.ALL[idx];
        String token = Ha.token(c);
        if (token.isEmpty()) return;
        StringBuilder ids = new StringBuilder();
        for (int k = 0; k < r.targets.length; k++) {
            if (k > 0) ids.append(',');
            ids.append('"').append(r.targets[k]).append('"');
        }
        String body = "{\"entity_id\":[" + ids + "]}";
        Ha.post(Ha.base(c) + "/api/services/" + r.service, token, body);
    }

    /** Recalcule et pousse l'affichage sur tous les widgets posés. */
    static void update(Context c) {
        AppWidgetManager m = AppWidgetManager.getInstance(c);
        int[] ids = m.getAppWidgetIds(new ComponentName(c, PlanWidget.class));
        if (ids.length == 0) return;
        m.updateAppWidget(ids, build(c));
    }

    static RemoteViews build(Context c) {
        RemoteViews rv = new RemoteViews(c.getPackageName(), R.layout.widget_plan);
        // boutons fixes
        rv.setOnClickPendingIntent(R.id.r, broadcast(c, ACT_REFRESH, 100, -1));
        rv.setOnClickPendingIntent(R.id.g, activity(c, 101, null, true));
        rv.setOnClickPendingIntent(R.id.plan, activity(c, 102, "/dashboard-home/lumiere", false));
        for (int k = 0; k < Room.ALL.length && k < CHIPS.length; k++) {
            rv.setTextViewText(CHIPS[k], Room.ALL[k].label);
            rv.setOnClickPendingIntent(CHIPS[k], broadcast(c, ACT_TOGGLE, k, k));
        }

        String token = Ha.token(c);
        if (token.isEmpty()) {
            rv.setViewVisibility(R.id.msg, View.VISIBLE);
            rv.setTextViewText(R.id.msg, "Touche ⚙ puis saisis le jeton d'accès Home Assistant");
            return rv;
        }
        try {
            String base = Ha.base(c);
            Map<String, JSONObject> st = states(base, token);
            Bitmap bmp = render(c, base, st);
            rv.setImageViewBitmap(R.id.plan, bmp);
            rv.setViewVisibility(R.id.msg, View.GONE);
            for (int k = 0; k < Room.ALL.length && k < CHIPS.length; k++) {
                Room r = Room.ALL[k];
                boolean on = isOn(st, r.stateEnt);
                int[] rgb = colorOf(st, r.colorEnt);
                int bg = on ? Color.rgb(rgb[0], rgb[1], rgb[2]) : 0xFF1C2A3D;
                double lum = 0.299 * rgb[0] + 0.587 * rgb[1] + 0.114 * rgb[2];
                int fg = on ? (lum > 150 ? 0xFF000000 : 0xFFFFFFFF) : 0xFF9FE8FF;
                rv.setInt(CHIPS[k], "setBackgroundColor", bg);
                rv.setTextColor(CHIPS[k], fg);
            }
        } catch (Throwable t) {
            rv.setViewVisibility(R.id.msg, View.VISIBLE);
            rv.setTextViewText(R.id.msg, "Connexion impossible (" + t.getClass().getSimpleName() + "). Touche ⟳");
        }
        return rv;
    }

    static PendingIntent broadcast(Context c, String action, int code, int idx) {
        Intent i = new Intent(c, PlanWidget.class).setAction(action);
        i.putExtra("idx", idx);
        return PendingIntent.getBroadcast(c, code, i,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    static PendingIntent activity(Context c, int code, String path, boolean settings) {
        Intent i = new Intent(c, MainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        if (path != null) i.putExtra("path", path);
        if (settings) i.putExtra("settings", true);
        return PendingIntent.getActivity(c, code, i,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    // ---------- états Home Assistant ----------

    static Map<String, JSONObject> states(String base, String token) throws Exception {
        JSONArray arr = new JSONArray(Ha.get(base + "/api/states", token));
        Map<String, JSONObject> m = new HashMap<String, JSONObject>();
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.getJSONObject(i);
            m.put(o.getString("entity_id"), o);
        }
        return m;
    }

    static boolean isOn(Map<String, JSONObject> st, String ent) {
        JSONObject o = st.get(ent);
        return o != null && "on".equals(o.optString("state"));
    }

    static int[] colorOf(Map<String, JSONObject> st, String ent) {
        JSONObject o = st.get(ent);
        if (o != null) {
            JSONObject at = o.optJSONObject("attributes");
            if (at != null) {
                JSONArray a = at.optJSONArray("rgb_color");
                if (a != null && a.length() >= 3) {
                    return new int[] {a.optInt(0), a.optInt(1), a.optInt(2)};
                }
            }
        }
        return new int[] {255, 190, 120}; // blanc chaud par défaut
    }

    static int brightness(Map<String, JSONObject> st, String ent) {
        JSONObject o = st.get(ent);
        if (o != null) {
            JSONObject at = o.optJSONObject("attributes");
            if (at != null && at.has("brightness") && !at.isNull("brightness")) {
                return at.optInt("brightness", 255);
            }
        }
        return 255;
    }

    // ---------- dessin du plan ----------

    static SVG loadSvg(Context c, String base, String name) throws Exception {
        File f = new File(c.getFilesDir(), "plan_" + name.replace('?', '_').replace('=', '_'));
        if (!f.exists() || f.length() == 0) {
            byte[] data = Ha.getBytes(base + "/local/plan3d/" + name);
            FileOutputStream o = new FileOutputStream(f);
            o.write(data);
            o.close();
        }
        FileInputStream in = new FileInputStream(f);
        try {
            return SVG.getFromInputStream(in);
        } finally {
            in.close();
        }
    }

    static Bitmap render(Context c, String base, Map<String, JSONObject> st) throws Exception {
        SVG baseSvg = loadSvg(c, base, Room.BASE);
        float ar = 2.2f;
        RectF vb = baseSvg.getDocumentViewBox();
        if (vb != null && vb.height() > 0) ar = vb.width() / vb.height();
        else if (baseSvg.getDocumentAspectRatio() > 0) ar = baseSvg.getDocumentAspectRatio();
        int w = 1000;
        int h = Math.min(800, Math.max(200, (int) (w / ar)));
        Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas cv = new Canvas(bmp);
        cv.drawColor(0xFF0B1622);
        RectF dst = new RectF(0, 0, w, h);
        baseSvg.renderToCanvas(cv, dst);

        // pièces éteintes : calque sombre
        for (Room r : Room.ALL) {
            if (r.overlay == null || isOn(st, r.stateEnt)) continue;
            try {
                loadSvg(c, base, r.overlay).renderToCanvas(cv, dst);
            } catch (Throwable ignored) { }
        }
        // pièces allumées : halo de la couleur réelle
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SCREEN));
        for (Room r : Room.ALL) {
            if (r.overlay == null || !isOn(st, r.stateEnt)) continue;
            int[] rgb = colorOf(st, r.colorEnt);
            float b = brightness(st, r.colorEnt) / 255f;
            int a1 = (int) (255 * (0.35f + 0.5f * b));
            int a2 = (int) (a1 * 0.45f);
            float cx = r.cx / 100f * w;
            float cy = r.cy / 100f * h;
            float rx = Math.max(r.w / 100f * w * 0.75f, 50f);
            float ry = Math.max(r.h / 100f * h * 0.75f, 50f);
            cv.save();
            cv.translate(cx, cy);
            cv.scale(1f, ry / rx);
            p.setShader(new RadialGradient(0, 0, rx,
                new int[] {
                    Color.argb(a1, rgb[0], rgb[1], rgb[2]),
                    Color.argb(a2, rgb[0], rgb[1], rgb[2]),
                    Color.argb(0, rgb[0], rgb[1], rgb[2])},
                new float[] {0f, 0.5f, 1f}, Shader.TileMode.CLAMP));
            cv.drawCircle(0, 0, rx, p);
            cv.restore();
        }
        return bmp;
    }
}
