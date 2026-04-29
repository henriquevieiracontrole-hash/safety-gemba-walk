package com.rork.safetygembawalk.viewmodels

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import com.rork.safetygembawalk.data.Inspection
import com.rork.safetygembawalk.data.InspectionStatus
import com.rork.safetygembawalk.data.formattedDate
import com.rork.safetygembawalk.data.formattedWorkOrderOpenDate
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class PptReportGenerator(private val context: Context) {

    private val slideW = 1280
    private val slideH = 720

    fun generateReport(inspections: List<Inspection>): String {
        val fileName = "Safety_Gemba_Walk_${SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())}.pptx"
        val file = File(context.getExternalFilesDir(null), fileName)

        val slides = inspections.mapIndexed { index, inspection ->
            createSlideImage(inspection, index + 1)
        }

        createPptx(file, slides)

        return file.absolutePath
    }

    private fun createSlideImage(inspection: Inspection, number: Int): ByteArray {
        val bitmap = Bitmap.createBitmap(slideW, slideH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        drawBackground(canvas)

        val navy = Color.rgb(9, 31, 65)
        val purple = Color.rgb(107, 35, 120)
        val orange = Color.rgb(255, 95, 35)
        val red = Color.rgb(220, 38, 38)
        val dark = Color.rgb(30, 41, 59)
        val gray = Color.rgb(230, 230, 230)

        drawText(canvas, "SAFETY GEMBA WALK", 58f, 62f, 32f, navy, true)
        drawText(canvas, "Relatório de inspeção #$number", 60f, 98f, 18f, purple, false)
        drawText(
            canvas,
            "Data: ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())}",
            1030f,
            98f,
            13f,
            dark,
            false
        )

        drawLine(canvas, 58f, 130f, 1180f, 130f, purple, 3f)

        drawBox(canvas, 58f, 158f, 220f, 410f, Color.TRANSPARENT, gray)
        drawText(canvas, "INSPEÇÃO #$number", 78f, 188f, 15f, Color.WHITE, true, purple)

        var y = 235f
        drawInfo(canvas, "Data da inspeção", inspection.formattedDate(), 78f, y); y += 58f
        drawInfo(canvas, "Categoria", inspection.category.ifBlank { "Segurança" }, 78f, y); y += 58f
        drawInfo(canvas, "Local", inspection.location.ifBlank { "-" }, 78f, y); y += 58f
        drawInfo(canvas, "Inspetor", inspection.inspectorName.ifBlank { "-" }, 78f, y); y += 62f

        val statusText = when (inspection.status) {
            InspectionStatus.COMPLETED -> "CONCLUÍDO"
            InspectionStatus.IN_PROGRESS -> "EM ANDAMENTO"
            InspectionStatus.PENDING -> "PENDENTE"
            InspectionStatus.CANCELLED -> "CANCELADO"
        }

        val statusColor = when (inspection.status) {
            InspectionStatus.COMPLETED -> Color.rgb(34, 140, 80)
            InspectionStatus.IN_PROGRESS -> orange
            InspectionStatus.PENDING -> red
            InspectionStatus.CANCELLED -> dark
        }

        drawBox(canvas, 78f, y, 170f, 38f, Color.WHITE, statusColor)
        drawText(canvas, statusText, 96f, y + 25f, 14f, statusColor, true)
        y += 62f

        if (inspection.hasWorkOrder) {
            drawInfo(canvas, "Ordem de Serviço", inspection.workOrderNumber ?: "N/A", 78f, y); y += 58f
            drawInfo(canvas, "Data abertura O.S.", inspection.formattedWorkOrderOpenDate(), 78f, y)
        }

        drawLine(canvas, 300f, 180f, 300f, 575f, gray, 2f)

        drawText(canvas, "RISCO IDENTIFICADO", 330f, 205f, 16f, red, true)
        drawWrappedText(
            canvas,
            "${inspection.unsafeCondition}\n\n${inspection.description}".trim(),
            330f,
            235f,
            290f,
            15f,
            dark
        )

        drawText(canvas, "AÇÃO IMEDIATA", 330f, 430f, 16f, orange, true)
        drawWrappedText(
            canvas,
            inspection.immediateAction.ifBlank { "-" },
            330f,
            460f,
            290f,
            15f,
            dark
        )

        drawBox(canvas, 660f, 175f, 540f, 405f, Color.TRANSPARENT, gray)
        drawText(canvas, "FOTO ANTES", 735f, 205f, 15f, purple, true)
        drawText(canvas, "FOTO DEPOIS", 990f, 205f, 15f, purple, true)

        drawPhoto(canvas, inspection.beforePhotoPath, 705f, 230f, 210f, 310f)
        drawPhoto(canvas, inspection.afterPhotoPath, 970f, 230f, 210f, 310f)

        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        bitmap.recycle()
        return out.toByteArray()
    }

    private fun drawBackground(canvas: Canvas) {
        canvas.drawColor(Color.WHITE)

        try {
            val resId = context.resources.getIdentifier(
                "report_ppt_background",
                "drawable",
                context.packageName
            )

            if (resId != 0) {
                val bg = BitmapFactory.decodeStream(context.resources.openRawResource(resId))
                canvas.drawBitmap(bg, null, Rect(0, 0, slideW, slideH), null)
                bg.recycle()
            }
        } catch (_: Exception) {
        }
    }

    private fun drawPhoto(canvas: Canvas, path: String?, x: Float, y: Float, w: Float, h: Float) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = Color.rgb(245, 245, 245)
        canvas.drawRect(RectF(x, y, x + w, y + h), paint)

        if (path.isNullOrBlank()) {
            drawText(canvas, "Sem foto", x + 60f, y + 160f, 14f, Color.GRAY, false)
            return
        }

        try {
            val file = File(path)
            if (!file.exists()) {
                drawText(canvas, "Sem foto", x + 60f, y + 160f, 14f, Color.GRAY, false)
                return
            }

            val bmp = BitmapFactory.decodeFile(file.absolutePath) ?: return
            canvas.drawBitmap(bmp, null, RectF(x, y, x + w, y + h), null)
            bmp.recycle()
        } catch (_: Exception) {
            drawText(canvas, "Sem foto", x + 60f, y + 160f, 14f, Color.GRAY, false)
        }
    }

    private fun drawInfo(canvas: Canvas, label: String, value: String, x: Float, y: Float) {
        drawText(canvas, label, x, y, 12f, Color.rgb(107, 35, 120), true)
        drawText(canvas, value, x, y + 22f, 12f, Color.rgb(30, 41, 59), false)
    }

    private fun drawText(
        canvas: Canvas,
        text: String,
        x: Float,
        y: Float,
        size: Float,
        color: Int,
        bold: Boolean,
        background: Int? = null
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.textSize = size
        paint.color = color
        paint.typeface = if (bold) android.graphics.Typeface.DEFAULT_BOLD else android.graphics.Typeface.DEFAULT

        background?.let {
            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
            bgPaint.color = it
            canvas.drawRoundRect(RectF(x - 10f, y - 24f, x + 165f, y + 8f), 4f, 4f, bgPaint)
        }

        canvas.drawText(text, x, y, paint)
    }

    private fun drawWrappedText(
        canvas: Canvas,
        text: String,
        x: Float,
        y: Float,
        maxWidth: Float,
        size: Float,
        color: Int
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.textSize = size
        paint.color = color
        paint.typeface = android.graphics.Typeface.DEFAULT

        var currentY = y
        text.split("\n").forEach { paragraph ->
            val words = paragraph.split(" ")
            var line = ""

            words.forEach { word ->
                val testLine = if (line.isEmpty()) word else "$line $word"
                if (paint.measureText(testLine) > maxWidth) {
                    canvas.drawText(line, x, currentY, paint)
                    currentY += size + 7f
                    line = word
                } else {
                    line = testLine
                }
            }

            if (line.isNotBlank()) {
                canvas.drawText(line, x, currentY, paint)
                currentY += size + 10f
            }

            currentY += 6f
        }
    }

    private fun drawBox(canvas: Canvas, x: Float, y: Float, w: Float, h: Float, fill: Int, stroke: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.style = Paint.Style.FILL
        paint.color = fill
        canvas.drawRect(RectF(x, y, x + w, y + h), paint)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        paint.color = stroke
        canvas.drawRect(RectF(x, y, x + w, y + h), paint)
    }

    private fun drawLine(canvas: Canvas, x1: Float, y1: Float, x2: Float, y2: Float, color: Int, width: Float) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = color
        paint.strokeWidth = width
        canvas.drawLine(x1, y1, x2, y2, paint)
    }

    private fun createPptx(file: File, slideImages: List<ByteArray>) {
        ZipOutputStream(FileOutputStream(file)).use { zip ->
            addEntry(zip, "[Content_Types].xml", contentTypes(slideImages.size))
            addEntry(zip, "_rels/.rels", rootRels())
            addEntry(zip, "ppt/presentation.xml", presentationXml(slideImages.size))
            addEntry(zip, "ppt/_rels/presentation.xml.rels", presentationRels(slideImages.size))

            slideImages.forEachIndexed { index, bytes ->
                val n = index + 1
                addEntry(zip, "ppt/slides/slide$n.xml", slideXml(n))
                addEntry(zip, "ppt/slides/_rels/slide$n.xml.rels", slideRel(n))
                addBinaryEntry(zip, "ppt/media/image$n.png", bytes)
            }
        }
    }

    private fun addEntry(zip: ZipOutputStream, name: String, content: String) {
        zip.putNextEntry(ZipEntry(name))
        zip.write(content.toByteArray(Charsets.UTF_8))
        zip.closeEntry()
    }

    private fun addBinaryEntry(zip: ZipOutputStream, name: String, bytes: ByteArray) {
        zip.putNextEntry(ZipEntry(name))
        zip.write(bytes)
        zip.closeEntry()
    }

    private fun contentTypes(count: Int): String {
        val slides = (1..count).joinToString("") {
            """<Override PartName="/ppt/slides/slide$it.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.slide+xml"/>"""
        }

        return """<?xml version="1.0" encoding="UTF-8"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
<Default Extension="xml" ContentType="application/xml"/>
<Default Extension="png" ContentType="image/png"/>
<Override PartName="/ppt/presentation.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.presentation.main+xml"/>
$slides
</Types>"""
    }

    private fun rootRels(): String {
        return """<?xml version="1.0" encoding="UTF-8"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="ppt/presentation.xml"/>
</Relationships>"""
    }

    private fun presentationXml(count: Int): String {
        val ids = (1..count).joinToString("") {
            """<p:sldId id="${256 + it}" r:id="rId$it"/>"""
        }

        return """<?xml version="1.0" encoding="UTF-8"?>
<p:presentation xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
<p:sldMasterIdLst/>
<p:sldIdLst>$ids</p:sldIdLst>
<p:sldSz cx="12192000" cy="6858000" type="screen16x9"/>
<p:notesSz cx="6858000" cy="9144000"/>
</p:presentation>"""
    }

    private fun presentationRels(count: Int): String {
        val rels = (1..count).joinToString("") {
            """<Relationship Id="rId$it" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/slide" Target="slides/slide$it.xml"/>"""
        }

        return """<?xml version="1.0" encoding="UTF-8"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
$rels
</Relationships>"""
    }

    private fun slideRel(n: Int): String {
        return """<?xml version="1.0" encoding="UTF-8"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/image" Target="../media/image$n.png"/>
</Relationships>"""
    }

    private fun slideXml(n: Int): String {
        return """<?xml version="1.0" encoding="UTF-8"?>
<p:sld xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships" xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main">
<p:cSld>
<p:spTree>
<p:nvGrpSpPr><p:cNvPr id="1" name=""/><p:cNvGrpSpPr/><p:nvPr/></p:nvGrpSpPr>
<p:grpSpPr><a:xfrm><a:off x="0" y="0"/><a:ext cx="0" cy="0"/><a:chOff x="0" y="0"/><a:chExt cx="0" cy="0"/></a:xfrm></p:grpSpPr>
<p:pic>
<p:nvPicPr><p:cNvPr id="2" name="Slide Image $n"/><p:cNvPicPr/><p:nvPr/></p:nvPicPr>
<p:blipFill><a:blip r:embed="rId1"/><a:stretch><a:fillRect/></a:stretch></p:blipFill>
<p:spPr><a:xfrm><a:off x="0" y="0"/><a:ext cx="12192000" cy="6858000"/></a:xfrm><a:prstGeom prst="rect"><a:avLst/></a:prstGeom></p:spPr>
</p:pic>
</p:spTree>
</p:cSld>
<p:clrMapOvr><a:masterClrMapping/></p:clrMapOvr>
</p:sld>"""
    }
}
