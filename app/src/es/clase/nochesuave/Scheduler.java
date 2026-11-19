package es.clase.nochesuave;

import android.app.*;
import android.content.*;
import android.os.Build;

final class Scheduler {
    static PendingIntent pending(Context c,String action,int id) {
        return PendingIntent.getBroadcast(c,id,new Intent(c,ScheduleReceiver.class).setAction(action),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
    }
    static boolean exact(Context c) { return Build.VERSION.SDK_INT<31 || c.getSystemService(AlarmManager.class).canScheduleExactAlarms(); }
    private static void alarm(Context c,String action,int id,long at) {
        AlarmManager manager=c.getSystemService(AlarmManager.class);
        if(at<=0) { manager.cancel(pending(c,action,id)); return; }
        if(exact(c)) {
            try { manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,at,pending(c,action,id)); return; }
            catch(SecurityException ignored) {}
        }
        manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,at,pending(c,action,id));
    }
    static void reschedule(Context c) {
        // Retire alarms created by older versions; removing the UI alone is insufficient.
        alarm(c,"scheduled_on",20,-1); alarm(c,"scheduled_off",21,-1);
        if(State.prefs(c).contains("schedule") || State.prefs(c).contains("days")) {
            State.prefs(c).edit().remove("schedule").remove("days").remove("start").remove("stop").remove("nextOn").remove("nextOff").apply();
        }
        long now=System.currentTimeMillis();
        long timer=State.prefs(c).getLong("timer",0);
        if(timer>0 && timer<=now) {
            State.prefs(c).edit().putBoolean("dim",false).putBoolean("audio",false).putLong("timer",0).apply();
            alarm(c,"timer",22,-1);
        } else if(timer>now) alarm(c,"timer",22,timer);
    }
    static void timer(Context c,int minutes) {
        if(minutes<1 || minutes>1440) throw new IllegalArgumentException("Minutos fuera de rango");
        long at=System.currentTimeMillis()+minutes*60_000L;
        State.prefs(c).edit().putLong("timer",at).apply(); alarm(c,"timer",22,at);
    }
    static void cancelTimer(Context c) {
        State.prefs(c).edit().putLong("timer",0).apply();
        c.getSystemService(AlarmManager.class).cancel(pending(c,"timer",22));
    }
}
