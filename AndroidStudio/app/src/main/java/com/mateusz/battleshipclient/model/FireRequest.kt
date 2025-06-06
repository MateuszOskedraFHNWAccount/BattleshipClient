package com.mateusz.battleshipclient.model

data class FireRequest(
    val player: String,
    val gamekey: String,
    val x: Int,
    val y: Int
)