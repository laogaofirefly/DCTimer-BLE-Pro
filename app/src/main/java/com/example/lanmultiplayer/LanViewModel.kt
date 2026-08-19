package com.example.lanmultiplayer

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class LanViewModel(app: Application) : AndroidViewModel(app) {
    private val gameId = "demo-game"
    private val client = LanClient(app, gameId)
    private var discoveryJob: Job? = null
    private var matchJob: Job? = null
    private var server: LanServer? = null
    private val _name = MutableStateFlow("Player")
    private val _roomName = MutableStateFlow("我的房间")
    private val _message = MutableStateFlow<String?>(null)
    private val _searching = MutableStateFlow(false)
    private val _match = MutableStateFlow(MatchState())
    val name = _name.asStateFlow(); val roomName = _roomName.asStateFlow()
    val message = _message.asStateFlow(); val searching = _searching.asStateFlow()
    val match = _match.asStateFlow(); val rooms = client.rooms
    val state = client.state; val stats = client.stats
    fun setName(value: String) { _name.value = value.take(24) }
    fun setRoomName(value: String) { _roomName.value = value.take(24) }
    fun search() {
        discoveryJob?.cancel()
        discoveryJob = viewModelScope.launch { _searching.value = true; runCatching { client.startDiscovery() }; awaitCancellation() }
    }
    fun stopSearch() { discoveryJob?.cancel(); discoveryJob = null; client.stopDiscovery(); _searching.value = false }
    fun join(room: Room) = viewModelScope.launch {
        val ok = client.join(room, _name.value)
        _message.value = if (ok) "已加入：${room.name}" else "加入失败，请检查 Wi-Fi 和房间状态"
        if (ok) { _match.value = MatchState(true, room.name, _name.value, role = MatchRole.GUEST); observeGuestMessages() }
    }
    fun createRoom() = viewModelScope.launch {
        server?.stop(); server = LanServer(getApplication(), RoomConfig(_roomName.value, gameId, mode = SyncMode.RELIABLE))
        runCatching { server?.start() }.onSuccess {
            _message.value = "房间已创建，等待玩家加入"
            _match.value = MatchState(true, _roomName.value, _name.value, role = MatchRole.HOST, scramble = newScramble())
        }.onFailure { _message.value = "创建失败：${it.message}" }
    }
    fun startRound() {
        val m = _match.value; if (!m.active || m.role != MatchRole.HOST) return
        viewModelScope.launch { server?.broadcastReliable("START|${m.round}|${m.scramble}".toByteArray()); _match.update { it.copy(message = "本轮开始") } }
    }
    fun publishFinish(timeMs: Long) {
        val m = _match.value; if (!m.active) return
        viewModelScope.launch { val p = "FINISH|${m.playerName}|$timeMs".toByteArray(); if (m.role == MatchRole.HOST) server?.broadcastReliable(p) else client.sendReliable(p) }
    }
    private fun observeGuestMessages() {
        matchJob?.cancel(); matchJob = viewModelScope.launch {
            client.reliableMessages.collect { message ->
                val t = message.payload.toString(Charsets.UTF_8)
                when {
                    t.startsWith("START|") -> { val p=t.split("|", limit=3); _match.update { it.copy(round=p.getOrNull(1)?.toIntOrNull() ?: it.round, scramble=p.getOrNull(2) ?: it.scramble, message="本轮开始") } }
                    t.startsWith("FINISH|") -> { val p=t.split("|", limit=3); val who=p.getOrNull(1) ?: "对手"; val time=p.getOrNull(2)?.toLongOrNull() ?: return@collect; if (who != _match.value.playerName) _match.update { it.copy(opponentName=who, opponentTimeMs=time, message="对手成绩：${formatTime(time)}") } }
                }
            }
        }
    }
    fun clearMessage() { _message.value = null }
    fun leaveMatch() { matchJob?.cancel(); matchJob=null; client.close(); server?.stop(); server=null; _match.value=MatchState() }
    private fun newScramble() = listOf("R U R' U'", "F R U R' U' F'", "L U2 L' F' L U2 L' F").random()
    private fun formatTime(ms: Long) = "${ms/1000}.${(ms%1000).toString().padStart(3,'0')}"
    override fun onCleared() { matchJob?.cancel(); server?.stop(); client.close(); super.onCleared() }
}