package com.mateusz.battleshipclient.model

data class ShipPosition(
    val ship: String,
    val x: Int,
    val y: Int,
    val orientation: String
)

data class JoinGameRequest(
    val player: String,
    val gamekey: String,
    val ships: List<ShipPosition>
)
