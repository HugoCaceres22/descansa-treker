package es.clase.nochesuave;

import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Looper;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

/** Bounded diagnostics query, only while audio is enabled in an open app session. */
final class AudioSessionDetector {
    private final Context context;
    private final Consumer<Set<Integer>> listener;
    private final Handler main=new Handler(Looper.getMainLooper());
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private volatile Process command;
    private boolean running,disposed;
    private int generation;
    private String summary="Sin explorar";
    private final Runnable tick=this::scan;
    AudioSessionDetector(Context c,Consumer<Set<Integer>> listener) { context=c; this.listener=listener; }
    static boolean permission(Context c) { return c.checkSelfPermission("android.permission.DUMP")==PackageManager.PERMISSION_GRANTED; }
    void sync(boolean enabled) {
        if(disposed) return;
        if(!enabled || !permission(context)) {
            stop(); summary=!permission(context)?"Falta conceder el permiso DUMP una vez desde el ordenador.":"Detección detenida"; return;
        }
        if(running) return;
        running=true; generation++; main.post(tick);
    }
    void stop() {
        if(running) { running=false; generation++; }
        main.removeCallbacks(tick);
        Process process=command; if(process!=null) process.destroy();
    }
    void dispose() { stop(); disposed=true; worker.shutdownNow(); }
    String status() { return summary; }
    private void scan() {
        if(!running || disposed) return;
        final int token=generation;
        worker.execute(() -> {
            Set<Integer> result=null; String message;
            Process process=null;
            Runnable timeout=null;
            try {
                process=new ProcessBuilder("/system/bin/dumpsys","-t","2","audio").redirectErrorStream(true).start();
                command=process;
                final Process watched=process;
                timeout=() -> watched.destroyForcibly(); main.postDelayed(timeout,3500);
                ByteArrayOutputStream bytes=new ByteArrayOutputStream();
                try(InputStream input=process.getInputStream()) {
                    byte[] buffer=new byte[4096]; int count;
                    while((count=input.read(buffer))!=-1) {
                        if(bytes.size()+count>1024*1024) throw new IOException("Informe de audio demasiado grande");
                        bytes.write(buffer,0,count);
                    }
                }
                String dump=new String(bytes.toByteArray(),StandardCharsets.UTF_8);
                if(dump.contains("Permission Denial") || dump.contains("Permission denied")) throw new IOException("Android deniega DUMP");
                if(!dump.contains("PlaybackActivityMonitor") && !dump.contains("AudioPlaybackConfiguration")) throw new IOException("Formato del informe de audio no reconocido");
                result=PlaybackSessions.parse(dump,context.getApplicationInfo().uid);
                message="Detección avanzada: "+result.size()+" sesión(es) multimedia encontradas";
                // The full system report is not stored or shared.
            } catch(Exception ex) { message="Detección avanzada: "+ex.getClass().getSimpleName()+": "+ex.getMessage(); }
            finally {
                if(timeout!=null) main.removeCallbacks(timeout);
                if(process!=null) process.destroy();
                if(command==process) command=null;
            }
            final Set<Integer> found=result; final String status=message;
            main.post(() -> {
                if(!running || disposed || generation!=token) return;
                summary=status;
                if(found!=null) listener.accept(found);
                boolean playing=((AudioManager)context.getSystemService(Context.AUDIO_SERVICE)).isMusicActive();
                main.postDelayed(tick,playing?4000:8000);
            });
        });
    }
}
