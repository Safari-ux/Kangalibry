package com.example.turkids.backend.controller;

import com.example.turkids.backend.model.Post;
import com.example.turkids.backend.service.PostService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/posts")
@CrossOrigin(origins = "*")
public class PostController {

    private final PostService servicio;

    public PostController(PostService servicio) {
        this.servicio = servicio;
    }

    // get /posts: lista completa
    @GetMapping
    public List<Post> listar() {
        return servicio.listar();
    }

    // post /posts: crea y responde 201 con el id asignado
    @PostMapping
    public ResponseEntity<Post> crear(@RequestBody Post post) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicio.crear(post));
    }

    // put /posts/{id}: 200 con el post actualizado o 404
    @PutMapping("/{id}")
    public ResponseEntity<Post> actualizar(@PathVariable int id, @RequestBody Post post) {
        return servicio.actualizar(id, post)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // delete /posts/{id}: 200 sin cuerpo o 404
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable int id) {
        return servicio.eliminar(id)
                ? ResponseEntity.ok().build()
                : ResponseEntity.notFound().build();
    }
}
