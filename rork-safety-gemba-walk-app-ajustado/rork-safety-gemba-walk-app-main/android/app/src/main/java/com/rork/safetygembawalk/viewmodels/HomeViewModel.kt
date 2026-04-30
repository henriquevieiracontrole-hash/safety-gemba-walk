package com.rork.safetygembawalk.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rork.safetygembawalk.data.Inspection
import com.rork.safetygembawalk.data.InspectionRepository
import com.rork.safetygembawalk.data.InspectionStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class HomeUiState(
    val inspections: List<Inspection> = emptyList(),
    val isLoading: Boolean = false,
    val searchQuery: String = "",
    val selectedFilter: InspectionStatus? = null,
    val totalCount: Int = 0,
    val pendingCount: Int = 0,
    val completedCount: Int = 0
)

sealed interface HomeAction {
    data class OnSearchQueryChange(val query: String) : HomeAction
    data class OnFilterSelect(val status: InspectionStatus?) : HomeAction
    data class DeleteInspection(val inspection: Inspection) : HomeAction
    data object Refresh : HomeAction
}

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = InspectionRepository.getInstance(application)

    private val _uiState = MutableStateFlow(HomeUiState(isLoading = true))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val searchQuery = MutableStateFlow("")
    private val selectedFilter = MutableStateFlow<InspectionStatus?>(null)

    init {
        viewModelScope.launch {
            combine(
                repository.getAllInspections(),
                searchQuery,
                selectedFilter
            ) { inspections, query, filter ->

                val filteredInspections = inspections.filter { inspection ->
                    val firstAction = inspection.actions.firstOrNull()

                    val matchesSearch = query.isBlank() ||
                        inspection.title.contains(query, ignoreCase = true) ||
                        inspection.location.contains(query, ignoreCase = true) ||
                        inspection.inspectorName.contains(query, ignoreCase = true) ||
                        inspection.actions.any { action ->
                            action.unsafeCondition.contains(query, ignoreCase = true) ||
                                action.description.contains(query, ignoreCase = true) ||
                                action.immediateAction.contains(query, ignoreCase = true)
                        }

                    val matchesFilter = filter == null ||
                        inspection.status == filter ||
                        inspection.actions.any { it.status == filter } ||
                        firstAction?.status == filter

                    matchesSearch && matchesFilter
                }

                val total = inspections.size

                val pending = inspections.count { inspection ->
                    inspection.status == InspectionStatus.PENDING ||
                        inspection.actions.any { it.status == InspectionStatus.PENDING }
                }

                val completed = inspections.count { inspection ->
                    inspection.status == InspectionStatus.COMPLETED ||
                        (
                            inspection.actions.isNotEmpty() &&
                                inspection.actions.all { it.status == InspectionStatus.COMPLETED }
                        )
                }

                HomeUiState(
                    inspections = filteredInspections,
                    isLoading = false,
                    searchQuery = query,
                    selectedFilter = filter,
                    totalCount = total,
                    pendingCount = pending,
                    completedCount = completed
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun onAction(action: HomeAction) {
        when (action) {
            is HomeAction.OnSearchQueryChange -> {
                searchQuery.value = action.query
            }

            is HomeAction.OnFilterSelect -> {
                selectedFilter.value = action.status
            }

            is HomeAction.DeleteInspection -> {
                repository.deleteInspectionById(action.inspection.id)
            }

            is HomeAction.Refresh -> {
                // Flow atualiza automaticamente
            }
        }
    }

    companion object {
        fun Factory(application: Application): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return HomeViewModel(application) as T
                }
            }
        }
    }
}
