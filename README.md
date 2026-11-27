# Kangalibry / TurKids

Aplicación móvil Android para el aprendizaje inicial de español de niños y niñas de habla turca, integrada con un microservicio propio en Spring Boot y con una API externa pública. Proyecto de la asignatura DSY1105 · Desarrollo de Aplicaciones Móviles, presentado en la Evaluación Parcial 5 como avance hacia el Examen Final Transversal (EFT).

## 1. Introducción y contexto

**Problemática (simulada).** Los niños y niñas de habla turca que llegan a un entorno hispanohablante necesitan un apoyo lúdico para aprender sus primeras palabras en español. Kangalibry lo resuelve con un juego de lecciones por unidades: se muestra una palabra con su emoji, el usuario elige entre 3 alternativas, tiene una segunda oportunidad tras un error y gana estrellas por unidad. El contenido es ficticio y de prueba, sin datos personales reales.

**Público objetivo.** Público infantil: niños y niñas de alrededor de 10 años cuya lengua materna es el turco y quieren aprender español. Como extensión propia del proyecto, la app permite elegir el idioma de práctica (turco o español), de modo que también sirve para que alguien que sabe español practique turco.

**Integración de funcionalidades móviles avanzadas con servicios externos y propios.**

| Componente | Qué aporta |
|---|---|
| Recursos nativos | Ubicación (GPS, fused y red), síntesis de voz (TextToSpeech) y reconocimiento de voz (SpeechRecognizer) |
| Microservicio propio (`backend-Kangalibry`) | API REST `/posts` con CRUD de publicaciones, consumida desde la pantalla **Publicaciones** de la app |
| API externa pública (REST Countries) | Nombre y bandera del país a partir de la ubicación del dispositivo |
| Persistencia local (app) | Progreso del usuario (idioma, estrellas por unidad y lección en curso) con DataStore |
| Persistencia remota (backend) | Publicaciones guardadas en una base de datos H2 mediante Spring Data JPA |

## 2. Estructura del repositorio

```
Kangalibry/
├── Kangalibry-android/       app Android (proyecto de Android Studio)
├── backend-Kangalibry/    microservicio Spring Boot (API REST /posts)
└── README.md              este archivo
```

| Carpeta | Contenido | Detalle técnico |
|---|---|---|
| [`Kangalibry-android/`](Kangalibry-android/) | App Android: pantallas Compose, ViewModels, repositorios, pruebas unitarias e instrumentadas y configuración de firma del APK | [`Kangalibry-android/README.md`](Kangalibry-android/README.md) |
| [`backend-Kangalibry/`](backend-Kangalibry/) | Microservicio: `PostController`, `PostService`, modelo `Post` y pruebas con MockMvc | [`backend-Kangalibry/README.md`](backend-Kangalibry/README.md) |

## 3. Arquitectura y tecnologías

### App Android (`Kangalibry-android`)

Patrón **MVVM** con una sola actividad (`MainActivity`) y navegación por eventos (`MainViewModel` emite un `NavigationEvent`).

| Capa | Responsabilidad |
|---|---|
| `ui/screens` (Jetpack Compose) | Pantallas; solo conocen ViewModels |
| `viewmodel` | Estado con `StateFlow` y corrutinas (`viewModelScope`); reciben repositorios o el validador por constructor mediante `TurKidsViewModelFactory` |
| `repository` | Único punto de acceso a DataStore, `LocationManager`/`Geocoder` y a la red |
| `data/remote` | Interfaces Retrofit y clientes: `ApiService` + `RetrofitInstance` (microservicio) y `PaisesApiService` + `RetrofitPaises` (API externa) |
| `model` | Estados de UI, reglas de negocio (`BancoDeContenido`, `ValidadorUsuario`) y modelos |

Tecnologías: Kotlin 2.2.10, Jetpack Compose (BOM 2026.02.01) con Navigation Compose, corrutinas y `Flow`, Retrofit 2.11.0 con Gson, DataStore Preferences 1.1.7, `minSdk` 24 / `targetSdk` 36, Android Gradle Plugin 9.2.1. Pruebas con JUnit 5, Kotest y MockK.

### Microservicio (`backend-Kangalibry`)

Spring Boot **3.5.6** con `spring-boot-starter-web` y `spring-boot-starter-data-jpa`, base de datos **H2**, Java 17 o superior y Gradle Wrapper 8.14.3. Arquitectura en capas:

| Capa | Clase | Responsabilidad |
|---|---|---|
| Controlador | `PostController` | Expone `/posts`, traduce el resultado a códigos HTTP (201, 200, 404) y habilita CORS (`@CrossOrigin(origins = "*")`) |
| Servicio | `PostService` | Reglas del CRUD con `@Transactional`: el `id` lo asigna la base y en `PUT` siempre manda el `id` de la ruta |
| Repositorio | `PostRepository` | `JpaRepository<Post, Integer>`: Spring Data genera las consultas (`findAll`, `findById`, `save`, `existsById`, `deleteById`) |
| Entidad / modelo | `Post` | `@Entity` de la tabla `posts` con `id` (`@Id` + `@GeneratedValue`), `userId`, `title` y `body`; es también el JSON que usa la app |
| Datos iniciales | `DatosIniciales` | Carga las 4 publicaciones solo si la tabla está vacía |

