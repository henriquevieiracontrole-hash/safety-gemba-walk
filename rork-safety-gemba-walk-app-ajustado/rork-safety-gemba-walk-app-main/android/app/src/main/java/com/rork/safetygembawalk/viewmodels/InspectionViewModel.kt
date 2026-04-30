package com.rork.safetygembawalk.viewmodels

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.rork.safetygembawalk.data.AiAnalysisResult
import com.rork.safetygembawalk.data.Inspection
import com.rork.safetygembawalk.data.InspectionActionItem
import com.rork.safetygembawalk.data.InspectionRepository
import com.rork.safetygembawalk.data.InspectionStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

data class InspectionFormState(
    val unsafeCondition: String = "",
    val description: String = "",
    val immediateAction: String = "",
    val hasWorkOrder: Boolean = false,
    val workOrderNumber: String = "",
    val workOrderOpenDate: Long? = null,
    val category: String = "Segurança",
    val isImmediateAction: Boolean = false,
    val location: String = "",
    val inspectorName: String = "",
    val beforePhotoUri: Uri? = null,
    val beforePhotoPath: String? = null,
    val afterPhotoUri: Uri? = null,
    val afterPhotoPath: String? = null,
    val status: InspectionStatus = InspectionStatus.PENDING,
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val errorMessage: String? = null,
    val isAnalyzing: Boolean = false,
    val aiAnalysisResult: AiAnalysisResult? = null
)

sealed interface InspectionAction {
    data class UpdateUnsafeCondition(val value: String) : InspectionAction
    data class UpdateDescription(val value: String) : InspectionAction
    data class UpdateImmediateAction(val value: String) : InspectionAction
    data class UpdateHasWorkOrder(val value: Boolean) : InspectionAction
    data class UpdateWorkOrderNumber(val value: String) : InspectionAction
    data class UpdateWorkOrderOpenDate(val value: Long?) : InspectionAction
    data class UpdateCategory(val value: String) : InspectionAction
    data class UpdateIsImmediateAction(val value: Boolean) : InspectionAction
    data class UpdateLocation(val value: String) : InspectionAction
    data class UpdateInspectorName(val value: String) : InspectionAction
    data class UpdateStatus(val value: InspectionStatus) : InspectionAction
    data class SetBeforePhoto(val uri: Uri) : InspectionAction
    data class SetAfterPhoto(val uri: Uri) : InspectionAction
    data object SaveInspection : InspectionAction
    data class LoadInspection(val id: Long) : InspectionAction
    data class StartNewAction(val inspectionId: Long) : InspectionAction
    data class LoadAction(val inspectionId: Long, val actionId: Long) : InspectionAction
    data object ClearError : InspectionAction
    data object AnalyzeWithAi : InspectionAction
    data class ApplyAiAnalysis(val result: AiAnalysisResult) : InspectionAction
}

class InspectionViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = InspectionRepository.getInstance(application)
    private val userRepo = com.rork.safetygembawalk.data.UserRepository(application)
    private val context = application

    private val _formState = MutableStateFlow(InspectionFormState())
    val formState: StateFlow<InspectionFormState> = _formState.asStateFlow()

    private var currentInspectionId: Long = 0L
    private var currentActionId: Long = 0L

    init {
        val user = userRepo.getCurrentUser()
        _formState.value = _formState.value.copy(
            inspectorName = user?.fullName ?: ""
        )
    }

    fun onAction(action: InspectionAction) {
        when (action) {
            is InspectionAction.UpdateUnsafeCondition -> _formState.value =
                _formState.value.copy(unsafeCondition = action.value)

            is InspectionAction.UpdateDescription -> _formState.value =
                _formState.value.copy(description = action.value)

            is InspectionAction.UpdateImmediateAction -> _formState.value =
                _formState.value.copy(immediateAction = action.value)

            is InspectionAction.UpdateHasWorkOrder -> _formState.value =
                _formState.value.copy(
                    hasWorkOrder = action.value,
                    workOrderOpenDate = if (action.value) System.currentTimeMillis() else null
                )

            is InspectionAction.UpdateWorkOrderNumber -> _formState.value =
                _formState.value.copy(workOrderNumber = action.value)

            is InspectionAction.UpdateWorkOrderOpenDate -> _formState.value =
                _formState.value.copy(workOrderOpenDate = action.value)

            is InspectionAction.UpdateCategory -> _formState.value =
                _formState.value.copy(category = action.value)

            is InspectionAction.UpdateIsImmediateAction -> _formState.value =
                _formState.value.copy(isImmediateAction = action.value)

            is InspectionAction.UpdateLocation -> _formState.value =
                _formState.value.copy(location = action.value)

            is InspectionAction.UpdateInspectorName -> _formState.value =
                _formState.value.copy(inspectorName = action.value)

            is InspectionAction.UpdateStatus -> _formState.value =
                _formState.value.copy(status = action.value)

            is InspectionAction.SetBeforePhoto -> {
                val path = saveImageToInternalStorage(action.uri, "before_")
                _formState.value = _formState.value.copy(
                    beforePhotoUri = action.uri,
                    beforePhotoPath = path
                )
            }

            is InspectionAction.SetAfterPhoto -> {
                val path = saveImageToInternalStorage(action.uri, "after_")
                _formState.value = _formState.value.copy(
                    afterPhotoUri = action.uri,
                    afterPhotoPath = path
                )
            }

            is InspectionAction.SaveInspection -> saveInspection()
            is InspectionAction.LoadInspection -> loadInspection(action.id)
            is InspectionAction.StartNewAction -> startNewAction(action.inspectionId)
            is InspectionAction.LoadAction -> loadAction(action.inspectionId, action.actionId)

            is InspectionAction.ClearError -> _formState.value =
                _formState.value.copy(errorMessage = null)

            is InspectionAction.AnalyzeWithAi -> analyzeWithAi()
            is InspectionAction.ApplyAiAnalysis -> applyAiAnalysis(action.result)
        }
    }

    private fun startNewAction(inspectionId: Long) {
        currentInspectionId = inspectionId
        currentActionId = 0L

        val inspection = repository.getInspectionById(inspectionId)
        val user = userRepo.getCurrentUser()

        _formState.value = InspectionFormState(
            location = inspection?.location ?: "",
            inspectorName = inspection?.inspectorName ?: user?.fullName ?: "",
            category = "Segurança"
        )
    }

    private fun loadAction(inspectionId: Long, actionId: Long) {
        val inspection = repository.getInspectionById(inspectionId) ?: return
        val action = inspection.actions.firstOrNull { it.id == actionId } ?: return

        currentInspectionId = inspection.id
        currentActionId = action.id

        _formState.value = InspectionFormState(
            unsafeCondition = action.unsafeCondition,
            description = action.description,
            immediateAction = action.immediateAction,
            hasWorkOrder = action.hasWorkOrder,
            workOrderNumber = action.workOrderNumber ?: "",
            workOrderOpenDate = action.workOrderOpenDate,
            category = action.category,
            isImmediateAction = action.isImmediateAction,
            location = inspection.location,
            inspectorName = inspection.inspectorName,
            beforePhotoPath = action.beforePhotoPath,
            afterPhotoPath = action.afterPhotoPath,
            status = action.status
        )
    }

    private fun saveImageToInternalStorage(uri: Uri, prefix: String): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val filename = "${prefix}${UUID.randomUUID()}.jpg"
            val file = File(context.filesDir, filename)

            FileOutputStream(file).use { outputStream ->
                inputStream?.copyTo(outputStream)
            }

            inputStream?.close()
            file.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    private fun saveInspection() {
        val state = _formState.value

        if (state.unsafeCondition.isBlank() || state.description.isBlank()) {
            _formState.value = state.copy(errorMessage = "Preencha os campos obrigatórios")
            return
        }

        if (state.beforePhotoPath == null) {
            _formState.value = state.copy(errorMessage = "Tire uma foto da condição insegura")
            return
        }

        _formState.value = state.copy(isLoading = true)

        val actionItem = InspectionActionItem(
            id = currentActionId,
            unsafeCondition = state.unsafeCondition,
            description = state.description,
            immediateAction = state.immediateAction,
            hasWorkOrder = state.hasWorkOrder,
            workOrderNumber = state.workOrderNumber.takeIf { it.isNotBlank() },
            workOrderOpenDate = state.workOrderOpenDate,
            category = state.category,
            isImmediateAction = state.isImmediateAction,
            beforePhotoPath = state.beforePhotoPath,
            afterPhotoPath = state.afterPhotoPath,
            status = state.status
        )

        if (currentInspectionId == 0L) {
            val newActionId = System.currentTimeMillis()

            val inspection = Inspection(
                id = 0L,
                title = state.unsafeCondition,
                location = state.location,
                inspectorName = state.inspectorName,
                status = InspectionStatus.IN_PROGRESS,
                actions = listOf(actionItem.copy(id = newActionId))
            )

            repository.insertInspection(inspection)
        } else {
            val existing = repository.getInspectionById(currentInspectionId)

            if (existing != null) {
                val updatedStatus =
                    if (existing.actions.isNotEmpty() && existing.actions.all { it.status == InspectionStatus.COMPLETED }) {
                        InspectionStatus.COMPLETED
                    } else {
                        InspectionStatus.IN_PROGRESS
                    }

                val updatedInspection = existing.copy(
                    title = existing.title.ifBlank { state.unsafeCondition },
                    location = state.location,
                    inspectorName = state.inspectorName,
                    status = updatedStatus
                )

                repository.updateInspection(updatedInspection)

                if (currentActionId == 0L) {
                    repository.addAction(currentInspectionId, actionItem)
                } else {
                    repository.updateAction(currentInspectionId, actionItem)
                }
            }
        }

        _formState.value = state.copy(isLoading = false, isSaved = true)
    }

    private fun loadInspection(id: Long) {
        if (id == 0L) return

        val inspection = repository.getInspectionById(id) ?: return
        currentInspectionId = inspection.id

        val firstAction = inspection.actions.firstOrNull()
        currentActionId = firstAction?.id ?: 0L

        _formState.value = InspectionFormState(
            unsafeCondition = firstAction?.unsafeCondition ?: inspection.title,
            description = firstAction?.description ?: "",
            immediateAction = firstAction?.immediateAction ?: "",
            hasWorkOrder = firstAction?.hasWorkOrder ?: false,
            workOrderNumber = firstAction?.workOrderNumber ?: "",
            workOrderOpenDate = firstAction?.workOrderOpenDate,
            category = firstAction?.category ?: "Segurança",
            isImmediateAction = firstAction?.isImmediateAction ?: false,
            location = inspection.location,
            inspectorName = inspection.inspectorName,
            beforePhotoPath = firstAction?.beforePhotoPath,
            afterPhotoPath = firstAction?.afterPhotoPath,
            status = firstAction?.status ?: inspection.status
        )
    }

    fun resetForm() {
        currentInspectionId = 0L
        currentActionId = 0L

        val user = userRepo.getCurrentUser()
        _formState.value = InspectionFormState(
            inspectorName = user?.fullName ?: ""
        )
    }

    private fun analyzeWithAi() {
        _formState.value = _formState.value.copy(
            isAnalyzing = false,
            errorMessage = "Função de IA removida nesta versão"
        )
    }

    private fun applyAiAnalysis(result: AiAnalysisResult) {
        _formState.value = _formState.value.copy(
            unsafeCondition = result.riskDescription.take(100),
            description = result.riskDescription,
            aiAnalysisResult = null
        )
    }

    companion object {
        fun Factory(application: Application): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return InspectionViewModel(application) as T
                }
            }
        }
    }
}
