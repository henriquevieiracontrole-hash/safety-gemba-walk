package com.rork.safetygembawalk.data

import android.net.Uri
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.File

class FirebaseSyncService {

    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    fun syncInspection(inspection: Inspection) {
 val data = hashMapOf(
    "id" to inspection.id,
    "title" to inspection.title,
    "location" to inspection.location,
    "inspector" to inspection.inspectorName,
    "status" to inspection.status.name,
    "createdAt" to inspection.createdAt,
    "updatedAt" to inspection.updatedAt,

    "deleted" to inspection.deleted,
    "deletedAt" to inspection.deletedAt,

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

    fun uploadInspectionPdf(
        inspection: Inspection,
        pdfFile: File
    ) {
        if (!pdfFile.exists()) return

        val pdfRef = storage.reference
            .child("inspection_pdfs")
            .child(inspection.id.toString())
            .child("relatorio.pdf")

        pdfRef.putFile(Uri.fromFile(pdfFile))
            .addOnSuccessListener {
                pdfRef.downloadUrl
                    .addOnSuccessListener { downloadUri ->
                        db.collection("inspections")
                            .document(inspection.id.toString())
                            .update(
                                mapOf(
                                    "pdfUrl" to downloadUri.toString(),
                                    "pdfFileName" to pdfFile.name,
                                    "pdfUpdatedAt" to System.currentTimeMillis()
                                )
                            )
                    }
                    .addOnFailureListener { error ->
                        error.printStackTrace()
                    }
            }
            .addOnFailureListener { error ->
                error.printStackTrace()
            }
    }
}