**Persistencia.** H2 en modo archivo (`jdbc:h2:file:./data/kangalibry`, carpeta `data/` dentro de `backend-Kangalibry`, ignorada por Git): no requiere instalar ni levantar un servidor de base de datos y los datos **sobreviven al reiniciar** el servidor. Hibernate crea la tabla en el primer arranque (`ddl-auto=update`). La consola web de H2 está en `http://localhost:8080/h2-console` (JDBC URL `jdbc:h2:file:./data/kangalibry`, usuario `sa`, sin contraseña). Las pruebas usan H2 solo en memoria, así que no tocan esos datos.

Al primer arranque hay 4 publicaciones precargadas (ids 1 a 4). Para volver a ese estado basta con borrar la carpeta `data/` con el servidor apagado.

## 4. APIs y endpoints utilizados

### Microservicio propio (`http://10.0.2.2:8080/` desde el emulador)

El modelo JSON es el mismo en la app (`Post.kt`) y en el servidor.

```json
{ "userId": 1, "id": 1, "title": "Merhaba = Hola", "body": "..." }
```

| Método | Ruta | Descripción | Respuesta |
|---|---|---|---|
| `GET` | `/posts` | Lista completa, ordenada por `id` | `200` |
| `POST` | `/posts` | Crea un post; el servidor asigna el `id` | `201` con el post creado |
| `PUT` | `/posts/{id}` | Reemplaza el post con el enviado (la app lo envía desde su diálogo de edición) | `200` con el post actualizado · `404` |
| `DELETE` | `/posts/{id}` | Elimina el post | `200` sin cuerpo · `404` |

En la app, la pantalla **Publicaciones** usa `PostViewModel` → `PostRepository` → `ApiService`, con estados de carga, éxito y error (con **Reintentar**).

### API externa pública: REST Countries

| Método | URL | Uso |
|---|---|---|
| `GET` | `https://restcountries.com/v3.1/alpha/{codigo}?fields=name,flag,translations` | Con el código de país obtenido por geocodificación se muestra nombre y bandera. Si falla, la app usa el nombre del geocodificador o las coordenadas |

Se accede con su propio cliente (`RetrofitPaises`) y repositorio (`PaisRepository`), sin interferir con el microservicio.

## 5. Recursos nativos y APK firmado

### Recursos nativos

| Recurso | Implementación | Permisos del manifest |
|---|---|---|
| Ubicación (GPS / fused / red) | `UbicacionRepository` y `ProveedoresUbicacion`: con ubicación aproximada usa el proveedor fused (Android 12+) o la red; con ubicación precisa suma el GPS | `ACCESS_COARSE_LOCATION`, `ACCESS_FINE_LOCATION` |
| Reconocimiento de voz | `ReconocedorDeVoz` (envuelve `SpeechRecognizer`), usado en el test oral | `RECORD_AUDIO` |
| Síntesis de voz | `Narrador` (envuelve `TextToSpeech`), botón **Escuchar** en las lecciones | — |

El manifest también declara `INTERNET` y `usesCleartextTraffic="true"`, necesario porque el microservicio local usa HTTP (solo para desarrollo).

### APK firmado (release)

`app/build.gradle.kts` define el `signingConfig` **release** leyendo las credenciales de `keystore.properties` (en la raíz de `Kangalibry-android`, a partir de `keystore.properties.example`) o de las variables de entorno `TURKIDS_STORE_FILE`, `TURKIDS_STORE_PASSWORD`, `TURKIDS_KEY_ALIAS` y `TURKIDS_KEY_PASSWORD`.

- El archivo `.jks` **se mantiene fuera del repositorio**: `*.jks`, `*.keystore` y `keystore.properties` están en el `.gitignore`.
- Compilar: `.\gradlew.bat assembleRelease` dentro de `Kangalibry-android`. Resultado: `app/build/outputs/apk/release/app-release.apk`.
- El `app-release.apk` firmado se publica en la sección **Releases** del repositorio de GitHub.

La guía paso a paso (crear el keystore con `keytool`, configurar credenciales y verificar la firma) está en [`Kangalibry-android/README.md`](Kangalibry-android/README.md).

## 6. Instalación, ejecución y pruebas unitarias

**Requisitos:** JDK 17 o superior (sirve el de Android Studio), Android Studio con un emulador configurado e internet la primera vez para descargar las dependencias de Gradle.

### Paso 1 · Levantar el backend (puerto 8080)

