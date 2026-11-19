package es.clase.nochesuave;

import android.content.Context;
import android.media.audiofx.AudioEffect;
import android.media.audiofx.DynamicsProcessing;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

final class AudioEngine {
    private final Context context;
    private final Map<Integer, DynamicsProcessing> effects = new HashMap<>();
    private final Set<Integer> sessions = new HashSet<>();
    private final Set<Integer> detected = new HashSet<>();
    private final AudioSessionDetector detector;
    private boolean enabled, global;
    private float gain;
    private String error = "";
    private boolean attemptedGlobal;
    private float confirmedGain=Float.NaN;
    private boolean probing;
    AudioEngine(Context context) { this.context = context; detector=new AudioSessionDetector(context,this::replaceDetected); }
    void open(int session) { if(session > 0 && (sessions.contains(session) || sessions.size()<16)) { sessions.add(session); if(enabled && !global && !probing) attach(session); } }
    void close(int session) { sessions.remove(session); if(!detected.contains(session)) release(session); }
    private void replaceDetected(Set<Integer> found) {
        for(int old:new HashSet<>(detected)) if(!found.contains(old) && !sessions.contains(old)) release(old);
        detected.clear(); detected.addAll(found); update();
    }
    void update() {
        if(probing) return;
        boolean want = State.flag(context,"audio");
        boolean mode = State.flag(context,"global");
        if (want != enabled || mode != global) { releaseAll(); error=""; attemptedGlobal=false; }
        enabled=want; global=mode; gain=State.gain(context);
        detector.sync(enabled && !global && State.flag(context,"advanced") && State.flag(context,"sessionOpen"));
        if(!enabled) { detected.clear(); return; }
        if(global) { if(!attemptedGlobal) { attemptedGlobal=true; attach(0); } }
        else { Set<Integer> all=new HashSet<>(sessions); all.addAll(detected); for(int session:all) attach(session); }
        for(DynamicsProcessing effect : effects.values()) {
            try {
                effect.setInputGainAllChannelsTo(gain);
                confirmedGain=effect.getInputGainByChannelIndex(0);
                if(!Float.isFinite(confirmedGain) || Math.abs(confirmedGain-gain)>.25f) throw new IllegalStateException("El motor devuelve "+confirmedGain+" dB al solicitar "+gain+" dB");
            }
            catch(RuntimeException e) { recordError("actualizar ganancia",e); }
        }
    }
    private void attach(int session) {
        if(effects.containsKey(session)) return;
        DynamicsProcessing effect = null;
        String stage="configurar arquitectura";
        try {
            DynamicsProcessing.Config config = AudioConfiguration.standard(gain);
            stage="crear motor (sesión "+session+")";
            effect = new DynamicsProcessing(0, session, config);
            stage="activar motor (sesión "+session+")";
            int result=effect.setEnabled(true);
            if(result != AudioEffect.SUCCESS || !effect.getEnabled()) throw new IllegalStateException("El dispositivo rechazó el efecto ("+result+")");
            effect.setControlStatusListener((fx, control) -> {
                if(!control) error="Otra aplicación tiene el control del efecto. Desactiva otros ecualizadores.";
            });
            effects.put(session,effect);
            Diagnostics.event(context,"Audio configurado · arquitectura estándar neutra · sesión "+session);
        } catch(RuntimeException e) {
            if(effect != null) effect.release();
            recordError(stage,e);
        }
    }
    private void recordError(String stage,RuntimeException ex) {
        String detail=stage+": "+ex.getClass().getSimpleName()+": "+ex.getMessage();
        if(!detail.equals(error)) Diagnostics.failure(context,"Audio · "+stage,ex);
        error=detail;
    }
    private void release(int session) {
        DynamicsProcessing fx=effects.remove(session);
        if(fx != null) {
            Diagnostics.event(context,"Retirar efecto de audio · sesión "+session);
            try { fx.setEnabled(false); } catch(RuntimeException ex) { Diagnostics.failure(context,"Desactivar audio",ex); }
            try { fx.release(); } catch(RuntimeException ex) { Diagnostics.failure(context,"Liberar audio",ex); }
            Diagnostics.event(context,"Efecto de audio retirado · sesión "+session);
        }
    }
    void releaseAll() { for(int id : new HashSet<>(effects.keySet())) release(id); confirmedGain=Float.NaN; }
    void dispose() { detector.dispose(); releaseAll(); }
    void retry() { releaseAll(); attemptedGlobal=false; error=""; update(); }
    void probe(java.util.function.Consumer<String> complete) {
        if(probing) return;
        probing=true; detector.stop(); releaseAll();
        new Thread(() -> {
            String result;
            try { result=AudioProbe.run(); } catch(RuntimeException ex) { result="Fallo de prueba: "+ex; }
            final String report=result;
            State.prefs(context).edit().putString("audioProbe",report).apply();
            new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                probing=false; attemptedGlobal=false; error="";
                // If the service was destroyed during the probe, never recreate effects on it.
                if(ShadeService.instance!=null && ShadeService.instance.audio==this) update();
                complete.accept(report);
            });
        },"TrekerAudioProbe").start();
    }
    String status() {
        if(probing) return "Comprobando compatibilidad sin reproducir sonido…";
        if(!enabled) return "Audio desactivado · sin efectos propios";
        if(!error.isEmpty()) return "No se ha podido aplicar: "+error;
        if(effects.isEmpty()) return "Esperando audio multimedia. "+(State.flag(context,"advanced")?detector.status():"Activa la detección avanzada para YouTube.");
        return (global ? "Efecto global creado" : "Efecto asociado a "+effects.size()+" sesión(es)")+
            " · solicitado "+gain+" dB · lectura del motor "+confirmedGain+" dB.\n"+detector.status()+"\nLa atenuación real debe comprobarse escuchando.";
    }
}
