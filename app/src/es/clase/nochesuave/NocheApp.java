package es.clase.nochesuave;

import android.app.Application;

public class NocheApp extends Application {
    @Override public void onCreate() {
        super.onCreate();
        if(!State.flag(this,"sessionDetectionV1")) {
            State.prefs(this).edit().putBoolean("sessionDetectionV1",true).putBoolean("advanced",true).putBoolean("global",false).putBoolean("audio",false).apply();
        }
        Thread.UncaughtExceptionHandler previous=Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread,error) -> {
            Diagnostics.failure(this,"Excepción no controlada · "+thread.getName(),error);
            if(previous!=null) previous.uncaughtException(thread,error);
            else { android.os.Process.killProcess(android.os.Process.myPid()); System.exit(10); }
        });
        Diagnostics.event(this,"Proceso iniciado · versión 0.2.4 · PID "+android.os.Process.myPid());
        Session.recoverAfterProcessDeath(this);
    }
}
