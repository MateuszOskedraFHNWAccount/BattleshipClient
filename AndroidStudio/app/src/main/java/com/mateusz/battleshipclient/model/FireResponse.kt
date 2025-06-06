package com.mateusz.battleshipclient.model

data class FireResponse(
    val hit: Boolean,
    val shipsSunk: List<String>
)