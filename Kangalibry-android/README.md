# Kangalibry (TurKids) — Türkçe · Español

App móvil Android nativa (Kotlin + Jetpack Compose) para el aprendizaje inicial de español de niños y niñas de habla turca menores de 10 años. Funciona como un juego tipo Duolingo: se muestra una palabra con su emoji y el usuario elige entre 3 alternativas.

| Dato | Valor |
|---|---|
| Nombre en pantalla | Kangalibry |
| `rootProject.name` / `applicationId` | `TurKids` / `com.example.turkids` |
| Asignatura | DSY1105 · Desarrollo de Aplicaciones Móviles |
| Evaluación | Evaluación Parcial 3 (indicadores IE 3.1.1 a IE 3.3.2) |
| Contenido | Ficticio, de prueba, sin datos personales reales |

---

## 1. Funcionalidades

| Pantalla | Archivo | Descripción |
|---|---|---|
| Práctica | `PracticaScreen.kt` | Elección del idioma de práctica (Turco / Español). |
| Home | `HomeScreen.kt` | Perfil de prueba, idioma, indicador de ubicación y unidades con estrellas. Menú lateral. Adaptable a 1, 2 o 3 columnas. |
| Lección | `LeccionScreen.kt` | Barra de progreso, emoji, botón **Escuchar** (TTS), 3 opciones y **Continuar**. |
| Resumen | `ResumenScreen.kt` | Estrellas, aciertos y confeti. |
| Registro / ResumenRegistro | `RegistroScreen.kt`, `ResumenRegistroScreen.kt` | Formulario validado en tiempo real. |
| Test oral | `TestOralScreen.kt` | Juego de 5 palabras con reconocimiento de voz. |
| Publicaciones | `PostScreen.kt` | CRUD contra el microservicio propio con Retrofit: lista (**GET**), crear (**POST**), editar con un diálogo (**PUT**) y eliminar (**DELETE**). |
| Acerca de | `AcercaDeScreen.kt` | Descripción del proyecto. |

```
Práctica ──> Home ──> Unidad ──> Lección ──> Resumen ──> Home
               ├─ Menú: Acerca de · Registro · Test oral · Publicaciones (API) · Reiniciar progreso
               └─ "Cambiar" ──> Práctica
Registro ──> ResumenRegistro ──> Home
```

### Reglas de la lección (`LeccionViewModel`)
- **Acierto a la primera:** suma un acierto y aparece **Continuar**.
- **Primer error:** la opción se descarta y se da una **segunda oportunidad** (el error no castiga). Una opción ya descartada no gasta la oportunidad.
- **Segundo error:** se muestra la respuesta correcta, no suma y el flujo continúa.
- **Retoma de la lección:** el punto de avance (unidad, semilla, pregunta y aciertos) se guarda al pulsar **Continuar** y se borra al terminar; al volver a la misma unidad e idioma se continúa donde quedó. Un índice guardado fuera de rango se acota a la última pregunta, y una lección guardada de otra unidad se ignora.
- **Protecciones:** responder o continuar antes de que cargue la lección no hace nada, y un doble toque en **Continuar** no salta preguntas ni finaliza dos veces.
- **Estrellas** (`calcularEstrellas`): todas correctas = 3, mitad o más = 2, menos = 1, sin preguntas = 0. Se conserva el mejor resultado por unidad, perfil e idioma.

---

## 2. Arquitectura

Patrón **MVVM** con una sola actividad, **StateFlow** para el estado, **corrutinas** (`viewModelScope`, `suspend`, `Flow`) y navegación por eventos. Las dependencias se crean en `MainActivity` y se entregan por constructor con `TurKidsViewModelFactory`.

