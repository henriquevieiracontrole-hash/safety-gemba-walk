package com.rork.safetygembawalk.data

import com.google.firebase.firestore.FirebaseFirestore


class FirebaseSyncService {

    private val db = FirebaseFirestore.getInstance()

    fun syncInspection(inspection: Inspection) {
        val data = hashMapOf(
            "id" to inspection.id,
            "title" to inspection.title,
            "location" to inspection.location,
            "inspector" to inspection.inspectorName,
            "status" to inspection.status.name,
            "createdAt" to inspection.createdAt,
            "updatedAt" to inspection.updatedAt,
            "actionsCount" to inspection.actions.size,
            "pendingActions" to inspection.actions.count { it.status == InspectionStatus.PENDING },
            "completedActions" to inspection.actions.count { it.status == InspectionStatus.COMPLETED },
            "actions" to inspection.actions.map { action ->
                hashMapOf(
                    "id" to action.id,
                    "unsafeCondition" to action.unsafeCondition,
                    "description" to action.description,
                    "immediateAction" to action.immediateAction,
                    "hasWorkOrder" to action.hasWorkOrder,
                    "workOrderNumber" to action.workOrderNumber,
                    "workOrderOpenDate" to action.workOrderOpenDate,
                    "category" to action.category,
                    "status" to action.status.name,
                    "beforePhotoPath" to action.beforePhotoPath,
                    "afterPhotoPath" to action.afterPhotoPath,
                    "createdAt" to action.createdAt,
                    "updatedAt" to action.updatedAt
                )
            }
        )

        db.collection("inspections")
    .document(inspection.id.toString())
    .set(data)
    }
}
