package es.clase.nochesuave;

import android.app.*;
import android.content.Context;
import android.os.Build;
import java.util.List;

final class Session {
    static void open(Context c) {
        if(!State.flag(c,"sessionOpen")) Diagnostics.event(c,"Sesión abierta desde la interfaz");
        State.prefs(c).edit().putBoolean("sessionOpen",true).putLong("sessionOpenedAt",System.currentTimeMillis()).apply();
    }
    static void close(Context c) {
        State.set(c,"sessionOpen",false);
        State.both(c,false);
        Scheduler.reschedule(c);
    }
    static void recoverAfterProcessDeath(Context c) {
        boolean clean=!State.flag(c,"sessionOpen");
        if(Build.VERSION.SDK_INT>=30) {
            try {
                List<ApplicationExitInfo> exits=c.getSystemService(ActivityManager.class).getHistoricalProcessExitReasons(c.getPackageName(),0,1);
                if(!exits.isEmpty()) {
                    ApplicationExitInfo last=exits.get(0);
                    String description=last.getDescription();
                    if(last.getTimestamp()>=State.prefs(c).getLong("sessionOpenedAt",0) && description!=null && description.contains("SwipeUpClean")) {
                        clean=true;
                        Diagnostics.event(c,"Cierre de recientes detectado tras reiniciar el proceso · descartar efectos anteriores");
                    }
                }
            } catch(RuntimeException ex) { Diagnostics.failure(c,"Consultar cierre anterior",ex); }
        }
        if(clean) {
            State.prefs(c).edit().putBoolean("sessionOpen",false).putBoolean("dim",false).putBoolean("audio",false).apply();
            Scheduler.cancelTimer(c);
        }
    }
}
