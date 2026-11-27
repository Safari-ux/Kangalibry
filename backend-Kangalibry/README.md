# backend-turkids

Microservicio REST en **Spring Boot 3.5** (Java 17 o superior, `spring-boot-starter-web`) que usa la app Android Kangalibry / TurKids (`../Turkids-main`) en su pantalla **Publicaciones**. Guarda los datos **en memoria** (`ConcurrentHashMap` + `AtomicInteger`), así que no necesita base de datos.

## Cómo levantarlo
Solo necesitas un JDK 17 o 21 (el de Android Studio sirve). El Gradle Wrapper descarga todo lo demás la primera vez (requiere internet).

```bash
cd backend-turkids
./gradlew bootRun          # Windows: gradlew.bat bootRun
```
Queda escuchando en `http://localhost:8080`. El emulador de Android lo ve como `http://10.0.2.2:8080/`.

Pruebas automáticas: `./gradlew test` (Windows: `gradlew.bat test`). Son **5 tests con MockMvc** (`PostControllerTest`): `GET` con las 4 publicaciones precargadas, `POST` con id autoincremental, `PUT` de un post existente, `PUT` de un id inexistente (404) y `DELETE` (200 y luego 404). Cada test parte de un contexto nuevo, así que no dependen entre sí.

## Modelo JSON
Igual que `data/model/Post.kt` de la app:
```json
{ "userId": 1, "id": 1, "title": "Merhaba = Hola", "body": "..." }
```

## Endpoints (`/posts`)

| Método | Ruta | Descripción | Respuestas |
|---|---|---|---|
| `GET` | `/posts` | Lista completa, ordenada por `id` | `200` |
| `POST` | `/posts` | Crea un post; el servidor asigna el `id` (ignora el que llegue) | `201` + post creado |
| `PUT` | `/posts/{id}` | Reemplaza el post existente con el que llega (la app envía el post editado desde su diálogo con título y contenido); el `id` es el de la ruta | `200` + post actualizado · `404` |
| `DELETE` | `/posts/{id}` | Elimina el post | `200` sin cuerpo · `404` |

CORS habilitado para cualquier origen (`@CrossOrigin(origins = "*")`).

Al iniciar hay 4 publicaciones precargadas (ids 1 a 4) sobre aprendizaje de español para niños turcos: saludos, animales, números y palabras de cortesía. Los ids nuevos parten en 5.

## Probar con curl
```bash
curl http://localhost:8080/posts
curl -X POST http://localhost:8080/posts -H "Content-Type: application/json" \
     -d '{"userId":1,"id":0,"title":"Nuevo","body":"Cuerpo"}'
curl -X PUT http://localhost:8080/posts/2 -H "Content-Type: application/json" \
     -d '{"userId":1,"id":2,"title":"Editado","body":"Cuerpo"}'
curl -X DELETE http://localhost:8080/posts/1
```
(En Windows PowerShell usa `curl.exe` y comillas dobles escapadas, o prueba con Postman.)

## Estructura
```
src/main/java/com/example/turkids/backend/
├── BackendTurkidsApplication.java
├── model/Post.java              record con userId, id, title, body
├── service/PostService.java     almacenamiento en memoria y reglas
└── controller/PostController.java   endpoints REST
```

## Notas
- Al apagar el servidor se pierden los cambios (memoria).
- Desde un celular real, usa la IP del PC (`http://<IP-del-PC>:8080/`) y permite el puerto 8080 en el firewall.
- El Wrapper usa Gradle 8.14.3 (distinto del de la app Android).
