package es.clase.nochesuave;

import android.app.*;
import android.content.*;
import android.os.Build;
import android.provider.Settings;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.*;

final class Diagnostics {
    static boolean permissionEnabled(Context c) {
        String enabled=Settings.Secure.getString(c.getContentResolver(),Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if(enabled==null) return false;
        ComponentName own=new ComponentName(c,ShadeService.class);
        for(String item:enabled.split(":")) if(own.equals(ComponentName.unflattenFromString(item))) return true;
        return false;
    }
    static synchronized void event(Context c,String text) {
        try {
            String old=State.prefs(c).getString("serviceEvents","");
            String stamp=new SimpleDateFormat("MM-dd HH:mm:ss.SSS",Locale.ROOT).format(new Date());
            String next=old+stamp+" "+text+"\n";
            if(next.length()>6000) { next=next.substring(next.length()-6000); int end=next.indexOf('\n'); if(end>=0) next=next.substring(end+1); }
            State.prefs(c).edit().putString("serviceEvents",next).commit();
        } catch(RuntimeException ignored) {}
    }
    static void failure(Context c,String where,Throwable error) {
        try {
            StringWriter writer=new StringWriter(); error.printStackTrace(new PrintWriter(writer));
            String trace=where+"\n"+writer;
            State.prefs(c).edit().putString("lastFailure",trace.substring(0,Math.min(trace.length(),10000))).commit();
            event(c,"ERROR "+where+" · "+error);
        } catch(RuntimeException ignored) {}
    }
    static String serviceState(Context c) {
        if(ShadeService.instance!=null) return "Servicio conectado · listo para probar";
        if(permissionEnabled(c)) return "Permiso activado, pero servicio desconectado. Pulsa Reconectar servicio.";
        return "Permiso de accesibilidad desactivado";
    }
    static String report(Context c) {
        StringBuilder out=new StringBuilder();
        out.append("\nPermiso de accesibilidad: ").append(permissionEnabled(c));
        out.append("\nPID actual: ").append(android.os.Process.myPid());
        out.append("\nServicio en primer plano: ").append(LifetimeService.instance!=null);
        out.append("\nSesión de uso abierta: ").append(State.flag(c,"sessionOpen"));
        out.append("\nPrueba de audio:\n").append(State.prefs(c).getString("audioProbe","No ejecutada"));
        out.append("\nÚltimo fallo registrado:\n").append(State.prefs(c).getString("lastFailure","ninguno"));
        out.append("\nEventos locales del servicio:\n").append(State.prefs(c).getString("serviceEvents","ninguno"));
        out.append("\nCierres recientes registrados por Android:\n");
        if(Build.VERSION.SDK_INT>=30) {
            try {
                List<ApplicationExitInfo> exits=c.getSystemService(ActivityManager.class).getHistoricalProcessExitReasons(c.getPackageName(),0,6);
                if(exits.isEmpty()) out.append("ninguno\n");
                for(ApplicationExitInfo info:exits) {
                    out.append(new Date(info.getTimestamp())).append(" · motivo ").append(reason(info.getReason()))
                       .append(" · código ").append(info.getStatus()).append(" · ").append(info.getDescription()).append('\n');
                }
            } catch(RuntimeException ex) { out.append("No disponible: ").append(ex); }
        } else out.append("No disponible en esta versión de Android.");
        return out.toString();
    }
    private static String reason(int code) {
        switch(code) {
            case 1:return "salida solicitada"; case 2:return "señal del sistema";
            case 3:return "falta de memoria"; case 4:return "excepción Java";
            case 5:return "fallo nativo"; case 6:return "app sin respuesta";
            case 7:return "error de inicialización"; case 8:return "cambio de permisos";
            case 9:return "recursos excesivos"; case 10:return "solicitud del usuario / detención";
            case 11:return "usuario detenido"; case 12:return "dependencia terminada";
            case 13:return "cierre por el sistema / fabricante";
            case 14:return "congelador del sistema"; case 15:return "actualización de la app";
            case 16:return "cambio de estado del paquete"; default:return "otro ("+code+")";
        }
    }
}
