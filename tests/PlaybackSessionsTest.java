package es.clase.nochesuave;
import java.util.*;
public class PlaybackSessionsTest {
    private static String player(int uid,int session,String state,String usage) {
        return "  AudioPlaybackConfiguration piid:24471 deviceIds:[3] type:android.media.AudioTrack u/pid:"+uid+"/1959 state:"+state+" attr:AudioAttributes: usage="+usage+" content=CONTENT_TYPE_MUSIC flags=0xA00 sessionId:"+session+" mutedState:none";
    }
    private static void expect(String input,int own,int... ids) {
        Set<Integer> expected=new LinkedHashSet<>(); for(int id:ids) expected.add(id);
        Set<Integer> actual=PlaybackSessions.parse(input,own);
        if(!actual.equals(expected)) throw new AssertionError("Expected "+expected+" got "+actual);
    }
    public static void main(String[] args) {
        expect(player(10364,22265,"paused","USAGE_MEDIA"),99999,22265);
        expect(player(10223,22355,"started","USAGE_MEDIA"),99999,22355);
        expect(player(10223,22355,"started","USAGE_GAME"),99999,22355);
        expect(player(10223,22355,"stopped","USAGE_MEDIA"),99999);
        expect(player(10223,22355,"idle","USAGE_MEDIA"),99999);
        expect(player(10223,22355,"started","USAGE_ALARM"),99999);
        expect(player(10223,22355,"started","USAGE_NOTIFICATION"),99999);
        expect(player(10223,22355,"started","USAGE_VOICE_COMMUNICATION"),99999);
        expect(player(1000,22355,"started","USAGE_MEDIA"),99999);
        expect(player(99999,22355,"started","USAGE_MEDIA"),99999);
        expect(player(10223,0,"started","USAGE_MEDIA"),99999);
        expect("11-18 new player piid:8 uid/pid:10223/123 session:22355",99999);
        expect("AudioRecordingConfiguration piid:24471 u/pid:10223/1959 state:started usage=USAGE_MEDIA sessionId:22355",99999);
        expect(player(10223,22355,"started","USAGE_MEDIA")+"\n"+player(10223,22355,"paused","USAGE_MEDIA"),99999,22355);
        expect("AudioPlaybackConfiguration piid:9 state:started usage=USAGE_MEDIA sessionId:22355",99999);
        expect(player(10223,22355,"started","USAGE_MEDIA").replace("sessionId:22355","sessionId:99999999999999999999"),99999);
        System.out.println("PASS: 16 playback-session parsing checks, including device format, filtering and history exclusion.");
    }
}
