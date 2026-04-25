package com.rork.safetygembawalk.viewmodels

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
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
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ReportUiState(
    val inspections: List<Inspection> = emptyList(),
    val isLoading: Boolean = false,
    val pdfGenerated: Boolean = false,
    val pptGenerated: Boolean = false,
    val pdfFilePath: String? = null,
    val pptFilePath: String? = null,
    val errorMessage: String? = null,
    val dateFrom: Long? = null,
    val dateTo: Long? = null,
    val statusFilter: InspectionStatus? = null
)

sealed interface ReportAction {
    data class SetDateRange(val from: Long?, val to: Long?) : ReportAction
    data class SetStatusFilter(val status: InspectionStatus?) : ReportAction
    data object GeneratePdf : ReportAction
    data object GeneratePpt : ReportAction
    data object SendEmail : ReportAction
    data object ClearError : ReportAction
    data object ResetGenerated : ReportAction
}

class ReportViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = InspectionRepository.getInstance(application)
    private val context = application

    private val _uiState = MutableStateFlow(ReportUiState())
    val uiState: StateFlow<ReportUiState> = _uiState.asStateFlow()

    init {
        // Load inspections
        viewModelScope.launch {
            repository.getAllInspections().collect { inspections ->
                _uiState.value = _uiState.value.copy(inspections = inspections)
            }
        }
    }

    fun onAction(action: ReportAction) {
        when (action) {
            is ReportAction.SetDateRange -> {
                _uiState.value = _uiState.value.copy(dateFrom = action.from, dateTo = action.to)
            }
            is ReportAction.SetStatusFilter -> {
                _uiState.value = _uiState.value.copy(statusFilter = action.status)
            }
            is ReportAction.GeneratePdf -> generatePdf()
            is ReportAction.GeneratePpt -> generatePpt()
            is ReportAction.SendEmail -> sendEmail()
            is ReportAction.ClearError -> {
                _uiState.value = _uiState.value.copy(errorMessage = null)
            }
            is ReportAction.ResetGenerated -> {
                _uiState.value = _uiState.value.copy(pdfGenerated = false, pptGenerated = false)
            }
        }
    }

    private fun getFilteredInspections(): List<Inspection> {
        val state = _uiState.value
        return state.inspections.filter { inspection ->
            val matchesDateFrom = state.dateFrom == null || inspection.createdAt >= state.dateFrom
            val matchesDateTo = state.dateTo == null || inspection.createdAt <= state.dateTo
            val matchesStatus = state.statusFilter == null || inspection.status == state.statusFilter
            matchesDateFrom && matchesDateTo && matchesStatus
        }
    }

    private fun generatePdf() {
        _uiState.value = _uiState.value.copy(isLoading = true, pdfGenerated = false)

        try {
            val inspections = getFilteredInspections()
            if (inspections.isEmpty()) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Nenhuma inspeção encontrada para os filtros selecionados"
                )
                return
            }

            val pdfGenerator = PdfReportGenerator(context)
            val filePath = pdfGenerator.generateReport(inspections)

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                pdfGenerated = true,
                pdfFilePath = filePath
            )
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                errorMessage = "Erro ao gerar PDF: ${e.message}"
            )
        }
    }

    private fun generatePpt() {
        _uiState.value = _uiState.value.copy(isLoading = true, pptGenerated = false)

        try {
            val inspections = getFilteredInspections()
            if (inspections.isEmpty()) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Nenhuma inspeção encontrada para os filtros selecionados"
                )
                return
            }

            val pptGenerator = PptReportGenerator(context)
            val filePath = pptGenerator.generateReport(inspections)

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                pptGenerated = true,
                pptFilePath = filePath
            )
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                errorMessage = "Erro ao gerar PPT: ${e.message}"
            )
        }
    }

    private fun sendEmail() {
        val state = _uiState.value

        if (!state.pdfGenerated && !state.pptGenerated) {
            _uiState.value = state.copy(errorMessage = "Gere um relatório primeiro (PDF ou PPT)")
            return
        }

        val attachments = mutableListOf<Uri>()

        state.pdfFilePath?.let { path ->
            val file = File(path)
            if (file.exists()) {
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
                attachments.add(uri)
            }
        }

        state.pptFilePath?.let { path ->
            val file = File(path)
            if (file.exists()) {
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
                attachments.add(uri)
            }
        }

        if (attachments.isEmpty()) {
            _uiState.value = state.copy(errorMessage = "Nenhum arquivo para enviar")
            return
        }

        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val currentDate = dateFormat.format(Date())

        val emailIntent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "message/rfc822"
            putExtra(Intent.EXTRA_SUBJECT, "Relatório Safety Gemba Walk - $currentDate")
            putExtra(
                Intent.EXTRA_TEXT,
                "Segue em anexo o relatório de inspeções de segurança (Safety Gemba Walk).\n\n" +
                "Data de geração: $currentDate\n" +
                "Total de inspeções no relatório: ${getFilteredInspections().size}"
            )
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(attachments))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(emailIntent, "Enviar relatório via:")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    companion object {
        fun Factory(application: Application): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ReportViewModel(application) as T
                }
            }
        }
    }
}
