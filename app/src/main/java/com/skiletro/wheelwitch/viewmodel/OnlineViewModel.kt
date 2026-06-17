package com.skiletro.wheelwitch.viewmodel

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skiletro.wheelwitch.model.LeaderboardEntry
import com.skiletro.wheelwitch.model.RaceStats
import com.skiletro.wheelwitch.model.ServerConnectivity
import com.skiletro.wheelwitch.model.ServerHealth
import com.skiletro.wheelwitch.model.TimeTrialTrack
import com.skiletro.wheelwitch.network.VersionFileParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class OnlineMenuPage {
    Hub, Rooms, Leaderboard, Health, RaceStats, TimeTrial
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
    data class Success(val stats: RaceStats) : RaceStatsState()
    data class Error(val message: String) : RaceStatsState()
}

class OnlineViewModel : ViewModel() {
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

    val playerCount: Int?
        get() = (_roomsState.value as? RoomsState.Success)?.playerCount

    val serverConnectivity: ServerConnectivity
        get() = (_roomsState.value as? RoomsState.Success)?.serverConnectivity ?: ServerConnectivity.Unknown

    init {
        initialFetch()
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
            OnlineMenuPage.RaceStats -> fetchRaceStats()
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
                _roomsState.value = RoomsState.Error(e.message ?: "Failed to load rooms")
            }
        }
    }

    fun fetchLeaderboard() {
        val currentState = _leaderboardState.value
        if (currentState is LeaderboardState.Loading) return
        if (currentState is LeaderboardState.Success && !currentState.hasMore) return

        val nextPage = when (currentState) {
            is LeaderboardState.Success -> currentState.page + 1
            else -> 1
        }
        viewModelScope.launch {
            _leaderboardState.value = if (nextPage == 1) {
                LeaderboardState.Loading
            } else {
                LeaderboardState.Loading
            }
            val result = withContext(Dispatchers.IO) {
                VersionFileParser.fetchLeaderboard(page = nextPage)
            }
            result.onSuccess { response ->
                val existing = (currentState as? LeaderboardState.Success)?.entries ?: emptyList()
                _leaderboardState.value = LeaderboardState.Success(
                    entries = existing + response.entries,
                    hasMore = response.hasMore,
                    page = nextPage
                )
            }.onFailure { e ->
                _leaderboardState.value = if (currentState is LeaderboardState.Success) {
                    currentState
                } else {
                    LeaderboardState.Error(e.message ?: "Failed to load leaderboard")
                }
            }
        }
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
                _healthState.value = HealthState.Error(e.message ?: "Failed to fetch server health")
            }
        }
    }

    fun fetchRaceStats() {
        viewModelScope.launch {
            _raceStatsState.value = RaceStatsState.Loading
            val result = withContext(Dispatchers.IO) {
                VersionFileParser.fetchGlobalRaceStats()
            }
            result.onSuccess { stats ->
                _raceStatsState.value = RaceStatsState.Success(stats)
            }.onFailure { e ->
                _raceStatsState.value = RaceStatsState.Error(e.message ?: "Failed to fetch race stats")
            }
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

    fun refreshHealth() {
        fetchHealth()
    }
}
