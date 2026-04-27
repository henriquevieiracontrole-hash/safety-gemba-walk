package com.rork.safetygembawalk.viewmodels

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.rork.safetygembawalk.data.AiAnalysisResult
import com.rork.safetygembawalk.data.Inspection
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

    private var currentInspectionId: Long = 0

    init {
        val user = userRepo.getCurrentUser()
        user?.let {
            _formState.value = _formState.value.copy(
                inspectorName = it.fullName
            )
        }
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
            is InspectionAction.ClearError -> _formState.value =
                _formState.value.copy(errorMessage = null)

            is InspectionAction.AnalyzeWithAi -> analyzeWithAi()
            is InspectionAction.ApplyAiAnalysis -> applyAiAnalysis(action.result)
        }
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

        val inspection = Inspection(
            id = currentInspectionId,
            unsafeCondition = state.unsafeCondition,
            description = state.description,
            immediateAction = state.immediateAction,
            hasWorkOrder = state.hasWorkOrder,
            workOrderNumber = state.workOrderNumber.takeIf { it.isNotBlank() },
            workOrderOpenDate = state.workOrderOpenDate,
            category = state.category,
            isImmediateAction = state.isImmediateAction,
            location = state.location,
            inspectorName = state.inspectorName,
            beforePhotoPath = state.beforePhotoPath,
            afterPhotoPath = state.afterPhotoPath,
            status = state.status,
            createdAt = if (currentInspectionId == 0L)
                System.currentTimeMillis()
            else
                repository.getInspectionById(currentInspectionId)?.createdAt
                    ?: System.currentTimeMillis()
        )

        repository.insertInspection(inspection)
        _formState.value = state.copy(isLoading = false, isSaved = true)
    }

    private fun loadInspection(id: Long) {
        if (id == 0L) return

        val inspection = repository.getInspectionById(id)
        inspection?.let {
            currentInspectionId = inspection.id
            _formState.value = InspectionFormState(
                unsafeCondition = inspection.unsafeCondition,
                description = inspection.description,
                immediateAction = inspection.immediateAction,
                hasWorkOrder = inspection.hasWorkOrder,
                workOrderNumber = inspection.workOrderNumber ?: "",
                workOrderOpenDate = inspection.workOrderOpenDate,
                category = inspection.category,
                isImmediateAction = inspection.isImmediateAction,
                location = inspection.location,
                inspectorName = inspection.inspectorName,
                beforePhotoPath = inspection.beforePhotoPath,
                afterPhotoPath = inspection.afterPhotoPath,
                status = inspection.status
            )
        }
    }

    fun resetForm() {
        currentInspectionId = 0
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
