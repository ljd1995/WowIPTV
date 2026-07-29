package com.dream.wowiptv.presentation.movies

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dream.wowiptv.domain.model.VodCategory
import com.dream.wowiptv.domain.model.VodStream
import com.dream.wowiptv.domain.usecase.GetVodCategoriesUseCase
import com.dream.wowiptv.domain.usecase.GetVodStreamsUseCase
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
class MoviesViewModel @Inject constructor(
    private val getVodCategoriesUseCase: GetVodCategoriesUseCase,
    private val getVodStreamsUseCase: GetVodStreamsUseCase
) : ViewModel() {

    val categories: StateFlow<UiState<List<VodCategory>>> = getVodCategoriesUseCase()
        .map { UiState.Success(it) as UiState<List<VodCategory>> }
        .catch { emit(UiState.Error(it.message ?: "加载分类失败")) }
        .onStart { emit(UiState.Loading) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)

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
                .catch { emit(UiState.Error(it.message ?: "加载电影失败")) }
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
