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
    val myTimeMs: Long? = null,
    val myDnf: Boolean = false,
    val opponentDnf: Boolean = false,
    val roundResult: RoundResult = RoundResult.PENDING,
    val myWins: Int = 0,
    val opponentWins: Int = 0,
    val players: List<String> = emptyList(),
    val message: String? = null
)

class LanViewModel(app: Application) : AndroidViewModel(app) {
    private val gameId = "demo-game"
    private val client = LanClient(app, gameId)
    private var discoveryJob: Job? = null
    private var messageJob: Job? = null
    private var serverJob: Job? = null
    private val evaluatedRounds = mutableSetOf<Int>()
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
        if(ok) { _match.value=LanMatchState(true,room.name,_name.value,players=listOf(_name.value)); attachBridge(); listenMessages() }
    }
    fun createRoom() = viewModelScope.launch {
        server?.stop(); server=LanServer(getApplication(),RoomConfig(_roomName.value,gameId,mode=SyncMode.RELIABLE))
        runCatching { server?.start() }.onSuccess { _message.value="房间已创建，等待玩家加入"; _match.value=LanMatchState(true,_roomName.value,_name.value,players=listOf(_name.value)); attachBridge(); serverJob = viewModelScope.launch { server?.reliableMessages?.collect { handleMessage(it.payload.toString(Charsets.UTF_8)) } } }.onFailure { _message.value="创建失败：${it.message}" }
    }
    fun leaveMatch() { messageJob?.cancel(); serverJob?.cancel(); LanMatchBridge.detach(); client.close(); server?.stop(); server=null; _match.value=LanMatchState() }
    private fun attachBridge() { LanMatchBridge.attach(object : LanMatchBridge.Sender { override fun publish(timeMs: Long, penalty: Int, dnf: Boolean, scramble: String) { publishFinish(timeMs, penalty, dnf, scramble) } }) }
    fun setSelectedScramble(category: Int, scramble: String) { _match.update { it.copy(selectedCategory=category,selectedScramble=scramble) } }
    fun startRound() {
        val m=_match.value
        if(!m.active || m.players.size < 2) { _match.update { it.copy(message="至少需要两名玩家才能开始") }; return }
        if(m.selectedScramble.isBlank()) { _match.update { it.copy(message="请先在原版计时器中选择分组并生成打乱") }; return }
        viewModelScope.launch { val next=m.round+1; server?.broadcastReliable("START|$next|${m.selectedCategory}|${m.selectedScramble}".toByteArray()); _match.update { it.copy(round=next,selectedCategory=m.selectedCategory,selectedScramble=m.selectedScramble,myTimeMs=null,opponentTimeMs=null,roundResult=RoundResult.PENDING,message="已发送本轮打乱") } }
    }
    fun publishFinish(timeMs: Long, penalty: Int = 0, dnf: Boolean = false, scramble: String = "") { val m=_match.value; if(m.active) viewModelScope.launch { if(m.myTimeMs != null) return@launch; _match.update { it.copy(myTimeMs=timeMs, myDnf=dnf, roundResult=RoundResult.PENDING) }; val p=MatchProtocol.finish(m.playerName,m.round,timeMs,penalty,dnf,scramble).toByteArray(); if(server!=null) { server?.broadcastReliable(p); evaluate(m.round) } else client.sendReliable(p) } }
    private fun evaluate(round: Int) { val m=_match.value; if (!evaluatedRounds.add(round)) return; val a=m.myTimeMs ?: run { evaluatedRounds.remove(round); return }; val b=m.opponentTimeMs ?: run { evaluatedRounds.remove(round); return }; val aw=if(m.myDnf) Long.MAX_VALUE else a; val bw=if(m.opponentDnf) Long.MAX_VALUE else b; val r=when { aw < bw -> RoundResult.WIN; aw > bw -> RoundResult.LOSS; else -> RoundResult.DRAW }; _match.update { it.copy(roundResult=r,myWins=it.myWins + if(r==RoundResult.WIN) 1 else 0,opponentWins=it.opponentWins + if(r==RoundResult.LOSS) 1 else 0,message=when(r){RoundResult.WIN->"本轮胜利";RoundResult.LOSS->"本轮失败";RoundResult.DRAW->"本轮平局";else->""}) } }
    private fun handleMessage(t: String) {
        val player = t.split("|", limit=4)
        if (player.firstOrNull()=="PLAYER") { val action=player.getOrNull(1); val name=player.getOrNull(3) ?: return; if(action=="JOIN") _match.update { it.copy(players=(it.players + name).distinct(), opponentName=if(it.opponentName=="对手" && name!=it.playerName) name else it.opponentName, message="$name 已加入房间") }; if(action=="LEAVE") _match.update { it.copy(players=it.players.filterNot { n -> n==name }, opponentName=if(it.opponentName==name) "对手" else it.opponentName, message="$name 已离开房间") }; return }
        val p=t.split("|", limit=4)
        if (p.firstOrNull()=="START" && p.size>=4) { val round=p[1].toIntOrNull() ?: return; _match.update { it.copy(round=round, selectedCategory=p[2].toIntOrNull() ?: it.selectedCategory, selectedScramble=p[3], myTimeMs=null, opponentTimeMs=null, roundResult=RoundResult.PENDING, message="房主已开始第 $round 轮") }; return }
        val f=MatchProtocol.parseFinish(t) ?: return
        val m=_match.value
        if(f.player!=m.playerName && f.round==m.round){ _match.update { it.copy(opponentName=f.player, opponentTimeMs=f.timeMs, opponentDnf=f.dnf, message="对手成绩已上传") }; evaluate(f.round) }
    }
    private fun listenMessages() { messageJob?.cancel(); messageJob=viewModelScope.launch { client.reliableMessages.collect { handleMessage(it.payload.toString(Charsets.UTF_8)) } } }
    fun clearMessage(){_message.value=null}
    override fun onCleared(){messageJob?.cancel();server?.stop();client.close();super.onCleared()}
}