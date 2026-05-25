package com.rork.safetygembawalk.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.rork.safetygembawalk.viewmodels.PdfReportGenerator
import java.io.File

class InspectionRepository private constructor(context: Context) {
    private val appContext = context.applicationContext
    private val json = Json { ignoreUnknownKeys = true }
    private val firebaseSyncService = FirebaseSyncService()

    private val prefs: SharedPreferences =
        appContext.getSharedPreferences("inspections", Context.MODE_PRIVATE)

    private val inspectionsKey = "inspections_list"

    private val _inspections = MutableStateFlow<List<Inspection>>(emptyList())
    val inspections: StateFlow<List<Inspection>> = _inspections.asStateFlow()

    init {
        loadInspections()
    }

    private fun loadInspections() {
        val data = prefs.getString(inspectionsKey, "[]") ?: "[]"
        _inspections.value = try {
            json.decodeFromString(data)
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveInspections(list: List<Inspection>) {
        prefs.edit().putString(inspectionsKey, json.encodeToString(list)).apply()
    }

    private fun syncToFirebase(inspection: Inspection) {
        try {
            firebaseSyncService.syncInspection(inspection)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun syncPdfToFirebase(inspection: Inspection) {
        if (inspection.actions.isEmpty()) return

        Thread {
            try {
                val pdfGenerator = PdfReportGenerator(appContext)
                val filePath = pdfGenerator.generateReport(listOf(inspection))
                val file = File(filePath)

                if (file.exists()) {
                    firebaseSyncService.uploadInspectionPdf(
                        inspection = inspection,
                        pdfFile = file
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    private fun syncInspectionAndPdf(inspection: Inspection) {
        syncToFirebase(inspection)
        syncPdfToFirebase(inspection)
    }

    fun getAllInspections(): Flow<List<Inspection>> = inspections

    fun getInspectionsByStatus(status: InspectionStatus): Flow<List<Inspection>> =
        inspections.map { list ->
            list.filter { inspection ->
                inspection.status == status
            }
        }

    fun getInspectionById(id: Long): Inspection? {
        return _inspections.value.find { it.id == id }
    }

    fun insertInspection(inspection: Inspection): Long {
        val currentList = _inspections.value.toMutableList()
        val newId = if (inspection.id == 0L) System.currentTimeMillis() else inspection.id

        val newInspection = inspection.copy(
            id = newId,
            updatedAt = System.currentTimeMillis()
        )

        val existingIndex = currentList.indexOfFirst { it.id == newId }

        if (existingIndex >= 0) {
            currentList[existingIndex] = newInspection
        } else {
            currentList.add(0, newInspection)
        }

        _inspections.value = currentList
        saveInspections(currentList)
        syncInspectionAndPdf(newInspection)

        return newId
    }

    fun addAction(
        inspectionId: Long,
        action: InspectionActionItem
    ) {
        val currentList = _inspections.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == inspectionId }

        if (index >= 0) {
            val inspection = currentList[index]

            val newAction = action.copy(
                id = if (action.id == 0L) System.currentTimeMillis() else action.id,
                updatedAt = System.currentTimeMillis()
            )

            val updatedInspection = inspection.copy(
                actions = inspection.actions + newAction,
                updatedAt = System.currentTimeMillis()
            )

            currentList[index] = updatedInspection

            _inspections.value = currentList
            saveInspections(currentList)
            syncInspectionAndPdf(updatedInspection)
        }
    }

    fun updateAction(
        inspectionId: Long,
        action: InspectionActionItem
    ) {
        val currentList = _inspections.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == inspectionId }

        if (index >= 0) {
            val inspection = currentList[index]

            val updatedActions = inspection.actions.map {
                if (it.id == action.id) {
                    action.copy(updatedAt = System.currentTimeMillis())
                } else {
                    it
                }
            }

            val updatedInspection = inspection.copy(
                actions = updatedActions,
                updatedAt = System.currentTimeMillis()
            )

            currentList[index] = updatedInspection

            _inspections.value = currentList
            saveInspections(currentList)
            syncInspectionAndPdf(updatedInspection)
        }
    }

    fun deleteAction(
        inspectionId: Long,
        actionId: Long
    ) {
        val currentList = _inspections.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == inspectionId }

        if (index >= 0) {
            val inspection = currentList[index]

            val updatedInspection = inspection.copy(
                actions = inspection.actions.filterNot { it.id == actionId },
                updatedAt = System.currentTimeMillis()
            )

            currentList[index] = updatedInspection

            _inspections.value = currentList
            saveInspections(currentList)
            syncInspectionAndPdf(updatedInspection)
        }
    }

    fun updateInspection(inspection: Inspection) {
        val currentList = _inspections.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == inspection.id }

        if (index >= 0) {
            val updatedInspection = inspection.copy(
                updatedAt = System.currentTimeMillis()
            )

            currentList[index] = updatedInspection

            _inspections.value = currentList
            saveInspections(currentList)
            syncInspectionAndPdf(updatedInspection)
        }
    }

  fun deleteInspectionById(id: Long) {
    val currentList = _inspections.value.toMutableList()

    val inspectionIndex = currentList.indexOfFirst { it.id == id }

    if (inspectionIndex >= 0) {

        val inspection = currentList[inspectionIndex]

        val updatedInspection = inspection.copy(
            updatedAt = System.currentTimeMillis(),
            deleted = true,
            deletedAt = System.currentTimeMillis()
        )

        currentList[inspectionIndex] = updatedInspection

        _inspections.value = currentList
        saveInspections(currentList)

        syncInspectionAndPdf(updatedInspection)
    }
}

    fun getInspectionCount(): Flow<Int> =
        inspections.map { it.size }

    fun getInspectionCountByStatus(status: InspectionStatus): Flow<Int> =
        inspections.map { list ->
            list.count { it.status == status }
        }

    companion object {
        @Volatile
        private var INSTANCE: InspectionRepository? = null

        fun getInstance(context: Context): InspectionRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: InspectionRepository(
                    context.applicationContext
                ).also { INSTANCE = it }
            }
        }
    }
}
