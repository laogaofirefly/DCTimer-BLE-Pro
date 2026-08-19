package com.example.lanmultiplayer

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class LanMatchState(
    val active: Boolean = false,
    val roomName: String = "",
    val playerName: String = "",
    val opponentName: String = "对手",
    val round: Int = 1,
    val selectedCategory: Int = 32,
    val selectedScramble: String = "",
    val opponentTimeMs: Long? = null,
    val message: String? = null
)

class LanViewModel(app: Application) : AndroidViewModel(app) {
    private val gameId = "demo-game"
    private val client = LanClient(app, gameId)
    private var discoveryJob: Job? = null
    private var messageJob: Job? = null
    private var server: LanServer? = null
    private val _name = MutableStateFlow("Player")
    private val _roomName = MutableStateFlow("我的房间")
    private val _message = MutableStateFlow<String?>(null)
    private val _searching = MutableStateFlow(false)
    private val _match = MutableStateFlow(LanMatchState())
    val name = _name.asStateFlow(); val roomName = _roomName.asStateFlow()
    val message = _message.asStateFlow(); val searching = _searching.asStateFlow()
    val match = _match.asStateFlow(); val rooms = client.rooms
    val state = client.state; val stats = client.stats
    fun setName(value: String) { _name.value = value.take(24) }
    fun setRoomName(value: String) { _roomName.value = value.take(24) }
    fun search() { discoveryJob?.cancel(); discoveryJob = viewModelScope.launch { _searching.value=true; runCatching { client.startDiscovery() }; awaitCancellation() } }
    fun stopSearch() { discoveryJob?.cancel(); discoveryJob=null; client.stopDiscovery(); _searching.value=false }
    fun join(room: Room) = viewModelScope.launch {
        val ok=client.join(room,_name.value); _message.value=if(ok) "已加入：${room.name}" else "加入失败，请检查 Wi-Fi 和房间状态"
        if(ok) { _match.value=LanMatchState(true,room.name,_name.value); listenMessages() }
    }
    fun createRoom() = viewModelScope.launch {
        server?.stop(); server=LanServer(getApplication(),RoomConfig(_roomName.value,gameId,mode=SyncMode.RELIABLE))
        runCatching { server?.start() }.onSuccess { _message.value="房间已创建，等待玩家加入"; _match.value=LanMatchState(true,_roomName.value,_name.value) }.onFailure { _message.value="创建失败：${it.message}" }
    }
    fun leaveMatch() { messageJob?.cancel(); client.close(); server?.stop(); server=null; _match.value=LanMatchState() }
    fun setSelectedScramble(category: Int, scramble: String) { _match.update { it.copy(selectedCategory=category,selectedScramble=scramble) } }
    fun startRound() {
        val m=_match.value
        if(!m.active || m.selectedScramble.isBlank()) { _match.update { it.copy(message="请先在原版计时器中选择分组并生成打乱") }; return }
        viewModelScope.launch { server?.broadcastReliable("START|${m.selectedCategory}|${m.selectedScramble}".toByteArray()); _match.update { it.copy(message="已发送本轮打乱") } }
    }
    fun publishFinish(timeMs: Long) { val m=_match.value; if(m.active) viewModelScope.launch { val p="FINISH|${m.playerName}|$timeMs".toByteArray(); if(server!=null) server?.broadcastReliable(p) else client.sendReliable(p) } }
    private fun listenMessages() { messageJob?.cancel(); messageJob=viewModelScope.launch { client.reliableMessages.collect { val t=it.payload.toString(Charsets.UTF_8); when { t.startsWith("START|")->{val p=t.split("|",limit=3); _match.update { s->s.copy(selectedCategory=p[1].toIntOrNull()?:s.selectedCategory,selectedScramble=p.getOrNull(2)?:"",message="房主已选择本轮打乱") }}; t.startsWith("FINISH|")->{val p=t.split("|",limit=3); val ms=p.getOrNull(2)?.toLongOrNull()?:return@collect; if(p.getOrNull(1)!=_match.value.playerName)_match.update{s->s.copy(opponentName=p[1],opponentTimeMs=ms,message="对手成绩已上传")}} } } } }
    fun clearMessage(){_message.value=null}
    override fun onCleared(){messageJob?.cancel();server?.stop();client.close();super.onCleared()}
}