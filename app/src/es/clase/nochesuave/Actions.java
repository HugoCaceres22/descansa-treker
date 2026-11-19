package es.clase.nochesuave;
import android.content.*;
public class Actions extends BroadcastReceiver {
    @Override public void onReceive(Context c,Intent i) {
        if("off".equals(i.getAction())) State.both(c,false);
        else if("on".equals(i.getAction())) {
            if(ShadeService.instance!=null && State.flag(c,"sessionOpen")) State.both(c,true);
            else Notices.unavailable(c);
        }
        else if("panel".equals(i.getAction()) && ShadeService.instance!=null) ShadeService.instance.openPanelFromNotification();
        Notices.active(c);
    }
}
