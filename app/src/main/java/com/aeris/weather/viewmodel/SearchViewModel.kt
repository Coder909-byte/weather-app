package com.aeris.weather.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aeris.weather.data.repository.WeatherRepository
import com.aeris.weather.domain.model.City
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*

data class SearchState(val query: String = "", val loading: Boolean = false, val results: List<City> = emptyList(), val error: String? = null, val searched: Boolean = false)

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModel(repository: WeatherRepository) : ViewModel() {
    private val query = MutableStateFlow("")
    private val retryVersion = MutableStateFlow(0)
    val state: StateFlow<SearchState> = combine(query, retryVersion) { text, _ -> text }.flatMapLatest { text ->
        flow {
            if (text.trim().length < 2) { emit(SearchState(query = text)); return@flow }
            emit(SearchState(query = text, loading = true))
            delay(350)
            try { emit(SearchState(query = text, results = repository.search(text), searched = true)) }
            catch (error: CancellationException) { throw error }
            catch (_: Exception) { emit(SearchState(query = text, searched = true, error = "Unable to search locations. Check your connection and try again.")) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchState())

    fun search(text: String) { query.value = text }
    fun retry() { retryVersion.update { it + 1 } }
}
