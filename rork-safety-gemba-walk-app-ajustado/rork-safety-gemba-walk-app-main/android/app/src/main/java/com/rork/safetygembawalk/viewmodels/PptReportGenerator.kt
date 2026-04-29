package com.rork.safetygembawalk.viewmodels

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
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

    private val navy = Color.rgb(9, 31, 65)
    private val purple = Color.rgb(107, 35, 120)
    private val orange = Color.rgb(255, 95, 35)
    private val red = Color.rgb(220, 38, 38)
    private val green = Color.rgb(34, 140, 80)
    private val yellow = Color.rgb(234, 179, 8)
    private val dark = Color.rgb(30, 41, 59)
    private val gray = Color.rgb(248, 248, 248)
    private val borderGray = Color.rgb(205, 205, 205)

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

        drawText(canvas, "SAFETY GEMBA WALK", 58f, 58f, 32f, navy, true)
        drawText(canvas, "Safety is my first job!!", 60f, 88f, 17f, red, true)
        drawText(canvas, "Relatório de inspeção #$number", 60f, 118f, 16f, purple, true)
        drawText(
            canvas,
            "Data: ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())}",
            1010f,
            118f,
            13f,
            dark,
            false
        )

        drawLine(canvas, 58f, 140f, 1185f, 140f, purple, 3f)

        drawInfoBlock(canvas, inspection)
        drawMainCards(canvas, inspection)
        drawPhotos(canvas, inspection)

        drawText(
            canvas,
            "Ahlstrom • Safety Gemba Walk Report",
            520f,
            690f,
            12f,
            Color.rgb(140, 140, 140),
            false
        )

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

    private fun drawInfoBlock(canvas: Canvas, inspection: Inspection) {
        val startX = 58f
        val startY = 158f
        val cardW = 184f
        val cardH = 54f
        val gap = 10f

        drawInfoCard(canvas, "Data da inspeção", inspection.formattedDate(), startX, startY, cardW, cardH)
        drawInfoCard(canvas, "Categoria", inspection.category.ifBlank { "Segurança" }, startX + cardW + gap, startY, cardW, cardH)
        drawInfoCard(canvas, "Local", inspection.location.ifBlank { "-" }, startX + (cardW + gap) * 2, startY, cardW, cardH)

        drawInfoCard(canvas, "Inspetor", inspection.inspectorName.ifBlank { "-" }, startX, startY + cardH + gap, cardW, cardH)

        if (inspection.hasWorkOrder) {
            drawInfoCard(canvas, "Ordem de Serviço", inspection.workOrderNumber ?: "N/A", startX + cardW + gap, startY + cardH + gap, cardW, cardH)
            drawInfoCard(canvas, "Abertura O.S.", inspection.formattedWorkOrderOpenDate(), startX + (cardW + gap) * 2, startY + cardH + gap, cardW, cardH)
        } else {
            drawInfoCard(canvas, "Ordem de Serviço", "Não aplicável", startX + cardW + gap, startY + cardH + gap, cardW, cardH)
            drawInfoCard(canvas, "Abertura O.S.", "Não aplicável", startX + (cardW + gap) * 2, startY + cardH + gap, cardW, cardH)
        }

        val statusText = when (inspection.status) {
            InspectionStatus.COMPLETED -> "CONCLUÍDO"
            InspectionStatus.IN_PROGRESS -> "EM ANDAMENTO"
            InspectionStatus.PENDING -> "PENDENTE"
            InspectionStatus.CANCELLED -> "CANCELADO"
        }

        val statusColor = when (inspection.status) {
            InspectionStatus.COMPLETED -> green
            InspectionStatus.IN_PROGRESS -> yellow
            InspectionStatus.PENDING -> red
            InspectionStatus.CANCELLED -> dark
        }

        drawRoundBox(canvas, 650f, 158f, 190f, 54f, Color.WHITE, statusColor, 10f, 3f)
        drawText(canvas, statusText, 682f, 192f, 17f, statusColor, true)
    }

    private fun drawInfoCard(
        canvas: Canvas,
        label: String,
        value: String,
        x: Float,
        y: Float,
        w: Float,
        h: Float
    ) {
        drawRoundBox(canvas, x, y, w, h, gray, borderGray, 8f, 1.5f)
        drawText(canvas, label, x + 10f, y + 20f, 11f, purple, true)
        drawText(canvas, value, x + 10f, y + 40f, 12f, dark, false)
    }

    private fun drawMainCards(canvas: Canvas, inspection: Inspection) {
        val x1 = 58f
        val y1 = 300f
        val cardW = 360f
        val cardH = 112f

        drawExecutiveCard(
            canvas = canvas,
            title = "RISCO IDENTIFICADO",
            text = inspection.unsafeCondition.ifBlank { "-" },
            x = x1,
            y = y1,
            w = cardW,
            h = cardH,
            titleColor = red
        )

        drawExecutiveCard(
            canvas = canvas,
            title = "AÇÃO IMEDIATA",
            text = inspection.immediateAction.ifBlank { "-" },
            x = x1 + cardW + 18f,
            y = y1,
            w = cardW,
            h = cardH,
            titleColor = orange
        )

        drawRoundBox(canvas, 58f, 430f, 738f, 142f, Color.WHITE, borderGray, 10f, 1.5f)

        drawText(
            canvas,
            "DESCRIÇÃO DETALHADA",
            322f,
            460f,
            17f,
            dark,
            true
        )

        drawWrappedText(
            canvas,
            inspection.description.ifBlank { "-" },
            82f,
            492f,
            690f,
            15f,
            dark,
            4
        )
    }

    private fun drawExecutiveCard(
        canvas: Canvas,
        title: String,
        text: String,
        x: Float,
        y: Float,
        w: Float,
        h: Float,
        titleColor: Int
    ) {
        drawRoundBox(canvas, x, y, w, h, Color.WHITE, borderGray, 10f, 1.5f)
        drawText(canvas, title, x + 18f, y + 30f, 16f, titleColor, true)
        drawWrappedText(canvas, text, x + 18f, y + 62f, w - 36f, 15f, dark, 3)
    }

    private fun drawPhotos(canvas: Canvas, inspection: Inspection) {
        val boxX = 830f
        val boxY = 245f
        val boxW = 370f
        val boxH = 330f

        drawRoundBox(canvas, boxX, boxY, boxW, boxH, Color.WHITE, borderGray, 10f, 1.5f)

        drawText(canvas, "FOTO ANTES", boxX + 52f, boxY + 34f, 15f, purple, true)
        drawText(canvas, "FOTO DEPOIS", boxX + 212f, boxY + 34f, 15f, purple, true)

        drawPhoto(canvas, inspection.beforePhotoPath, boxX + 30f, boxY + 56f, 135f, 230f)
        drawPhoto(canvas, inspection.afterPhotoPath, boxX + 205f, boxY + 56f, 135f, 230f)
    }

    private fun drawPhoto(canvas: Canvas, path: String?, x: Float, y: Float, w: Float, h: Float) {
        drawRoundBox(canvas, x, y, w, h, Color.rgb(245, 245, 245), borderGray, 8f, 1f)

        if (path.isNullOrBlank()) {
            drawText(canvas, "Sem foto", x + 32f, y + h / 2f, 13f, Color.GRAY, false)
            return
        }

        try {
            val file = File(path)
            if (!file.exists()) {
                drawText(canvas, "Sem foto", x + 32f, y + h / 2f, 13f, Color.GRAY, false)
                return
            }

            val bmp = BitmapFactory.decodeFile(file.absolutePath) ?: return
            canvas.drawBitmap(bmp, null, RectF(x + 6f, y + 6f, x + w - 6f, y + h - 6f), null)
            bmp.recycle()
        } catch (_: Exception) {
            drawText(canvas, "Sem foto", x + 32f, y + h / 2f, 13f, Color.GRAY, false)
        }
    }

    private fun drawText(
        canvas: Canvas,
        text: String,
        x: Float,
        y: Float,
        size: Float,
        color: Int,
        bold: Boolean
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.textSize = size
        paint.color = color
        paint.typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        canvas.drawText(text, x, y, paint)
    }

    private fun drawWrappedText(
        canvas: Canvas,
        text: String,
        x: Float,
        y: Float,
        maxWidth: Float,
        size: Float,
        color: Int,
        maxLines: Int
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.textSize = size
        paint.color = color
        paint.typeface = Typeface.DEFAULT

        var currentY = y
        var line = ""
        var lines = 0

        text.split(" ").forEach { word ->
            val testLine = if (line.isEmpty()) word else "$line $word"

            if (paint.measureText(testLine) > maxWidth) {
                if (lines < maxLines) {
                    canvas.drawText(line, x, currentY, paint)
                    currentY += size + 7f
                    lines++
                    line = word
                }
            } else {
                line = testLine
            }
        }

        if (line.isNotBlank() && lines < maxLines) {
            canvas.drawText(line, x, currentY, paint)
        }
    }

    private fun drawRoundBox(
        canvas: Canvas,
        x: Float,
        y: Float,
        w: Float,
        h: Float,
        fill: Int,
        stroke: Int,
        radius: Float,
        strokeWidth: Float
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val rect = RectF(x, y, x + w, y + h)

        paint.style = Paint.Style.FILL
        paint.color = fill
        canvas.drawRoundRect(rect, radius, radius, paint)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = strokeWidth
        paint.color = stroke
        canvas.drawRoundRect(rect, radius, radius, paint)
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
            addEntry(zip, "docProps/core.xml", coreProps())
            addEntry(zip, "docProps/app.xml", appProps(slideImages.size))
            addEntry(zip, "ppt/presentation.xml", presentationXml(slideImages.size))
            addEntry(zip, "ppt/_rels/presentation.xml.rels", presentationRels(slideImages.size))
            addEntry(zip, "ppt/theme/theme1.xml", themeXml())
            addEntry(zip, "ppt/slideMasters/slideMaster1.xml", slideMasterXml())
            addEntry(zip, "ppt/slideMasters/_rels/slideMaster1.xml.rels", slideMasterRels())
            addEntry(zip, "ppt/slideLayouts/slideLayout1.xml", slideLayoutXml())
            addEntry(zip, "ppt/slideLayouts/_rels/slideLayout1.xml.rels", slideLayoutRels())

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

        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
<Default Extension="xml" ContentType="application/xml"/>
<Default Extension="png" ContentType="image/png"/>
<Override PartName="/docProps/core.xml" ContentType="application/vnd.openxmlformats-package.core-properties+xml"/>
<Override PartName="/docProps/app.xml" ContentType="application/vnd.openxmlformats-officedocument.extended-properties+xml"/>
<Override PartName="/ppt/presentation.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.presentation.main+xml"/>
<Override PartName="/ppt/theme/theme1.xml" ContentType="application/vnd.openxmlformats-officedocument.theme+xml"/>
<Override PartName="/ppt/slideMasters/slideMaster1.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.slideMaster+xml"/>
<Override PartName="/ppt/slideLayouts/slideLayout1.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.slideLayout+xml"/>
$slides
</Types>"""
    }

    private fun rootRels(): String {
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="ppt/presentation.xml"/>
<Relationship Id="rId2" Type="http://schemas.openxmlformats.org/package/2006/relationships/metadata/core-properties" Target="docProps/core.xml"/>
<Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/extended-properties" Target="docProps/app.xml"/>
</Relationships>"""
    }

    private fun coreProps(): String {
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<cp:coreProperties xmlns:cp="http://schemas.openxmlformats.org/package/2006/metadata/core-properties" xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:dcterms="http://purl.org/dc/terms/" xmlns:dcmitype="http://purl.org/dc/dcmitype/" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
<dc:title>Safety Gemba Walk</dc:title>
<dc:creator>Ahlstrom</dc:creator>
<cp:lastModifiedBy>Ahlstrom</cp:lastModifiedBy>
</cp:coreProperties>"""
    }

    private fun appProps(count: Int): String {
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Properties xmlns="http://schemas.openxmlformats.org/officeDocument/2006/extended-properties" xmlns:vt="http://schemas.openxmlformats.org/officeDocument/2006/docPropsVTypes">
<Application>Microsoft PowerPoint</Application>
<Slides>$count</Slides>
</Properties>"""
    }

    private fun presentationXml(count: Int): String {
        val ids = (1..count).joinToString("") {
            """<p:sldId id="${255 + it}" r:id="rId$it"/>"""
        }

        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<p:presentation xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships" xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main">
<p:sldMasterIdLst><p:sldMasterId id="2147483648" r:id="rId${count + 1}"/></p:sldMasterIdLst>
<p:sldIdLst>$ids</p:sldIdLst>
<p:sldSz cx="12192000" cy="6858000" type="screen16x9"/>
<p:notesSz cx="6858000" cy="9144000"/>
</p:presentation>"""
    }

    private fun presentationRels(count: Int): String {
        val slideRels = (1..count).joinToString("") {
            """<Relationship Id="rId$it" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/slide" Target="slides/slide$it.xml"/>"""
        }

        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
