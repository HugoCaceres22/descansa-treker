# Compilar y probar

## Herramientas

- Python 3.10 o posterior.
- JDK 21 (define `JAVA_HOME`).
- Android SDK: plataforma **android-36** y **build-tools 36.0.0** (define `ANDROID_SDK_ROOT`, o `ANDROID_HOME`). Puedes instalarlos desde el SDK Manager de Android Studio.

La compilación publicada se prepara con herramientas oficiales: aapt2, javac, D8, zipalign y apksigner. No se descargan dependencias al ejecutar el script. El mínimo de Android es API 28 y el target de esta versión es API 35.

Ejemplo en PowerShell; adapta las rutas a tu instalación:

```powershell
$env:JAVA_HOME = 'C:\Herramientas\jdk-21'
$env:ANDROID_SDK_ROOT = 'C:\Herramientas\android-sdk'
python build.py
```

El resultado es `dist/DescansaTreker-0.2.4-dev.apk`. El script genera una clave propia de desarrollo en `.private/`, que está excluida de Git. Conserva esa carpeta si quieres actualizar tus propias compilaciones sin desinstalar.

**Esta clave de desarrollo no es la firma de la APK oficial.** Una compilación propia no puede actualizar la APK publicada encima de ella. Para cambiar entre ambas tendrás que desinstalar la anterior, perdiendo sus ajustes y permisos. No incluyas claves privadas en un repositorio.

## Pruebas locales sin sonido

```powershell
python tests/run.py
python tests/audio/run.py
```

La primera prueba comprueba el analizador de sesiones multimedia. La segunda ejecuta el motor con la clase oficial de AOSP y un doble del contrato nativo: verifica configuración, cambios de ganancia y liberación de efectos. Ninguna reproduce audio ni conecta con el móvil.

Estas pruebas y una compilación correcta no demuestran atenuación acústica en otro dispositivo. Esa parte requiere una comprobación voluntaria en el teléfono.

## Verificar la descarga

En PowerShell:

```powershell
Get-FileHash .\apk\DescansaTreker-0.2.4.apk -Algorithm SHA256
```

Compara el resultado con `apk/DescansaTreker-0.2.4.sha256` o con el archivo de la misma versión en Releases. Para verificar la firma, utiliza apksigner del Android SDK. El checksum identifica el archivo publicado; no convierte una compilación local en una copia con la firma oficial.
