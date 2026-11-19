package es.clase.nochesuave;

import android.app.PendingIntent;
import android.content.*;
import android.os.Build;
import android.service.quicksettings.*;

public class QuickTile extends TileService {
    static void refreshTile(Context c) { requestListeningState(c,new ComponentName(c,QuickTile.class)); }
    @Override public void onStartListening() { update(); }
    private void update() {
        Tile tile=getQsTile(); if(tile==null) return;
        tile.setState(State.active(this)?Tile.STATE_ACTIVE:Tile.STATE_INACTIVE); tile.updateTile();
    }
    @Override public void onClick() {
        super.onClick();
        if(ShadeService.instance==null || !State.flag(this,"sessionOpen")) {
            Intent intent=new Intent(this,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if(Build.VERSION.SDK_INT>=34) startActivityAndCollapse(PendingIntent.getActivity(this,7,intent,PendingIntent.FLAG_IMMUTABLE));
            else startActivityAndCollapse(intent);
            return;
        }
        State.both(this,!State.active(this)); update();
    }
}
