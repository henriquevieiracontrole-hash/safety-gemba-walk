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
import com.itextpdf.layout.properties.UnitValue
import com.rork.safetygembawalk.data.Inspection
import com.rork.safetygembawalk.data.InspectionStatus
import com.rork.safetygembawalk.data.formattedDate
import com.rork.safetygembawalk.data.formattedWorkOrderOpenDate
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PdfReportGenerator(private val context: Context) {

    private val ahlstromPurple = DeviceRgb(107, 35, 120)
    private val ahlstromPurpleDark = DeviceRgb(74, 18, 88)
    private val ahlstromOrange = DeviceRgb(242, 140, 40)
    private val navy = DeviceRgb(9, 31, 65)
    private val red = DeviceRgb(220, 38, 38)
    private val green = DeviceRgb(22, 138, 74)
    private val dark = DeviceRgb(30, 41, 59)
    private val white = DeviceRgb(255, 255, 255)
    private val lightGray = DeviceRgb(248, 250, 252)
    private val line = DeviceRgb(222, 210, 230)

    fun generateReport(inspections: List<Inspection>): String {
        val fileName = "Safety_Gemba_Walk_${SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())}.pdf"
        val file = File(context.getExternalFilesDir(null), fileName)

        PdfWriter(file.absolutePath).use { writer ->
            PdfDocument(writer).use { pdfDoc ->
                Document(pdfDoc, PageSize.A4).use { document ->
                    document.setMargins(28f, 28f, 28f, 28f)

                    inspections.forEachIndexed { index, inspection ->
                        if (index > 0) document.add(AreaBreak())
                        addHeader(document)
                        addInspection(document, inspection, index + 1)
                    }
                }
            }
        }

        return file.absolutePath
    }

    private fun addHeader(document: Document) {
        val bold = PdfFontFactory.createFont("Helvetica-Bold")
        val regular = PdfFontFactory.createFont("Helvetica")

        val header = Table(floatArrayOf(1.15f, 4.4f)).useAllAvailableWidth()
        header.setBackgroundColor(ahlstromPurple)
        header.setBorder(Border.NO_BORDER)
        header.setMarginBottom(14f)

        val logoCell = Cell()
            .setBorder(Border.NO_BORDER)
            .setPadding(14f)
            .setTextAlignment(TextAlignment.CENTER)

        logoCell.add(
            Paragraph("A")
                .setFont(bold)
                .setFontSize(30f)
                .setFontColor(white)
                .setTextAlignment(TextAlignment.CENTER)
        )

        logoCell.add(
            Paragraph("AHLSTROM")
                .setFont(bold)
                .setFontSize(7.5f)
                .setFontColor(white)
                .setTextAlignment(TextAlignment.CENTER)
        )

        val titleCell = Cell()
            .setBorder(Border.NO_BORDER)
            .setPaddingTop(16f)
            .setPaddingBottom(14f)
            .setPaddingLeft(10f)

        titleCell.add(
            Paragraph("SAFETY GEMBA WALK")
                .setFont(bold)
                .setFontSize(27f)
                .setFontColor(white)
                .setMarginBottom(2f)
        )

        titleCell.add(
            Paragraph("Safety is my first job!!!")
                .setFont(regular)
                .setFontSize(13f)
                .setFontColor(DeviceRgb(245, 225, 250))
        )

        titleCell.add(
            Paragraph("Relatorio executivo de inspecao de seguranca")
                .setFont(regular)
                .setFontSize(9.5f)
                .setFontColor(DeviceRgb(235, 210, 240))
                .setMarginTop(4f)
        )

        header.addCell(logoCell)
        header.addCell(titleCell)
        document.add(header)

        document.add(
            Paragraph("Emitido em: ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())}")
                .setFont(bold)
                .setFontSize(9.5f)
                .setFontColor(dark)
                .setTextAlignment(TextAlignment.RIGHT)
                .setMarginBottom(10f)
        )
    }

    private fun addInspection(document: Document, inspection: Inspection, number: Int) {
        val bold = PdfFontFactory.createFont("Helvetica-Bold")
        val regular = PdfFontFactory.createFont("Helvetica")

        val title = Table(floatArrayOf(4f, 1.6f)).useAllAvailableWidth()
        title.setMarginBottom(0f)

        title.addCell(
            Cell()
                .setBorder(Border.NO_BORDER)
                .setBackgroundColor(ahlstromPurpleDark)
                .setPadding(11f)
                .add(
                    Paragraph("INSPECAO #$number")
                        .setFont(bold)
                        .setFontSize(15f)
                        .setFontColor(white)
                )
        )

        title.addCell(
            Cell()
                .setBorder(Border.NO_BORDER)
                .setBackgroundColor(statusColor(inspection.status))
                .setPadding(11f)
                .setTextAlignment(TextAlignment.CENTER)
                .add(
                    Paragraph(statusText(inspection.status))
                        .setFont(bold)
                        .setFontSize(13f)
                        .setFontColor(white)
                )
        )

        document.add(title)

        val info = Table(floatArrayOf(1.25f, 1.25f, 1.25f, 1.25f)).useAllAvailableWidth()
        info.setBackgroundColor(white)
        info.setBorder(SolidBorder(line, 1f))
        info.setMarginBottom(12f)

        addMiniInfo(info, "Data da inspecao", inspection.formattedDate())
        addMiniInfo(info, "Area / Local", inspection.location)
        addMiniInfo(info, "Inspetor", inspection.inspectorName)
        addMiniInfo(info, "Status", statusText(inspection.status))
        addMiniInfo(info, "Criado por", valueOrDash(inspection.createdByName))
        addMiniInfo(info, "Atualizado por", valueOrDash(inspection.lastUpdatedByName))
        addMiniInfo(info, "Acoes", inspection.actions.size.toString())
        addMiniInfo(info, "OS abertas", inspection.actions.count { it.hasWorkOrder }.toString())

        document.add(info)

        val summary = Table(floatArrayOf(1.25f, 2f)).useAllAvailableWidth()
        summary.setMarginBottom(14f)

        val left = Cell()
            .setBorder(SolidBorder(line, 1f))
            .setBackgroundColor(white)
            .setPadding(12f)

        left.add(
            Paragraph("INDICADORES")
                .setFont(bold)
                .setFontSize(13f)
                .setFontColor(ahlstromPurple)
                .setMarginBottom(8f)
        )

        addIndicator(left, "Total de acoes", inspection.actions.size.toString())
        addIndicator(left, "Pendentes", inspection.actions.count { it.status == InspectionStatus.PENDING }.toString())
        addIndicator(left, "Em andamento", inspection.actions.count { it.status == InspectionStatus.IN_PROGRESS }.toString())
        addIndicator(left, "Concluidas", inspection.actions.count { it.status == InspectionStatus.COMPLETED }.toString())
        addIndicator(left, "Com OS", inspection.actions.count { it.hasWorkOrder }.toString())
        addIndicator(left, "Herdadas", inspection.actions.count { it.isInherited }.toString())

        val right = Cell()
            .setBorder(SolidBorder(line, 1f))
            .setBackgroundColor(white)
            .setPadding(12f)

        right.add(
            Paragraph("RESUMO DA INSPECAO")
                .setFont(bold)
                .setFontSize(13f)
                .setFontColor(ahlstromPurple)
                .setMarginBottom(8f)
        )

        right.add(
            Paragraph(valueOrDash(inspection.title))
                .setFont(regular)
                .setFontSize(11f)
                .setFontColor(dark)
                .setMarginBottom(10f)
        )

        right.add(
            Paragraph("Controle operacional")
                .setFont(bold)
                .setFontSize(11f)
                .setFontColor(navy)
                .setMarginTop(6f)
        )

        right.add(
            Paragraph("Relatorio com rastreabilidade de criacao, ultima atualizacao, OS vinculadas, evidencias fotograficas e controle de acoes herdadas.")
                .setFont(regular)
                .setFontSize(10f)
                .setFontColor(dark)
        )

        summary.addCell(left)
        summary.addCell(right)
        document.add(summary)

        addActions(document, inspection)
        addPhotos(document, inspection)
    }

    private fun addMiniInfo(table: Table, label: String, value: String) {
        val bold = PdfFontFactory.createFont("Helvetica-Bold")
        val regular = PdfFontFactory.createFont("Helvetica")

        val cell = Cell()
            .setBorder(SolidBorder(line, 0.7f))
            .setPadding(8f)
            .setBackgroundColor(white)

        cell.add(
            Paragraph(label)
                .setFont(bold)
                .setFontSize(8.5f)
                .setFontColor(ahlstromPurple)
                .setMarginBottom(2f)
        )

        cell.add(
            Paragraph(valueOrDash(value))
                .setFont(regular)
                .setFontSize(9.5f)
                .setFontColor(dark)
        )

        table.addCell(cell)
    }

    private fun addIndicator(cell: Cell, label: String, value: String) {
        val bold = PdfFontFactory.createFont("Helvetica-Bold")
        val regular = PdfFontFactory.createFont("Helvetica")

        val table = Table(floatArrayOf(2f, 0.8f)).useAllAvailableWidth()
        table.setMarginBottom(4f)

        table.addCell(
            Cell()
                .setBorder(Border.NO_BORDER)
                .add(Paragraph(label).setFont(regular).setFontSize(10f).setFontColor(dark))
        )

        table.addCell(
            Cell()
                .setBorder(Border.NO_BORDER)
                .setTextAlignment(TextAlignment.RIGHT)
                .add(Paragraph(value).setFont(bold).setFontSize(11f).setFontColor(ahlstromPurple))
        )

        cell.add(table)
    }

    private fun addActions(document: Document, inspection: Inspection) {
        val bold = PdfFontFactory.createFont("Helvetica-Bold")
        val regular = PdfFontFactory.createFont("Helvetica")

        document.add(
            Paragraph("ACOES REGISTRADAS")
                .setFont(bold)
                .setFontSize(15f)
                .setFontColor(ahlstromPurple)
                .setMarginTop(6f)
                .setMarginBottom(8f)
        )

        if (inspection.actions.isEmpty()) {
            document.add(Paragraph("Nenhuma acao registrada.").setFont(regular).setFontSize(10f).setFontColor(dark))
            return
        }

        inspection.actions.forEachIndexed { index, action ->
            val actionBox = Table(floatArrayOf(1f)).useAllAvailableWidth()
            actionBox.setBorder(SolidBorder(line, 1f))
            actionBox.setMarginBottom(10f)

            val header = Cell()
                .setBorder(Border.NO_BORDER)
                .setBackgroundColor(statusColor(action.status))
                .setPadding(9f)

            header.add(
                Paragraph("ACAO ${index + 1} - ${statusLabel(action.status)}")
                    .setFont(bold)
                    .setFontSize(12f)
                    .setFontColor(white)
            )

            actionBox.addCell(header)

            val content = Cell()
                .setBorder(Border.NO_BORDER)
                .setPadding(11f)
                .setBackgroundColor(lightGray)

            addActionText(content, "Risco identificado", action.unsafeCondition)
            addActionText(content, "Descricao", action.description)
            addActionText(content, "Acao imediata", action.immediateAction)

            if (action.hasWorkOrder) {
                addActionText(content, "Ordem de Servico", action.workOrderNumber ?: "N/A")
                addActionText(content, "Data abertura OS", action.formattedWorkOrderOpenDate())
            } else {
                addActionText(content, "Ordem de Servico", "Sem OS")
            }

            if (action.createdByName.isNotBlank()) addActionText(content, "Criado por", action.createdByName)
            if (action.lastUpdatedByName.isNotBlank()) addActionText(content, "Ultima atualizacao por", action.lastUpdatedByName)
            if (action.completedAt != null) addActionText(content, "Concluido em", formatMillis(action.completedAt))

            if (action.isInherited) {
                val inheritedText = "Sim" + (action.inheritedFromDate?.let { " - herdada de ${formatMillis(it)}" } ?: "")
                addActionText(content, "Acao herdada", inheritedText)
                addActionText(content, "Atualizacao no dia", if (action.hasChangesToday) "Com atualizacao" else "Sem modificacao")
                addActionText(content, "Quantidade de carregamentos", action.carriedCount.toString())
            }

            actionBox.addCell(content)
            document.add(actionBox)
        }
    }

    private fun addActionText(cell: Cell, label: String, value: String?) {
        val bold = PdfFontFactory.createFont("Helvetica-Bold")
        val regular = PdfFontFactory.createFont("Helvetica")
        cell.add(Paragraph(label).setFont(bold).setFontSize(9.7f).setFontColor(ahlstromPurple).setMarginBottom(1f))
        cell.add(Paragraph(valueOrDash(value)).setFont(regular).setFontSize(9.7f).setFontColor(dark).setMarginBottom(7f))
    }

    private fun addPhotos(document: Document, inspection: Inspection) {
        val bold = PdfFontFactory.createFont("Helvetica-Bold")
        val actionsWithPhotos = inspection.actions.filter { !it.beforePhotoPath.isNullOrBlank() || !it.afterPhotoPath.isNullOrBlank() }
        if (actionsWithPhotos.isEmpty()) return

        document.add(
            Paragraph("EVIDENCIAS FOTOGRAFICAS")
                .setFont(bold)
                .setFontSize(15f)
                .setFontColor(ahlstromPurple)
                .setMarginTop(8f)
                .setMarginBottom(8f)
        )

        actionsWithPhotos.forEachIndexed { index, action ->
            val box = Table(floatArrayOf(1f, 1f)).useAllAvailableWidth()
            box.setMarginTop(6f)
            box.setMarginBottom(12f)
            box.setBorder(SolidBorder(line, 1f))
            box.setBackgroundColor(white)

            val before = Cell().setBorder(Border.NO_BORDER).setPadding(10f)
            before.add(Paragraph("ACAO ${index + 1} - FOTO ANTES").setFont(bold).setFontSize(10f).setFontColor(ahlstromPurple).setMarginBottom(7f))
            if (!action.beforePhotoPath.isNullOrBlank()) addImage(before, action.beforePhotoPath, 250f) else before.add(Paragraph("[Sem foto antes]").setFont(PdfFontFactory.createFont("Helvetica-Oblique")).setFontSize(9f).setFontColor(dark))

            val after = Cell().setBorder(Border.NO_BORDER).setPadding(10f)
            after.add(Paragraph("ACAO ${index + 1} - FOTO DEPOIS").setFont(bold).setFontSize(10f).setFontColor(ahlstromPurple).setMarginBottom(7f))
            if (!action.afterPhotoPath.isNullOrBlank()) addImage(after, action.afterPhotoPath, 250f) else after.add(Paragraph("[Sem foto depois]").setFont(PdfFontFactory.createFont("Helvetica-Oblique")).setFontSize(9f).setFontColor(dark))

            box.addCell(before)
            box.addCell(after)
            document.add(box)
        }
    }

    private fun addImage(cell: Cell, imagePath: String, maxHeight: Float) {
        val italic = PdfFontFactory.createFont("Helvetica-Oblique")
        try {
            val file = File(imagePath)
            if (!file.exists()) {
                cell.add(Paragraph("[Imagem nao disponivel]").setFont(italic).setFontSize(9f).setFontColor(dark))
                return
            }

            val imageBytes = getCorrectedImageBytes(imagePath)
            val imageData = if (imageBytes != null) ImageDataFactory.create(imageBytes) else ImageDataFactory.create(imagePath)
            val image = Image(imageData)
            image.setAutoScale(true)
            image.setMaxHeight(maxHeight)
            cell.add(image)
        } catch (e: Exception) {
            cell.add(Paragraph("[Imagem nao disponivel]").setFont(italic).setFontSize(9f).setFontColor(dark))
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

            val corrected = if (!matrix.isIdentity) {
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

    private fun statusText(status: InspectionStatus): String = when (status) {
        InspectionStatus.COMPLETED -> "CONCLUIDO"
        InspectionStatus.IN_PROGRESS -> "EM ANDAMENTO"
        InspectionStatus.PENDING -> "PENDENTE"
        InspectionStatus.CANCELLED -> "CANCELADO"
    }

    private fun statusLabel(status: InspectionStatus): String = when (status) {
        InspectionStatus.COMPLETED -> "CONCLUIDA"
        InspectionStatus.IN_PROGRESS -> "EM ANDAMENTO"
        InspectionStatus.PENDING -> "PENDENTE"
        InspectionStatus.CANCELLED -> "CANCELADA"
    }

    private fun statusColor(status: InspectionStatus): DeviceRgb = when (status) {
        InspectionStatus.COMPLETED -> green
        InspectionStatus.IN_PROGRESS -> ahlstromOrange
        InspectionStatus.PENDING -> red
        InspectionStatus.CANCELLED -> dark
    }

    private fun valueOrDash(value: String?): String = if (value.isNullOrBlank()) "-" else value

    private fun formatMillis(value: Long): String = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(value))
}
