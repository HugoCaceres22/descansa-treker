package es.clase.nochesuave;

import android.accessibilityservice.AccessibilityService;
import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.view.*;
import android.view.accessibility.AccessibilityEvent;
import android.widget.*;

public class ShadeService extends AccessibilityService {
    static ShadeService instance;
    AudioEngine audio;
    private WindowManager windows;
    private View shade, panel;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final Runnable hidePanel=this::closePanel;
    private final BroadcastReceiver screen=new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) {
            Diagnostics.event(c,"Pantalla · "+i.getAction());
            closePanel(); refresh();
        }
    };
    @Override public void onCreate() { super.onCreate(); Diagnostics.event(this,"Servicio creado"); }
    @Override protected void onServiceConnected() {
        Diagnostics.event(this,"Android conecta el servicio");
        windows=(WindowManager)getSystemService(WINDOW_SERVICE);
        audio=new AudioEngine(this);
        IntentFilter filter=new IntentFilter(Intent.ACTION_SCREEN_ON); filter.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(screen,filter);
        Notices.channel(this);
        Scheduler.reschedule(this);
        instance=this;
        LifetimeService.ensure(this);
        refresh();
        Diagnostics.event(this,"Servicio preparado");
    }
    @Override public void onAccessibilityEvent(AccessibilityEvent event) { /* No screen content is requested or read. */ }
    @Override public void onInterrupt() { Diagnostics.event(this,"Android solicita interrupción"); State.both(this,false); }
    @Override public boolean onUnbind(Intent intent) {
        Diagnostics.event(this,"Android desvincula el servicio");
        if(instance==this) instance=null;
        if(!Diagnostics.permissionEnabled(this)) LifetimeService.stop(this);
        return super.onUnbind(intent);
    }
    @Override public void onTaskRemoved(Intent intent) { Diagnostics.event(this,"Tarea retirada de recientes"); super.onTaskRemoved(intent); }
    @Override public void onDestroy() {
        Diagnostics.event(this,"Servicio destruido por Android");
        handler.removeCallbacksAndMessages(null);
        if(audio != null) audio.dispose();
        remove(shade); remove(panel);
        try { unregisterReceiver(screen); } catch(IllegalArgumentException ignored) {}
        instance=null;
        if(LifetimeService.instance!=null) Notices.active(this);
        else getSystemService(NotificationManager.class).cancel(Notices.ACTIVE);
        super.onDestroy();
    }
    int dp(float value) { return (int)(getResources().getDisplayMetrics().density*value+.5f); }
    private WindowManager.LayoutParams layout(int width,int height, boolean touchable) {
        int flags=WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
        if(!touchable) flags |= WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
        WindowManager.LayoutParams lp=new WindowManager.LayoutParams(width,height,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,flags,PixelFormat.TRANSLUCENT);
        lp.layoutInDisplayCutoutMode=WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        if(Build.VERSION.SDK_INT>=30) lp.setFitInsetsTypes(touchable ? WindowInsets.Type.systemBars() : 0);
        return lp;
    }
    void refresh() {
        if(windows == null) return;
        try {
            if(State.flag(this,"dim")) {
                ensureShade();
                int alpha=Math.round((100-State.level(this))*.92f*255/100);
                shade.setBackgroundColor(Color.argb(alpha,0,0,0));
            } else { remove(shade); shade=null; }
            if(!State.active(this)) closePanel();
            audio.update();
            Notices.active(this);
            QuickTile.refreshTile(this);
        } catch(RuntimeException e) {
            Diagnostics.failure(this,"Actualizar filtro y efectos",e);
            State.prefs(this).edit().putString("overlayError",e.getClass().getSimpleName()+": "+e.getMessage()).apply();
            remove(shade); shade=null;
        }
    }
    private void ensureShade() {
        if(shade!=null) return;
        shade=new View(this); shade.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        WindowManager.LayoutParams lp=layout(-1,-1,false); lp.gravity=Gravity.TOP|Gravity.LEFT; windows.addView(shade,lp);
    }
    void openPanelFromNotification() {
        if(!State.flag(this,"sessionOpen")) return;
        if(Build.VERSION.SDK_INT>=31) performGlobalAction(GLOBAL_ACTION_DISMISS_NOTIFICATION_SHADE);
        handler.postDelayed(() -> { if(instance==this && State.flag(this,"sessionOpen")) openPanel(); },250);
    }
    void openPanel() {
        if(!State.flag(this,"sessionOpen")) return;
        if(panel != null) { keepPanel(); return; }
        // Put a transparent dimming surface below the panel before a first adjustment.
        ensureShade();
        FrameLayout outside=new FrameLayout(this);
        outside.setContentDescription("Toca fuera de la barra para cerrarla");
        outside.setOnClickListener(v -> closePanel());
        LinearLayout box=new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setPadding(dp(16),dp(6),dp(12),dp(10));
        box.setClickable(true); // Consume taps inside the card; only its outside closes it.
        GradientDrawable bg=background(0xFA171717,dp(20)); bg.setStroke(dp(1),0xFF65531B); box.setBackground(bg);
        LinearLayout header=new LinearLayout(this); header.setGravity(Gravity.CENTER_VERTICAL);
        TextView heading=text("Brillo",15); heading.setTextColor(0xFFFFC629); heading.setTypeface(null,android.graphics.Typeface.BOLD);
        header.addView(heading,new LinearLayout.LayoutParams(0,-2,1));
        TextView light=text(State.level(this)+" %",16); light.setTextColor(0xFFFFC629); header.addView(light);
        TextView close=text("×",24); close.setGravity(Gravity.CENTER); close.setContentDescription("Cerrar barra de brillo");
        close.setOnClickListener(v -> closePanel()); header.addView(close,new LinearLayout.LayoutParams(dp(44),dp(44))); box.addView(header);
        SeekBar brightness=new SeekBar(this); brightness.setMax(100); brightness.setProgress(State.level(this));
        brightness.setContentDescription("Luminosidad del filtro");
        brightness.setThumbTintList(android.content.res.ColorStateList.valueOf(0xFFFFC629));
        brightness.setProgressTintList(android.content.res.ColorStateList.valueOf(0xFFFFC629));
        brightness.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF4C4635));
        box.addView(brightness,new LinearLayout.LayoutParams(-1,dp(40)));
        brightness.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar bar,int value,boolean user) {
                if(!user) return;
                State.number(ShadeService.this,"light",value); State.set(ShadeService.this,"dim",true);
                light.setText(value+" %"); refresh();
            }
            public void onStartTrackingTouch(SeekBar bar) { handler.removeCallbacks(hidePanel); }
            public void onStopTrackingTouch(SeekBar bar) { keepPanel(); }
        });
        LinearLayout ends=new LinearLayout(this);
        TextView darker=text("Más oscuro",10),normal=text("Sin atenuación",10); darker.setTextColor(0xFFAAA69C); normal.setTextColor(0xFFAAA69C); normal.setGravity(Gravity.RIGHT);
        ends.addView(darker,new LinearLayout.LayoutParams(0,-2,1)); ends.addView(normal,new LinearLayout.LayoutParams(0,-2,1)); box.addView(ends);
        int width=Math.min(dp(280),getResources().getDisplayMetrics().widthPixels-dp(32));
        outside.addView(box,new FrameLayout.LayoutParams(width,-2,Gravity.CENTER));
        panel=outside;
        WindowManager.LayoutParams lp=layout(-1,-1,true);
        if(Build.VERSION.SDK_INT>=30) lp.setFitInsetsTypes(0);
        windows.addView(panel,lp); keepPanel();
    }
    private TextView text(String value,int size) { TextView view=new TextView(this); view.setText(value); view.setTextSize(size); view.setTextColor(0xFFF7F3E9); return view; }
    private void keepPanel() { handler.removeCallbacks(hidePanel); handler.postDelayed(hidePanel,7000); }
    private void closePanel() { handler.removeCallbacks(hidePanel); remove(panel); panel=null; if(!State.flag(this,"dim")) { remove(shade); shade=null; } }
    private void remove(View view) { if(view != null && windows != null) { try { windows.removeView(view); } catch(RuntimeException ignored) {} } }
    static GradientDrawable background(int color,int radius) { GradientDrawable bg=new GradientDrawable(); bg.setColor(color); bg.setCornerRadius(radius); return bg; }
}
