package com.rork.safetygembawalk.viewmodels

import android.content.Context
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
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PdfReportGenerator(private val context: Context) {

    private val navy = DeviceRgb(9, 31, 65)
    private val purple = DeviceRgb(107, 35, 120)
    private val orange = DeviceRgb(255, 95, 35)
    private val red = DeviceRgb(220, 38, 38)
    private val green = DeviceRgb(34, 197, 94)
    private val yellow = DeviceRgb(234, 179, 8)
    private val lightPurple = DeviceRgb(235, 220, 240)
    private val dark = DeviceRgb(30, 41, 59)
    private val white = DeviceRgb(255, 255, 255)
    private val lightGray = DeviceRgb(248, 250, 252)

    fun generateReport(inspections: List<Inspection>): String {
        val fileName = "Safety_Gemba_Walk_${SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())}.pdf"
        val file = File(context.getExternalFilesDir(null), fileName)

        PdfWriter(file.absolutePath).use { writer ->
            PdfDocument(writer).use { pdfDoc ->
                Document(pdfDoc, PageSize.A4).use { document ->
                    document.setMargins(36f, 36f, 32f, 36f)

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

        val table = Table(floatArrayOf(1.1f, 4f)).useAllAvailableWidth()

        val logo = Cell().setBorder(Border.NO_BORDER).setPadding(10f)
        logo.add(
            Paragraph("SG")
                .setFont(bold)
                .setFontSize(22f)
                .setFontColor(white)
                .setTextAlignment(TextAlignment.CENTER)
                .setBackgroundColor(purple)
                .setPadding(14f)
        )

        val title = Cell().setBorder(Border.NO_BORDER).setPaddingTop(14f).setPaddingLeft(6f)
        title.add(Paragraph("SAFETY GEMBA WALK").setFont(bold).setFontSize(30f).setFontColor(navy))
        title.add(
            Paragraph("Safety is my first job!!!")
                .setFont(regular)
                .setFontSize(17f)
                .setFontColor(purple)
                .setMarginTop(2f)
        )

        table.addCell(logo)
        table.addCell(title)
        document.add(table)

        document.add(
            Paragraph("")
                .setBorderBottom(SolidBorder(purple, 2f))
                .setMarginTop(8f)
                .setMarginBottom(14f)
        )

        document.add(
            Paragraph("Data: ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())}")
                .setFont(bold)
                .setFontSize(11f)
                .setFontColor(dark)
                .setTextAlignment(TextAlignment.RIGHT)
                .setMarginBottom(18f)
        )
    }

    private fun addInspection(document: Document, inspection: Inspection, number: Int) {
        val bold = PdfFontFactory.createFont("Helvetica-Bold")
        val regular = PdfFontFactory.createFont("Helvetica")

        val main = Table(floatArrayOf(1f)).useAllAvailableWidth()
        main.setBorder(SolidBorder(lightPurple, 1f))

        val titleCell = Cell().setBorder(Border.NO_BORDER).setPadding(0f)
        titleCell.add(
            Paragraph("  INSPEÇÃO #$number")
                .setFont(bold)
                .setFontSize(15f)
                .setFontColor(white)
                .setBackgroundColor(purple)
                .setPadding(10f)
                .setWidth(UnitValue.createPercentValue(38f))
        )
        main.addCell(titleCell)

        val body = Cell().setBorder(Border.NO_BORDER).setPadding(16f)
        val details = Table(floatArrayOf(1.1f, 2f)).useAllAvailableWidth()

        val left = Cell()
            .setBorder(Border.NO_BORDER)
            .setPaddingRight(14f)
            .setBorderRight(SolidBorder(lightPurple, 1f))

        addInfo(left, "Data da inspeção", inspection.formattedDate())
        addInfo(left, "Local", inspection.location)
        addInfo(left, "Inspetor", inspection.inspectorName)

        if (inspection.createdByName.isNotBlank()) addInfo(left, "Criado por", inspection.createdByName)
        if (inspection.lastUpdatedByName.isNotBlank()) addInfo(left, "Última atualização por", inspection.lastUpdatedByName)

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

        left.add(
            Paragraph(statusText)
                .setFont(bold)
                .setFontSize(18f)
                .setFontColor(statusColor)
                .setTextAlignment(TextAlignment.CENTER)
                .setBorder(SolidBorder(statusColor, 2f))
                .setPadding(8f)
                .setMarginTop(8f)
                .setMarginBottom(14f)
        )

        val right = Cell().setBorder(Border.NO_BORDER).setPaddingLeft(18f)
        right.add(Paragraph("RESUMO DA INSPEÇÃO").setFont(bold).setFontSize(15f).setFontColor(red))
        right.add(Paragraph(valueOrDash(inspection.title)).setFont(regular).setFontSize(11f).setFontColor(dark).setMarginBottom(12f))
        right.add(Paragraph("").setBorderBottom(SolidBorder(lightPurple, 1f)).setMarginBottom(10f))
        right.add(Paragraph("INDICADORES").setFont(bold).setFontSize(15f).setFontColor(orange))

        val totalActions = inspection.actions.size
        val pendingActions = inspection.actions.count { it.status == InspectionStatus.PENDING }
        val completedActions = inspection.actions.count { it.status == InspectionStatus.COMPLETED }
        val workOrders = inspection.actions.count { it.hasWorkOrder }

        right.add(
            Paragraph("Total de ações: $totalActions\nPendentes: $pendingActions\nConcluídas: $completedActions\nCom OS: $workOrders")
                .setFont(regular)
                .setFontSize(11f)
                .setFontColor(dark)
                .setMarginBottom(12f)
        )

        details.addCell(left)
        details.addCell(right)
        body.add(details)
        main.addCell(body)
        document.add(main)

        addActions(document, inspection)
        addPhotos(document, inspection)
    }

    private fun addActions(document: Document, inspection: Inspection) {
        val bold = PdfFontFactory.createFont("Helvetica-Bold")
        val regular = PdfFontFactory.createFont("Helvetica")

        document.add(
            Paragraph("AÇÕES REGISTRADAS")
                .setFont(bold)
                .setFontSize(15f)
                .setFontColor(purple)
                .setMarginTop(16f)
                .setMarginBottom(8f)
        )

        if (inspection.actions.isEmpty()) {
            document.add(Paragraph("Nenhuma ação registrada.").setFont(regular).setFontSize(10f).setFontColor(dark))
            return
        }

        inspection.actions.forEachIndexed { index, action ->
            val actionBox = Table(floatArrayOf(1f)).useAllAvailableWidth()
            actionBox.setBorder(SolidBorder(lightPurple, 1f))
            actionBox.setMarginBottom(10f)

            val headerColor = when (action.status) {
                InspectionStatus.COMPLETED -> green
                InspectionStatus.IN_PROGRESS -> yellow
                InspectionStatus.PENDING -> orange
                InspectionStatus.CANCELLED -> dark
            }

            val header = Cell().setBorder(Border.NO_BORDER).setBackgroundColor(headerColor).setPadding(8f)
            header.add(Paragraph("AÇÃO ${index + 1} - ${statusLabel(action.status)}").setFont(bold).setFontSize(12f).setFontColor(white))
            actionBox.addCell(header)

            val content = Cell().setBorder(Border.NO_BORDER).setPadding(12f).setBackgroundColor(lightGray)
            addActionText(content, "Risco identificado", action.unsafeCondition)
            addActionText(content, "Descrição", action.description)
            addActionText(content, "Ação imediata", action.immediateAction)

            if (action.hasWorkOrder) {
                addActionText(content, "Ordem de Serviço", action.workOrderNumber ?: "N/A")
                addActionText(content, "Data abertura OS", action.formattedWorkOrderOpenDate())
            } else {
                addActionText(content, "Ordem de Serviço", "Sem OS")
            }

            if (action.createdByName.isNotBlank()) addActionText(content, "Criado por", action.createdByName)
            if (action.lastUpdatedByName.isNotBlank()) addActionText(content, "Última atualização por", action.lastUpdatedByName)
            if (action.completedAt != null) addActionText(content, "Concluído em", formatMillis(action.completedAt))

            if (action.isInherited) {
                val inheritedText = "Sim" + (action.inheritedFromDate?.let { " - herdada de ${formatMillis(it)}" } ?: "")
                addActionText(content, "Ação herdada", inheritedText)
                addActionText(content, "Atualização no dia", if (action.hasChangesToday) "Com atualização" else "Sem modificação")
            }

            actionBox.addCell(content)
            document.add(actionBox)
        }
    }

    private fun addActionText(cell: Cell, label: String, value: String?) {
        val bold = PdfFontFactory.createFont("Helvetica-Bold")
        val regular = PdfFontFactory.createFont("Helvetica")
        cell.add(Paragraph(label).setFont(bold).setFontSize(10f).setFontColor(dark).setMarginBottom(1f))
        cell.add(Paragraph(valueOrDash(value)).setFont(regular).setFontSize(10f).setFontColor(dark).setMarginBottom(8f))
    }

    private fun addPhotos(document: Document, inspection: Inspection) {
        val bold = PdfFontFactory.createFont("Helvetica-Bold")
        val actionsWithPhotos = inspection.actions.filter { !it.beforePhotoPath.isNullOrBlank() || !it.afterPhotoPath.isNullOrBlank() }
        if (actionsWithPhotos.isEmpty()) return

        document.add(
            Paragraph("EVIDÊNCIAS FOTOGRÁFICAS")
                .setFont(bold)
                .setFontSize(15f)
                .setFontColor(purple)
                .setMarginTop(12f)
                .setMarginBottom(8f)
        )

        actionsWithPhotos.forEachIndexed { index, action ->
            val box = Table(floatArrayOf(1f, 1f)).useAllAvailableWidth()
            box.setMarginTop(8f)
            box.setMarginBottom(12f)
            box.setBorder(SolidBorder(lightPurple, 1f))

            val before = Cell().setBorder(Border.NO_BORDER).setPadding(12f)
            before.add(Paragraph("AÇÃO ${index + 1} - FOTO ANTES").setFont(bold).setFontSize(12f).setFontColor(purple).setMarginBottom(8f))
            if (!action.beforePhotoPath.isNullOrBlank()) addImage(before, action.beforePhotoPath) else before.add(Paragraph("[Sem foto antes]").setFont(PdfFontFactory.createFont("Helvetica-Oblique")).setFontSize(9f))

            val after = Cell().setBorder(Border.NO_BORDER).setPadding(12f)
            after.add(Paragraph("AÇÃO ${index + 1} - FOTO DEPOIS").setFont(bold).setFontSize(12f).setFontColor(purple).setMarginBottom(8f))
            if (!action.afterPhotoPath.isNullOrBlank()) addImage(after, action.afterPhotoPath) else after.add(Paragraph("[Sem foto depois]").setFont(PdfFontFactory.createFont("Helvetica-Oblique")).setFontSize(9f))

            box.addCell(before)
            box.addCell(after)
            document.add(box)
        }
    }

    private fun addInfo(cell: Cell, label: String, value: String) {
        val bold = PdfFontFactory.createFont("Helvetica-Bold")
        val regular = PdfFontFactory.createFont("Helvetica")
        cell.add(Paragraph(label).setFont(bold).setFontSize(11f).setFontColor(dark).setMarginBottom(0f))
        cell.add(Paragraph(valueOrDash(value)).setFont(regular).setFontSize(11f).setFontColor(dark).setMarginBottom(14f))
    }

    private fun addImage(cell: Cell, imagePath: String) {
        val italic = PdfFontFactory.createFont("Helvetica-Oblique")
        try {
            val file = File(imagePath)
            if (!file.exists()) {
                cell.add(Paragraph("[Imagem não disponível]").setFont(italic).setFontSize(9f))
                return
            }
            val image = Image(ImageDataFactory.create(imagePath))
            image.setAutoScale(true)
            image.setMaxHeight(210f)
            cell.add(image)
        } catch (e: Exception) {
            cell.add(Paragraph("[Imagem não disponível]").setFont(italic).setFontSize(9f))
        }
    }

    private fun valueOrDash(value: String?): String = if (value.isNullOrBlank()) "-" else value

    private fun statusLabel(status: InspectionStatus): String {
        return when (status) {
            InspectionStatus.COMPLETED -> "CONCLUÍDA"
            InspectionStatus.IN_PROGRESS -> "EM ANDAMENTO"
            InspectionStatus.PENDING -> "PENDENTE"
            InspectionStatus.CANCELLED -> "CANCELADA"
        }
    }

    private fun formatMillis(value: Long): String {
        return SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(value))
    }
}
