"""Parser tests on the desktop JVM; no device or audio playback."""
from pathlib import Path
import os, shutil, subprocess
root=Path(__file__).resolve().parents[1]
jdk=Path(os.environ['JAVA_HOME'])/'bin' if os.environ.get('JAVA_HOME') else Path(shutil.which('javac') or '').resolve().parent
exe='.exe' if os.name=='nt' else ''
out=root/'build/parser-tests';out.mkdir(parents=True,exist_ok=True)
subprocess.run([str(jdk/('javac'+exe)),'-encoding','UTF-8','--release','8','-d',str(out),str(root/'app/src/es/clase/nochesuave/PlaybackSessions.java'),str(root/'tests/PlaybackSessionsTest.java')],check=True)
subprocess.run([str(jdk/('java'+exe)),'-cp',str(out),'es.clase.nochesuave.PlaybackSessionsTest'],check=True)
