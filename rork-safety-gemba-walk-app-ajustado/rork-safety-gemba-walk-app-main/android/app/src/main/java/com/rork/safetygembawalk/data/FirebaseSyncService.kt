package com.rork.safetygembawalk.data

import android.net.Uri
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.File

class FirebaseSyncService {

    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    fun syncInspection(inspection: Inspection) {
        if (inspection.deleted) {
            saveInspectionToFirestore(inspection, null, null)
            return
        }

        val firstAction = inspection.actions.firstOrNull()

        uploadPhotoIfExists(
            inspectionId = inspection.id,
            path = firstAction?.beforePhotoPath,
            name = "before.jpg"
        ) { beforeUrl ->

            uploadPhotoIfExists(
                inspectionId = inspection.id,
                path = firstAction?.afterPhotoPath,
                name = "after.jpg"
            ) { afterUrl ->

                saveInspectionToFirestore(
                    inspection = inspection,
                    beforePhotoUrl = beforeUrl,
                    afterPhotoUrl = afterUrl
                )
            }
        }
    }

    private fun uploadPhotoIfExists(
        inspectionId: Long,
        path: String?,
        name: String,
        onComplete: (String?) -> Unit
    ) {
        if (path.isNullOrBlank()) {
            onComplete(null)
            return
        }

        val file = File(path)

        if (!file.exists()) {
            onComplete(null)
            return
        }

        val ref = storage.reference
            .child("inspection_photos")
            .child(inspectionId.toString())
            .child(name)

        ref.putFile(Uri.fromFile(file))
            .addOnSuccessListener {
                ref.downloadUrl
                    .addOnSuccessListener { uri ->
                        onComplete(uri.toString())
                    }
                    .addOnFailureListener {
                        it.printStackTrace()
                        onComplete(null)
                    }
            }
            .addOnFailureListener {
                it.printStackTrace()
                onComplete(null)
            }
    }

    private fun saveInspectionToFirestore(
        inspection: Inspection,
        beforePhotoUrl: String?,
        afterPhotoUrl: String?
    ) {
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

            "actions" to inspection.actions.mapIndexed { index, action ->
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
                    "beforePhotoUrl" to if (index == 0) beforePhotoUrl else null,
                    "afterPhotoUrl" to if (index == 0) afterPhotoUrl else null,
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
        if (!pdfFile.exists() || inspection.deleted) return

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
            }
    }
}
