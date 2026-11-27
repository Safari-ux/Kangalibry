package com.example.turkids.backend.service;

import com.example.turkids.backend.model.Post;
import com.example.turkids.backend.repository.PostRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class PostService {

    private final PostRepository repositorio;

    public PostService(PostRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Transactional(readOnly = true)
    public List<Post> listar() {
        return repositorio.findAll(Sort.by("id"));
    }

    // se copian los datos a una entidad nueva: el id que llegue se ignora y lo asigna la base
    @Transactional
    public Post crear(Post entrada) {
        return repositorio.save(new Post(entrada.getUserId(), entrada.getTitle(), entrada.getBody()));
    }

    // put: reemplaza los datos del post; el id siempre es el de la ruta, vacio si no existe
    @Transactional
    public Optional<Post> actualizar(int id, Post entrada) {
        return repositorio.findById(id).map(existente -> {
            existente.setUserId(entrada.getUserId());
            existente.setTitle(entrada.getTitle());
            existente.setBody(entrada.getBody());
            return repositorio.save(existente);
        });
    }

    @Transactional
    public boolean eliminar(int id) {
        if (!repositorio.existsById(id)) {
            return false;
        }
        repositorio.deleteById(id);
        return true;
    }
}