```
app/src/main/java/com/example/turkids/
├── MainActivity.kt              raíz de composición, NavHost y permiso de ubicación
├── data/
│   ├── model/                   Post, PaisJson
│   └── remote/                  ApiService + RetrofitInstance (microservicio backend-turkids)
│                                PaisesApiService + RetrofitPaises (REST Countries)
├── model/                       estados de UI, BancoDeContenido, ValidadorUsuario, etc.
├── navigation/                  Screen (rutas) y NavigationEvent
├── repository/                  ProgresoRepository (DataStore), UbicacionRepository,
│                                ProveedoresUbicacion, PostRepository, PaisRepository
├── viewmodel/                   Leccion, Progreso, Usuario, Ubicacion, TestOral, Post, Main + Factory
└── ui/
    ├── screens/                 pantallas Compose
    ├── theme/                   Color, Theme, Type
    └── utils/                   Narrador (TTS), ReconocedorDeVoz, ObservarUbicacion, etc.
```

- **Capas:** las pantallas solo conocen ViewModels; los ViewModels solo reciben repositorios o el validador; solo los repositorios hablan con Android o con la red.
- **Navegación:** las pantallas llaman a `MainViewModel.navigateTo(...)`, que emite un `NavigationEvent`; `MainActivity` lo ejecuta sobre el `NavController`.
- **DataStore (Preferences):** `ProgresoRepository` guarda perfil activo, idioma, estrellas (`estrellas_{perfil}_{IDIOMA}_{unidad}`) y lección en curso (`leccion_{perfil}_{IDIOMA}_…`). Las lecturas son `Flow` y las escrituras `suspend`. El progreso de cada idioma es independiente. No se guardan datos del formulario de registro.

### Formulario de registro
`ValidadorUsuario` (sin dependencias de Android) define las reglas y `UsuarioViewModel` mantiene el estado. El error de un campo aparece al salir de él (o al pulsar **Registrar**) y desde ahí se actualiza mientras se escribe.

| Campo | Regla |
|---|---|
| Nombre / Dirección | No vacío |
| Correo | Formato de correo válido |
| Contraseña | Mínimo 6 caracteres |
| Términos | Deben estar marcados |

---

## 3. Recursos nativos

### Ubicación (GPS / Fused / red)
- Permisos: `ACCESS_COARSE_LOCATION` y `ACCESS_FINE_LOCATION` (con `uses-feature` no obligatorio). El diálogo nativo se muestra al iniciar.
- `UbicacionRepository` usa `LocationManager` y `Geocoder`. `elegirProveedores` decide el orden:
  - **Ubicación aproximada** (solo COARSE): proveedor **fused** en Android 12+ y proveedor de **red**; nunca GPS.
  - **Ubicación precisa:** además se usa **GPS**.
  - Si ningún proveedor responde en 6 s se usa la última ubicación conocida (incluido el proveedor pasivo).
- `UbicacionViewModel` expone `Verificando`, `NoEnUso` y `EnUso`, más el motivo (`ProblemaUbicacion`): permiso denegado, permiso bloqueado (solo Ajustes) o ubicación del dispositivo desactivada. `ObservarUbicacion` reconsulta al volver a la app y al encender o apagar la ubicación.
- Con el código del país se consulta REST Countries para mostrar bandera y nombre; si falla se usa el nombre del geocodificador o las coordenadas. La app no guarda la ubicación ni la envía a servicios propios.

### Síntesis de voz (TTS)
`Narrador` envuelve `TextToSpeech` (turco `tr-TR`, español `es-US`). Se reproduce al aparecer la pregunta y con **Escuchar**, y se detiene al salir de la app. El manifest declara `<queries>` para el servicio TTS (Android 11+).

### Reconocimiento de voz
`ReconocedorDeVoz` envuelve `SpeechRecognizer` (permiso `RECORD_AUDIO`, pedido al primer toque en **Hablar**). `TestOralViewModel` contiene la lógica del juego y `coincideRespuesta` compara lo dicho sin tildes ni mayúsculas. Si se rechaza el permiso o el teléfono no tiene reconocimiento de voz, se muestra un mensaje en vez de fallar.

---

## 4. Servicios web: microservicio propio y API externa (Retrofit)

La app usa **dos** Retrofit independientes, cada uno con su propio `Retrofit.Builder`, interfaz y repositorio, para que no se interfieran:

