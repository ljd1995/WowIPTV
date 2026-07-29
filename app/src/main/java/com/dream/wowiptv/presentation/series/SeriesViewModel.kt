package com.dream.wowiptv.presentation.series

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dream.wowiptv.domain.model.SeriesCategory
import com.dream.wowiptv.domain.model.SeriesItem
import com.dream.wowiptv.domain.usecase.GetSeriesCategoriesUseCase
import com.dream.wowiptv.domain.usecase.GetSeriesUseCase
import com.dream.wowiptv.presentation.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
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
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SeriesViewModel @Inject constructor(
    private val getSeriesCategoriesUseCase: GetSeriesCategoriesUseCase,
    private val getSeriesUseCase: GetSeriesUseCase
) : ViewModel() {

    val categories: StateFlow<UiState<List<SeriesCategory>>> = getSeriesCategoriesUseCase()
        .map { UiState.Success(it) as UiState<List<SeriesCategory>> }
        .catch { emit(UiState.Error(it.message ?: "加载分类失败")) }
        .onStart { emit(UiState.Loading) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)

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
                .catch { emit(UiState.Error(it.message ?: "加载剧集失败")) }
                .onStart { emit(UiState.Loading) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)

    fun selectCategory(id: Int?) {
        _selectedCategoryId.value = id
    }

    fun refresh() {
        _refreshTrigger.value = System.currentTimeMillis()
    }
}
