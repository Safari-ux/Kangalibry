package com.example.turkids.backend.model;

// mismo json que post.kt de la app: userId, id, title y body
public record Post(int userId, int id, String title, String body) {
}
