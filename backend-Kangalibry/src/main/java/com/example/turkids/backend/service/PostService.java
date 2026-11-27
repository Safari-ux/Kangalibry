package com.example.turkids.backend.service;

import com.example.turkids.backend.model.Post;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

// concurrentHashMap para evitar choques si llegan varias peticiones juntas; atomicInteger para los ids
@Service
public class PostService {

    private final Map<Integer, Post> posts = new ConcurrentHashMap<>();
    private final AtomicInteger ultimoId = new AtomicInteger(0);

    public PostService() {
        crear(new Post(1, 0, "Merhaba = Hola",
                "Aprende a saludar en español: Merhaba se dice Hola. ¡Escúchalo y repítelo en voz alta!"));
        crear(new Post(1, 0, "Los animales en español",
                "Kedi es gato, köpek es perro y kuş es pájaro. Practica las palabras de la unidad Animales."));
        crear(new Post(1, 0, "Cuenta del uno al diez",
                "Bir es uno, iki es dos y üç es tres. Cuenta con tus dedos y gana tus tres estrellas."));
        crear(new Post(1, 0, "Gracias y por favor",
                "Teşekkürler se dice Gracias. Las palabras mágicas ayudan a hacer amigos en cualquier idioma."));
    }

    public List<Post> listar() {
        return posts.values().stream()
                .sorted(Comparator.comparingInt(Post::id))
                .toList();
    }

    public Post crear(Post entrada) {
        int id = ultimoId.incrementAndGet();
        Post nuevo = new Post(entrada.userId(), id, entrada.title(), entrada.body());
        posts.put(id, nuevo);
        return nuevo;
    }

    // put: reemplaza el post completo; el id siempre es el de la ruta, vacio si no existe
    public Optional<Post> actualizar(int id, Post entrada) {
        Post actualizado = new Post(entrada.userId(), id, entrada.title(), entrada.body());
        return Optional.ofNullable(posts.computeIfPresent(id, (clave, anterior) -> actualizado));
    }

    public boolean eliminar(int id) {
        return posts.remove(id) != null;
    }
}
