package es.clase.nochesuave;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioManager;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import java.text.DateFormat;
import java.util.*;

public class MainActivity extends Activity {
    private static final int GOLD=0xFFFFC629, WHITE=0xFFF7F3E9, MUTED=0xFFA7A59F, BG=0xFF080808, SURFACE=0xFF171717;
    private LinearLayout root;
    private Switch dim,audio;
    private TextView serviceStatus,audioStatus,timerStatus,homeStatus;
    private Button master;
    private int page=0;
    private boolean syncing;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final Runnable ticker=new Runnable() { public void run() { sync(); handler.postDelayed(this,1500); } };
    int dp(int value) { return (int)(getResources().getDisplayMetrics().density*value+.5f); }
    @Override public void onCreate(Bundle state) { super.onCreate(state); if(state!=null) page=state.getInt("page",0); Notices.channel(this); }
    @Override protected void onSaveInstanceState(Bundle state) { state.putInt("page",page); super.onSaveInstanceState(state); }
    @Override public void onResume() {
        super.onResume(); Session.open(this); LifetimeService.ensure(this); Scheduler.reschedule(this); build(); handler.removeCallbacks(ticker); handler.post(ticker);
        State.refresh();
    }
    @Override public void onPause() { handler.removeCallbacks(ticker); super.onPause(); }
    private void build() {
        dim=null; audio=null; serviceStatus=null; audioStatus=null; timerStatus=null; master=null; homeStatus=null;
        LinearLayout shell=new LinearLayout(this); shell.setOrientation(LinearLayout.VERTICAL); shell.setBackgroundColor(BG);
        ScrollView scroll=new ScrollView(this); scroll.setFillViewport(true); scroll.setClipToPadding(false);
        root=column(); root.setPadding(dp(22),dp(20),dp(22),dp(20)); scroll.addView(root);
        shell.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout nav=new LinearLayout(this); nav.setPadding(dp(14),dp(10),dp(14),dp(10)); nav.setBackgroundColor(0xFF111111);
        String[] names={"Inicio","Temporizador","Ajustes"};
        for(int i=0;i<3;i++) {
            final int index=i; Button tab=button(nav,names[i],() -> { page=index; build(); sync(); });
            tab.setTextSize(12); tab.setTextColor(page==i?GOLD:MUTED); tab.setBackground(bg(page==i?0xFF302812:0xFF111111,14,0));
        }
        shell.addView(nav); setContentView(shell);
        shell.setOnApplyWindowInsetsListener((view,insets) -> {
            if(Build.VERSION.SDK_INT>=30) {
                android.graphics.Insets bars=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());
                shell.setPadding(bars.left,bars.top,bars.right,bars.bottom);
            } else shell.setFitsSystemWindows(true);
            return insets;
        }); shell.requestApplyInsets();
        if(page==0) home(); else if(page==1) programming(); else settings();
        sync();
    }
    private void home() {
        LinearLayout brand=new LinearLayout(this); brand.setGravity(Gravity.CENTER_VERTICAL);
        ImageView dog=new ImageView(this); dog.setImageResource(R.drawable.dog_logo); dog.setContentDescription("Treker, american bully negro con mancha blanca en el pecho");
        dog.setScaleType(ImageView.ScaleType.CENTER_CROP); dog.setBackground(bg(GOLD,24,0)); dog.setClipToOutline(true);
        LinearLayout.LayoutParams picture=new LinearLayout.LayoutParams(dp(82),dp(82)); picture.rightMargin=dp(16); brand.addView(dog,picture);
        LinearLayout words=column(); TextView eyebrow=label(words,"DESCANSA",12,GOLD); eyebrow.setLetterSpacing(.18f); bold(eyebrow);
        TextView name=label(words,"Treker",34,WHITE); bold(name); brand.addView(words,new LinearLayout.LayoutParams(0,-2,1)); root.addView(brand);
        label(root,"Tu momento de calma.",16,MUTED);
        homeStatus=label(root,"",12,GOLD); homeStatus.setOnClickListener(v -> { page=2; build(); });
        LinearLayout mode=card("Todo a tu ritmo");
        label(mode,"Pantalla suave y sonido más bajo, con un toque.",14,MUTED);
        master=button(mode,"Activar ambos",() -> {
            if(State.active(this)) State.both(this,false);
            else if(ready()) State.both(this,true);
            sync();
        }); master.setTextColor(0xFF0A0A0A); master.setTextSize(17); bold(master); master.setBackground(bg(GOLD,18,0));
        LinearLayout light=card("Pantalla");
        dim=toggle(light,"Oscurecimiento",State.flag(this,"dim"),checked -> {
            if(checked&&!ready()) { sync(); return; }
            State.set(this,"dim",checked); State.refresh(); sync();
        });
        TextView lightValue=label(light,State.level(this)+" %",30,GOLD); bold(lightValue);
        seek(light,100,State.level(this),value -> {
            State.number(this,"light",value); lightValue.setText(value+" %"); State.refresh();
        });
        endpoints(light,"Más oscuro","Sin atenuación");
        label(light,"Toca la notificación para regular el brillo sobre cualquier app.",12,MUTED);
        LinearLayout sound=card("Sonido");
        audio=toggle(sound,"Reducción de volumen",State.flag(this,"audio"),checked -> {
            if(checked&&!ready()) { sync(); return; }
            State.set(this,"audio",checked); State.refresh(); sync();
        });
        TextView gain=label(sound,gainLabel(),30,GOLD); bold(gain);
        seek(sound,100,State.prefs(this).getInt("gain",-20)+100,value -> {
            State.number(this,"gain",value-100); gain.setText(gainLabel()); State.refresh();
        });
        endpoints(sound,"Más bajo","Nivel normal");
        label(sound,"Ajuste fino en pasos de 0,5 dB.",12,MUTED);
        LinearLayout volume=new LinearLayout(this); button(volume,"Volumen −",() -> adjust(-1)); button(volume,"Volumen +",() -> adjust(1)); sound.addView(volume);
        label(root,"Puedes usar cualquier app. Al quitar Descansa Treker de recientes, los efectos se apagan.",12,MUTED);
    }
    private void programming() {
        title("Temporizador","Elige cuánto tiempo quieres descansar.");
        LinearLayout timer=card("Apagar dentro de…");
        label(timer,"Al terminar se desactivan ambos efectos.",14,MUTED);
        timerStatus=label(timer,"",16,GOLD);
        LinearLayout shortTimes=new LinearLayout(this);
        button(shortTimes,"10 min",() -> setTimer(10));
        button(shortTimes,"30 min",() -> setTimer(30));
        timer.addView(shortTimes);
        LinearLayout longTimes=new LinearLayout(this);
        button(longTimes,"2 horas",() -> setTimer(120));
        button(longTimes,"5 horas",() -> setTimer(300));
        timer.addView(longTimes);
        button(timer,"Personalizado",this::customTimer);
        button(timer,"Cancelar temporizador",() -> { Scheduler.cancelTimer(this); sync(); });
        label(root,"El temporizador no enciende los efectos. Al quitar Descansa Treker de recientes, también se cancela.",12,MUTED);
        if(!Scheduler.exact(this)) label(root,"Para que termine a tiempo, permite Temporizadores exactos en Ajustes.",12,GOLD);
    }
    private void setTimer(int minutes) { Scheduler.timer(this,minutes); sync(); }
    private void settings() {
        title("Ajustes","Todo preparado para descansar.");
        LinearLayout permissions=card("Permisos"); serviceStatus=label(permissions,"",13,GOLD);
        button(permissions,"Accesibilidad",() -> new AlertDialog.Builder(this).setTitle("Permitir el filtro")
            .setMessage("Activa Descansa Treker en Aplicaciones descargadas.\n\nSi HyperOS lo bloquea, ve a Información de la aplicación, baja hasta el final y permite los ajustes restringidos.\n\nLa app no lee tus pantallas ni captura audio. Cierra el panel de notificaciones al tocar su control de brillo.")
            .setPositiveButton("Abrir ajustes",(d,w) -> launch(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))).setNegativeButton("Cancelar",null).show());
        button(permissions,"Notificaciones",() -> {
            if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED) requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},1);
            else launch(new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,getPackageName()));
        });
        button(permissions,"Temporizadores exactos",() -> {
            if(Build.VERSION.SDK_INT>=31) launch(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,Uri.parse("package:"+getPackageName())));
            else toast("Los temporizadores exactos ya están disponibles.");
        });
        button(permissions,"Batería y ajustes del teléfono",() -> launch(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+getPackageName()))));
        LinearLayout extra=card("Accesos");
        button(extra,"Añadir a ajustes rápidos",() -> {
            if(Build.VERSION.SDK_INT>=33) getSystemService(StatusBarManager.class).requestAddTileService(new ComponentName(this,QuickTile.class),"Descansa Treker",android.graphics.drawable.Icon.createWithResource(this,R.drawable.icon),getMainExecutor(),result -> {});
            else toast("Edita los ajustes rápidos y añade Descansa Treker.");
        });
        label(extra,"La notificación abre solo la barra de brillo, sin entrar en la app. El volumen se regula desde Inicio.",12,MUTED);
        LinearLayout compatibility=card("Compatibilidad de audio");
        toggle(compatibility,"Detección avanzada de sesiones",State.flag(this,"advanced"),checked -> { State.set(this,"advanced",checked); State.set(this,"global",false); State.refresh(); sync(); });
        label(compatibility,AudioSessionDetector.permission(this)?"Permiso de detección concedido. Solo se consulta mientras el audio esté activado.":"Requiere conceder una vez el permiso DUMP desde el ordenador. No graba ni reproduce sonido.",12,MUTED);
        button(compatibility,"Cómo conceder el permiso",() -> new AlertDialog.Builder(this).setTitle("Permiso de detección")
            .setMessage("Desde un ordenador conectado por ADB, ejecuta una vez:\n\nadb shell pm grant es.clase.nochesuave android.permission.DUMP\n\nDespués puedes desconectar la depuración. La app consulta datos técnicos de las sesiones, no su contenido de audio.")
            .setPositiveButton("Entendido",null).show());
        audioStatus=label(compatibility,"",12,GOLD);
        button(compatibility,"Reintentar audio",() -> { if(ready()) { ShadeService.instance.audio.retry(); sync(); } });
        button(compatibility,"Comprobar compatibilidad",() -> {
            if(!ready()) return;
            toast("Comprobando el motor. El efecto de audio se retira temporalmente.");
            ShadeService.instance.audio.probe(report -> {
                if(isFinishing() || isDestroyed()) return;
                sync();
                new AlertDialog.Builder(this).setTitle("Resultado de compatibilidad").setMessage(report)
                    .setPositiveButton("Compartir diagnóstico",(dialog,which) -> shareDiagnostics()).setNegativeButton("Cerrar",null).show();
            });
        });
        LinearLayout help=card("Ayuda");
        button(help,"Compartir diagnóstico",this::shareDiagnostics);
        button(help,"Recuperar conexión",this::reconnect);
        label(help,"Descansa Treker · 0.2.4\nSin anuncios, sin cuenta y sin conexión a Internet.",12,MUTED);
    }
    private void sync() {
        syncing=true;
        if(dim!=null) dim.setChecked(State.flag(this,"dim")); if(audio!=null) audio.setChecked(State.flag(this,"audio"));
        syncing=false;
        if(master!=null) master.setText(State.active(this)?"Apagar ambos":"Activar ambos");
        if(homeStatus!=null) homeStatus.setText(ShadeService.instance==null?"●  Completa la configuración en Ajustes":"●  "+(State.active(this)?"Tu modo descanso está activo":"Todo preparado"));
        if(serviceStatus!=null) serviceStatus.setText(Diagnostics.serviceState(this));
        if(audioStatus!=null) audioStatus.setText(ShadeService.instance==null?"Servicio desconectado.":ShadeService.instance.audio.status());
        if(timerStatus!=null) { long at=State.prefs(this).getLong("timer",0); timerStatus.setText(at>0?"Apagado: "+date(at):"Sin temporizador"); }
    }
    private boolean ready() { if(ShadeService.instance!=null) return true; if(Diagnostics.permissionEnabled(this)) reconnect(); else { page=2; build(); toast("Activa el permiso de accesibilidad."); } return false; }
    private void reconnect() {
        if(ShadeService.instance!=null) { toast("El servicio está conectado."); return; }
        new AlertDialog.Builder(this).setTitle("Recuperar conexión").setMessage("Si el permiso está activado pero el servicio no conecta, entra en Accesibilidad y desactiva y reactiva Usar Descansa Treker.\n\nSi se repite, comparte el diagnóstico para revisar el cierre.")
            .setPositiveButton("Abrir accesibilidad",(d,w) -> launch(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))).setNegativeButton("Cancelar",null).show();
    }
    private void customTimer() {
        EditText input=new EditText(this); input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER); input.setHint("Minutos (1–1440)"); input.setPadding(dp(24),dp(12),dp(24),dp(12));
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Apagar dentro de…").setView(input).setPositiveButton("Programar",null).setNegativeButton("Cancelar",null).create();
        dialog.setOnShowListener(v -> dialog.getButton(-1).setOnClickListener(button -> {
            try { int value=Integer.parseInt(input.getText().toString()); if(value<1||value>1440) throw new NumberFormatException(); Scheduler.timer(this,value); sync(); dialog.dismiss(); }
            catch(NumberFormatException ex) { input.setError("Introduce entre 1 y 1440 minutos"); }
        })); dialog.show();
    }
    private void adjust(int direction) { ((AudioManager)getSystemService(AUDIO_SERVICE)).adjustStreamVolume(AudioManager.STREAM_MUSIC,direction,AudioManager.FLAG_SHOW_UI); }
    private String gainLabel() { return String.format(Locale.getDefault(),"%.1f dB",State.gain(this)); }
    private String date(long at) { return at<=0?"sin fecha":DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT).format(new Date(at)); }
    private LinearLayout column() { LinearLayout view=new LinearLayout(this); view.setOrientation(LinearLayout.VERTICAL); return view; }
    private void title(String text,String subtitle) { TextView view=label(root,text,32,WHITE); bold(view); label(root,subtitle,14,MUTED); }
    private LinearLayout card(String heading) {
        LinearLayout box=column(); box.setPadding(dp(18),dp(16),dp(18),dp(16)); box.setBackground(bg(SURFACE,23,0xFF29271F));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2); lp.topMargin=dp(16); root.addView(box,lp); bold(label(box,heading,20,WHITE)); return box;
    }
    private GradientDrawable bg(int color,int radius,int stroke) { GradientDrawable drawable=ShadeService.background(color,dp(radius)); if(stroke!=0) drawable.setStroke(dp(1),stroke); return drawable; }
    private void bold(TextView view) { view.setTypeface(null,Typeface.BOLD); }
    private TextView label(LinearLayout parent,String text,int size,int color) { TextView view=new TextView(this); view.setText(text); view.setTextSize(size); view.setTextColor(color); view.setPadding(0,dp(4),0,dp(5)); view.setLineSpacing(dp(2),1); parent.addView(view); return view; }
    private Button button(LinearLayout parent,String text,Runnable action) {
        Button view=new Button(this); view.setText(text); view.setAllCaps(false); view.setTextColor(WHITE); view.setTextSize(14); view.setMinHeight(dp(48)); view.setPadding(dp(10),dp(8),dp(10),dp(8));
        GradientDrawable normal=bg(0xFF282720,15,0); view.setBackground(new android.graphics.drawable.RippleDrawable(ColorStateList.valueOf(0x44FFC629),normal,null));
        view.setOnClickListener(v -> action.run()); boolean horizontal=parent.getOrientation()==LinearLayout.HORIZONTAL;
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(horizontal?0:-1,-2,horizontal?1:0); lp.setMargins(horizontal?dp(3):0,dp(8),horizontal?dp(3):0,0); parent.addView(view,lp); return view;
    }
    interface FlagChange { void apply(boolean value); } interface LevelChange { void apply(int value); }
    private Switch toggle(LinearLayout parent,String text,boolean checked,FlagChange action) {
        Switch view=new Switch(this); view.setText(text); view.setTextSize(14); view.setTextColor(MUTED); view.setPadding(0,dp(7),0,dp(7)); view.setSwitchPadding(dp(12));
        int[][] states={new int[]{android.R.attr.state_checked},new int[]{}};
        view.setThumbTintList(new ColorStateList(states,new int[]{GOLD,0xFF909090})); view.setTrackTintList(new ColorStateList(states,new int[]{0xFF786019,0xFF424242}));
        view.setChecked(checked); view.setOnCheckedChangeListener((button,on) -> { if(!syncing) action.apply(on); }); parent.addView(view,new LinearLayout.LayoutParams(-1,dp(48))); return view;
    }
    private void seek(LinearLayout parent,int max,int value,LevelChange action) {
        SeekBar view=new SeekBar(this); view.setMax(max); view.setProgress(value); view.setProgressTintList(ColorStateList.valueOf(GOLD)); view.setThumbTintList(ColorStateList.valueOf(GOLD)); view.setProgressBackgroundTintList(ColorStateList.valueOf(0xFF43413B));
        parent.addView(view,new LinearLayout.LayoutParams(-1,dp(48)));
        view.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() { public void onProgressChanged(SeekBar bar,int p,boolean user) { if(user) action.apply(p); } public void onStartTrackingTouch(SeekBar bar) {} public void onStopTrackingTouch(SeekBar bar) {} });
    }
    private void endpoints(LinearLayout parent,String left,String right) {
        LinearLayout row=new LinearLayout(this); TextView a=label(row,left,11,MUTED),b=label(row,right,11,MUTED); a.setLayoutParams(new LinearLayout.LayoutParams(0,-2,1)); b.setLayoutParams(new LinearLayout.LayoutParams(0,-2,1)); b.setGravity(Gravity.RIGHT); parent.addView(row);
    }
    private void toast(String text) { Toast.makeText(this,text,Toast.LENGTH_LONG).show(); }
    private void launch(Intent intent) { try { startActivity(intent); } catch(ActivityNotFoundException ex) { toast("Busca este permiso en los ajustes de Descansa Treker."); } }
    private void shareDiagnostics() {
        String report="Descansa Treker 0.2.4\nDispositivo: "+Build.MANUFACTURER+" "+Build.MODEL+"\nAndroid: "+Build.VERSION.RELEASE+" (API "+Build.VERSION.SDK_INT+")\nServicio: "+(ShadeService.instance!=null)+
            "\nPantalla: "+State.flag(this,"dim")+" · "+State.level(this)+" %\nAudio: "+State.flag(this,"audio")+" · "+State.gain(this)+" dB\nDetección avanzada: "+State.flag(this,"advanced")+"\nPermiso DUMP: "+AudioSessionDetector.permission(this)+
            "\nDiagnóstico: "+(ShadeService.instance!=null?ShadeService.instance.audio.status():"servicio desconectado")+"\nError de filtro: "+State.prefs(this).getString("overlayError","ninguno")+
            "\nTemporizadores exactos: "+Scheduler.exact(this)+"\nNotificaciones: "+getSystemService(NotificationManager.class).areNotificationsEnabled()+Diagnostics.report(this);
        startActivity(Intent.createChooser(new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT,report),"Compartir diagnóstico"));
    }
}
