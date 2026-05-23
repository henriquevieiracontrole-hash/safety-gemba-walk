package com.rork.safetygembawalk.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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

private enum class DashboardFilter {
    ALL,
    PENDING,
    WITH_OS,
    CRITICAL
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

    var selectedFilter by remember { mutableStateOf(DashboardFilter.ALL) }

    val visibleInspections =
        if (isAdmin) {
            uiState.inspections
        } else {
            uiState.inspections.filter {
                it.inspectorName.equals(
                    currentUser?.fullName ?: "",
                    ignoreCase = true
                )
            }
        }

    val allActions =
        visibleInspections.flatMap { inspection ->
            inspection.actions.map { action ->
                DashboardActionRow(inspection, action)
            }
        }

    val pendingActions =
        allActions.filter {
            it.action.status == InspectionStatus.PENDING
        }

    val completedActions =
        allActions.filter {
            it.action.status == InspectionStatus.COMPLETED
        }

    val pendingWithOs =
        pendingActions.filter {
            it.action.hasWorkOrder
        }

    val pendingWithoutOs =
        pendingActions.filter {
            !it.action.hasWorkOrder
        }

    val criticalActions =
        pendingActions.filter { row ->
            isCriticalAction(row)
        }

    val filteredPendingActions =
        when (selectedFilter) {
            DashboardFilter.ALL -> pendingActions
            DashboardFilter.PENDING -> pendingActions
            DashboardFilter.WITH_OS -> pendingWithOs
            DashboardFilter.CRITICAL -> criticalActions
        }.sortedWith(
            compareByDescending<DashboardActionRow> {
                isCriticalAction(it)
            }.thenByDescending {
                daysSince(it.action.createdAt)
            }
        )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Dashboard",
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            if (isAdmin)
                                "Visão geral ADM"
                            else
                                "Minha visão",
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
                            Icons.AutoMirrored.Filled.ArrowBack,
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
                        modifier = Modifier.weight(1f)
                    )

                    DashboardCard(
                        title = "Ações",
                        value = allActions.size.toString(),
                        icon = Icons.Default.PendingActions,
                        modifier = Modifier.weight(1f)
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
                        modifier = Modifier.weight(1f)
                    )

                    DashboardCard(
                        title = "Concluídas",
                        value = completedActions.size.toString(),
                        icon = Icons.Default.CheckCircle,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DashboardCard(
                        title = "Pend. com OS",
                        value = pendingWithOs.size.toString(),
                        icon = Icons.Default.Build,
                        modifier = Modifier.weight(1f)
                    )

                    DashboardCard(
                        title = "Críticas",
                        value = criticalActions.size.toString(),
                        icon = Icons.Default.Warning,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                DashboardFilterChips(
                    selectedFilter = selectedFilter,
                    onFilterSelected = { selectedFilter = it }
                )
            }

            item {
                Text(
                    text = when (selectedFilter) {
                        DashboardFilter.ALL -> "Ações pendentes"
                        DashboardFilter.PENDING -> "Pendentes"
                        DashboardFilter.WITH_OS -> "Pendentes com OS"
                        DashboardFilter.CRITICAL -> "Ações críticas"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (filteredPendingActions.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = when (selectedFilter) {
                                DashboardFilter.ALL -> "Nenhuma ação pendente encontrada."
                                DashboardFilter.PENDING -> "Nenhuma pendência encontrada."
                                DashboardFilter.WITH_OS -> "Nenhuma pendência com OS encontrada."
                                DashboardFilter.CRITICAL -> "Nenhuma ação crítica encontrada."
                            },
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            } else {
                items(
                    items = filteredPendingActions,
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

@Composable
private fun DashboardFilterChips(
    selectedFilter: DashboardFilter,
    onFilterSelected: (DashboardFilter) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        DashboardFilterChip(
            text = "Todas",
            selected = selectedFilter == DashboardFilter.ALL,
            onClick = { onFilterSelected(DashboardFilter.ALL) }
        )

        DashboardFilterChip(
            text = "Pendentes",
            selected = selectedFilter == DashboardFilter.PENDING,
            onClick = { onFilterSelected(DashboardFilter.PENDING) }
        )

        DashboardFilterChip(
            text = "Com OS",
            selected = selectedFilter == DashboardFilter.WITH_OS,
            onClick = { onFilterSelected(DashboardFilter.WITH_OS) }
        )

        DashboardFilterChip(
            text = "Críticas",
            selected = selectedFilter == DashboardFilter.CRITICAL,
            onClick = { onFilterSelected(DashboardFilter.CRITICAL) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DashboardFilterChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    ElevatedFilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = text,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
            )
        },
        colors = FilterChipDefaults.elevatedFilterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    )
}

@Composable
private fun DashboardCard(
    title: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(
            defaultElevation = 3.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(28.dp)
            )

            Spacer(
                modifier = Modifier.height(6.dp)
            )

            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall
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

    val osDays =
        action.workOrderOpenDate?.let {
            daysSince(it)
        }

    val isCritical = pendingDays >= 8
    val isWarning = pendingDays in 4..7

    val statusColor =
        when {
            isCritical -> Color(0xFFD32F2F)
            isWarning -> Color(0xFFF9A825)
            else -> MaterialTheme.colorScheme.primary
        }

    val statusText =
        when {
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
                    color =
                        if ((osDays ?: 0) >= 7)
                            Color(0xFFD32F2F)
                        else
                            MaterialTheme.colorScheme.onSurface
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

private fun isCriticalAction(
    row: DashboardActionRow
): Boolean {
    val pendingDays = daysSince(row.action.createdAt)
    val osDays = row.action.workOrderOpenDate?.let { daysSince(it) } ?: 0
    return pendingDays >= 8 || osDays >= 7
}

private fun daysSince(
    timestamp: Long
): Long {
    val diff =
        System.currentTimeMillis() - timestamp

    return TimeUnit.MILLISECONDS
        .toDays(diff)
        .coerceAtLeast(0)
}
