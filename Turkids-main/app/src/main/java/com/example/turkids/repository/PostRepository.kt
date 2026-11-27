package com.example.turkids.repository

import com.example.turkids.data.model.Post
import com.example.turkids.data.remote.ApiService
import com.example.turkids.data.remote.RetrofitInstance

class PostRepository(private val api: ApiService = RetrofitInstance.api) {

    suspend fun getPosts(): List<Post> = api.getPosts()

    suspend fun createPost(post: Post): Post = api.createPost(post)

    suspend fun updatePost(id: Int, post: Post): Post = api.updatePost(id, post)

    suspend fun deletePost(id: Int) = api.deletePost(id)
}
