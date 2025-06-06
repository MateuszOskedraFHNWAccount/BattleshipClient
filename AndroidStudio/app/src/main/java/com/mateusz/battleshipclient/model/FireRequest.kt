package com.example.battleships_mateusz_sander.model

data class FireRequest(
    val player: String,
    val gamekey: String,
    val x: Int,
    val y: Int
)