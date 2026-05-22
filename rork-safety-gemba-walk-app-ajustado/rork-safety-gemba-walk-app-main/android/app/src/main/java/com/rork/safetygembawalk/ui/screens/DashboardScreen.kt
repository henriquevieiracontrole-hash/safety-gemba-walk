package com.rork.safetygembawalk.ui.screens

import androidx.compose.foundation.layout.*
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

    val visibleInspections = if (isAdmin) {
        uiState.inspections
    } else {
        uiState.inspections.filter {
            it.inspectorName.equals(currentUser?.fullName ?: "", ignoreCase = true)
        }
    }

    val allActions = visibleInspections.flatMap { inspection ->
        inspection.actions.map { action ->
            DashboardActionRow(inspection, action)
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

    val pendingWithoutOs = pendingActions.filter {
        !it.action.hasWorkOrder
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Dashboard", fontWeight = FontWeight.Bold)
                        Text(
                            if (isAdmin) "Visão geral ADM" else "Minha visão",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
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
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
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
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
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
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DashboardCard(
                        title = "Pend. com OS",
                        value = pendingWithOs.size.toString(),
                        icon = Icons.Default.Build,
                        modifier = Modifier.weight(1f)
                    )

                    DashboardCard(
                        title = "Pend. sem OS",
                        value = pendingWithoutOs.size.toString(),
                        icon = Icons.Default.Warning,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Text(
                    "Ações pendentes",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }

            if (pendingActions.isEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "Nenhuma ação pendente encontrada.",
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            } else {
                items(pendingActions.sortedByDescending { it.action.createdAt }) { row ->
                    PendingActionCard(
                        row = row,
                        onClick = {
                            navController.navigate("inspection_detail/${row.inspection.id}")
                        }
                    )
                }
            }
        }
    }
}
@Composable
private fun DashboardCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.height(6.dp))
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(title, style = MaterialTheme.typography.labelSmall)
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
    val osDays = action.workOrderOpenDate?.let { daysSince(it) }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {

            Text(
                inspection.title.ifBlank { "Inspeção ${inspection.id}" },
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                action.unsafeCondition.ifBlank { "Ação sem título" },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text("Área: ${inspection.location.ifBlank { "-" }}")
            Text("Inspetor: ${inspection.inspectorName.ifBlank { "-" }}")
            Text("Dias pendente: $pendingDays")

            if (action.hasWorkOrder) {
                Text("OS aberta: ${action.workOrderNumber ?: "-"}")
                Text("Dias com OS aberta: ${osDays ?: "-"}")
            } else {
                Text("OS: não aberta")
            }
        }
    }
}

private fun daysSince(timestamp: Long): Long {
    val diff = System.currentTimeMillis() - timestamp
    return TimeUnit.MILLISECONDS.toDays(diff).coerceAtLeast(0)
}