$slideRels
<Relationship Id="rId${count + 1}" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/slideMaster" Target="slideMasters/slideMaster1.xml"/>
<Relationship Id="rId${count + 2}" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/theme" Target="theme/theme1.xml"/>
</Relationships>"""
    }

    private fun slideMasterXml(): String {
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<p:sldMaster xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships" xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main">
<p:cSld><p:spTree><p:nvGrpSpPr><p:cNvPr id="1" name=""/><p:cNvGrpSpPr/><p:nvPr/></p:nvGrpSpPr><p:grpSpPr/></p:spTree></p:cSld>
<p:sldLayoutIdLst><p:sldLayoutId id="2147483649" r:id="rId1"/></p:sldLayoutIdLst>
<p:txStyles><p:titleStyle/><p:bodyStyle/><p:otherStyle/></p:txStyles>
</p:sldMaster>"""
    }

    private fun slideMasterRels(): String {
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/slideLayout" Target="../slideLayouts/slideLayout1.xml"/>
<Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/theme" Target="../theme/theme1.xml"/>
</Relationships>"""
    }

    private fun slideLayoutXml(): String {
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<p:sldLayout xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships" xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main" type="blank" preserve="1">
<p:cSld name="Blank"><p:spTree><p:nvGrpSpPr><p:cNvPr id="1" name=""/><p:cNvGrpSpPr/><p:nvPr/></p:nvGrpSpPr><p:grpSpPr/></p:spTree></p:cSld>
</p:sldLayout>"""
    }

    private fun slideLayoutRels(): String {
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/slideMaster" Target="../slideMasters/slideMaster1.xml"/>
</Relationships>"""
    }

    private fun slideRel(n: Int): String {
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/image" Target="../media/image$n.png"/>
</Relationships>"""
    }

    private fun slideXml(n: Int): String {
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<p:sld xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships" xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main">
<p:cSld>
<p:spTree>
<p:nvGrpSpPr><p:cNvPr id="1" name=""/><p:cNvGrpSpPr/><p:nvPr/></p:nvGrpSpPr>
<p:grpSpPr/>
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

    private fun themeXml(): String {
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<a:theme xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" name="Office Theme">
<a:themeElements>
<a:clrScheme name="Office">
<a:dk1><a:srgbClr val="000000"/></a:dk1>
<a:lt1><a:srgbClr val="FFFFFF"/></a:lt1>
<a:dk2><a:srgbClr val="1F497D"/></a:dk2>
<a:lt2><a:srgbClr val="EEECE1"/></a:lt2>
<a:accent1><a:srgbClr val="4F81BD"/></a:accent1>
<a:accent2><a:srgbClr val="C0504D"/></a:accent2>
<a:accent3><a:srgbClr val="9BBB59"/></a:accent3>
<a:accent4><a:srgbClr val="8064A2"/></a:accent4>
<a:accent5><a:srgbClr val="4BACC6"/></a:accent5>
<a:accent6><a:srgbClr val="F79646"/></a:accent6>
<a:hlink><a:srgbClr val="0000FF"/></a:hlink>
<a:folHlink><a:srgbClr val="800080"/></a:folHlink>
</a:clrScheme>
<a:fontScheme name="Office"><a:majorFont><a:latin typeface="Arial"/></a:majorFont><a:minorFont><a:latin typeface="Arial"/></a:minorFont></a:fontScheme>
<a:fmtScheme name="Office"><a:fillStyleLst/><a:lnStyleLst/><a:effectStyleLst/><a:bgFillStyleLst/></a:fmtScheme>
</a:themeElements>
<a:objectDefaults/>
<a:extraClrSchemeLst/>
</a:theme>"""
    }
}
