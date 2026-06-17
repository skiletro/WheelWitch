package com.skiletro.wheelwitch.viewmodel

import android.app.Application
import androidx.compose.runtime.Immutable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.skiletro.wheelwitch.model.LeaderboardEntry
import com.skiletro.wheelwitch.model.RaceStats
import com.skiletro.wheelwitch.model.Room
import com.skiletro.wheelwitch.model.ServerConnectivity
import com.skiletro.wheelwitch.model.ServerHealth
import com.skiletro.wheelwitch.model.TimeTrialTrack
import com.skiletro.wheelwitch.network.VersionFileParser
import com.skiletro.wheelwitch.R
import com.skiletro.wheelwitch.util.PrefsKeys
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

enum class OnlineMenuPage {
    Hub, Rooms, Leaderboard, Health, RaceStats, TimeTrial
}

@Immutable
sealed class RoomsState {
    data object Idle : RoomsState()
    data object Loading : RoomsState()
    data class Success(
        val rooms: List<Room>,
        val playerCount: Int?,
        val serverConnectivity: ServerConnectivity
    ) : RoomsState()
    data class Error(val message: String) : RoomsState()
}

@Immutable
sealed class LeaderboardState {
    data object Idle : LeaderboardState()
    data object Loading : LeaderboardState()
    data class Success(
        val entries: List<LeaderboardEntry>,
        val hasMore: Boolean,
        val page: Int
    ) : LeaderboardState()
    data class Error(val message: String) : LeaderboardState()
}

@Immutable
sealed class HealthState {
    data object Idle : HealthState()
    data object Loading : HealthState()
    data class Success(val health: ServerHealth) : HealthState()
    data class Error(val message: String) : HealthState()
}

@Immutable
sealed class RaceStatsState {
    data object Idle : RaceStatsState()
    data object Loading : RaceStatsState()
    data class Success(val stats: RaceStats, val lastRefreshedAt: Long) : RaceStatsState()
    data class Error(val message: String) : RaceStatsState()
}

class OnlineViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences(PrefsKeys.RACE_STATS_PREFS, Application.MODE_PRIVATE)

    private val _currentPage = MutableStateFlow(OnlineMenuPage.Hub)
    val currentPage: StateFlow<OnlineMenuPage> = _currentPage.asStateFlow()

    private val _roomsState = MutableStateFlow<RoomsState>(RoomsState.Idle)
    val roomsState: StateFlow<RoomsState> = _roomsState.asStateFlow()

    private val _leaderboardState = MutableStateFlow<LeaderboardState>(LeaderboardState.Idle)
    val leaderboardState: StateFlow<LeaderboardState> = _leaderboardState.asStateFlow()

    private val _healthState = MutableStateFlow<HealthState>(HealthState.Idle)
    val healthState: StateFlow<HealthState> = _healthState.asStateFlow()

    private val _raceStatsState = MutableStateFlow<RaceStatsState>(RaceStatsState.Idle)
    val raceStatsState: StateFlow<RaceStatsState> = _raceStatsState.asStateFlow()

    private val _tracks = MutableStateFlow<List<TimeTrialTrack>>(emptyList())
    val tracks: StateFlow<List<TimeTrialTrack>> = _tracks.asStateFlow()

    private val _vrMultiplier = MutableStateFlow<Float?>(null)
    val vrMultiplier: StateFlow<Float?> = _vrMultiplier.asStateFlow()

    private val leaderboardRequests = Channel<Unit>(Channel.CONFLATED)

    val playerCount: Int?
        get() = (_roomsState.value as? RoomsState.Success)?.playerCount

    init {
        initialFetch()
        launchLeaderboardConsumer()
    }

    private fun launchLeaderboardConsumer() {
        viewModelScope.launch {
            leaderboardRequests.consumeAsFlow().collect { _ ->
                val currentState = _leaderboardState.value
                if (currentState is LeaderboardState.Success && !currentState.hasMore) return@collect
                val nextPage = when (currentState) {
                    is LeaderboardState.Success -> currentState.page + 1
                    else -> 1
                }
                if (nextPage == 1) {
                    _leaderboardState.value = LeaderboardState.Loading
                }
                val result = withContext(Dispatchers.IO) {
                    VersionFileParser.fetchLeaderboard(page = nextPage)
                }
                val snapshot = currentState
                result.onSuccess { response ->
                    val existing = (snapshot as? LeaderboardState.Success)?.entries ?: emptyList()
                    _leaderboardState.value = LeaderboardState.Success(
                        entries = existing + response.entries,
                        hasMore = response.hasMore,
                        page = nextPage
                    )
                }.onFailure { e ->
                    _leaderboardState.value = if (snapshot is LeaderboardState.Success) {
                        snapshot
                    } else {
                        LeaderboardState.Error(e.message ?: getApplication<Application>().getString(R.string.vm_leaderboard_failed))
                    }
                }
            }
        }
    }

    private fun initialFetch() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val roomsResult = VersionFileParser.fetchRooms()
                if (roomsResult.isSuccess) {
                    val rooms = roomsResult.getOrThrow()
                    _roomsState.value = RoomsState.Success(
                        rooms = rooms,
                        playerCount = rooms.sumOf { it.players.size },
                        serverConnectivity = ServerConnectivity.Online
                    )
                } else {
                    _roomsState.value = RoomsState.Success(
                        rooms = emptyList(),
                        playerCount = null,
                        serverConnectivity = ServerConnectivity.Offline
                    )
                }
                _vrMultiplier.value = VersionFileParser.fetchVrMultiplier().getOrNull()
            }
        }
    }

    fun navigateTo(page: OnlineMenuPage) {
        _currentPage.value = page
        when (page) {
            OnlineMenuPage.Rooms -> fetchRooms()
            OnlineMenuPage.Leaderboard -> fetchLeaderboard()
            OnlineMenuPage.Health -> fetchHealth()
            OnlineMenuPage.RaceStats -> loadRaceStats()
            OnlineMenuPage.TimeTrial -> fetchTracks()
            else -> {}
        }
    }

    fun goBack() {
        _currentPage.value = OnlineMenuPage.Hub
    }

    fun fetchRooms() {
        _currentPage.value = OnlineMenuPage.Rooms
        viewModelScope.launch {
            _roomsState.value = RoomsState.Loading
            val result = withContext(Dispatchers.IO) {
                VersionFileParser.fetchRooms()
            }
            result.onSuccess { rooms ->
                val old = _roomsState.value
                val connectivity = if (old is RoomsState.Success) old.serverConnectivity else ServerConnectivity.Online
                _roomsState.value = RoomsState.Success(
                    rooms = rooms,
                    playerCount = rooms.sumOf { it.players.size },
                    serverConnectivity = connectivity
                )
            }.onFailure { e ->
                _roomsState.value = RoomsState.Error(e.message ?: getApplication<Application>().getString(R.string.vm_rooms_failed))
            }
        }
    }

    fun fetchLeaderboard() {
        leaderboardRequests.trySend(Unit)
    }

    fun fetchHealth() {
        viewModelScope.launch {
            _healthState.value = HealthState.Loading
            val result = withContext(Dispatchers.IO) {
                VersionFileParser.fetchHealth()
            }
            result.onSuccess { health ->
                _healthState.value = HealthState.Success(health)
            }.onFailure { e ->
                val liveOk = withContext(Dispatchers.IO) {
                    VersionFileParser.fetchHealthLive().getOrDefault(false)
                }
                if (liveOk) {
                    val simpleHealth = ServerHealth(
                        status = "ok",
                        database = null,
                        postgresql = null,
                        retroWfcApi = null,
                        memory = null
                    )
                    _healthState.value = HealthState.Success(simpleHealth)
                } else {
                    _healthState.value = HealthState.Error(e.message ?: getApplication<Application>().getString(R.string.vm_health_failed))
                }
            }
        }
    }

    private fun loadRaceStats() {
        val cached = loadRaceStatsCache()
        if (cached != null) {
            _raceStatsState.value = RaceStatsState.Success(cached.first, cached.second)
            if (System.currentTimeMillis() - cached.second < MAX_CACHE_AGE_MS) return
        }
        fetchRaceStats()
    }

    fun fetchRaceStats() {
        viewModelScope.launch {
            _raceStatsState.value = RaceStatsState.Loading
            val result = withContext(Dispatchers.IO) {
                VersionFileParser.fetchGlobalRaceStatsRaw()
            }
            result.onSuccess { (stats, rawJson) ->
                val now = System.currentTimeMillis()
                saveRaceStatsCache(rawJson, now)
                _raceStatsState.value = RaceStatsState.Success(stats, now)
            }.onFailure { e ->
                val fallback = loadRaceStatsCache()
                if (fallback != null) {
                    _raceStatsState.value = RaceStatsState.Success(fallback.first, fallback.second)
                } else {
                    _raceStatsState.value = RaceStatsState.Error(e.message ?: getApplication<Application>().getString(R.string.vm_race_stats_failed))
                }
            }
        }
    }

    private fun saveRaceStatsCache(rawJson: String, timestamp: Long) {
        val wrapper = JSONObject().apply {
            put("json", rawJson)
            put("_cachedAt", timestamp)
        }
        prefs.edit().putString(PrefsKeys.RACE_STATS_KEY, wrapper.toString()).apply()
    }

    private fun loadRaceStatsCache(): Pair<RaceStats, Long>? {
        val raw = prefs.getString(PrefsKeys.RACE_STATS_KEY, null) ?: return null
        return try {
            val wrapper = JSONObject(raw)
            val json = wrapper.getString("json")
            val stats = com.skiletro.wheelwitch.model.parseRaceStats(json)
            val timestamp = wrapper.optLong("_cachedAt", 0L)
            Pair(stats, timestamp)
        } catch (_: Exception) {
            null
        }
    }

    fun fetchTracks() {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                VersionFileParser.fetchTracks()
            }
            result.onSuccess { tracks ->
                _tracks.value = tracks
            }
        }
    }

    companion object {
        private const val MAX_CACHE_AGE_MS = 24 * 60 * 60 * 1000L
    }
}
