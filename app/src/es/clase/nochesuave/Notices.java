package es.clase.nochesuave;

import android.app.*;
import android.content.*;

final class Notices {
    static final int ACTIVE=10;
    static void channel(Context c) {
        NotificationChannel channel=new NotificationChannel("controls","Controles y avisos",NotificationManager.IMPORTANCE_LOW);
        channel.setDescription("Acceso al filtro, apagado y programación");
        c.getSystemService(NotificationManager.class).createNotificationChannel(channel);
    }
    static PendingIntent action(Context c,String action,int id) {
        return PendingIntent.getBroadcast(c,id,new Intent(c,Actions.class).setAction(action),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
    }
    static void active(Context c) {
        NotificationManager manager=c.getSystemService(NotificationManager.class);
        if(!State.active(c) && LifetimeService.instance==null) { manager.cancel(ACTIVE); return; }
        channel(c);
        manager.notify(ACTIVE,build(c));
    }
    static Notification build(Context c) {
        String description=State.active(c)
            ? (State.flag(c,"dim")?"Pantalla atenuada":"Pantalla normal")+" · "+(State.flag(c,"audio")?State.gain(c)+" dB solicitados":"Audio normal")
            : "Efectos apagados · preparado para activarlos";
        if(ShadeService.instance==null) description="Esperando conexión de accesibilidad";
        if(!State.flag(c,"sessionOpen")) description="Sesión cerrada · finalizando servicio";
        else if(ShadeService.instance!=null) description+=" · Toca para regular el brillo";
        PendingIntent app=PendingIntent.getActivity(c,0,new Intent(c,MainActivity.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder builder=new Notification.Builder(c,"controls")
            .setSmallIcon(R.drawable.icon).setContentTitle("Descansa Treker")
            .setContentText(description)
            .setContentIntent(ShadeService.instance!=null && State.flag(c,"sessionOpen") ? action(c,"panel",4) : app)
            .setColor(0xFFFFC629)
            .setOngoing(true).setOnlyAlertOnce(true).setCategory(Notification.CATEGORY_SERVICE).setVisibility(Notification.VISIBILITY_PUBLIC);
        if(State.active(c) && State.flag(c,"sessionOpen")) {
            builder.addAction(new Notification.Action.Builder(null,"Apagar ambos",action(c,"off",2)).build());
        } else if(State.flag(c,"sessionOpen")) builder.addAction(new Notification.Action.Builder(null,"Activar ambos",action(c,"on",3)).build());
        builder.addAction(new Notification.Action.Builder(null,"Abrir app",app).build());
        return builder.build();
    }
    static void unavailable(Context c) {
        channel(c);
        c.getSystemService(NotificationManager.class).notify(11,new Notification.Builder(c,"controls")
            .setSmallIcon(R.drawable.icon).setContentTitle("Descansa Treker necesita accesibilidad")
            .setContentText("Abre la app y conecta el servicio para aplicar el encendido programado.")
            .setContentIntent(PendingIntent.getActivity(c,0,new Intent(c,MainActivity.class),PendingIntent.FLAG_IMMUTABLE))
            .setAutoCancel(true).build());
    }
}
