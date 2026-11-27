# Kangalibry / TurKids

Aplicación móvil Android para el aprendizaje inicial de español de niños y niñas de habla turca, conectada a un **microservicio propio** que gestiona publicaciones (CRUD) y que además consume una **API externa pública** (REST Countries). El contenido es ficticio, de prueba y sin datos personales reales.

- La **app** (Kotlin + Jetpack Compose) funciona como un juego de palabras con lecciones, estrellas por unidad, voz y ubicación. La pantalla **Publicaciones** permite listar, crear, editar y eliminar publicaciones en el microservicio.
- El **microservicio** (Spring Boot) expone la API REST `/posts` y guarda los datos en memoria.
- La **API externa** (REST Countries) se usa desde la app para mostrar el país y la bandera según la ubicación del dispositivo.

## Estructura del repositorio

```
.
├── Turkids-main/      app Android (proyecto de Android Studio)
├── backend-turkids/   microservicio Spring Boot (API REST /posts)
└── README.md          este archivo
```

| Carpeta | Contenido | Detalle técnico |
|---|---|---|
| [`Turkids-main/`](Turkids-main/) | App Android: pantallas, ViewModels, repositorios, pruebas unitarias e instrumentadas, configuración del APK firmado | [`Turkids-main/README.md`](Turkids-main/README.md) |
| [`backend-turkids/`](backend-turkids/) | Microservicio con `PostController`, `PostService`, modelo `Post` y pruebas con MockMvc | [`backend-turkids/README.md`](backend-turkids/README.md) |

## Arquitectura y tecnologías

```
App Android (Compose + MVVM)
   │
   ├── Retrofit ── HTTP ──> backend-turkids (Spring Boot, puerto 8080)   CRUD de /posts
   │                         emulador: http://10.0.2.2:8080/
   │
   └── Retrofit ── HTTPS ─> REST Countries (https://restcountries.com/)  país y bandera
```

| Lado | Tecnologías |
|---|---|
| **Android** | Kotlin 2.2.10, Jetpack Compose (BOM 2026.02.01) y Navigation Compose, arquitectura MVVM con `StateFlow` y corrutinas, Retrofit 2.11.0 con Gson, DataStore Preferences 1.1.7 para el progreso, `minSdk` 24 / `targetSdk` 36, Gradle con AGP 9.2.1 |
| **Recursos nativos** | Ubicación (GPS, fused y red), síntesis de voz (TextToSpeech) y reconocimiento de voz (SpeechRecognizer) |
| **Backend** | Spring Boot 3.5.6 (`spring-boot-starter-web`), Java 17 o superior, Gradle Wrapper 8.14.3, API REST `/posts` con almacenamiento en memoria (`ConcurrentHashMap` + `AtomicInteger`) y CORS abierto |
| **Pruebas** | JUnit 5, Kotest y MockK en Android; JUnit 5 y MockMvc (`spring-boot-starter-test`) en el backend |

### API del microservicio

El modelo JSON es el mismo en la app y en el servidor: `userId`, `id`, `title`, `body`. Al iniciar el servidor hay 4 publicaciones precargadas (ids 1 a 4).

| Método | Ruta | Respuesta |
|---|---|---|
| `GET` | `/posts` | `200` con la lista completa |
| `POST` | `/posts` | `201` con el post creado (el servidor asigna el `id`) |
| `PUT` | `/posts/{id}` | `200` con el post actualizado, o `404` |
| `DELETE` | `/posts/{id}` | `200` sin cuerpo, o `404` |

## Cómo ejecutar el proyecto completo (Quick Start)

**Requisitos:** JDK 17 o superior (sirve el que trae Android Studio), Android Studio con un emulador configurado e internet la primera vez para que Gradle descargue las dependencias.

**1. Levantar el backend** (puerto `8080`)

```powershell
cd backend-turkids
.\gradlew.bat bootRun
```
En Linux o macOS: `./gradlew bootRun`. Déjalo corriendo; para comprobarlo abre `http://localhost:8080/posts` en el navegador.

**2. Ejecutar la app**

1. Abre la carpeta `Turkids-main` en Android Studio y espera a que termine la sincronización de Gradle.
2. Elige un emulador y pulsa **Run**.
3. En el menú lateral entra a **Publicaciones**.

La app se conecta al backend con `http://10.0.2.2:8080/` (definido en `RetrofitInstance.kt`): `10.0.2.2` es el `localhost` del PC visto desde el emulador. Como esa conexión usa HTTP, el manifest declara `usesCleartextTraffic` (solo para desarrollo).

**Notas**
- En un **celular real** hay que cambiar esa URL por la IP del PC en la red local (por ejemplo `http://192.168.1.50:8080/`) y permitir el puerto 8080 en el firewall.
- Si el backend está apagado, la pantalla Publicaciones muestra el error de conexión con el botón **Reintentar**; el resto de la app funciona igual.
- El backend guarda en memoria: al reiniciarlo se pierden los cambios y vuelven las 4 publicaciones iniciales.
- Para generar el APK firmado (release) sigue la guía de [`Turkids-main/README.md`](Turkids-main/README.md).

## Pruebas automatizadas

| Módulo | Cantidad | Comando | Qué valida |
|---|---|---|---|
| App Android (`Turkids-main`) | **115 tests unitarios** | `.\gradlew.bat testDebugUnitTest` | Lógica de lecciones (acierto, segunda oportunidad, retoma), `ProgresoRepository` con DataStore en memoria, validación del formulario de registro, `PostRepository` y `PostViewModel` (GET, POST, PUT y DELETE), ubicación, país y test oral |
| Microservicio (`backend-turkids`) | **5 tests con MockMvc** | `.\gradlew.bat test` | `GET` con las 4 publicaciones precargadas, `POST` con id autoincremental, `PUT` de un post existente, `PUT` de un id inexistente (404) y `DELETE` (200 y luego 404) |

Cada comando se ejecuta dentro de su carpeta (`cd Turkids-main` o `cd backend-turkids`); en Linux o macOS usa `./gradlew`. Los tests unitarios no necesitan emulador. La app también tiene una prueba instrumentada de `PostScreen` (`.\gradlew.bat connectedDebugAndroidTest`, requiere emulador o celular).

El detalle de qué cubre cada archivo de prueba está en [`Turkids-main/README.md`](Turkids-main/README.md#5-testing).
