package es.clase.nochesuave;

import android.content.Context;
import android.content.SharedPreferences;

final class State {
    static SharedPreferences prefs(Context c) { return c.getSharedPreferences("noche", Context.MODE_PRIVATE); }
    static boolean flag(Context c, String key) { return prefs(c).getBoolean(key, false); }
    static int level(Context c) { return prefs(c).getInt("light", 55); }
    static float gain(Context c) { return prefs(c).getInt("gain", -20) / 2f; }
    static void set(Context c, String key, boolean value) { prefs(c).edit().putBoolean(key, value).apply(); }
    static void number(Context c, String key, int value) { prefs(c).edit().putInt(key, value).apply(); }
    static boolean active(Context c) { return flag(c,"dim") || flag(c,"audio"); }
    static void both(Context c, boolean on) {
        Diagnostics.event(c,on?"Activar ambos solicitado":"Apagar ambos solicitado");
        prefs(c).edit().putBoolean("dim", on).putBoolean("audio", on).apply();
        if (!on) Scheduler.cancelTimer(c);
        refresh();
        Diagnostics.event(c,"Cambio de ambos terminado · conectado="+(ShadeService.instance!=null));
    }
    static void refresh() { if (ShadeService.instance != null) ShadeService.instance.refresh(); }
}
