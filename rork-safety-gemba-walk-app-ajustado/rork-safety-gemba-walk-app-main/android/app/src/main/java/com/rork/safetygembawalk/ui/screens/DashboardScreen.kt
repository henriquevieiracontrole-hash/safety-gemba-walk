package com.rork.safetygembawalk.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.rork.safetygembawalk.data.Inspection
import com.rork.safetygembawalk.data.InspectionActionItem
import com.rork.safetygembawalk.data.InspectionStatus
import com.rork.safetygembawalk.ui.navigation.provideHomeViewModelFactory
import com.rork.safetygembawalk.viewmodels.AuthViewModel
import com.rork.safetygembawalk.viewmodels.HomeViewModel
import java.util.concurrent.TimeUnit

data class DashboardActionRow(
    val inspection: Inspection,
    val action: InspectionActionItem
)

private data class ChartItem(
    val label: String,
    val value: Int
)

private enum class DashboardFilter {
    INSPECTIONS,
    ALL,
    PENDING,
    WITH_OS,
    CRITICAL,
    GRAPHICS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    navController: NavController,
    homeViewModel: HomeViewModel = viewModel(
        factory = provideHomeViewModelFactory(
            androidx.compose.ui.platform.LocalContext.current.applicationContext as android.app.Application
        )
    ),
    authViewModel: AuthViewModel = viewModel()
) {
    val uiState by homeViewModel.uiState.collectAsState()
    val authState by authViewModel.authState.collectAsState()

    val currentUser = authState.user
    val isAdmin = currentUser?.isAdmin == true

    var selectedFilter by remember { mutableStateOf(DashboardFilter.INSPECTIONS) }
    var searchQuery by remember { mutableStateOf("") }

    val visibleInspections = if (isAdmin) {
        uiState.inspections
    } else {
        uiState.inspections.filter {
            it.inspectorName.equals(
                currentUser?.fullName ?: "",
                ignoreCase = true
            )
        }
    }

    val allActions = visibleInspections.flatMap { inspection ->
        inspection.actions.map { action ->
            DashboardActionRow(
                inspection = inspection,
                action = action
            )
        }
    }

    val pendingActions = allActions.filter {
        it.action.status == InspectionStatus.PENDING
    }

    val completedActions = allActions.filter {
        it.action.status == InspectionStatus.COMPLETED
    }

    val pendingWithOs = pendingActions.filter {
        it.action.hasWorkOrder
    }

    val criticalActions = pendingActions.filter {
        isCriticalAction(it)
    }

    val filteredInspections = visibleInspections
        .filter { inspection ->
            matchesInspectionSearch(
                inspection = inspection,
                search = searchQuery
            )
        }
        .sortedByDescending { it.createdAt }

    val filteredActions = when (selectedFilter) {
        DashboardFilter.INSPECTIONS -> emptyList()
        DashboardFilter.GRAPHICS -> emptyList()
        DashboardFilter.ALL -> allActions
        DashboardFilter.PENDING -> pendingActions
        DashboardFilter.WITH_OS -> pendingWithOs
        DashboardFilter.CRITICAL -> criticalActions
    }
        .filter { row ->
            matchesActionSearch(
                row = row,
                search = searchQuery
            )
        }
        .sortedWith(
            compareByDescending<DashboardActionRow> {
                isCriticalAction(it)
            }.thenByDescending {
                daysSince(it.action.createdAt)
            }
        )

    val areaRanking = pendingActions
        .groupBy { it.inspection.location.ifBlank { "Sem área" } }
        .map { (area, rows) ->
            ChartItem(
                label = area,
                value = rows.size
            )
        }
        .sortedByDescending { it.value }
        .take(5)

    val pendingAgeChart = listOf(
        ChartItem(
            label = "0-3 dias",
            value = pendingActions.count {
                daysSince(it.action.createdAt) <= 3
            }
        ),
        ChartItem(
            label = "4-7 dias",
            value = pendingActions.count {
                daysSince(it.action.createdAt) in 4..7
            }
        ),
        ChartItem(
            label = "8+ dias",
            value = pendingActions.count {
                daysSince(it.action.createdAt) >= 8
            }
        )
    )

    val osAgeChart = listOf(
        ChartItem(
            label = "0-7 dias",
            value = pendingWithOs.count {
                val days = it.action.workOrderOpenDate?.let { date ->
                    daysSince(date)
                } ?: 0
                days <= 7
            }
        ),
        ChartItem(
            label = "8-30 dias",
            value = pendingWithOs.count {
                val days = it.action.workOrderOpenDate?.let { date ->
                    daysSince(date)
                } ?: 0
                days in 8..30
            }
        ),
        ChartItem(
            label = "31+ dias",
            value = pendingWithOs.count {
                val days = it.action.workOrderOpenDate?.let { date ->
                    daysSince(date)
                } ?: 0
                days >= 31
            }
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Dashboard",
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = if (isAdmin) {
                                "Visão geral ADM"
                            } else {
                                "Minha visão"
                            },
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            navController.popBackStack()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar"
                        )
                    }
                }
            )
        }
    ) { padding ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DashboardCard(
                        title = "Inspeções",
                        value = visibleInspections.size.toString(),
                        icon = Icons.Default.Assessment,
                        selected = selectedFilter == DashboardFilter.INSPECTIONS,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            selectedFilter = DashboardFilter.INSPECTIONS
                        }
                    )

                    DashboardCard(
                        title = "Ações",
                        value = allActions.size.toString(),
                        icon = Icons.Default.PendingActions,
                        selected = selectedFilter == DashboardFilter.ALL,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            selectedFilter = DashboardFilter.ALL
                        }
                    )
                }
            }

            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DashboardCard(
                        title = "Pendentes",
                        value = pendingActions.size.toString(),
                        icon = Icons.Default.Warning,
                        selected = selectedFilter == DashboardFilter.PENDING,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            selectedFilter = DashboardFilter.PENDING
                        }
                    )

                    DashboardCard(
                            title = "Pend. com OS",
                        value = pendingWithOs.size.toString(),
                        icon = Icons.Default.Build,
                        selected = selectedFilter == DashboardFilter.WITH_OS,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            selectedFilter = DashboardFilter.WITH_OS
                       
                        }
                    )
                }
            }

            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DashboardCard(
                      title = "Críticas",
                        value = criticalActions.size.toString(),
                        icon = Icons.Default.Warning,
                        selected = selectedFilter == DashboardFilter.CRITICAL,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            selectedFilter = DashboardFilter.CRITICAL
                    
                        }
                    )

                    DashboardCard(
                         title = "Gráficos",
                        value = "3",
                        icon = Icons.Default.CheckCircle,
                        selected = selectedFilter == DashboardFilter.GRAPHICS,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            selectedFilter = DashboardFilter.GRAPHICS
                       
                        }
                    )
                }
            }

            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text("Buscar")
                    },
                    placeholder = {
                        Text("Área, OS, inspetor, ação...")
                    },
                    singleLine = true
                )
            }

            item {
                Text(
                    text = when (selectedFilter) {
                        DashboardFilter.INSPECTIONS -> "Inspeções"
                        DashboardFilter.GRAPHICS -> "Gráficos executivos"
                        DashboardFilter.ALL -> "Todas as ações"
                        DashboardFilter.PENDING -> "Pendentes"
                        DashboardFilter.WITH_OS -> "Pendentes com OS"
                        DashboardFilter.CRITICAL -> "Ações críticas"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (selectedFilter == DashboardFilter.INSPECTIONS) {
                if (filteredInspections.isEmpty()) {
                    item {
                        EmptyDashboardMessage(
                            message = "Nenhuma inspeção encontrada."
                        )
                    }
                } else {
                    items(
                        items = filteredInspections,
                        key = { it.id }
                    ) { inspection ->
                        InspectionSummaryCard(
                            inspection = inspection,
                            onClick = {
                                navController.navigate(
                                    "inspection_detail/${inspection.id}"
                                )
                            }
                        )
                    }
                }
            } else if (selectedFilter == DashboardFilter.GRAPHICS) {
                item {
                    ExecutiveChartsSection(
                        areaRanking = areaRanking,
                        pendingAgeChart = pendingAgeChart,
                        osAgeChart = osAgeChart
                    )
                }
            } else {
                if (filteredActions.isEmpty()) {
                    item {
                        EmptyDashboardMessage(
                            message = when (selectedFilter) {
                                DashboardFilter.INSPECTIONS -> "Nenhuma inspeção encontrada."
                                DashboardFilter.GRAPHICS -> "Nenhum gráfico disponível."
                                DashboardFilter.ALL -> "Nenhuma ação encontrada."
                                DashboardFilter.PENDING -> "Nenhuma pendência encontrada."
                                DashboardFilter.WITH_OS -> "Nenhuma pendência com OS encontrada."
                                DashboardFilter.CRITICAL -> "Nenhuma ação crítica encontrada."
                            }
                        )
                    }
                } else {
                    items(
                        items = filteredActions,
                        key = { "${it.inspection.id}_${it.action.id}" }
                    ) { row ->
                        PendingActionCard(
                            row = row,
                            onClick = {
                                navController.navigate(
                                    "inspection_detail/${row.inspection.id}"
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExecutiveChartsSection(
    areaRanking: List<ChartItem>,
    pendingAgeChart: List<ChartItem>,
    osAgeChart: List<ChartItem>
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ChartCard(
            title = "Ranking por área",
            subtitle = "Áreas com mais ações pendentes",
            items = areaRanking
        )

        ChartCard(
            title = "Idade das pendências",
            subtitle = "Tempo em aberto das ações pendentes",
            items = pendingAgeChart
        )

        ChartCard(
            title = "OS abertas por tempo",
            subtitle = "Tempo em aberto das OS vinculadas às pendências",
            items = osAgeChart
        )
    }
}

@Composable
private fun ChartCard(
    title: String,
    subtitle: String,
    items: List<ChartItem>
) {
    val safeItems = items.ifEmpty {
        listOf(
            ChartItem(
                label = "Sem dados",
                value = 0
            )
        )
    }

    val maxValue = safeItems.maxOfOrNull { it.value } ?: 0

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 3.dp
        ),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            safeItems.forEach { item ->
                ChartBarRow(
                    label = item.label,
                    value = item.value,
                    maxValue = maxValue
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
                )
            }
        }
    }
}

@Composable
private fun ChartBarRow(
    label: String,
    value: Int,
    maxValue: Int
) {
    val barWeight = if (maxValue <= 0) {
        0.01f
    } else {
        (value.toFloat() / maxValue.toFloat()).coerceAtLeast(0.05f)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            modifier = Modifier.width(90.dp),
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .height(18.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(20.dp)
                )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(barWeight)
                    .background(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(20.dp)
                    )
            )
        }

        Spacer(
            modifier = Modifier.width(8.dp)
        )

        Text(
            text = value.toString(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun DashboardCard(
    title: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    Card(
        onClick = {
            onClick?.invoke()
        },
        modifier = modifier,
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (selected) 8.dp else 3.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                Color(0xFFE3F2FD)
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )

            Spacer(
                modifier = Modifier.height(6.dp)
            )

            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )

            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
        }
    }
}

@Composable
private fun InspectionSummaryCard(
    inspection: Inspection,
    onClick: () -> Unit
) {
    val pendingCount = inspection.actions.count {
        it.status == InspectionStatus.PENDING
    }

    val completedCount = inspection.actions.count {
        it.status == InspectionStatus.COMPLETED
    }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 3.dp
        ),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Text(
                text = inspection.title.ifBlank {
                    "Inspeção ${inspection.id}"
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Text(
                text = "Área: ${inspection.location.ifBlank { "-" }}"
            )

            Text(
                text = "Inspetor: ${inspection.inspectorName.ifBlank { "-" }}"
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Text(
                text = "Ações: ${inspection.actions.size}",
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = "Pendentes: $pendingCount"
            )

            Text(
                text = "Concluídas: $completedCount"
            )
        }
    }
}

@Composable
private fun PendingActionCard(
    row: DashboardActionRow,
    onClick: () -> Unit
) {
    val action = row.action
    val inspection = row.inspection

    val pendingDays = daysSince(action.createdAt)

    val osDays = action.workOrderOpenDate?.let {
        daysSince(it)
    }

    val isCritical = pendingDays >= 8
    val isWarning = pendingDays in 4..7

    val statusColor = when {
        isCritical -> Color(0xFFD32F2F)
        isWarning -> Color(0xFFF9A825)
        else -> MaterialTheme.colorScheme.primary
    }

    val statusText = when {
        isCritical -> "CRÍTICO"
        isWarning -> "ATENÇÃO"
        else -> "NORMAL"
    }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 3.dp
        ),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = inspection.title.ifBlank {
                        "Inspeção ${inspection.id}"
                    },
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Surface(
                    color = statusColor,
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        text = statusText,
                        color = Color.White,
                        modifier = Modifier.padding(
                            horizontal = 10.dp,
                            vertical = 4.dp
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Text(
                text = action.unsafeCondition.ifBlank {
                    "Ação sem título"
                },
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Text(
                text = "Área: ${inspection.location.ifBlank { "-" }}"
            )

            Text(
                text = "Inspetor: ${inspection.inspectorName.ifBlank { "-" }}"
            )

            Spacer(
                modifier = Modifier.height(6.dp)
            )

            Text(
                text = "Dias pendente: $pendingDays",
                color = statusColor,
                fontWeight = FontWeight.Bold
            )

            if (action.hasWorkOrder) {
                Spacer(
                    modifier = Modifier.height(6.dp)
                )

                Text(
                    text = "OS aberta: ${action.workOrderNumber ?: "-"}",
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = "Dias com OS aberta: ${osDays ?: "-"}",
                    color = if ((osDays ?: 0) >= 7) {
                        Color(0xFFD32F2F)
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
            } else {
                Spacer(
                    modifier = Modifier.height(6.dp)
                )

                Text(
                    text = "OS: não aberta",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun EmptyDashboardMessage(
    message: String
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(16.dp)
        )
    }
}

private fun matchesInspectionSearch(
    inspection: Inspection,
    search: String
): Boolean {
    val query = search.trim()

    if (query.isBlank()) {
        return true
    }

    return inspection.title.contains(query, ignoreCase = true) ||
        inspection.location.contains(query, ignoreCase = true) ||
        inspection.inspectorName.contains(query, ignoreCase = true) ||
        inspection.actions.any { action ->
            action.unsafeCondition.contains(query, ignoreCase = true) ||
                action.description.contains(query, ignoreCase = true) ||
                action.immediateAction.contains(query, ignoreCase = true) ||
                (action.workOrderNumber ?: "").contains(query, ignoreCase = true)
        }
}

private fun matchesActionSearch(
    row: DashboardActionRow,
    search: String
): Boolean {
    val query = search.trim()

    if (query.isBlank()) {
        return true
    }

    return row.inspection.title.contains(query, ignoreCase = true) ||
        row.inspection.location.contains(query, ignoreCase = true) ||
        row.inspection.inspectorName.contains(query, ignoreCase = true) ||
        row.action.unsafeCondition.contains(query, ignoreCase = true) ||
        row.action.description.contains(query, ignoreCase = true) ||
        row.action.immediateAction.contains(query, ignoreCase = true) ||
        (row.action.workOrderNumber ?: "").contains(query, ignoreCase = true)
}

private fun isCriticalAction(
    row: DashboardActionRow
): Boolean {
    val pendingDays = daysSince(row.action.createdAt)

    val osDays = row.action.workOrderOpenDate?.let {
        daysSince(it)
    } ?: 0

    return pendingDays >= 8 || osDays >= 7
}

private fun daysSince(
    timestamp: Long
): Long {
    val diff = System.currentTimeMillis() - timestamp

    return TimeUnit.MILLISECONDS
        .toDays(diff)
        .coerceAtLeast(0)
}