| | Microservicio propio | API externa pública |
|---|---|---|
| Servicio | `backend-turkids` (Spring Boot, ver `../backend-turkids`) | REST Countries |
| Cliente | `RetrofitInstance` + `ApiService` | `RetrofitPaises` + `PaisesApiService` |
| URL base | `http://10.0.2.2:8080/` | `https://restcountries.com/` |
| Uso en la app | Pantalla **Publicaciones** (CRUD) | Bandera y nombre del país en el Home |
| Repositorio | `PostRepository` | `PaisRepository` |

### Microservicio propio (Publicaciones)

```
PostScreen ──> PostViewModel ──> PostRepository ──> ApiService (Retrofit + Gson) ──HTTP──> backend-turkids
   (Compose)   (StateFlow<PostUiState>)  (inyectable, mockeable)   (suspend)
```

| Acción en la UI | Método | Endpoint | `PostViewModel` |
|---|---|---|---|
| Carga inicial / **Reintentar** | `GET` | `/posts` | `fetchPosts()` |
| Botón **Crear post (POST)** | `POST` | `/posts` | `crearPost()` |
| Botón **Editar (PUT)** de cada tarjeta (abre el diálogo de edición) | `PUT` | `/posts/{id}` | `actualizarPost(post)` |
| Botón **Eliminar (DELETE)** de cada tarjeta | `DELETE` | `/posts/{id}` | `eliminarPost(id)` |

**Edición de una publicación:** al pulsar **Editar (PUT)** se abre un `AlertDialog` con dos campos, **Título** y **Contenido**, ya rellenos con los valores actuales. El botón **Guardar** solo se habilita si ninguno de los dos queda vacío (se ignoran los espacios en blanco). Al guardar, la pantalla arma el post con el título y el contenido escritos (sin recortes ni textos agregados) y `PostViewModel.actualizarPost` lo envía tal cual con `PUT /posts/{id}`; la respuesta del servidor reemplaza el post en la lista. **Cancelar** o tocar fuera del diálogo lo cierra sin enviar nada.

El modelo JSON es el mismo en la app (`data/model/Post.kt`) y en el servidor: `userId`, `id`, `title`, `body`. Como el servicio guarda en memoria, los cambios persisten mientras el servidor esté encendido y se ven al pulsar **Reintentar** (que vuelve a hacer el `GET`).

**Estados (`PostUiState`)**
- **Carga de la lista:** `cargando` (indicador), éxito (`posts`) o `error` con botón **Reintentar**. El mensaje distingue falta de conexión (`IOException`), error del servidor (`HttpException` con su código) y otros errores.
- **Escrituras:** `operacionEnCurso` desactiva los botones mientras se ejecuta una; el resultado llega en `mensaje` y se muestra en un `Snackbar`. Si una escritura falla, la lista no cambia.

**Cómo probarlo**
1. Levantar el microservicio (`cd ../backend-turkids && ./gradlew bootRun`, ver su README).
2. Ejecutar la app en un **emulador** de Android Studio: `10.0.2.2` es el `localhost` del PC visto desde el emulador.
3. En un **celular real** cambiar `BASE_URL` en `RetrofitInstance.kt` por la IP del PC en la red local (por ejemplo `http://192.168.1.50:8080/`); ambos deben estar en la misma red y el firewall debe permitir el puerto 8080.
4. Si el servidor no está encendido, la pantalla muestra el error de conexión con **Reintentar**.

**HTTP en texto plano:** el manifest declara `android:usesCleartextTraffic="true"` porque el microservicio local usa `http://`. Es solo para desarrollo; en producción el servicio debe ir por HTTPS y esa opción debe quitarse.

### API externa (REST Countries)
Se consulta con el código de país de la ubicación para mostrar bandera y nombre (ver sección 3). Usa HTTPS, su propio cliente y su propio repositorio, y si falla la app sigue funcionando con el nombre del geocodificador o las coordenadas.

---

## 5. Testing

Pruebas unitarias con **JUnit 5 (Jupiter)**, **Kotest**, **MockK** y `kotlinx-coroutines-test`; las pruebas JUnit 4 anteriores siguen corriendo por el motor *vintage*. No necesitan emulador.

