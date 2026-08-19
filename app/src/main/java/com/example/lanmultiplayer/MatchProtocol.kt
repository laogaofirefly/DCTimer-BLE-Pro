package com.example.lanmultiplayer

/** 房间比赛协议：可靠通道传输，成绩按轮次去重。 */
object MatchProtocol {
    fun finish(player: String, round: Int, timeMs: Long, penaltyMs: Int, dnf: Boolean, scramble: String): String =
        listOf("FINISH", player, round, timeMs, penaltyMs, dnf, scramble).joinToString("|")

    data class Finish(val player: String, val round: Int, val timeMs: Long, val penaltyMs: Int, val dnf: Boolean, val scramble: String)

    fun parseFinish(text: String): Finish? {
        val p = text.split("|", limit = 7)
        if (p.size < 7 || p[0] != "FINISH") return null
        return Finish(p[1], p[2].toIntOrNull() ?: return null, p[3].toLongOrNull() ?: return null,
            p[4].toIntOrNull() ?: 0, p[5].toBoolean(), p[6])
    }
}

data class PlayerScore(
    val player: String,
    val wins: Int = 0,
    val losses: Int = 0,
    val draws: Int = 0,
    val lastTimeMs: Long? = null,
    val lastDnf: Boolean = false
)

enum class RoundResult { WIN, LOSS, DRAW, PENDING }
