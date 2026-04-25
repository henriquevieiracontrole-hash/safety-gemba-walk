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
        inspections.map { list -> list.filter { it.status == status } }

    fun getInspectionById(id: Long): Inspection? {
        return _inspections.value.find { it.id == id }
    }

    fun insertInspection(inspection: Inspection): Long {
        val currentList = _inspections.value.toMutableList()
        val newId = if (inspection.id == 0L) System.currentTimeMillis() else inspection.id
        val newInspection = inspection.copy(id = newId, updatedAt = System.currentTimeMillis())

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

    fun updateInspection(inspection: Inspection) {
        val currentList = _inspections.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == inspection.id }
        if (index >= 0) {
            currentList[index] = inspection.copy(updatedAt = System.currentTimeMillis())
            _inspections.value = currentList
            saveInspections(currentList)
        }
    }

    fun deleteInspection(inspection: Inspection) {
        deleteInspectionById(inspection.id)
    }

    fun deleteInspectionById(id: Long) {
        val currentList = _inspections.value.toMutableList()
        currentList.removeAll { it.id == id }
        _inspections.value = currentList
        saveInspections(currentList)
    }

    fun getInspectionCount(): Flow<Int> = inspections.map { it.size }

    fun getInspectionCountByStatus(status: InspectionStatus): Flow<Int> =
        inspections.map { list -> list.count { it.status == status } }

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
