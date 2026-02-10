package com.android.idepro.data.github

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path

data class Repo(
    val id: Long,
    val name: String,
    val full_name: String,
    val html_url: String,
    val description: String?,
    val language: String?
)

interface GitHubService {
    @GET("users/{user}/repos")
    suspend fun listRepos(
        @Path("user") user: String,
        @Header("Authorization") token: String? = null
    ): List<Repo>

    companion object {
        fun create(): GitHubService {
            return Retrofit.Builder()
                .baseUrl("https://api.github.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(GitHubService::class.java)
        }
    }
}
