package es.clase.nochesuave;

import android.app.Service;
import android.content.*;
import android.content.pm.ServiceInfo;
import android.os.*;

/** Keeps the user-enabled engine in a foreground process, independent of its activity. */
public class LifetimeService extends Service {
    static LifetimeService instance;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final Runnable finishClose=() -> {
        if(State.flag(this,"sessionOpen")) return;
        Diagnostics.event(this,"Fin de sesión · se retira el servicio en primer plano");
        stopForeground(STOP_FOREGROUND_REMOVE); stopSelf();
    };
    static void ensure(Context c) {
        if(!State.flag(c,"sessionOpen") || !Diagnostics.permissionEnabled(c)) return;
        if(instance!=null) { instance.handler.removeCallbacks(instance.finishClose); return; }
        try { c.startForegroundService(new Intent(c,LifetimeService.class)); }
        catch(RuntimeException ex) { Diagnostics.failure(c,"Iniciar servicio en primer plano",ex); }
    }
    static void stop(Context c) { c.stopService(new Intent(c,LifetimeService.class)); }
    @Override public void onCreate() {
        super.onCreate();
        Notices.channel(this);
        // Promote immediately, before optional initialization or preference writes.
        if(Build.VERSION.SDK_INT>=34) startForeground(Notices.ACTIVE,Notices.build(this),ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        else startForeground(Notices.ACTIVE,Notices.build(this));
        instance=this;
        Diagnostics.event(this,"Servicio en primer plano iniciado");
    }
    @Override public int onStartCommand(Intent intent,int flags,int startId) {
        if(!State.flag(this,"sessionOpen") || !Diagnostics.permissionEnabled(this)) {
            Diagnostics.event(this,"Servicio en primer plano detenido: sesión cerrada o permiso retirado");
            stopForeground(STOP_FOREGROUND_REMOVE); stopSelf(); return START_NOT_STICKY;
        }
        Diagnostics.event(this,"Servicio en primer plano preparado · reinicio="+(intent==null));
        Notices.active(this);
        return START_STICKY;
    }
    @Override public void onTaskRemoved(Intent intent) {
        Diagnostics.event(this,"Interfaz retirada de recientes · apagar todos los efectos");
        Session.close(this);
        // Finish cleanup before dropping foreground priority. There is no periodic work.
        // This short grace is a compatibility measure for OEM recent-task cleaners,
        // not a guarantee against vendor force-stops.
        handler.removeCallbacks(finishClose); handler.postDelayed(finishClose,3000);
        super.onTaskRemoved(intent);
    }
    @Override public void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        Diagnostics.event(this,"Servicio en primer plano destruido");
        instance=null;
        super.onDestroy();
    }
    @Override public IBinder onBind(Intent intent) { return null; }
}
