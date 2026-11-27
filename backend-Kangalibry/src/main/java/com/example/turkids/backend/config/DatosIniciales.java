package com.example.turkids.backend.config;

import com.example.turkids.backend.model.Post;
import com.example.turkids.backend.repository.PostRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

// carga las 4 publicaciones solo si la tabla esta vacia: al reiniciar no se duplican ni se pisan los cambios
@Component
public class DatosIniciales implements ApplicationRunner {

    private final PostRepository repositorio;

    public DatosIniciales(PostRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (repositorio.count() > 0) {
            return;
        }
        repositorio.saveAll(List.of(
                new Post(1, "Merhaba = Hola",
                        "Aprende a saludar en español: Merhaba se dice Hola. ¡Escúchalo y repítelo en voz alta!"),
                new Post(1, "Los animales en español",
                        "Kedi es gato, köpek es perro y kuş es pájaro. Practica las palabras de la unidad Animales."),
                new Post(1, "Cuenta del uno al diez",
                        "Bir es uno, iki es dos y üç es tres. Cuenta con tus dedos y gana tus tres estrellas."),
                new Post(1, "Gracias y por favor",
                        "Teşekkürler se dice Gracias. Las palabras mágicas ayudan a hacer amigos en cualquier idioma.")
        ));
    }
}
