package com.example.battleships_mateusz_sander.model

data class FireResponse(
    val hit: Boolean,
    val shipsSunk: List<String>
)