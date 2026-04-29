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

class InspectionRepository private constructor(context: Context) {
    private val json = Json { ignoreUnknownKeys = true }
    private val prefs: SharedPreferences =
        context.getSharedPreferences("inspections", Context.MODE_PRIVATE)

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

            currentList[index] = inspection.copy(
                actions = inspection.actions + newAction,
                updatedAt = System.currentTimeMillis()
            )

            _inspections.value = currentList
            saveInspections(currentList)
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

            currentList[index] = inspection.copy(
                actions = updatedActions,
                updatedAt = System.currentTimeMillis()
            )

            _inspections.value = currentList
            saveInspections(currentList)
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

            currentList[index] = inspection.copy(
                actions = inspection.actions.filterNot { it.id == actionId },
                updatedAt = System.currentTimeMillis()
            )

            _inspections.value = currentList
            saveInspections(currentList)
        }
    }

    fun updateInspection(inspection: Inspection) {
        val currentList = _inspections.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == inspection.id }

        if (index >= 0) {
            currentList[index] = inspection.copy(
                updatedAt = System.currentTimeMillis()
            )

            _inspections.value = currentList
            saveInspections(currentList)
        }
    }

    fun deleteInspectionById(id: Long) {
        val currentList = _inspections.value.toMutableList()
        currentList.removeAll { it.id == id }

        _inspections.value = currentList
        saveInspections(currentList)
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
