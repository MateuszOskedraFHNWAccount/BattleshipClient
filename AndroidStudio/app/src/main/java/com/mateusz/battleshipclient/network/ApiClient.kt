package com.mateusz.battleshipclient.network

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.MINUTES)
        .readTimeout(5, TimeUnit.MINUTES)
        .writeTimeout(5, TimeUnit.MINUTES)
        .build()

    val api: BattleshipApi = Retrofit.Builder()
        .baseUrl("http://brad-home.ch:50003")
        .addConverterFactory(GsonConverterFactory.create())
        .client(client)
        .build()
        .create(BattleshipApi::class.java)
}
