package com.example.battleships_mateusz_sander.model

data class JoinGameRequest(
    val player: String,
    val gamekey: String,
    val ships: List<Ship>
)
