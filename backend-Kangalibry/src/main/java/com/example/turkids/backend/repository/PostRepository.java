package com.example.turkids.backend.repository;

import com.example.turkids.backend.model.Post;
import org.springframework.data.jpa.repository.JpaRepository;

// spring data genera findAll, findById, save, existsById, deleteById, etc.
public interface PostRepository extends JpaRepository<Post, Integer> {
}
