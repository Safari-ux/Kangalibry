# backend-Kangalibry

Microservicio REST en **Spring Boot 3.5.6** (Java 17 o superior) que usa la app Android Kangalibry / TurKids (`../Kangalibry-android`) en su pantalla **Publicaciones**. Guarda los datos con **Spring Data JPA** en una base **H2** local, así que no necesita instalar ni levantar ningún servidor de base de datos.

## Cómo levantarlo
Solo necesitas un JDK 17 o 21 (el de Android Studio sirve). El Gradle Wrapper descarga todo lo demás la primera vez (requiere internet).

```powershell
cd backend-Kangalibry
.\gradlew.bat bootRun
```
En Linux o macOS: `./gradlew bootRun`. Queda escuchando en `http://localhost:8080`. El emulador de Android lo ve como `http://10.0.2.2:8080/`.

## Pruebas
```powershell
.\gradlew.bat test
```
Son **5 tests con MockMvc** (`PostControllerTest`): `GET` con las 4 publicaciones precargadas, `POST` con id autoincremental, `PUT` de un post existente, `PUT` de un id inexistente (404) y `DELETE` (200 y luego 404). Cada test parte de un contexto nuevo con una base H2 en memoria vacía (`src/test/resources/application.properties`), por lo que no dependen entre sí ni tocan los datos de `./data`.

## Persistencia: Spring Data JPA + H2

| Elemento | Detalle |
|---|---|
| Dependencias | `spring-boot-starter-data-jpa` y `com.h2database:h2` (en `build.gradle.kts`) |
| Entidad | `Post`: `@Entity` de la tabla `posts` con `id` (`@Id` + `@GeneratedValue(strategy = IDENTITY)`), `userId`, `title` y `body` |
| Repositorio | `PostRepository extends JpaRepository<Post, Integer>` |
| Base de datos | H2 en archivo: `jdbc:h2:file:./data/kangalibry` (crea `data/kangalibry.mv.db` dentro de esta carpeta, ignorada por Git) |
| Esquema | Hibernate lo crea en el primer arranque (`spring.jpa.hibernate.ddl-auto=update`) y agrega columnas nuevas si cambia la entidad |
| Datos iniciales | `DatosIniciales` carga 4 publicaciones solo si la tabla está vacía |
| Consola web | `http://localhost:8080/h2-console`: JDBC URL `jdbc:h2:file:./data/kangalibry`, usuario `sa`, contraseña vacía. Detener el servidor antes de abrir el archivo con otra herramienta |

Los datos **sobreviven al reiniciar** el servidor. Para volver a las 4 publicaciones iniciales, apaga el servidor y borra la carpeta `data/`. Si prefieres que no se guarde nada entre arranques, cambia la URL a `jdbc:h2:mem:kangalibry` en `src/main/resources/application.properties`.

## Modelo JSON
Igual que `data/model/Post.kt` de la app:
```json
{ "userId": 1, "id": 1, "title": "Merhaba = Hola", "body": "..." }
```

## Endpoints (`/posts`)

| Método | Ruta | Descripción | Respuestas |
|---|---|---|---|
| `GET` | `/posts` | Lista completa, ordenada por `id` | `200` |
| `POST` | `/posts` | Crea un post; la base asigna el `id` (ignora el que llegue) | `201` + post creado |
| `PUT` | `/posts/{id}` | Reemplaza el post existente con el que llega (la app envía el post editado desde su diálogo con título y contenido); el `id` es el de la ruta | `200` + post actualizado · `404` |
| `DELETE` | `/posts/{id}` | Elimina el post | `200` sin cuerpo · `404` |

CORS habilitado para cualquier origen (`@CrossOrigin(origins = "*")`). Al primer arranque hay 4 publicaciones (ids 1 a 4) sobre aprendizaje de español para niños turcos: saludos, animales, números y palabras de cortesía; los ids nuevos parten en 5.

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
├── controller/PostController.java     endpoints REST
├── service/PostService.java           reglas del CRUD (@Transactional)
├── repository/PostRepository.java     JpaRepository<Post, Integer>
├── model/Post.java                    entidad JPA y JSON
└── config/DatosIniciales.java         carga las 4 publicaciones si la tabla está vacía
src/main/resources/application.properties   puerto, H2 en archivo y consola H2
src/test/resources/application.properties   H2 en memoria para las pruebas
```

## Notas
- Desde un celular real, usa la IP del PC (`http://<IP-del-PC>:8080/`) y permite el puerto 8080 en el firewall.
- El Wrapper usa Gradle 8.14.3 (distinto del de la app Android).
