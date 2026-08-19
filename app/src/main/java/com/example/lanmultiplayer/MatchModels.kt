package com.example.lanmultiplayer

data class MatchState(
    val active: Boolean = false,
    val roomName: String = "",
    val playerName: String = "",
    val opponentName: String = "对手",
    val role: MatchRole = MatchRole.GUEST,
    val round: Int = 1,
    val scramble: String = "R U R' U'",
    val opponentTimeMs: Long? = null,
    val message: String? = null
)

enum class MatchRole { HOST, GUEST }