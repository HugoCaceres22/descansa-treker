package es.clase.nochesuave;

import java.util.*;
import java.util.regex.*;

/** Parses only current media/game player records, never the AudioService event history. */
final class PlaybackSessions {
    private static final Pattern SESSION=Pattern.compile("\\bsessionId:(\\d+)\\b");
    private static final Pattern UID=Pattern.compile("\\bu/pid:(\\d+)/(\\d+)\\b");
    private static final Pattern STATE=Pattern.compile("\\bstate:(started|paused)\\b");
    private static final Pattern USAGE=Pattern.compile("\\busage=(USAGE_MEDIA|USAGE_GAME)\\b");
    static Set<Integer> parse(String dump,int ownUid) {
        Set<Integer> ids=new LinkedHashSet<>();
        for(String line:dump.split("\\r?\\n")) {
            if(!line.trim().startsWith("AudioPlaybackConfiguration piid:")) continue;
            if(!STATE.matcher(line).find() || !USAGE.matcher(line).find()) continue;
            Matcher uid=UID.matcher(line),session=SESSION.matcher(line);
            if(!uid.find() || !session.find()) continue;
            try {
                int owner=Integer.parseInt(uid.group(1)),id=Integer.parseInt(session.group(1));
                if(owner>=10000 && owner!=ownUid && id>0 && ids.size()<16) ids.add(id);
            } catch(NumberFormatException ignored) {}
        }
        return ids;
    }
}
