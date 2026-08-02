package com.dream.wowiptv.presentation.movies

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dream.wowiptv.R
import com.dream.wowiptv.data.local.AppPreferences
import com.dream.wowiptv.domain.model.VodCategory
import com.dream.wowiptv.domain.model.VodStream
import com.dream.wowiptv.domain.usecase.CreateFavoriteUseCase
import com.dream.wowiptv.domain.usecase.GetVodCategoriesUseCase
import com.dream.wowiptv.domain.usecase.GetVodStreamsUseCase
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
class MoviesViewModel @Inject constructor(
    private val getVodCategoriesUseCase: GetVodCategoriesUseCase,
    private val getVodStreamsUseCase: GetVodStreamsUseCase,
    private val createFavoriteUseCase: CreateFavoriteUseCase,
    private val appPreferences: AppPreferences,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val gridColumns: StateFlow<Int> = appPreferences.contentGridColumns
        .stateIn(viewModelScope, SharingStarted.Eagerly, 2)

    val categories: StateFlow<UiState<List<VodCategory>>> = getVodCategoriesUseCase()
        .map { UiState.Success(it) as UiState<List<VodCategory>> }
        .catch { emit(UiState.Error(it.message ?: context.getString(R.string.err_load_categories))) }
        .onStart { emit(UiState.Loading) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, UiState.Loading)

    private val _selectedCategoryId = MutableStateFlow<Int?>(null)
    val selectedCategoryId: StateFlow<Int?> = _selectedCategoryId.asStateFlow()

    private val _refreshTrigger = MutableStateFlow(0L)

    val streams: StateFlow<UiState<List<VodStream>>> = combine(
        _selectedCategoryId,
        _refreshTrigger
    ) { categoryId, _ -> categoryId }
        .flatMapLatest { categoryId ->
            getVodStreamsUseCase(categoryId)
                .map { UiState.Success(it) as UiState<List<VodStream>> }
                .catch { emit(UiState.Error(it.message ?: context.getString(R.string.err_load_movies))) }
                .onStart { emit(UiState.Loading) }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, UiState.Loading)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val filteredStreams: StateFlow<UiState<List<VodStream>>> = combine(
        streams, _searchQuery
    ) { s, query ->
        if (s !is UiState.Success) return@combine s
        if (query.isBlank()) return@combine s
        UiState.Success(s.data.filter { it.name.contains(query, ignoreCase = true) })
    }.stateIn(viewModelScope, SharingStarted.Eagerly, UiState.Loading)

    val categoryCounts: StateFlow<Map<Int, Int>> = filteredStreams.map { s ->
        if (s !is UiState.Success) return@map emptyMap()
        s.data.groupBy { it.categoryId }.mapValues { it.value.size }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    private val _favoriteIds = MutableStateFlow<Set<Int>>(emptySet())
    val favoriteIds: StateFlow<Set<Int>> = _favoriteIds.asStateFlow()

    init {
        viewModelScope.launch {
            createFavoriteUseCase.getMovieFavoriteIds().collect { ids ->
                _favoriteIds.value = ids
            }
        }
    }

    fun toggleFavorite(vodId: Int, name: String, icon: String?, categoryId: Int) {
        viewModelScope.launch {
            createFavoriteUseCase.toggleMovie(vodId, name, icon, categoryId)
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