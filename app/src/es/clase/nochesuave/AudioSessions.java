package es.clase.nochesuave;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.audiofx.AudioEffect;

public class AudioSessions extends BroadcastReceiver {
    @Override public void onReceive(Context c, Intent intent) {
        ShadeService service=ShadeService.instance;
        if(service == null || service.audio == null) return;
        int session=intent.getIntExtra(AudioEffect.EXTRA_AUDIO_SESSION, -1);
        if(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION.equals(intent.getAction())) service.audio.open(session);
        else if(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION.equals(intent.getAction())) service.audio.close(session);
    }
}
