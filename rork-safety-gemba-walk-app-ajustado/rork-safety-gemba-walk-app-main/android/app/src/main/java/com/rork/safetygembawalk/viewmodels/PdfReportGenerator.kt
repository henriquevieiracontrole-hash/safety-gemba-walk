package com.rork.safetygembawalk.viewmodels

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.kernel.colors.DeviceRgb
import com.itextpdf.kernel.font.PdfFontFactory
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.borders.Border
import com.itextpdf.layout.borders.SolidBorder
import com.itextpdf.layout.element.AreaBreak
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Image
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.TextAlignment
import com.rork.safetygembawalk.data.Inspection
import com.rork.safetygembawalk.data.InspectionActionItem
import com.rork.safetygembawalk.data.InspectionStatus
import com.rork.safetygembawalk.data.formattedDate
import com.rork.safetygembawalk.data.formattedWorkOrderOpenDate
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PdfReportGenerator(private val context: Context) {

    private val purple = DeviceRgb(107, 35, 120)
    private val purpleDark = DeviceRgb(72, 18, 88)
    private val orange = DeviceRgb(242, 140, 40)
    private val green = DeviceRgb(22, 138, 74)
    private val red = DeviceRgb(220, 38, 38)
    private val dark = DeviceRgb(30, 41, 59)
    private val muted = DeviceRgb(100, 116, 139)
    private val white = DeviceRgb(255, 255, 255)
    private val line = DeviceRgb(220, 205, 228)
    private val cardBg = DeviceRgb(255, 255, 255)
    private val softCard = DeviceRgb(252, 249, 253)

    fun generateReport(inspections: List<Inspection>): String {
        val fileName =
            "Safety_Gemba_Walk_${SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())}.pdf"

        val file = File(context.getExternalFilesDir(null), fileName)

        PdfWriter(file.absolutePath).use { writer ->
            PdfDocument(writer).use { pdfDoc ->
                Document(pdfDoc, PageSize.A4).use { document ->
                    document.setMargins(20f, 20f, 16f, 20f)

                    var firstPage = true

                    inspections.forEachIndexed { inspectionIndex, inspection ->
                        val actions = inspection.actions

                        if (actions.isEmpty()) {
                            if (!firstPage) document.add(AreaBreak())
                            addOneActionPage(document, inspection, inspectionIndex + 1, null, 1, 1)
                            firstPage = false
                        } else {
                            actions.forEachIndexed { actionIndex, action ->
                                if (!firstPage) document.add(AreaBreak())
                                addOneActionPage(
                                    document = document,
                                    inspection = inspection,
                                    inspectionNumber = inspectionIndex + 1,
                                    action = action,
                                    actionIndex = actionIndex + 1,
                                    totalActions = actions.size
                                )
                                firstPage = false
                            }
                        }
                    }
                }
            }
        }

        return file.absolutePath
    }

    private fun addOneActionPage(
        document: Document,
        inspection: Inspection,
        inspectionNumber: Int,
        action: InspectionActionItem?,
        actionIndex: Int,
        totalActions: Int
    ) {
        addHeader(document)
        addInspectionHero(document, inspection, inspectionNumber, action, actionIndex, totalActions)
        addSingleAction(document, action, actionIndex, totalActions)
        addPhotoEvidence(document, action, actionIndex)
        addFooter(document)
    }

    private fun addHeader(document: Document) {
        val bold = PdfFontFactory.createFont("Helvetica-Bold")
        val regular = PdfFontFactory.createFont("Helvetica")

        val table = Table(floatArrayOf(0.55f, 4.8f, 1.3f)).useAllAvailableWidth()
        table.setMarginBottom(10f)

        val logo = Cell()
            .setBorder(Border.NO_BORDER)
            .setBackgroundColor(purple)
            .setPadding(9f)
            .setTextAlignment(TextAlignment.CENTER)

        logo.add(
            Paragraph("SG")
                .setFont(bold)
                .setFontSize(18f)
                .setFontColor(white)
                .setTextAlignment(TextAlignment.CENTER)
                .setMargin(0f)
        )

        val title = Cell()
            .setBorder(Border.NO_BORDER)
            .setBackgroundColor(purple)
            .setPaddingTop(12f)
            .setPaddingLeft(10f)
            .setPaddingBottom(10f)

        title.add(
            Paragraph("SAFETY GEMBA WALK")
                .setFont(bold)
                .setFontSize(24f)
                .setFontColor(white)
                .setMarginBottom(1f)
        )

        title.add(
            Paragraph("Safety is my first job!!! · Relatório Executivo de Inspeção")
                .setFont(regular)
                .setFontSize(10f)
                .setFontColor(DeviceRgb(246, 222, 250))
        )

        val date = Cell()
            .setBorder(Border.NO_BORDER)
            .setBackgroundColor(purple)
            .setPaddingTop(19f)
            .setPaddingRight(9f)
            .setTextAlignment(TextAlignment.RIGHT)

        date.add(
            Paragraph(SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date()))
                .setFont(bold)
                .setFontSize(8.5f)
                .setFontColor(white)
        )

        table.addCell(logo)
        table.addCell(title)
        table.addCell(date)
        document.add(table)

        val orangeLine = Table(floatArrayOf(1f)).useAllAvailableWidth()
        orangeLine.setMarginTop(-8f)
        orangeLine.setMarginBottom(10f)
        orangeLine.addCell(
            Cell()
                .setBorder(Border.NO_BORDER)
                .setBackgroundColor(orange)
                .setPadding(1.2f)
                .add(Paragraph(" ").setFontSize(1f).setMargin(0f))
        )
        document.add(orangeLine)
    }

    private fun addInspectionHero(
        document: Document,
        inspection: Inspection,
        inspectionNumber: Int,
        action: InspectionActionItem?,
        actionIndex: Int,
        totalActions: Int
    ) {
        val bold = PdfFontFactory.createFont("Helvetica-Bold")
        val regular = PdfFontFactory.createFont("Helvetica")

        val status = action?.status ?: inspection.status
        val statusText = statusText(status)
        val statusColor = statusColor(status)

        val top = Table(floatArrayOf(3.7f, 1.3f)).useAllAvailableWidth()
        top.setMarginBottom(0f)

        top.addCell(
            Cell()
                .setBorder(Border.NO_BORDER)
                .setBackgroundColor(purpleDark)
                .setPadding(9f)
                .add(
                    Paragraph("INSPEÇÃO #$inspectionNumber — AÇÃO $actionIndex DE $totalActions")
                        .setFont(bold)
                        .setFontSize(13f)
                        .setFontColor(white)
                )
        )

        top.addCell(
            Cell()
                .setBorder(Border.NO_BORDER)
                .setBackgroundColor(statusColor)
                .setPadding(9f)
                .setTextAlignment(TextAlignment.CENTER)
                .add(
                    Paragraph(statusText)
                        .setFont(bold)
                        .setFontSize(12f)
                        .setFontColor(white)
                )
        )

        document.add(top)

        val grid = Table(floatArrayOf(1f, 1f, 1f, 1f)).useAllAvailableWidth()
        grid.setBorder(SolidBorder(line, 1f))
        grid.setMarginBottom(8f)

        addMiniInfo(grid, "Data da inspeção", inspection.formattedDate())
        addMiniInfo(grid, "Área / Local", inspection.location)
        addMiniInfo(grid, "Inspetor", inspection.inspectorName)
        addMiniInfo(grid, "Status", statusText)

        addMiniInfo(grid, "Criado por", valueOrDash(action?.createdByName ?: inspection.createdByName))
        addMiniInfo(grid, "Atualizado por", valueOrDash(action?.lastUpdatedByName ?: inspection.lastUpdatedByName))
        addMiniInfo(grid, "Ações totais", inspection.actions.size.toString())
        addMiniInfo(grid, "OS abertas", inspection.actions.count { it.hasWorkOrder }.toString())

        document.add(grid)

        val summary = Table(floatArrayOf(1.18f, 2.42f)).useAllAvailableWidth()
        summary.setMarginBottom(8f)

        val indicators = Cell()
            .setBorder(SolidBorder(line, 1f))
            .setBackgroundColor(softCard)
            .setPadding(8f)

        indicators.add(
            Paragraph("INDICADORES")
                .setFont(bold)
                .setFontSize(11f)
                .setFontColor(purple)
                .setMarginBottom(4f)
        )

        addIndicator(indicators, "Total de ações", inspection.actions.size.toString())
        addIndicator(indicators, "Pendentes", inspection.actions.count { it.status == InspectionStatus.PENDING }.toString())
        addIndicator(indicators, "Em andamento", inspection.actions.count { it.status == InspectionStatus.IN_PROGRESS }.toString())
        addIndicator(indicators, "Concluídas", inspection.actions.count { it.status == InspectionStatus.COMPLETED }.toString())
        addIndicator(indicators, "Com OS", inspection.actions.count { it.hasWorkOrder }.toString())
        addIndicator(indicators, "Herdadas", inspection.actions.count { it.isInherited }.toString())

        val resume = Cell()
            .setBorder(SolidBorder(line, 1f))
            .setBackgroundColor(cardBg)
            .setPadding(8f)

        resume.add(
            Paragraph("RESUMO DA INSPEÇÃO")
                .setFont(bold)
                .setFontSize(11f)
                .setFontColor(purple)
                .setMarginBottom(5f)
        )

        resume.add(
            Paragraph(valueOrDash(inspection.title))
                .setFont(bold)
                .setFontSize(10f)
                .setFontColor(dark)
                .setMarginBottom(5f)
        )

        resume.add(
            Paragraph("Cada página apresenta uma única ação da inspeção, com seus dados, status, rastreabilidade, OS e evidências fotográficas.")
                .setFont(regular)
                .setFontSize(8.8f)
                .setFontColor(muted)
        )

        summary.addCell(indicators)
        summary.addCell(resume)
        document.add(summary)
    }

    private fun addMiniInfo(table: Table, label: String, value: String) {
        val bold = PdfFontFactory.createFont("Helvetica-Bold")
        val regular = PdfFontFactory.createFont("Helvetica")

        val cell = Cell()
            .setBorder(SolidBorder(line, 0.6f))
            .setPadding(6f)
            .setBackgroundColor(cardBg)

        cell.add(
            Paragraph(label)
                .setFont(bold)
                .setFontSize(7.4f)
                .setFontColor(purple)
                .setMarginBottom(1f)
        )

        cell.add(
            Paragraph(valueOrDash(value))
                .setFont(regular)
                .setFontSize(8.3f)
                .setFontColor(dark)
        )

        table.addCell(cell)
    }

    private fun addIndicator(cell: Cell, label: String, value: String) {
        val bold = PdfFontFactory.createFont("Helvetica-Bold")
        val regular = PdfFontFactory.createFont("Helvetica")

        val table = Table(floatArrayOf(2.1f, 0.7f)).useAllAvailableWidth()
        table.setMarginBottom(1f)

        table.addCell(
            Cell()
                .setBorder(Border.NO_BORDER)
                .add(
                    Paragraph(label)
                        .setFont(regular)
                        .setFontSize(8.2f)
                        .setFontColor(dark)
                )
        )

        table.addCell(
            Cell()
                .setBorder(Border.NO_BORDER)
                .setTextAlignment(TextAlignment.RIGHT)
                .add(
                    Paragraph(value)
                        .setFont(bold)
                        .setFontSize(8.8f)
                        .setFontColor(purple)
                )
        )

        cell.add(table)
    }

    private fun addSingleAction(
        document: Document,
        action: InspectionActionItem?,
        actionIndex: Int,
        totalActions: Int
    ) {
        val bold = PdfFontFactory.createFont("Helvetica-Bold")
        val regular = PdfFontFactory.createFont("Helvetica")

        document.add(
            Paragraph("AÇÃO REGISTRADA")
                .setFont(bold)
                .setFontSize(12f)
                .setFontColor(purple)
                .setMarginTop(2f)
                .setMarginBottom(5f)
        )

        if (action == null) {
            document.add(
                Paragraph("Nenhuma ação registrada nesta inspeção.")
                    .setFont(regular)
                    .setFontSize(9f)
                    .setFontColor(dark)
            )
            return
        }

        val box = Table(floatArrayOf(1f)).useAllAvailableWidth()
        box.setBorder(SolidBorder(line, 1f))
        box.setMarginBottom(8f)

        box.addCell(
            Cell()
                .setBorder(Border.NO_BORDER)
                .setBackgroundColor(statusColor(action.status))
                .setPadding(7f)
                .add(
                    Paragraph("AÇÃO $actionIndex DE $totalActions - ${statusLabel(action.status)}")
                        .setFont(bold)
                        .setFontSize(10f)
                        .setFontColor(white)
                )
        )

        val body = Table(floatArrayOf(1.25f, 1f)).useAllAvailableWidth()

        val left = Cell()
            .setBorder(Border.NO_BORDER)
            .setBackgroundColor(softCard)
            .setPadding(8f)

        left.add(lineText("Risco identificado", action.unsafeCondition))
        left.add(lineText("Descrição", action.description))
        left.add(lineText("Ação imediata", action.immediateAction))

        val osText =
            if (action.hasWorkOrder) {
                "${action.workOrderNumber ?: "N/A"} · abertura: ${action.formattedWorkOrderOpenDate()}"
            } else {
                "Sem OS"
            }

        left.add(lineText("Ordem de Serviço", osText))

        val right = Cell()
            .setBorder(Border.NO_BORDER)
            .setBackgroundColor(softCard)
            .setPadding(8f)

        right.add(lineText("Criado por", action.createdByName))
        right.add(lineText("Atualizado por", action.lastUpdatedByName))

        if (action.completedAt != null) {
            right.add(lineText("Concluído em", formatMillis(action.completedAt)))
        }

        if (action.isInherited) {
            right.add(lineText("Ação herdada", "Sim"))
            right.add(lineText("Carregada", "${action.carriedCount} vez(es)"))
            right.add(lineText("Atualização no dia", if (action.hasChangesToday) "Com atualização" else "Sem modificação"))
        } else {
            right.add(lineText("Ação herdada", "Não"))
        }

        body.addCell(left)
        body.addCell(right)

        box.addCell(
            Cell()
                .setBorder(Border.NO_BORDER)
                .setPadding(0f)
                .add(body)
        )

        document.add(box)
    }

    private fun lineText(label: String, value: String?): Paragraph {
        val bold = PdfFontFactory.createFont("Helvetica-Bold")
        val regular = PdfFontFactory.createFont("Helvetica")

        return Paragraph()
            .add(
                com.itextpdf.layout.element.Text("$label: ")
                    .setFont(bold)
                    .setFontSize(8.5f)
                    .setFontColor(purple)
            )
            .add(
                com.itextpdf.layout.element.Text(valueOrDash(value))
                    .setFont(regular)
                    .setFontSize(8.5f)
                    .setFontColor(dark)
            )
            .setMarginBottom(3f)
    }

    private fun addPhotoEvidence(
        document: Document,
        action: InspectionActionItem?,
        actionIndex: Int
    ) {
        val bold = PdfFontFactory.createFont("Helvetica-Bold")

        if (action == null) return

        document.add(
            Paragraph("EVIDÊNCIAS FOTOGRÁFICAS")
                .setFont(bold)
                .setFontSize(12f)
                .setFontColor(purple)
                .setMarginTop(2f)
                .setMarginBottom(5f)
        )

        val photos = Table(floatArrayOf(1f, 1f)).useAllAvailableWidth()
        photos.setBorder(SolidBorder(line, 1f))
        photos.setBackgroundColor(cardBg)

        val before = Cell()
            .setBorder(Border.NO_BORDER)
            .setPadding(7f)

        before.add(
            Paragraph("AÇÃO $actionIndex - FOTO ANTES")
                .setFont(bold)
                .setFontSize(8.5f)
                .setFontColor(purple)
                .setMarginBottom(4f)
        )

        if (!action.beforePhotoPath.isNullOrBlank()) {
            addImage(before, action.beforePhotoPath, 230f)
        } else {
            addNoImage(before)
        }

        val after = Cell()
            .setBorder(Border.NO_BORDER)
            .setPadding(7f)

        after.add(
            Paragraph("AÇÃO $actionIndex - FOTO DEPOIS")
                .setFont(bold)
                .setFontSize(8.5f)
                .setFontColor(purple)
                .setMarginBottom(4f)
        )

        if (!action.afterPhotoPath.isNullOrBlank()) {
            addImage(after, action.afterPhotoPath, 230f)
        } else {
            addNoImage(after)
        }

        photos.addCell(before)
        photos.addCell(after)
        document.add(photos)
    }

    private fun addNoImage(cell: Cell) {
        cell.add(
            Paragraph("[Sem foto]")
                .setFont(PdfFontFactory.createFont("Helvetica-Oblique"))
                .setFontSize(8f)
                .setFontColor(muted)
        )
    }

    private fun addImage(cell: Cell, imagePath: String, maxHeight: Float) {
        val italic = PdfFontFactory.createFont("Helvetica-Oblique")

        try {
            val file = File(imagePath)

            if (!file.exists()) {
                cell.add(
                    Paragraph("[Imagem não disponível]")
                        .setFont(italic)
                        .setFontSize(8f)
                        .setFontColor(muted)
                )
                return
            }

            val bytes = getCorrectedImageBytes(imagePath)
            val imageData = if (bytes != null) ImageDataFactory.create(bytes) else ImageDataFactory.create(imagePath)

            val image = Image(imageData)
            image.setAutoScale(true)
            image.setMaxHeight(maxHeight)

            cell.add(image)

        } catch (e: Exception) {
            cell.add(
                Paragraph("[Imagem não disponível]")
                    .setFont(italic)
                    .setFontSize(8f)
                    .setFontColor(muted)
            )
        }
    }

    private fun getCorrectedImageBytes(path: String): ByteArray? {
        return try {
            val original = BitmapFactory.decodeFile(path) ?: return null

            val exif = ExifInterface(path)
            val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)

            val matrix = Matrix()

            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
            }

            val corrected =
                if (!matrix.isIdentity) {
                    Bitmap.createBitmap(original, 0, 0, original.width, original.height, matrix, true)
                } else {
                    original
                }

            val output = ByteArrayOutputStream()
            corrected.compress(Bitmap.CompressFormat.JPEG, 82, output)

            if (corrected != original) corrected.recycle()
            original.recycle()

            output.toByteArray()

        } catch (e: Exception) {
            null
        }
    }

    private fun addFooter(document: Document) {
        val regular = PdfFontFactory.createFont("Helvetica")

        document.add(
            Paragraph("Safety Gemba Walk · Ahlstrom · Desenvolvido por Henrique Vieira")
                .setFont(regular)
                .setFontSize(7.2f)
                .setFontColor(muted)
                .setTextAlignment(TextAlignment.RIGHT)
                .setMarginTop(6f)
        )
    }

    private fun statusText(status: InspectionStatus): String {
        return when (status) {
            InspectionStatus.COMPLETED -> "CONCLUÍDO"
            InspectionStatus.IN_PROGRESS -> "EM ANDAMENTO"
            InspectionStatus.PENDING -> "PENDENTE"
            InspectionStatus.CANCELLED -> "CANCELADO"
        }
    }

    private fun statusLabel(status: InspectionStatus): String {
        return when (status) {
            InspectionStatus.COMPLETED -> "CONCLUÍDA"
            InspectionStatus.IN_PROGRESS -> "EM ANDAMENTO"
            InspectionStatus.PENDING -> "PENDENTE"
            InspectionStatus.CANCELLED -> "CANCELADA"
        }
    }

    private fun statusColor(status: InspectionStatus): DeviceRgb {
        return when (status) {
            InspectionStatus.COMPLETED -> green
            InspectionStatus.IN_PROGRESS -> orange
            InspectionStatus.PENDING -> red
            InspectionStatus.CANCELLED -> dark
        }
    }

    private fun valueOrDash(value: String?): String {
        return if (value.isNullOrBlank()) "-" else value
    }

    private fun formatMillis(value: Long): String {
        return SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(value))
    }
}
