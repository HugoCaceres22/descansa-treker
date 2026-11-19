package es.clase.nochesuave;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.media.audiofx.DynamicsProcessing;

/** On-device configuration check. The AudioTrack is never played or written to. */
final class AudioProbe {
    static String run() {
        StringBuilder report=new StringBuilder("Compatibilidad de audio 0.2.4\n");
        AudioTrack track=null;
        try {
            track=new AudioTrack.Builder()
                .setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                .setAudioFormat(new AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(48000).setChannelMask(AudioFormat.CHANNEL_OUT_STEREO).build())
                .setBufferSizeInBytes(16384).setTransferMode(AudioTrack.MODE_STREAM).build();
            if(track.getState()!=AudioTrack.STATE_INITIALIZED) throw new IllegalStateException("No se inicializa la pista de prueba");
            int session=track.getAudioSessionId();
            if(session<=0) throw new IllegalStateException("No se asigna una sesión propia");
            report.append("Sesión propia ").append(session).append(" (sin reproducir sonido)\n");
            check(report,"Propia / mínima",session,false);
            check(report,"Propia / estándar neutra",session,true);
        } catch(RuntimeException ex) { report.append("Preparar sesión propia: ").append(ex).append('\n'); }
        finally { if(track!=null) track.release(); }
        check(report,"Global / mínima",0,false);
        check(report,"Global / estándar neutra",0,true);
        report.append("Estas pruebas configuran y leen el motor. No demuestran atenuación acústica en YouTube.");
        return report.toString();
    }
    private static void check(StringBuilder report,String name,int session,boolean standard) {
        DynamicsProcessing effect=null;
        String stage="construcción";
        try {
            effect=new DynamicsProcessing(0,session,standard?AudioConfiguration.standard(-20):AudioConfiguration.minimal(-20));
            // Do not enable the probe effect on the global output mix.
            stage="escribir y leer ganancia";
            effect.setInputGainAllChannelsTo(-20);
            float actual=effect.getInputGainByChannelIndex(0);
            if(!Float.isFinite(actual) || Math.abs(actual+20)>.25f) throw new IllegalStateException("Lectura inesperada: "+actual+" dB");
            report.append(name).append(": OK, lectura ").append(actual).append(" dB\n");
        } catch(RuntimeException ex) { report.append(name).append(": ERROR en ").append(stage).append(" · ").append(ex).append('\n'); }
        finally { if(effect!=null) { try { effect.release(); } catch(RuntimeException ex) { report.append("Liberar prueba: ").append(ex).append('\n'); } } }
    }
}