**Estado actual:** los **115 tests unitarios de la app Android pasan** con `./gradlew testDebugUnitTest`, y los **5 tests con MockMvc** del microservicio `backend-turkids` también (`./gradlew test` dentro de esa carpeta).
```bash
./gradlew testDebugUnitTest          # Windows: gradlew.bat testDebugUnitTest
```
Reporte: `app/build/reports/tests/testDebugUnitTest/index.html`. En Android Studio: clic derecho en `app/src/test` → *Run Tests in 'test'*.

| Archivo | Qué valida |
|---|---|
| `LeccionViewModelTest` | Acierto a la primera, segunda oportunidad, dos errores sin bloquear el flujo, guardado del punto de retoma, retoma de una lección guardada (misma semilla, pregunta y aciertos), índice fuera de rango, lección de otra unidad, finalización y protecciones (antes de cargar, sin responder, doble toque). |
| `ProgresoRepositoryTest` | DataStore **en memoria** (`InMemoryPreferencesDataStore`, definido en el mismo test; evita el error de renombrado de archivos de DataStore en Windows): estrellas guardadas y leídas, independencia por idioma y por perfil, mejor resultado, perfil e idioma activos, reinicio y lección en curso. |
| `UsuarioViewModelTest` | Validación reactiva del registro: sin errores al inicio, error al salir del campo, actualización mientras se escribe, clave, nombre, dirección, términos, `validarFormulario`, `limpiar` y delegación en el validador (MockK). |
| `PostRepositoryTest` | `getPosts`, `createPost`, `updatePost` y `deletePost` con `ApiService` mockeado; propagación de errores. |
| `PostViewModelTest` | Estados de carga, éxito, error y reintento; `POST` (respeta los ids del servidor), `PUT` (envía el post editado con su título y contenido nuevos y lo reemplaza en la lista) y `DELETE`, cada uno en éxito y en error, y `limpiarMensaje`. |
| `ValidadorUsuarioTest`, `ProgresoTest`, `BancoDeContenidoTest`, `ClavesProgresoTest` | Reglas del formulario, estrellas, generación de preguntas y claves de DataStore. |
| `ProveedoresUbicacionTest`, `UbicacionEstadoTest`, `PaisTest`, `PaisRepositoryTest` | Elección de proveedores (fused/red/GPS), textos de ubicación, país y bandera. |
| `RespuestaOralTest`, `TestOralViewModelTest` | Comparación de lo dicho y lógica del test oral. |

### Pruebas del microservicio (`backend-turkids`)
`PostControllerTest` usa MockMvc sobre el contexto de Spring Boot y valida 5 casos: `GET /posts` (4 publicaciones precargadas), `POST` (id autoincremental desde 5, responde 201), `PUT` (actualiza usando el id de la ruta), `PUT` de un id inexistente (404) y `DELETE` (200 y luego 404).
```bash
cd ../backend-turkids
./gradlew test               # Windows: gradlew.bat test
```

### Pruebas instrumentadas (requieren emulador o celular)
`PostScreenTest` renderiza `PostScreen` con una API falsa: lista de posts, error con **Reintentar** y creación con **Crear post (POST)**.
```bash
./gradlew connectedDebugAndroidTest   # Windows: gradlew.bat connectedDebugAndroidTest
```

---

## 6. Generación del APK firmado (release)

`app/build.gradle.kts` define el `signingConfig` **release** leyendo las credenciales de `keystore.properties` (raíz del proyecto) o de variables de entorno. Las contraseñas nunca van en el código. Si no hay credenciales, `assembleRelease` genera un APK **sin firmar** en lugar de fallar.

### Paso 1 · Crear el keystore (una sola vez)
```bash
mkdir keystore
keytool -genkeypair -v -keystore keystore/turkids-release.jks -alias turkids \
  -keyalg RSA -keysize 2048 -validity 10000
```
Guarda el `.jks` y sus contraseñas en un lugar seguro: sin ellos no se pueden publicar actualizaciones.
(Android Studio: *Build → Generate Signed App Bundle / APK → APK → Create new…*.)

