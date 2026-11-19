# Activar la detección de audio

La atenuación necesita encontrar la sesión de audio del reproductor. Algunas apps la anuncian directamente; otras, como YouTube en el teléfono probado, necesitan la detección avanzada. Para ello hay que conceder **una vez en cada teléfono** el permiso `android.permission.DUMP` desde un ordenador. No requiere root y no se concede con el cuadro normal de permisos de Android.

Este permiso permite consultar información de diagnóstico del sistema. Concédelo solo a una APK de confianza.

## 1. Preparar el móvil y el ordenador

1. Instala y abre Descansa Treker en el móvil.
2. Descarga las [Android SDK Platform-Tools oficiales](https://developer.android.com/tools/releases/platform-tools) y extrae el ZIP en el ordenador.
3. En el móvil, activa las Opciones de desarrollador. Normalmente se habilitan pulsando siete veces el número de compilación o la versión del sistema dentro de Información del teléfono.
4. En Opciones de desarrollador, activa **Depuración USB**.
5. Conecta el móvil con un cable que transfiera datos, desbloquéalo y acepta el aviso de autorización del ordenador. Si no se detecta, prueba el modo Transferencia de archivos y otro cable o puerto. En Windows algunos fabricantes requieren su controlador USB.

No hace falta iniciar vídeos, tonos ni ninguna reproducción para conceder el permiso.

## 2. Conceder el permiso

En Windows, abre PowerShell dentro de la carpeta extraída `platform-tools` y ejecuta:

```powershell
.\adb.exe devices
```

El móvil debe aparecer con estado `device`. Si pone `unauthorized`, acepta la autorización en el teléfono y repite la comprobación.

Después ejecuta:

```powershell
.\adb.exe shell pm grant es.clase.nochesuave android.permission.DUMP
```

Si funciona, normalmente no muestra ningún mensaje. Para comprobarlo:

```powershell
.\adb.exe shell dumpsys package es.clase.nochesuave | Select-String 'android.permission.DUMP'
```

Busca `android.permission.DUMP: granted=true` en el estado de permisos. En macOS o Linux utiliza `./adb` en lugar de `.\adb.exe`; puedes revisar el informe completo con `./adb shell dumpsys package es.clase.nochesuave`.

## 3. Activarlo en la app

1. Vuelve a Descansa Treker y activa la opción de **detección avanzada de sesiones** en Ajustes.
2. Activa el audio y ajusta la atenuación. Deja el método global experimental desactivado inicialmente: el teléfono probado rechaza ese motor global.
3. Cuando tú quieras probarlo, reproduce tu contenido habitual, mantén fijo el volumen del sistema y compara 0 dB con −20 dB. Dale unos segundos para detectar la sesión. Desactiva otros ecualizadores durante la comprobación para evitar conflictos.

La detección consulta las sesiones aproximadamente cada 4 segundos mientras hay música y cada 8 segundos en espera, únicamente con el audio de la app activado y la sesión de uso abierta. No captura las muestras de sonido.

**Después puedes desconectar el cable y desactivar la depuración USB e inalámbrica.** El permiso permanece concedido; no necesitas el ordenador ni Tailscale para usar la app. Si activaste un ajuste de seguridad adicional para ADB, también puedes desactivarlo tras comprobar el permiso. Desinstalar la app elimina la concesión.

## Si falla

- **`unauthorized`**: desbloquea el móvil y acepta el aviso RSA del ordenador.
- **No aparece ningún dispositivo**: revisa cable de datos, modo USB y controlador del fabricante.
- **`Unknown package`**: comprueba que la APK esté instalada en el usuario principal del teléfono.
- **`GRANT_RUNTIME_PERMISSIONS` o permiso denegado al concederlo**: algunos fabricantes restringen las operaciones de ADB. En Xiaomi/POCO puede ser necesario activar además **Depuración USB (Ajustes de seguridad)**, distinta de Depuración USB normal. Después reconecta el cable y vuelve a autorizarlo. La disponibilidad de ese ajuste varía por versión. No es un problema que se solucione cambiando la barra de volumen.
- **No existe ese ajuste**: no todos los modelos lo incluyen. Consulta la documentación de tu fabricante. No cambies opciones de seguridad al azar: puede que ese firmware no permita la concesión con este procedimiento.
- **Se concede DUMP pero no cambia el audio**: revisa el diagnóstico de la app con el reproductor activo. El fabricante puede impedir aplicar efectos a sesiones ajenas o usar una ruta de audio incompatible. No podemos garantizarlo para todos los teléfonos.

La depuración inalámbrica también existe, pero depende de la red, el emparejamiento y las restricciones del fabricante. El procedimiento recomendado aquí es USB, que fue el utilizado con éxito en el teléfono probado.

## Revocar el permiso

Si quieres retirarlo sin desinstalar:

```powershell
.\adb.exe shell pm revoke es.clase.nochesuave android.permission.DUMP
```

La detección avanzada dejará de funcionar; el filtro de pantalla no depende de DUMP. Después desactiva otra vez la depuración.

Referencias: [ADB oficial](https://developer.android.com/tools/adb), [detección de audio y permiso DUMP en Precise Volume](https://precisevolume.phascinate.com/docs/better-audio-detection/).
