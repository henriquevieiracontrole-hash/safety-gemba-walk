package com.rork.safetygembawalk.viewmodels

import android.content.Context
import com.rork.safetygembawalk.data.Inspection
import com.rork.safetygembawalk.data.InspectionStatus
import com.rork.safetygembawalk.data.formattedDate
import com.rork.safetygembawalk.data.formattedWorkOrderOpenDate
import org.apache.poi.sl.usermodel.PictureData
import org.apache.poi.xslf.usermodel.XMLSlideShow
import org.apache.poi.xslf.usermodel.XSLFSlide
import java.awt.Color
import java.awt.Rectangle
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PptReportGenerator(private val context: Context) {

    private val navy = Color(9, 31, 65)
    private val purple = Color(107, 35, 120)
    private val orange = Color(255, 95, 35)
    private val red = Color(220, 38, 38)
    private val green = Color(34, 140, 80)
    private val dark = Color(30, 41, 59)
    private val lightBorder = Color(210, 210, 210)

    fun generateReport(inspections: List<Inspection>): String {
        val fileName = "Safety_Gemba_Walk_${SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())}.pptx"
        val file = File(context.getExternalFilesDir(null), fileName)

        XMLSlideShow().use { ppt ->
            ppt.pageSize = java.awt.Dimension(1280, 720)

            inspections.forEachIndexed { index, inspection ->
                createInspectionSlide(ppt, inspection, index + 1)
            }

            FileOutputStream(file).use { out ->
                ppt.write(out)
            }
        }

        return file.absolutePath
    }

    private fun createInspectionSlide(ppt: XMLSlideShow, inspection: Inspection, number: Int) {
        val slide = ppt.createSlide()

        addBackground(ppt, slide)

        addText(slide, "SAFETY GEMBA WALK", 56, 56, 520, 42, 30.0, navy, true)
        addText(slide, "Relatório de inspeção #$number", 58, 96, 420, 30, 17.0, purple, false)
        addText(
            slide,
            "Data: ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())}",
            1050,
            96,
            180,
            24,
            11.0,
            dark,
            false
        )

        addLine(slide, 58, 130, 1160, 3, purple)

        addInfoPanel(slide, inspection)
        addRiskPanel(slide, inspection)
        addActionPanel(slide, inspection)
        addPhotoPanel(ppt, slide, inspection)
    }

    private fun addBackground(ppt: XMLSlideShow, slide: XSLFSlide) {
        try {
            val resId = context.resources.getIdentifier(
                "report_ppt_background",
                "drawable",
                context.packageName
            )

            if (resId == 0) return

            val bytes = context.resources.openRawResource(resId).use { it.readBytes() }
            val pic = ppt.addPicture(bytes, PictureData.PictureType.PNG)
            val shape = slide.createPicture(pic)
            shape.anchor = Rectangle(0, 0, 1280, 720)
        } catch (_: Exception) {
        }
    }

    private fun addInfoPanel(slide: XSLFSlide, inspection: Inspection) {
        addText(slide, "INSPEÇÃO #", 70, 158, 165, 28, 12.0, Color.WHITE, true, purple)

        val x = 70
        var y = 205

        addInfo(slide, x, y, "Data da inspeção", inspection.formattedDate())
        y += 58

        addInfo(slide, x, y, "Categoria", inspection.category.ifBlank { "Segurança" })
        y += 58

        addInfo(slide, x, y, "Local", inspection.location.ifBlank { "-" })
        y += 58

        addInfo(slide, x, y, "Inspetor", inspection.inspectorName.ifBlank { "-" })
        y += 68

        val status = when (inspection.status) {
            InspectionStatus.COMPLETED -> "CONCLUÍDO"
            InspectionStatus.IN_PROGRESS -> "EM ANDAMENTO"
            InspectionStatus.PENDING -> "PENDENTE"
            InspectionStatus.CANCELLED -> "CANCELADO"
        }

        val statusColor = when (inspection.status) {
            InspectionStatus.COMPLETED -> green
            InspectionStatus.IN_PROGRESS -> orange
            InspectionStatus.PENDING -> red
            InspectionStatus.CANCELLED -> dark
        }

        addBox(slide, x, y, 180, 34, Color.WHITE, statusColor)
        addText(slide, status, x + 8, y + 7, 164, 20, 12.0, statusColor, true)
        y += 54

        if (inspection.hasWorkOrder) {
            addInfo(slide, x, y, "Ordem de Serviço", inspection.workOrderNumber ?: "N/A")
            y += 58
            addInfo(slide, x, y, "Data abertura O.S.", inspection.formattedWorkOrderOpenDate())
        }
    }

    private fun addRiskPanel(slide: XSLFSlide, inspection: Inspection) {
        addText(slide, "RISCO IDENTIFICADO", 315, 188, 230, 26, 14.0, red, true)

        val riskText = "${inspection.unsafeCondition}\n\n${inspection.description}".trim()
        addText(slide, riskText, 315, 225, 300, 150, 12.0, dark, false)

        addLine(slide, 292, 180, 2, 395, lightBorder)
    }

    private fun addActionPanel(slide: XSLFSlide, inspection: Inspection) {
        addText(slide, "AÇÃO IMEDIATA", 315, 405, 230, 26, 14.0, orange, true)

        addText(
            slide,
            inspection.immediateAction.ifBlank { "-" },
            315,
            442,
            300,
            120,
            12.0,
            dark,
            false
        )
    }

    private fun addPhotoPanel(ppt: XMLSlideShow, slide: XSLFSlide, inspection: Inspection) {
        addBox(slide, 650, 175, 555, 405, Color(255, 255, 255), lightBorder)

        addText(slide, "FOTO ANTES", 720, 195, 170, 24, 13.0, purple, true)
        addText(slide, "FOTO DEPOIS", 995, 195, 170, 24, 13.0, purple, true)

        addPictureIfExists(ppt, slide, inspection.beforePhotoPath, 700, 230, 210, 310)
        addPictureIfExists(ppt, slide, inspection.afterPhotoPath, 975, 230, 210, 310)
    }

    private fun addPictureIfExists(
        ppt: XMLSlideShow,
        slide: XSLFSlide,
        path: String?,
        x: Int,
        y: Int,
        w: Int,
        h: Int
    ) {
        try {
            if (path.isNullOrBlank()) return
            val file = File(path)
            if (!file.exists()) return

            val bytes = file.readBytes()
            val pic = ppt.addPicture(bytes, PictureData.PictureType.JPEG)
            val shape = slide.createPicture(pic)
            shape.anchor = Rectangle(x, y, w, h)
        } catch (_: Exception) {
        }
    }

    private fun addInfo(slide: XSLFSlide, x: Int, y: Int, label: String, value: String) {
        addText(slide, label, x, y, 190, 18, 10.0, purple, true)
        addText(slide, value, x, y + 18, 190, 26, 10.0, dark, false)
    }

    private fun addText(
        slide: XSLFSlide,
        text: String,
        x: Int,
        y: Int,
        w: Int,
        h: Int,
        size: Double,
        color: Color,
        bold: Boolean,
        fill: Color? = null
    ) {
        val shape = slide.createTextBox()
        shape.anchor = Rectangle(x, y, w, h)
        fill?.let {
            shape.fillColor = it
        }

        val p = shape.addNewTextParagraph()
        val r = p.addNewTextRun()
        r.setText(text)
        r.setFontSize(size)
        r.setFontColor(color)
        r.isBold = bold
        r.setFontFamily("Arial")
    }

    private fun addBox(slide: XSLFSlide, x: Int, y: Int, w: Int, h: Int, fill: Color, border: Color) {
        val box = slide.createAutoShape()
        box.anchor = Rectangle(x, y, w, h)
        box.fillColor = fill
        box.lineColor = border
    }

    private fun addLine(slide: XSLFSlide, x: Int, y: Int, w: Int, h: Int, color: Color) {
        val line = slide.createAutoShape()
        line.anchor = Rectangle(x, y, w, h)
        line.fillColor = color
        line.lineColor = color
    }
}