```powershell
cd backend-Kangalibry
.\gradlew.bat bootRun
```
En Linux o macOS: `./gradlew bootRun`. Comprueba que responde abriendo `http://localhost:8080/posts`. Déjalo corriendo.

### Paso 2 · Ejecutar la app en el emulador

1. Abre la carpeta `Kangalibry-android` en Android Studio y espera la sincronización de Gradle.
2. Elige un emulador y pulsa **Run**.
3. En el menú lateral entra a **Publicaciones**.

La app se conecta al backend con `http://10.0.2.2:8080/` (constante `BASE_URL` en `RetrofitInstance.kt`): en el emulador, `10.0.2.2` es el `localhost` del PC. En un celular real hay que usar la IP del PC en la red local (por ejemplo `http://192.168.1.50:8080/`) y permitir el puerto 8080 en el firewall. Si el backend está apagado, Publicaciones muestra el error de conexión con **Reintentar** y el resto de la app funciona igual. Los cambios hechos desde la app quedan guardados en la base H2 y siguen ahí al reiniciar el backend.

### Paso 3 · Pruebas automatizadas

| Módulo | Pruebas | Comando (dentro de la carpeta) |
|---|---|---|
| `Kangalibry-android` | **115 pruebas unitarias** (JUnit 5, Kotest, MockK) | `.\gradlew.bat testDebugUnitTest` |
| `backend-Kangalibry` | **5 pruebas con MockMvc** | `.\gradlew.bat test` |

Qué validan:
- **Android:** lógica de lecciones (acierto a la primera, segunda oportunidad y retoma), `ProgresoRepository` con DataStore en memoria, validación reactiva del formulario de registro, `PostRepository` y `PostViewModel` (GET, POST, PUT y DELETE), ubicación, país y test oral.
- **Backend (`PostControllerTest`):** `GET` con las 4 publicaciones precargadas, `POST` con id autoincremental, `PUT` de un post existente, `PUT` de un id inexistente (404) y `DELETE` (200 y luego 404).

Las pruebas unitarias no necesitan emulador. La app incluye además una prueba instrumentada de `PostScreen` (`.\gradlew.bat connectedDebugAndroidTest`, requiere emulador o celular). En Linux o macOS usa `./gradlew` en lugar de `.\gradlew.bat`.

## 7. Mapa para la defensa (Evaluación Parcial 5)

Dónde está cada elemento que pide la pauta de defensa individual.

| Paso de la defensa | Dónde verlo |
|---|---|
| 1. Inicio y entorno | Backend: `.\gradlew.bat bootRun` en `backend-Kangalibry` (puerto 8080). App: emulador desde Android Studio. Dependencias: `Kangalibry-android/gradle/libs.versions.toml` y `backend-Kangalibry/build.gradle.kts` |
| 2. Arquitectura y paquetes | App: `ui/screens`, `viewmodel`, `repository`, `data/remote`, `model` (sección 3). Backend: `controller`, `service`, `repository`, `model` (entidad) y `config` (datos iniciales) |
| 3. Microservicio y endpoints | `PostController` (`GET`, `POST`, `PUT`, `DELETE` sobre `/posts`), consumidos por `ApiService`; pantalla **Publicaciones** |
| 4. API externa | `PaisesApiService` + `RetrofitPaises` + `PaisRepository`. Diferencia con el microservicio: REST Countries es de terceros, solo lectura (`GET`), por HTTPS y con un contrato que no controlamos; `backend-Kangalibry` es propio, con CRUD completo, HTTP local (`10.0.2.2`) y contrato definido por nosotros |
| 5. Persistencia remota | Flujo: `PostScreen` → `PostViewModel` → `PostRepository` → `ApiService` → `PostController` → `PostService` → `PostRepository` (Spring Data JPA) → base H2 (tabla `posts`). Los datos persisten en el archivo `data/kangalibry.mv.db` |
| 6. Pruebas unitarias | `.\gradlew.bat testDebugUnitTest` (app) y `.\gradlew.bat test` (backend). Se mockea `ApiService` en `PostRepositoryTest`, `PostRepository` en `PostViewModelTest` y `ProgresoRepository` en `LeccionViewModelTest`; `ProgresoRepositoryTest` usa un DataStore en memoria |
| 7. APK firmado | Sección 5: `signingConfigs.release` en `app/build.gradle.kts`, `keystore.properties`, `.\gradlew.bat assembleRelease` |
| 8. Colaboración | GitHub (commits y ramas) y Trello: `<COMPLETAR: URL del repositorio>` · `<COMPLETAR: URL del tablero>` |
| 9. Modificación en tiempo real | Backend: campo nuevo en la entidad `Post` (H2 agrega la columna con `ddl-auto=update`) y, si cambia la lógica, `PostService`; endpoint nuevo: `PostRepository` (consulta, si hace falta) → `PostService` → `PostController`. App: `ApiService` → `PostRepository` → `PostViewModel` → `PostScreen`, más su prueba |
