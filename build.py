"""Reproducible APK build using only official JDK and Android SDK tools."""
from pathlib import Path
import json
import os
import secrets
import shutil
import subprocess
import zipfile
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parent
TOOLS = ROOT / 'tools'
JAVA_HOME = os.environ.get('JAVA_HOME')
JDK = Path(JAVA_HOME) / 'bin' if JAVA_HOME else Path(shutil.which('javac') or '').resolve().parent
SDK = Path(os.environ.get('ANDROID_SDK_ROOT') or os.environ.get('ANDROID_HOME') or TOOLS / 'android-sdk')
EXE = '.exe' if os.name == 'nt' else ''
if not (JDK / ('javac' + EXE)).is_file():
    raise RuntimeError('Instala JDK 21 y configura JAVA_HOME')
BT = SDK / 'build-tools' / '36.0.0'
ANDROID = SDK / 'platforms' / 'android-36' / 'android.jar'
BUILD = ROOT / 'build'
DIST = ROOT / 'dist'
for folder in (BUILD, BUILD / 'gen', BUILD / 'classes', BUILD / 'dex', DIST):
    folder.mkdir(parents=True, exist_ok=True)
# Do not carry obsolete anonymous classes or resource identifiers into a new APK.
for output_name in ('gen', 'classes', 'dex'):
    output_dir = (BUILD / output_name).resolve()
    if not output_dir.is_relative_to(BUILD.resolve()):
        raise RuntimeError('Unsafe build output directory')
    for generated in output_dir.rglob('*'):
        if generated.is_file():
            if not generated.resolve().is_relative_to(output_dir):
                raise RuntimeError('Unsafe generated file path')
            generated.unlink()
env = os.environ.copy()
env['JAVA_HOME'] = str(JDK.parent)
env['PATH'] = str(JDK) + os.pathsep + env.get('PATH', '')

def run(*args):
    subprocess.run([str(arg) for arg in args], cwd=ROOT, env=env, check=True)

run(BT / ('aapt2' + EXE), 'compile', '--dir', ROOT / 'app/res', '-o', BUILD / 'compiled.zip')
run(BT / ('aapt2' + EXE), 'link', '-I', ANDROID, '--manifest', ROOT / 'app/AndroidManifest.xml',
    '--java', BUILD / 'gen', '-o', BUILD / 'resources.apk', BUILD / 'compiled.zip')
sources = sorted((ROOT / 'app/src').rglob('*.java')) + sorted((BUILD / 'gen').rglob('*.java'))
args_file = BUILD / 'javac.args'
args_file.write_text('\n'.join('"' + str(path).replace('\\', '/') + '"' for path in sources), encoding='utf-8')
run(JDK / ('javac' + EXE), '-encoding', 'UTF-8', '--release', '8', '-classpath', ANDROID,
    '-d', BUILD / 'classes', '@' + str(args_file))
classes_jar = BUILD / 'classes.jar'
with zipfile.ZipFile(classes_jar, 'w', zipfile.ZIP_DEFLATED) as archive:
    for path in sorted((BUILD / 'classes').rglob('*.class')):
        archive.write(path, path.relative_to(BUILD / 'classes').as_posix())
run(JDK / ('java' + EXE), '-cp', BT / 'lib/d8.jar', 'com.android.tools.r8.D8', '--min-api', '28',
    '--lib', ANDROID, '--output', BUILD / 'dex', classes_jar)
unsigned = BUILD / 'unsigned.apk'
with zipfile.ZipFile(BUILD / 'resources.apk') as resources, zipfile.ZipFile(unsigned, 'w', zipfile.ZIP_DEFLATED) as archive:
    for entry in resources.infolist():
        archive.writestr(entry, resources.read(entry.filename))
    for path in (BUILD / 'dex').glob('*.dex'):
        archive.write(path, path.name)
run(BT / ('zipalign' + EXE), '-f', '-p', '4', unsigned, BUILD / 'aligned.apk')
private = ROOT / '.private'
private.mkdir(exist_ok=True)
credentials = private / 'signing.json'
keystore = private / 'descansa-treker-development.jks'
if not credentials.exists():
    if keystore.exists() or any(DIST.glob('*.apk')):
        raise RuntimeError('No se puede regenerar la firma: recupera signing.json de la copia de seguridad.')
    credentials.write_text(json.dumps({'password': secrets.token_urlsafe(32)}), encoding='utf-8')
env['NOCHE_SIGNING_PASSWORD'] = json.loads(credentials.read_text(encoding='utf-8'))['password']
if not keystore.exists():
    if any(DIST.glob('*.apk')):
        raise RuntimeError('Falta la clave de firma. Recupera la copia de seguridad para mantener las actualizaciones.')
    run(JDK / ('keytool' + EXE), '-genkeypair', '-keystore', keystore, '-storetype', 'JKS',
        '-storepass:env', 'NOCHE_SIGNING_PASSWORD', '-keypass:env', 'NOCHE_SIGNING_PASSWORD',
        '-alias', 'nochesuave', '-keyalg', 'RSA', '-keysize', '2048', '-validity', '10000',
        '-dname', 'CN=Descansa Treker Development')
version = ET.parse(ROOT / 'app/AndroidManifest.xml').getroot().attrib['{http://schemas.android.com/apk/res/android}versionName']
apk = DIST / f'DescansaTreker-{version}-dev.apk'
run(JDK / ('java' + EXE), '-jar', BT / 'lib/apksigner.jar', 'sign', '--ks', keystore, '--ks-key-alias', 'nochesuave',
    '--ks-pass', 'env:NOCHE_SIGNING_PASSWORD', '--key-pass', 'env:NOCHE_SIGNING_PASSWORD',
    '--out', apk, BUILD / 'aligned.apk')
run(JDK / ('java' + EXE), '-jar', BT / 'lib/apksigner.jar', 'verify', '--verbose', '--print-certs', apk)
run(BT / ('zipalign' + EXE), '-c', '-v', '4', apk)
print(f'APK: {apk}')
