package com.rork.safetygembawalk.data

import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Serializable
data class Inspection(
    val id: Long = System.currentTimeMillis(),

    val unsafeCondition: String,
    val description: String,
    val immediateAction: String = "",

    val hasWorkOrder: Boolean = false,
    val workOrderNumber: String? = null,
    val workOrderOpenDate: Long? = null,

    val category: String = "Segurança",

    val isImmediateAction: Boolean = false,
    val location: String = "",
    val inspectorName: String = "",

    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),

    val beforePhotoPath: String? = null,
    val afterPhotoPath: String? = null,

    val status: InspectionStatus = InspectionStatus.PENDING
)

@Serializable
enum class InspectionStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED
}

fun Inspection.formattedDate(): String {
    val instant = Instant.ofEpochMilli(createdAt)
    val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
        .withZone(ZoneId.systemDefault())
    return formatter.format(instant)
}

fun Inspection.formattedWorkOrderOpenDate(): String {
    if (workOrderOpenDate == null) return "-"
    val instant = Instant.ofEpochMilli(workOrderOpenDate)
    val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        .withZone(ZoneId.systemDefault())
    return formatter.format(instant)
}

fun Inspection.shortDescription(): String {
    return if (description.length > 100) {
        description.take(100) + "..."
    } else {
        description
    }
}
