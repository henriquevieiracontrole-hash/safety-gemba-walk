package com.rork.safetygembawalk.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.rork.safetygembawalk.data.InspectionStatus
import com.rork.safetygembawalk.ui.navigation.provideReportViewModelFactory
import com.rork.safetygembawalk.viewmodels.ReportAction
import com.rork.safetygembawalk.viewmodels.ReportViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    navController: NavController,
    reportType: String = "pdf",
    viewModel: ReportViewModel = viewModel(
        factory = provideReportViewModelFactory(
            LocalContext.current.applicationContext as android.app.Application
        )
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var showDateFromPicker by remember { mutableStateOf(false) }
    var showDateToPicker by remember { mutableStateOf(false) }
    var showFilterMenu by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.onAction(ReportAction.ClearError)
        }
    }

    val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (reportType == "pdf") "Relatório PDF" else "Apresentação PPT",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Header Card
            HeaderCard(reportType = reportType)

            Spacer(modifier = Modifier.height(24.dp))

            // Filters Section
            Text(
                text = "Filtros",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Date Range
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Período",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // From Date
                        DateField(
                            label = "De",
                            date = uiState.dateFrom,
                            onClick = { showDateFromPicker = true },
                            modifier = Modifier.weight(1f)
                        )

                        // To Date
                        DateField(
                            label = "Até",
                            date = uiState.dateTo,
                            onClick = { showDateToPicker = true },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Status Filter
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Status",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Medium
                        )

                        Box {
                            OutlinedButton(
                                onClick = { showFilterMenu = true },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FilterList,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    uiState.statusFilter?.let { getStatusLabel(it) } ?: "Todos"
                                )
                            }

                            DropdownMenu(
                                expanded = showFilterMenu,
                                onDismissRequest = { showFilterMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Todos os status") },
                                    onClick = {
                                        viewModel.onAction(ReportAction.SetStatusFilter(null))
                                        showFilterMenu = false
                                    }
                                )
                                InspectionStatus.entries.forEach { status ->
                                    DropdownMenuItem(
                                        text = { Text(getStatusLabel(status)) },
                                        onClick = {
                                            viewModel.onAction(ReportAction.SetStatusFilter(status))
                                            showFilterMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Status chips
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        InspectionStatus.entries.forEach { status ->
                            FilterChip(
                                selected = uiState.statusFilter == status,
                                onClick = {
                                    viewModel.onAction(
                                        ReportAction.SetStatusFilter(
                                            if (uiState.statusFilter == status) null else status
                                        )
                                    )
                                },
                                label = { Text(getStatusLabel(status)) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Summary
            SummaryCard(
                totalInspections = uiState.inspections.size,
                filteredInspections = getFilteredCount(uiState)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Generate Button
            if (reportType == "pdf") {
                GeneratePdfButton(
                    isLoading = uiState.isLoading,
                    isGenerated = uiState.pdfGenerated,
                    onGenerate = { viewModel.onAction(ReportAction.GeneratePdf) }
                )
            } else {
                GeneratePptButton(
                    isLoading = uiState.isLoading,
                    isGenerated = uiState.pptGenerated,
                    onGenerate = { viewModel.onAction(ReportAction.GeneratePpt) }
                )
            }

            // Email Button
            if (uiState.pdfGenerated || uiState.pptGenerated) {
                Spacer(modifier = Modifier.height(16.dp))
                EmailButton(
                    onSend = { viewModel.onAction(ReportAction.SendEmail) }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Date Pickers
    if (showDateFromPicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = uiState.dateFrom
        )
        DatePickerDialog(
            onDismissRequest = { showDateFromPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let {
                            viewModel.onAction(ReportAction.SetDateRange(it, uiState.dateTo))
                        }
                        showDateFromPicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDateFromPicker = false }) {
                    Text("Cancelar")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showDateToPicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = uiState.dateTo
        )
        DatePickerDialog(
            onDismissRequest = { showDateToPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let {
                            viewModel.onAction(ReportAction.SetDateRange(uiState.dateFrom, it))
                        }
                        showDateToPicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDateToPicker = false }) {
                    Text("Cancelar")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun HeaderCard(reportType: String) {
    val icon = if (reportType == "pdf") Icons.Default.PictureAsPdf else Icons.Default.Slideshow
    val title = if (reportType == "pdf") "Relatório em PDF" else "Apresentação em PPT"
    val description = if (reportType == "pdf") {
        "Gere um relatório completo em PDF com todas as inspeções selecionadas"
    } else {
        "Gere uma apresentação em PowerPoint para reuniões e apresentações"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (reportType == "pdf") {
                MaterialTheme.colorScheme.errorContainer
            } else {
                MaterialTheme.colorScheme.primaryContainer
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = if (reportType == "pdf") {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                }
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun DateField(
    label: String,
    date: Long?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    val dateText = date?.let { dateFormatter.format(Date(it)) } ?: "Selecionar"

    Card(
        onClick = onClick,
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = dateText,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun SummaryCard(
    totalInspections: Int,
    filteredInspections: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = totalInspections.toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Total",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(40.dp)
                    .background(MaterialTheme.colorScheme.outline)
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = filteredInspections.toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
                Text(
                    text = "Filtradas",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun GeneratePdfButton(
    isLoading: Boolean,
    isGenerated: Boolean,
    onGenerate: () -> Unit
) {
    Button(
        onClick = onGenerate,
        modifier = Modifier.fillMaxWidth(),
        enabled = !isLoading,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = MaterialTheme.colorScheme.onError
            )
        } else if (isGenerated) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("PDF Gerado!")
        } else {
            Icon(
                imageVector = Icons.Default.PictureAsPdf,
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Gerar Relatório PDF")
        }
    }
}

@Composable
private fun GeneratePptButton(
    isLoading: Boolean,
    isGenerated: Boolean,
    onGenerate: () -> Unit
) {
    Button(
        onClick = onGenerate,
        modifier = Modifier.fillMaxWidth(),
        enabled = !isLoading,
        shape = RoundedCornerShape(12.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = MaterialTheme.colorScheme.onPrimary
            )
        } else if (isGenerated) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("PPT Gerado!")
        } else {
            Icon(
                imageVector = Icons.Default.Slideshow,
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Gerar Apresentação PPT")
        }
    }
}

@Composable
private fun EmailButton(
    onSend: () -> Unit
) {
    FilledTonalButton(
        onClick = onSend,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Email,
            contentDescription = null
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text("Enviar por Email")
    }
}

private fun getFilteredCount(uiState: com.rork.safetygembawalk.viewmodels.ReportUiState): Int {
    return uiState.inspections.filter { inspection ->
        val matchesDateFrom = uiState.dateFrom == null || inspection.createdAt >= uiState.dateFrom
        val matchesDateTo = uiState.dateTo == null || inspection.createdAt <= uiState.dateTo
        val matchesStatus = uiState.statusFilter == null || inspection.status == uiState.statusFilter
        matchesDateFrom && matchesDateTo && matchesStatus
    }.size
}

private fun getStatusLabel(status: InspectionStatus): String {
    return when (status) {
        InspectionStatus.PENDING -> "Pendente"
        InspectionStatus.IN_PROGRESS -> "Em Andamento"
        InspectionStatus.COMPLETED -> "Concluída"
        InspectionStatus.CANCELLED -> "Cancelada"
    }
}
