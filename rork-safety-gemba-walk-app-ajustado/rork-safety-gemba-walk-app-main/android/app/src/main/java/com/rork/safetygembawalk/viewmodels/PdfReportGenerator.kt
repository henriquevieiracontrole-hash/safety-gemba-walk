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
import com.itextpdf.layout.properties.HorizontalAlignment
import com.itextpdf.layout.properties.TextAlignment
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
    private val green = DeviceRgb(34, 140, 80)
    private val yellow = DeviceRgb(234, 179, 8)
    private val gray = DeviceRgb(248, 248, 248)
    private val borderGray = DeviceRgb(205, 205, 205)
    private val dark = DeviceRgb(30, 41, 59)
    private val white = DeviceRgb(255, 255, 255)

    fun generateReport(inspections: List<Inspection>): String {
        val fileName = "Safety_Gemba_Walk_${SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())}.pdf"
        val file = File(context.getExternalFilesDir(null), fileName)

        PdfWriter(file.absolutePath).use { writer ->
            PdfDocument(writer).use { pdfDoc ->
                Document(pdfDoc, PageSize.A4).use { document ->
                    document.setMargins(28f, 34f, 28f, 34f)

                    inspections.forEachIndexed { index, inspection ->
                        if (index > 0) document.add(AreaBreak())
                        addBackground(document)
                        addHeader(document, inspection, index + 1)
                        addInfoBlock(document, inspection)
                        addRiskActionDescription(document, inspection)
                        addPhotos(document, inspection)
                        addFooter(document)
                    }
                }
            }
        }

        return file.absolutePath
    }

    private fun addBackground(document: Document) {
        try {
            val resId = context.resources.getIdentifier(
                "report_pdf_background",
                "drawable",
                context.packageName
            )

            if (resId == 0) return

            val bytes = context.resources.openRawResource(resId).use { it.readBytes() }

            val pdfDoc = document.pdfDocument
            if (pdfDoc.numberOfPages == 0) {
                pdfDoc.addNewPage(PageSize.A4)
            }

            val pageNumber = pdfDoc.numberOfPages
            val pageSize = pdfDoc.getPage(pageNumber).pageSize

            val bg = Image(ImageDataFactory.create(bytes))
                .scaleAbsolute(pageSize.width, pageSize.height)
                .setFixedPosition(pageNumber, 0f, 0f)

            document.add(bg)
        } catch (_: Exception) {
        }
    }

    private fun addHeader(document: Document, inspection: Inspection, number: Int) {
        val bold = PdfFontFactory.createFont("Helvetica-Bold")

        document.add(
            Paragraph("SAFETY GEMBA WALK")
                .setFont(bold)
                .setFontSize(25f)
                .setFontColor(navy)
                .setMarginTop(0f)
                .setMarginBottom(0f)
        )

        document.add(
            Paragraph("Safety is my first job!!")
                .setFont(bold)
                .setFontSize(13f)
                .setFontColor(red)
                .setMarginTop(0f)
                .setMarginBottom(8f)
        )

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

        val header = Table(floatArrayOf(2.5f, 1f)).useAllAvailableWidth()
        header.setMarginBottom(8f)

        header.addCell(
            Cell()
                .setBorder(Border.NO_BORDER)
                .add(
                    Paragraph("Relatório de inspeção #$number")
                        .setFont(bold)
                        .setFontSize(12f)
                        .setFontColor(purple)
                )
        )

        header.addCell(
            Cell()
                .setBorder(SolidBorder(statusColor, 1.5f))
                .setPadding(5f)
                .add(
                    Paragraph(statusText)
                        .setFont(bold)
                        .setFontSize(11f)
                        .setFontColor(statusColor)
                        .setTextAlignment(TextAlignment.CENTER)
                )
        )

        document.add(header)
    }

    private fun addInfoBlock(document: Document, inspection: Inspection) {
        val table = Table(floatArrayOf(1f, 1f, 1f)).useAllAvailableWidth()
        table.setMarginBottom(8f)

        addInfoCell(table, "Data da inspeção", inspection.formattedDate())
        addInfoCell(table, "Categoria", inspection.category.ifBlank { "Segurança" })
        addInfoCell(table, "Local", inspection.location.ifBlank { "-" })
        addInfoCell(table, "Inspetor", inspection.inspectorName.ifBlank { "-" })

        if (inspection.hasWorkOrder) {
            addInfoCell(table, "Ordem de Serviço", inspection.workOrderNumber ?: "N/A")
            addInfoCell(table, "Abertura O.S.", inspection.formattedWorkOrderOpenDate())
        } else {
            addInfoCell(table, "Ordem de Serviço", "Não aplicável")
            addInfoCell(table, "Abertura O.S.", "Não aplicável")
        }

        document.add(table)
    }

    private fun addInfoCell(table: Table, label: String, value: String) {
        val bold = PdfFontFactory.createFont("Helvetica-Bold")
        val regular = PdfFontFactory.createFont("Helvetica")

        val cell = Cell()
            .setBackgroundColor(gray)
            .setBorder(SolidBorder(borderGray, 0.6f))
            .setPadding(6f)

        cell.add(
            Paragraph(label)
                .setFont(bold)
                .setFontSize(8f)
                .setFontColor(purple)
                .setMarginBottom(1f)
        )

        cell.add(
            Paragraph(value)
                .setFont(regular)
                .setFontSize(9f)
                .setFontColor(dark)
        )

        table.addCell(cell)
    }

    private fun addRiskActionDescription(document: Document, inspection: Inspection) {
        val top = Table(floatArrayOf(1f, 1f)).useAllAvailableWidth()
        top.setMarginBottom(0f)

        top.addCell(
            executiveBox(
                title = "RISCO IDENTIFICADO",
                text = inspection.unsafeCondition.ifBlank { "-" },
                titleColor = red,
                height = 72f
            )
        )

        top.addCell(
            executiveBox(
                title = "AÇÃO IMEDIATA",
                text = inspection.immediateAction.ifBlank { "-" },
                titleColor = orange,
                height = 72f
            )
        )

        document.add(top)

        val description = Cell()
            .setBorder(SolidBorder(borderGray, 0.8f))
            .setPadding(10f)
            .setHeight(96f)

        val bold = PdfFontFactory.createFont("Helvetica-Bold")
        val regular = PdfFontFactory.createFont("Helvetica")

        description.add(
            Paragraph("DESCRIÇÃO DETALHADA")
                .setFont(bold)
                .setFontSize(13f)
                .setFontColor(dark)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(8f)
        )

        description.add(
            Paragraph(inspection.description.ifBlank { "-" })
                .setFont(regular)
                .setFontSize(11f)
                .setFontColor(dark)
                .setMultipliedLeading(1.05f)
        )

        val table = Table(floatArrayOf(1f)).useAllAvailableWidth()
        table.setMarginBottom(8f)
        table.addCell(description)

        document.add(table)
    }

    private fun executiveBox(
        title: String,
        text: String,
        titleColor: DeviceRgb,
        height: Float
    ): Cell {
        val bold = PdfFontFactory.createFont("Helvetica-Bold")
        val regular = PdfFontFactory.createFont("Helvetica")

        val cell = Cell()
            .setBorder(SolidBorder(borderGray, 0.8f))
            .setPadding(10f)
            .setHeight(height)

        cell.add(
            Paragraph(title)
                .setFont(bold)
                .setFontSize(11f)
                .setFontColor(titleColor)
                .setMarginBottom(6f)
        )

        cell.add(
            Paragraph(text)
                .setFont(regular)
                .setFontSize(10f)
                .setFontColor(dark)
                .setMultipliedLeading(1.05f)
        )

        return cell
    }

    private fun addPhotos(document: Document, inspection: Inspection) {
        val bold = PdfFontFactory.createFont("Helvetica-Bold")

        val table = Table(floatArrayOf(1f, 1f)).useAllAvailableWidth()
        table.setMarginTop(2f)

        val before = Cell()
            .setBorder(SolidBorder(borderGray, 0.8f))
            .setPadding(8f)
            .setHeight(250f)

        before.add(
            Paragraph("FOTO ANTES")
                .setFont(bold)
                .setFontSize(11f)
                .setFontColor(purple)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(6f)
        )

        inspection.beforePhotoPath?.let {
            addImage(before, it)
        } ?: before.add(emptyImageText())

        val after = Cell()
            .setBorder(SolidBorder(borderGray, 0.8f))
            .setPadding(8f)
            .setHeight(250f)

        after.add(
            Paragraph("FOTO DEPOIS")
                .setFont(bold)
                .setFontSize(11f)
                .setFontColor(purple)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(6f)
        )

        inspection.afterPhotoPath?.let {
            addImage(after, it)
        } ?: after.add(emptyImageText())

        table.addCell(before)
        table.addCell(after)

        document.add(table)
    }

    private fun addImage(cell: Cell, imagePath: String) {
        val italic = PdfFontFactory.createFont("Helvetica-Oblique")

        try {
            val file = File(imagePath)

            if (!file.exists()) {
                cell.add(emptyImageText())
                return
            }

            val image = Image(ImageDataFactory.create(imagePath))
            val width = image.imageWidth
            val height = image.imageHeight

            if (height > width) {
                image.scaleToFit(205f, 205f)
            } else {
                image.scaleToFit(235f, 178f)
            }

            image.setHorizontalAlignment(HorizontalAlignment.CENTER)
            cell.add(image)

        } catch (e: Exception) {
            cell.add(
                Paragraph("[Imagem não disponível]")
                    .setFont(italic)
                    .setFontSize(9f)
                    .setTextAlignment(TextAlignment.CENTER)
            )
        }
    }

    private fun addFooter(document: Document) {
        val regular = PdfFontFactory.createFont("Helvetica")

        document.add(
            Paragraph("Ahlstrom • Safety Gemba Walk Report")
                .setFont(regular)
                .setFontSize(8f)
                .setFontColor(DeviceRgb(140, 140, 140))
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(6f)
        )
    }

    private fun emptyImageText(): Paragraph {
        val italic = PdfFontFactory.createFont("Helvetica-Oblique")

        return Paragraph("[Imagem não disponível]")
            .setFont(italic)
            .setFontSize(9f)
            .setFontColor(dark)
            .setTextAlignment(TextAlignment.CENTER)
    }
}
