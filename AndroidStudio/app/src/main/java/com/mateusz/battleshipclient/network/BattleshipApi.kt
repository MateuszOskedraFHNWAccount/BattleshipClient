package com.mateusz.battleshipclient.network

import com.example.battleships_mateusz_sander.model.*
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface BattleshipApi {
    @GET("/ping")
    suspend fun ping(): Map<String, Boolean>

    @POST("/game/join")
    suspend fun joinGame(@Body body: JoinGameRequest): EnemyFireResponse

    @POST("/game/fire")
    suspend fun fire(@Body body: FireRequest): FireResponse

    @POST("/game/enemyFire")
    suspend fun enemyFire(@Body body: EnemyFireRequest): EnemyFireResponse
}
