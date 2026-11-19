package es.clase.nochesuave;

import android.content.*;

public class ScheduleReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c,Intent i) {
        String action=i.getAction();
        if(Intent.ACTION_BOOT_COMPLETED.equals(action)) Session.close(c);
        long deadline=State.prefs(c).getLong("timer",0);
        if("timer".equals(action) && deadline>0 && System.currentTimeMillis()>=deadline) {
            State.both(c,false); Notices.active(c);
        }
        Scheduler.reschedule(c); State.refresh();
    }
}
