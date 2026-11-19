"""Run the real AudioEngine and upstream DynamicsProcessing on a bounded native-contract double.

This reproduces the AOSP variant rejection, not a physical Xiaomi audio path.
The unmodified upstream Java source is in reference/DynamicsProcessing.java.
"""
from pathlib import Path
import subprocess
import os
import shutil

ROOT=Path(__file__).resolve().parents[2]
OUT=ROOT/'build/audio-contract'
OUT.mkdir(parents=True,exist_ok=True)
FILES={
'android/annotation/NonNull.java':'package android.annotation; public @interface NonNull {}',
'android/annotation/Nullable.java':'package android.annotation; public @interface Nullable {}',
'android/media/AudioTrack.java':'package android.media; public class AudioTrack {}',
'android/media/MediaPlayer.java':'package android.media; public class MediaPlayer {}',
'android/util/Log.java':'package android.util; public class Log { public static int w(String a,String b) { return 0; } }',
'android/content/Context.java':'package android.content; public class Context {}',
'android/os/Looper.java':'package android.os; public class Looper { public static Looper getMainLooper() {return new Looper();} }',
'android/os/Handler.java':'package android.os; public class Handler { public Handler(Looper l) {} public boolean post(Runnable r) {r.run();return true;} }',
'es/clase/nochesuave/AudioProbe.java':'package es.clase.nochesuave; final class AudioProbe { static String run() {return "Not exercised by contract tests";} }',
'es/clase/nochesuave/ShadeService.java':'package es.clase.nochesuave; final class ShadeService { static ShadeService instance; AudioEngine audio; }',
'es/clase/nochesuave/AudioSessionDetector.java':'''package es.clase.nochesuave; import android.content.Context; import java.util.*; import java.util.function.Consumer;
final class AudioSessionDetector { AudioSessionDetector(Context c,Consumer<Set<Integer>> f) {} void sync(boolean v) {} void stop() {} void dispose() {} String status() {return "detector not exercised in contract test";} }''',
'es/clase/nochesuave/State.java':'''package es.clase.nochesuave;
import android.content.Context;
final class State { static boolean audio=true; static boolean global=true; static float gain=-20;
static boolean flag(Context c,String key) { return key.equals("audio")?audio:global; }
static float gain(Context c) { return gain; }
static Prefs prefs(Context c) {return new Prefs();}
static class Prefs { Prefs edit(){return this;} Prefs putString(String k,String v){return this;} void apply(){} }
}''',
'es/clase/nochesuave/Diagnostics.java':'''package es.clase.nochesuave;
import android.content.Context;
final class Diagnostics { static void event(Context c,String s) {} static void failure(Context c,String s,Throwable e) {} }''',
'android/media/audiofx/AudioEffect.java':'''package android.media.audiofx;
import java.util.*; import java.nio.*;
public class AudioEffect {
 public static final int SUCCESS=0;
 public static final UUID EFFECT_TYPE_DYNAMICS_PROCESSING=new UUID(0,1), EFFECT_TYPE_NULL=new UUID(0,0);
 public static int live=0, architecture=-1; public static float appliedGain=0; public static boolean toneStageEnabled=false;
 private boolean enabled=false, released=false;
 private final Map<String,byte[]> values=new HashMap<>();
 public AudioEffect(UUID type,UUID implementation,int priority,int session) { live++; }
 public interface OnControlStatusChangeListener { void onControlStatusChange(AudioEffect effect,boolean control); }
 public interface OnParameterChangeListener { void onParameterChange(AudioEffect effect,int status,byte[] param,byte[] value); }
 public void setControlStatusListener(OnControlStatusChangeListener listener) {}
 public void setParameterListener(OnParameterChangeListener listener) {}
 public int setEnabled(boolean state) { enabled=state; return 0; }
 public boolean getEnabled() { return enabled; }
 public void release() { if(!released) { live--; released=true; } }
 protected void checkStatus(int status) {
   if(status==-4) throw new UnsupportedOperationException("AudioEffect: invalid parameter operation");
   if(status<0) throw new IllegalStateException("AudioEffect: status "+status);
 }
 protected int getParameter(int[] param,int[] result) { if(param[0]==0x10) result[0]=2; return 0; }
 protected int getParameter(int[] param,byte[] result) { return getParameter(ints(param),result); }
 protected int getParameter(byte[] param,byte[] result) {
   byte[] stored=values.get(Arrays.toString(param));
   if(stored!=null) System.arraycopy(stored,0,result,0,Math.min(stored.length,result.length));
   return result.length;
 }
 protected int setParameter(int[] param,byte[] value) { return setParameter(ints(param),value); }
 protected int setParameter(byte[] param,byte[] value) {
   int key=byteArrayToInt(param,0);
   // EffectDynamicsProcessing.cpp: DP_changeVariant only creates variant 0.
   // With an empty engine, selecting variant 1 returns -EINVAL at architecture setup.
   if(key==0x30) { architecture=byteArrayToInt(value,0); if(architecture!=0) return -4; }
   if(key==0x20) appliedGain=byteArrayToFloat(value);
   if((key==0x40 || key==0x50 || key==0x60 || key==0x70) && byteArrayToInt(value,4)!=0) toneStageEnabled=true;
   values.put(Arrays.toString(param),value.clone()); return 0;
 }
 private static byte[] ints(int[] values) { ByteBuffer b=ByteBuffer.allocate(values.length*4).order(ByteOrder.nativeOrder()); for(int v:values)b.putInt(v); return b.array(); }
 protected static byte[] floatToByteArray(float v) { return ByteBuffer.allocate(4).order(ByteOrder.nativeOrder()).putFloat(v).array(); }
 protected static int byteArrayToInt(byte[] a,int off) { return ByteBuffer.wrap(a).order(ByteOrder.nativeOrder()).getInt(off); }
 protected static float byteArrayToFloat(byte[] a,int off) { return ByteBuffer.wrap(a).order(ByteOrder.nativeOrder()).getFloat(off); }
 protected static float byteArrayToFloat(byte[] a) { return byteArrayToFloat(a,0); }
}''',
'es/clase/nochesuave/AudioContractTest.java':'''package es.clase.nochesuave;
import android.content.Context; import android.media.audiofx.AudioEffect;
public class AudioContractTest {
 public static void main(String[] args) {
   AudioEngine engine=new AudioEngine(new Context()); engine.update();
   System.out.println(engine.status());
   if(engine.status().contains("UnsupportedOperationException")) throw new AssertionError("Unsupported architecture prevents applying audio gain");
   if(AudioEffect.architecture!=0 || AudioEffect.appliedGain!=-20) throw new AssertionError("Gain was not sent to supported engine");
   if(AudioEffect.toneStageEnabled) throw new AssertionError("A tone/dynamics stage was enabled instead of bypassed");
   State.gain=-40; engine.update(); if(AudioEffect.appliedGain!=-40) throw new AssertionError("Slider failed to update gain");
   State.audio=false; engine.update();
   // A constructor that failed in the platform can leave native resources until GC;
   // only assert lifecycle after a successful construction.
   if(AudioEffect.live!=0) throw new AssertionError("Audio effect leaked after disable");
   State.global=false; State.audio=true; engine.open(123); engine.update();
   if(AudioEffect.live!=1) throw new AssertionError("Announced session failed to attach");
   engine.close(123); if(AudioEffect.live!=0) throw new AssertionError("Closed session leaked effect");
   System.out.println("PASS: architecture, gain changes, disable/release and announced-session lifecycle (contract double, not device).");
 }
}'''
}
for name,source in FILES.items():
    target=OUT/name; target.parent.mkdir(parents=True,exist_ok=True); target.write_text(source,encoding='utf-8')
classes=OUT/'classes'; classes.mkdir(exist_ok=True)
jdk=Path(os.environ['JAVA_HOME'])/'bin' if os.environ.get('JAVA_HOME') else Path(shutil.which('javac') or '').resolve().parent
exe='.exe' if os.name=='nt' else ''
sources=[str(OUT/name) for name in FILES]+[str(ROOT/'tests/audio/reference/DynamicsProcessing.java'),str(ROOT/'app/src/es/clase/nochesuave/AudioEngine.java'),str(ROOT/'app/src/es/clase/nochesuave/AudioConfiguration.java')]
subprocess.run([str(jdk/('javac'+exe)),'-encoding','UTF-8','--release','8','-d',str(classes),*sources],check=True)
raise SystemExit(subprocess.run([str(jdk/('java'+exe)),'-cp',str(classes),'es.clase.nochesuave.AudioContractTest']).returncode)
