<p align="center"><img src="app/res/drawable/dog_logo.png" width="150" alt="Treker, un American Bully negro con una mancha blanca en el pecho"></p>

# Descansa Treker

Una app para bajar el brillo por debajo del mínimo y ajustar el volumen multimedia con más precisión. Diseño negro y amarillo, sin anuncios ni conexión a Internet.

**Versión estable: 0.2.4 · Android 9 o posterior.** Probada por su usuario en un POCO X7 Pro con Android 16 y HyperOS 3, usando principalmente YouTube y Redmi Buds 3. La compatibilidad del audio depende del teléfono y del reproductor.

[Descargar APK](https://github.com/HugoCaceres22/descansa-treker/releases/download/v0.2.4/DescansaTreker-0.2.4.apk) · [Activar el volumen paso a paso](docs/ACTIVAR-VOLUMEN.md) · [Compilar el proyecto](docs/COMPILAR.md)

## Qué hace

- Atenúa la pantalla con un filtro negro neutro, sin añadir un tono cálido. La barra solo reduce la luminosidad: 100 % significa sin atenuación adicional. A intensidades muy bajas puede perderse detalle por los límites del panel y del filtro.
- Usa una superposición de accesibilidad para cubrir también ajustes y notificaciones donde Android lo permita. Algunas pantallas protegidas o capas del fabricante pueden comportarse de otra forma.
- Permite regular el audio entre **−50 y 0 dB**, en pasos de 0,5 dB. Puedes subir desde un nivel atenuado hasta el original; no amplifica por encima de 0 dB. Está pensado para multimedia, con Bluetooth, altavoz o cable según la compatibilidad del dispositivo.
- Ofrece controles independientes y botones para activar o apagar ambos efectos.
- Al tocar la notificación aparece una barra pequeña de brillo en el centro de la pantalla, sin salir del vídeo. Se cierra al tocar fuera, pulsar la X o esperar unos segundos. El volumen se configura dentro de la app.
- Incluye un temporizador de apagado: 10 minutos, media hora, 2 horas, 5 horas o un tiempo personalizado de 1 a 1440 minutos. Apaga los efectos, no el teléfono.

## Instalar y empezar

1. Descarga la APK de la sección [Releases](https://github.com/HugoCaceres22/descansa-treker/releases). Autoriza la instalación desde el navegador o gestor de archivos si Android lo pide.
2. Abre Descansa Treker y permite sus notificaciones.
3. Desde los ajustes de la app, abre Accesibilidad y activa **Descansa Treker · filtro de pantalla**. Si Android bloquea el acceso, ve a la información de la aplicación y busca **Permitir ajustes restringidos** en el menú. Su ubicación cambia según el fabricante. [Ayuda oficial de Android](https://support.google.com/android/answer/12623953?hl=es).
4. Para el temporizador, concede el acceso a alarmas o temporizadores exactos si se solicita.
5. En móviles que cierren la app durante su uso, revisa las restricciones de batería del fabricante.
6. Para el audio de YouTube y otros reproductores que no anuncian su sesión, completa la [activación del volumen por USB](docs/ACTIVAR-VOLUMEN.md). **Instalar la APK por sí solo no concede ese permiso.**

Después ajusta las barras y activa los efectos que quieras utilizar. El método global experimental es una alternativa de compatibilidad; en el teléfono probado se utiliza la detección avanzada de sesiones, porque el método global es rechazado por el sistema.

## Cuándo sigue funcionando

Abrir otra aplicación, ir a Inicio o bloquear la pantalla mantiene los efectos que hayas activado. No se activan automáticamente al abrir YouTube.

**Al quitar Descansa Treker de recientes se apagan los efectos y se cancela el temporizador.** El servicio de uso se retira poco después. La conexión de accesibilidad puede permanecer disponible, sin mantener el filtro ni el procesamiento de audio activos.

Mientras la sesión de uso siga abierta aparece una notificación, incluso si los dos efectos están apagados. Sirve para acceder al brillo y mantener estable el servicio en HyperOS. Para terminar la sesión, quita la app de recientes.

## Actualizaciones y compartir

Puedes pasar la APK a otra persona. Esa persona tendrá que conceder los permisos en su propio móvil, incluido DUMP mediante ADB para la detección avanzada de audio. No se garantiza el mismo resultado en todos los modelos.

Una nueva APK con el mismo paquete y la misma firma se instala como actualización, normalmente conservando ajustes y permisos. No hace falta desinstalar antes. Si desinstalas, tendrás que configurar de nuevo la app y conceder DUMP otra vez. Esta versión no tiene actualizador automático: las nuevas versiones se descargan desde Releases.

## Privacidad y permisos

La app no solicita Internet ni micrófono y no graba audio. El servicio de accesibilidad no solicita leer el contenido de las ventanas. El permiso especial DUMP permite consultar diagnósticos del sistema: aquí se usa para encontrar las sesiones multimedia a las que aplicar la atenuación. El informe completo no se guarda ni se envía. La detección se detiene al desactivar el audio o cerrar la sesión de uso.

Los diagnósticos locales pueden incluir datos del dispositivo y errores. Revísalos antes de compartirlos públicamente.

## Código y pruebas

El paquete conserva su identificador original: `es.clase.nochesuave`. Se compila con herramientas oficiales de Java y Android, sin Gradle. Consulta [cómo compilar y ejecutar las pruebas](docs/COMPILAR.md).

La APK publicada conserva su firma original. Las claves privadas, herramientas descargadas, credenciales y registros del dispositivo no están en este repositorio. Consulta los [avisos de terceros](THIRD_PARTY_NOTICES.md).
