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

        val logo = Cell()
            .setBorder(Border.NO_BORDER)
            .setPadding(10f)

        val title = Cell()
            .setBorder(Border.NO_BORDER)
            .setPaddingTop(14f)
            .setPaddingLeft(6f)

        title.add(
            Paragraph("SAFETY GEMBA WALK")
                .setFont(bold)
                .setFontSize(30f)
                .setFontColor(navy)
        )

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

        val titleCell = Cell()
            .setBorder(Border.NO_BORDER)
            .setPadding(0f)

        titleCell.add(
            Paragraph("  INSPEÇÃO #$number")
                .setFont(bold)
                .setFontSize(15f)
                .setFontColor(white)
                .setBackgroundColor(purple)
                .setPadding(10f)
                .setWidth(UnitValue.createPercentValue(35f))
        )

        main.addCell(titleCell)

        val body = Cell()
            .setBorder(Border.NO_BORDER)
            .setPadding(16f)

        val details = Table(floatArrayOf(1.1f, 2f)).useAllAvailableWidth()

        val left = Cell()
            .setBorder(Border.NO_BORDER)
            .setPaddingRight(14f)
            .setBorderRight(SolidBorder(lightPurple, 1f))

        addInfo(left, "Data da inspeção", inspection.formattedDate())
        addInfo(left, "Categoria", inspection.category)
        addInfo(left, "Local", inspection.location)
        addInfo(left, "Inspetor", inspection.inspectorName)

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

        if (inspection.hasWorkOrder) {
            addInfo(left, "Ordem de Serviço", inspection.workOrderNumber ?: "N/A")
            addInfo(left, "Data abertura O.S.", inspection.formattedWorkOrderOpenDate())
        }

        val right = Cell()
            .setBorder(Border.NO_BORDER)
            .setPaddingLeft(18f)

        right.add(
            Paragraph("RISCO IDENTIFICADO")
                .setFont(bold)
                .setFontSize(15f)
                .setFontColor(red)
        )

        right.add(
            Paragraph(valueOrDash("${inspection.unsafeCondition} ${inspection.description}".trim()))
                .setFont(regular)
                .setFontSize(11f)
                .setFontColor(dark)
                .setMarginBottom(12f)
        )

        right.add(
            Paragraph("")
                .setBorderBottom(SolidBorder(lightPurple, 1f))
                .setMarginBottom(10f)
        )

        right.add(
            Paragraph("AÇÃO IMEDIATA")
                .setFont(bold)
                .setFontSize(15f)
                .setFontColor(orange)
        )

        right.add(
            Paragraph(valueOrDash(inspection.immediateAction))
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

        addPhotos(document, inspection)
    }

    private fun addPhotos(document: Document, inspection: Inspection) {
        val bold = PdfFontFactory.createFont("Helvetica-Bold")

        val box = Table(floatArrayOf(1f, 1f)).useAllAvailableWidth()
        box.setMarginTop(14f)
        box.setBorder(SolidBorder(lightPurple, 1f))

        val before = Cell()
            .setBorder(Border.NO_BORDER)
            .setPadding(12f)

        before.add(
            Paragraph("FOTO ANTES")
                .setFont(bold)
                .setFontSize(13f)
                .setFontColor(purple)
                .setMarginBottom(8f)
        )

        inspection.beforePhotoPath?.let { addImage(before, it) }

        val after = Cell()
            .setBorder(Border.NO_BORDER)
            .setPadding(12f)

        after.add(
            Paragraph("FOTO DEPOIS")
                .setFont(bold)
                .setFontSize(13f)
                .setFontColor(purple)
                .setMarginBottom(8f)
        )

        inspection.afterPhotoPath?.let { addImage(after, it) }

        box.addCell(before)
        box.addCell(after)

        document.add(box)
    }

    private fun addInfo(cell: Cell, label: String, value: String) {
        val bold = PdfFontFactory.createFont("Helvetica-Bold")
        val regular = PdfFontFactory.createFont("Helvetica")

        cell.add(
            Paragraph(label)
                .setFont(bold)
                .setFontSize(11f)
                .setFontColor(dark)
                .setMarginBottom(0f)
        )

        cell.add(
            Paragraph(valueOrDash(value))
                .setFont(regular)
                .setFontSize(11f)
                .setFontColor(dark)
                .setMarginBottom(14f)
        )
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

            val width = image.imageWidth
            val height = image.imageHeight

            if (height > width) {
                image.scaleToFit(220f, 300f)
            } else {
                image.scaleToFit(260f, 200f)
            }

            cell.add(image)

        } catch (e: Exception) {
            cell.add(Paragraph("[Imagem não disponível]").setFont(italic).setFontSize(9f))
        }
    }

    private fun valueOrDash(value: String?): String {
        return if (value.isNullOrBlank()) "-" else value
    }
}
