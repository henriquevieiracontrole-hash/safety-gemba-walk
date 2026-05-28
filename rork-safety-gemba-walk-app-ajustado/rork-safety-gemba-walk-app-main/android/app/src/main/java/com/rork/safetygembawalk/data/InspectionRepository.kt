package com.rork.safetygembawalk.data

import android.content.Context
import android.content.SharedPreferences
import com.rork.safetygembawalk.viewmodels.PdfReportGenerator
import java.io.File
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class InspectionRepository(context: Context) {

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
            json.decodeFromString<List<Inspection>>(data)
                .filter { !it.deleted && it.deletedAt == null }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveInspections(list: List<Inspection>) {
        prefs.edit()
            .putString(inspectionsKey, json.encodeToString(list))
            .apply()
    }

    private fun syncToFirebase(inspection: Inspection) {
        try {
            firebaseSyncService.syncInspection(inspection)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun syncPdfToFirebase(inspection: Inspection) {
        if (inspection.actions.isEmpty() || inspection.deleted) return

        Thread {
            try {
                Thread.sleep(1500)

                val latestInspection = getInspectionById(inspection.id) ?: inspection

                if (latestInspection.actions.isEmpty() || latestInspection.deleted) {
                    return@Thread
                }

                val pdfGenerator = PdfReportGenerator(appContext)
                val filePath = pdfGenerator.generateReport(listOf(latestInspection))
                val file = File(filePath)

                if (file.exists() && file.length() > 10_000) {
                    firebaseSyncService.uploadInspectionPdf(
                        inspection = latestInspection,
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
                !inspection.deleted &&
                    inspection.deletedAt == null &&
                    inspection.status == status
            }
        }

    fun getInspectionById(id: Long): Inspection? {
        return _inspections.value.find {
            it.id == id && !it.deleted && it.deletedAt == null
        }
    }

    fun insertInspection(inspection: Inspection): Long {
        val savedListData = prefs.getString(inspectionsKey, "[]") ?: "[]"

        val fullList = try {
            json.decodeFromString<List<Inspection>>(savedListData).toMutableList()
        } catch (e: Exception) {
            _inspections.value.toMutableList()
        }

        val newId = if (inspection.id == 0L) System.currentTimeMillis() else inspection.id
        val now = System.currentTimeMillis()

        val newInspection = inspection.copy(
            id = newId,
            deleted = false,
            deletedAt = null,
            updatedAt = now,
            lastUpdatedAt = now
        )

        val existingIndex = fullList.indexOfFirst { it.id == newId }

        if (existingIndex >= 0) {
            fullList[existingIndex] = newInspection
        } else {
            fullList.add(0, newInspection)
        }

        saveInspections(fullList)

        _inspections.value = fullList.filter { !it.deleted && it.deletedAt == null }

        syncInspectionAndPdf(newInspection)

        return newId
    }

    fun addAction(inspectionId: Long, action: InspectionActionItem) {
        val currentList = _inspections.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == inspectionId }

        if (index >= 0) {
            val inspection = currentList[index]
            val now = System.currentTimeMillis()

            val newAction = action.copy(
                id = if (action.id == 0L) now else action.id,
                updatedAt = now,
                lastUpdatedAt = now
            )

            val updatedInspection = inspection.copy(
                actions = inspection.actions + newAction,
                updatedAt = now,
                lastUpdatedAt = now
            )

            currentList[index] = updatedInspection
            _inspections.value = currentList.filter { !it.deleted && it.deletedAt == null }
            saveInspections(currentList)
            syncInspectionAndPdf(updatedInspection)
        }
    }

    fun updateAction(inspectionId: Long, action: InspectionActionItem) {
        val currentList = _inspections.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == inspectionId }

        if (index >= 0) {
            val inspection = currentList[index]
            val now = System.currentTimeMillis()

            val updatedActions = inspection.actions.map {
                if (it.id == action.id) {
                    action.copy(updatedAt = now, lastUpdatedAt = now)
                } else {
                    it
                }
            }

            val updatedInspection = inspection.copy(
                actions = updatedActions,
                updatedAt = now,
                lastUpdatedAt = now
            )

            currentList[index] = updatedInspection
            _inspections.value = currentList.filter { !it.deleted && it.deletedAt == null }
            saveInspections(currentList)
            syncInspectionAndPdf(updatedInspection)
        }
    }

    fun deleteAction(inspectionId: Long, actionId: Long) {
        val currentList = _inspections.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == inspectionId }

        if (index >= 0) {
            val inspection = currentList[index]
            val now = System.currentTimeMillis()

            val updatedInspection = inspection.copy(
                actions = inspection.actions.filterNot { it.id == actionId },
                updatedAt = now,
                lastUpdatedAt = now
            )

            currentList[index] = updatedInspection
            _inspections.value = currentList.filter { !it.deleted && it.deletedAt == null }
            saveInspections(currentList)
            syncInspectionAndPdf(updatedInspection)
        }
    }

    fun updateInspection(inspection: Inspection) {
        val currentList = _inspections.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == inspection.id }

        if (index >= 0) {
            val now = System.currentTimeMillis()

            val updatedInspection = inspection.copy(updatedAt = now, lastUpdatedAt = now)

            currentList[index] = updatedInspection
            _inspections.value = currentList.filter { !it.deleted && it.deletedAt == null }
            saveInspections(currentList)
            syncInspectionAndPdf(updatedInspection)
        }
    }

    fun deleteInspection(id: Long) {
        deleteInspectionById(id)
    }

    fun deleteInspection(inspection: Inspection) {
        deleteInspectionById(inspection.id)
    }

    fun deleteInspectionById(id: Long) {
        val savedListData = prefs.getString(inspectionsKey, "[]") ?: "[]"

        val fullList = try {
            json.decodeFromString<List<Inspection>>(savedListData).toMutableList()
        } catch (e: Exception) {
            _inspections.value.toMutableList()
        }

        val inspectionIndex = fullList.indexOfFirst { it.id == id }

        if (inspectionIndex >= 0) {
            val now = System.currentTimeMillis()

            val updatedInspection = fullList[inspectionIndex].copy(
                deleted = true,
                deletedAt = now,
                updatedAt = now,
                lastUpdatedAt = now
            )

            fullList[inspectionIndex] = updatedInspection
            saveInspections(fullList)
            _inspections.value = fullList.filter { !it.deleted && it.deletedAt == null }
            syncToFirebase(updatedInspection)
        }
    }

    fun getInspectionCount(): Flow<Int> =
        inspections.map { list ->
            list.count { !it.deleted && it.deletedAt == null }
        }

    fun getInspectionCountByStatus(status: InspectionStatus): Flow<Int> =
        inspections.map { list ->
            list.count {
                !it.deleted &&
                    it.deletedAt == null &&
                    it.status == status
            }
        }

    companion object {
        @Volatile
        private var INSTANCE: InspectionRepository? = null

        fun getInstance(context: Context): InspectionRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: InspectionRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
