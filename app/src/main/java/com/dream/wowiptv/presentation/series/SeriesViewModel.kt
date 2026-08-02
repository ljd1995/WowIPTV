package com.dream.wowiptv.presentation.series

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dream.wowiptv.R
import com.dream.wowiptv.domain.model.SeriesCategory
import com.dream.wowiptv.domain.model.SeriesItem
import com.dream.wowiptv.domain.usecase.CreateFavoriteUseCase
import com.dream.wowiptv.domain.usecase.GetSeriesCategoriesUseCase
import com.dream.wowiptv.domain.usecase.GetSeriesUseCase
import com.dream.wowiptv.presentation.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SeriesViewModel @Inject constructor(
    private val getSeriesCategoriesUseCase: GetSeriesCategoriesUseCase,
    private val getSeriesUseCase: GetSeriesUseCase,
    private val createFavoriteUseCase: CreateFavoriteUseCase,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val categories: StateFlow<UiState<List<SeriesCategory>>> = getSeriesCategoriesUseCase()
        .map { UiState.Success(it) as UiState<List<SeriesCategory>> }
        .catch { emit(UiState.Error(it.message ?: context.getString(R.string.err_load_categories))) }
        .onStart { emit(UiState.Loading) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, UiState.Loading)

    private val _selectedCategoryId = MutableStateFlow<Int?>(null)
    val selectedCategoryId: StateFlow<Int?> = _selectedCategoryId.asStateFlow()

    private val _refreshTrigger = MutableStateFlow(0L)

    val seriesList: StateFlow<UiState<List<SeriesItem>>> = combine(
        _selectedCategoryId,
        _refreshTrigger
    ) { categoryId, _ -> categoryId }
        .flatMapLatest { categoryId ->
            getSeriesUseCase(categoryId)
                .map { UiState.Success(it) as UiState<List<SeriesItem>> }
                .catch { emit(UiState.Error(it.message ?: context.getString(R.string.err_load_series))) }
                .onStart { emit(UiState.Loading) }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, UiState.Loading)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val filteredSeriesList: StateFlow<UiState<List<SeriesItem>>> = combine(
        seriesList, _searchQuery
    ) { s, query ->
        if (s !is UiState.Success) return@combine s
        if (query.isBlank()) return@combine s
        UiState.Success(s.data.filter { it.name.contains(query, ignoreCase = true) })
    }.stateIn(viewModelScope, SharingStarted.Eagerly, UiState.Loading)

    val categoryCounts: StateFlow<Map<Int, Int>> = filteredSeriesList.map { s ->
        if (s !is UiState.Success) return@map emptyMap()
        s.data.groupBy { it.categoryId }.mapValues { it.value.size }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    private val _favoriteIds = MutableStateFlow<Set<Int>>(emptySet())
    val favoriteIds: StateFlow<Set<Int>> = _favoriteIds.asStateFlow()

    init {
        viewModelScope.launch {
            createFavoriteUseCase.getSeriesFavoriteIds().collect { ids ->
                _favoriteIds.value = ids
            }
        }
    }

    fun toggleFavorite(seriesId: Int, name: String, icon: String?, categoryId: Int) {
        viewModelScope.launch {
            createFavoriteUseCase.toggleSeries(seriesId, name, icon, categoryId)
        }
    }

    fun selectCategory(id: Int?) {
        _selectedCategoryId.value = id
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun refresh() {
        _refreshTrigger.value = System.currentTimeMillis()
    }
}