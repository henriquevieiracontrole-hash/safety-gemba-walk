package com.rork.safetygembawalk.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.rork.safetygembawalk.data.Inspection
import com.rork.safetygembawalk.data.InspectionRepository
import com.rork.safetygembawalk.data.InspectionStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InspectionDetailScreen(
    navController: NavController,
    inspectionId: Long
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val repository = remember { InspectionRepository.getInstance(context) }

    var inspection by remember { mutableStateOf<Inspection?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(inspectionId) {
        inspection = repository.getInspectionById(inspectionId)
    }

    if (inspection == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalhes da Inspeção") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, null)
                    }
                }
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text(
                "Inspeção #${inspection!!.id}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text("Local: ${inspection!!.location}")
            Text("Inspetor: ${inspection!!.inspectorName}")

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    navController.navigate("add_action/${inspection!!.id}")
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("➕ Adicionar nova ação")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "AÇÕES (${inspection!!.actions.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(inspection!!.actions) { action ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(3.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp)
                        ) {
                            Text(
                                action.unsafeCondition,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                action.description,
                                style = MaterialTheme.typography.bodySmall
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Button(
                                onClick = {
                                    navController.navigate(
                                        "edit_action/${inspection!!.id}/${action.id}"
                                    )
                                }
                            ) {
                                Icon(Icons.Default.Edit, null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Editar")
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    repository.updateInspection(
                        inspection!!.copy(
                            status = InspectionStatus.COMPLETED
                        )
                    )
                    navController.popBackStack()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("✅ Fechar inspeção")
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Excluir inspeção") },
            text = { Text("Deseja realmente excluir?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        repository.deleteInspectionById(inspection!!.id)
                        navController.popBackStack()
                    }
                ) {
                    Text("Excluir")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = false }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
}