### Paso 2 · Configurar las credenciales
Copia `keystore.properties.example` como `keystore.properties` y completa los datos:
```properties
storeFile=keystore/turkids-release.jks
storePassword=TU_CLAVE_DEL_KEYSTORE
keyAlias=turkids
keyPassword=TU_CLAVE_DE_LA_LLAVE
```
Alternativa: variables de entorno `TURKIDS_STORE_FILE`, `TURKIDS_STORE_PASSWORD`, `TURKIDS_KEY_ALIAS` y `TURKIDS_KEY_PASSWORD`.

`*.jks`, `*.keystore` y `keystore.properties` están en el `.gitignore` de la raíz y de `app/`.

### Paso 3 · Compilar
- **Gradle:** `./gradlew assembleRelease` (Windows: `gradlew.bat assembleRelease`).
  Resultado: `app/build/outputs/apk/release/app-release.apk`.
- **Android Studio:** *Build → Generate Signed App Bundle / APK → APK*, elegir el `.jks`, el alias y la variante **release**.

### Paso 4 · Verificar la firma
```bash
apksigner verify --print-certs app/build/outputs/apk/release/app-release.apk
```
(`apksigner` está en `build-tools` del SDK de Android.)

---

## 7. Configuración de compilación

| Elemento | Versión |
|---|---|
| Android Gradle Plugin / Gradle | 9.2.1 / 9.4.1 |
| Kotlin | 2.2.10 (plugin `kotlin.compose`) |
| compileSdk / targetSdk / minSdk | 37 / 36 / 24 |
| Java | 11 |
| Compose BOM | 2026.02.01 |
| Navigation Compose / Lifecycle / Activity Compose | 2.9.8 / 2.10.0 / 1.13.0 |
| Coroutines / DataStore Preferences | 1.10.2 / 1.1.7 |
| Retrofit y converter-gson | 2.11.0 |
| JUnit 4 / JUnit 5 / Kotest / MockK | 4.13.2 / 5.10.0 / 5.8.0 / 1.13.10 |

Todas las dependencias están en `gradle/libs.versions.toml`. `local.properties` y `keystore.properties` no se versionan.

Permisos del manifest: `ACCESS_COARSE_LOCATION`, `ACCESS_FINE_LOCATION`, `RECORD_AUDIO` e `INTERNET`. REST Countries usa HTTPS; el microservicio local usa HTTP y por eso el manifest tiene `usesCleartextTraffic` (solo desarrollo).

---

## 8. Flujo de trabajo y evidencias

| Evidencia | Enlace |
|---|---|
| Repositorio GitHub | `<COMPLETAR: URL del repositorio>` |
| Tablero Trello | `<COMPLETAR: URL del tablero>` |
| Microservicio `backend-turkids` | Carpeta `../backend-turkids` de este repositorio |

- **Control de versiones:** ramas por funcionalidad y *commits* técnicos descriptivos; los cambios se suben con `git add .`, `git commit -m "…"` y `git push`.
- **Planificación:** tareas distribuidas entre integrantes en Trello (columnas: Pendiente · En curso · En revisión · Hecho), cada una enlazada a su rama o *commit*.
- No versionar `.idea/`, `.kotlin/`, `local.properties`, `*.jks`, `*.keystore` ni `keystore.properties`.

---

## 9. Limitaciones conocidas
- No hay login: el progreso es por perfil ficticio y vive solo en el dispositivo; si se borran los datos de la app se pierde.
- Un solo punto de retoma por perfil e idioma.
- El contenido es mínimo (3 unidades de 4 palabras).
- El microservicio guarda en memoria: al apagarlo se pierden los cambios y vuelve a las 4 publicaciones iniciales. Para ver cambios hechos desde otro dispositivo hay que pulsar **Reintentar**.
- La app solo funciona con las publicaciones si el microservicio está encendido y la `BASE_URL` es alcanzable desde el dispositivo.
