package com.rork.safetygembawalk.viewmodels

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.kernel.colors.DeviceRgb
import com.itextpdf.kernel.events.Event
import com.itextpdf.kernel.events.IEventHandler
import com.itextpdf.kernel.events.PdfDocumentEvent
import com.itextpdf.kernel.font.PdfFontFactory
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfPage
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.kernel.pdf.canvas.PdfCanvas
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

    private val purple = DeviceRgb(107, 35, 120)
    private val purpleDark = DeviceRgb(72, 18, 88)
    private val purpleSoft = DeviceRgb(246, 238, 249)
    private val orange = DeviceRgb(242, 140, 40)
    private val green = DeviceRgb(22, 138, 74)
    private val red = DeviceRgb(220, 38, 38)
    private val navy = DeviceRgb(9, 31, 65)
    private val dark = DeviceRgb(30, 41, 59)
    private val muted = DeviceRgb(100, 116, 139)
    private val white = DeviceRgb(255, 255, 255)
    private val line = DeviceRgb(220, 205, 228)
    private val cardBg = DeviceRgb(255, 255, 255)
    private val softCard = DeviceRgb(251, 248, 253)

    fun generateReport(inspections: List<Inspection>): String {
        val fileName =
            "Safety_Gemba_Walk_${SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())}.pdf"

        val file = File(context.getExternalFilesDir(null), fileName)

        PdfWriter(file.absolutePath).use { writer ->
            PdfDocument(writer).use { pdfDoc ->
                pdfDoc.addEventHandler(PdfDocumentEvent.START_PAGE, AhlstromBackgroundHandler())

                Document(pdfDoc, PageSize.A4).use { document ->
                    document.setMargins(22f, 22f, 22f, 22f)

                    inspections.forEachIndexed { index, inspection ->
                        if (index > 0) {
                            document.add(AreaBreak())
                        }

                        addOneInspectionPage(document, inspection, index + 1)
                    }
                }
            }
        }

        return file.absolutePath
    }

    private inner class AhlstromBackgroundHandler : IEventHandler {
        override fun handleEvent(event: Event) {
            val docEvent = event as PdfDocumentEvent
            val page: PdfPage = docEvent.page
            val pageSize = page.pageSize
            val canvas = PdfCanvas(page.newContentStreamBefore(), page.resources, docEvent.document)

            canvas.saveState()

            canvas.setFillColor(purpleSoft)
            canvas.rectangle(
                0.0,
                0.0,
                pageSize.width.toDouble(),
                pageSize.height.toDouble()
            )
            canvas.fill()

            canvas.setFillColor(purple)
            canvas.rectangle(
                0.0,
                0.0,
                18.0,
                pageSize.height.toDouble()
            )
            canvas.fill()

            canvas.setFillColor(purpleDark)
            canvas.rectangle(
                pageSize.width.toDouble() - 42.0,
                0.0,
                42.0,
                pageSize.height.toDouble()
            )
            canvas.fill()

            canvas.setFillColor(DeviceRgb(255, 255, 255))
            canvas.rectangle(
                22.0,
                22.0,
                (pageSize.width - 86).toDouble(),
                (pageSize.height - 44).toDouble()
            )
            canvas.fill()

            canvas.setStrokeColor(DeviceRgb(235, 222, 240))
            canvas.setLineWidth(1f)
            canvas.rectangle(
                22.0,
                22.0,
                (pageSize.width - 86).toDouble(),
                (pageSize.height - 44).toDouble()
            )
            canvas.stroke()

            canvas.restoreState()
        }
    }

    private fun addOneInspectionPage(
        document: Document,
        inspection: Inspection,
        number: Int
    ) {
        addHeader(document)
        addInspectionHero(document, inspection, number)
        addCompactActions(document, inspection)
        addPhotoEvidence(document, inspection)
        addFooter(document)
    }

    private fun addHeader(document: Document) {
        val bold = PdfFontFactory.createFont("Helvetica-Bold")
        val regular = PdfFontFactory.createFont("Helvetica")

        val table = Table(floatArrayOf(0.9f, 4.6f)).useAllAvailableWidth()
        table.setMarginBottom(10f)

        val logo = Cell()
            .setBorder(Border.NO_BORDER)
            .setBackgroundColor(purple)
            .setPadding(8f)
            .setTextAlignment(TextAlignment.CENTER)

        logo.add(
            Paragraph("A")
                .setFont(bold)
                .setFontSize(28f)
                .setFontColor(white)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(0f)
        )

        logo.add(
            Paragraph("AHLSTROM")
                .setFont(bold)
                .setFontSize(6.5f)
                .setFontColor(white)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(0f)
        )

        val title = Cell()
            .setBorder(Border.NO_BORDER)
            .setBackgroundColor(purple)
            .setPaddingTop(13f)
            .setPaddingLeft(10f)
            .setPaddingBottom(10f)

        title.add(
            Paragraph("SAFETY GEMBA WALK")
                .setFont(bold)
                .setFontSize(25f)
                .setFontColor(white)
                .setMarginBottom(1f)
        )

        title.add(
            Paragraph("Safety is my first job!!! · Relatório executivo de inspeção")
                .setFont(regular)
                .setFontSize(10.5f)
                .setFontColor(DeviceRgb(246, 222, 250))
        )

        table.addCell(logo)
        table.addCell(title)
        document.add(table)
    }

    private fun addInspectionHero(
        document: Document,
        inspection: Inspection,
        number: Int
    ) {
        val bold = PdfFontFactory.createFont("Helvetica-Bold")
        val regular = PdfFontFactory.createFont("Helvetica")

        val statusText = statusText(inspection.status)
        val statusColor = statusColor(inspection.status)

        val top = Table(floatArrayOf(3.5f, 1.4f)).useAllAvailableWidth()
        top.setMarginBottom(0f)

        top.addCell(
            Cell()
                .setBorder(Border.NO_BORDER)
                .setBackgroundColor(purpleDark)
                .setPadding(9f)
                .add(
                    Paragraph("INSPEÇÃO #$number")
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

        addMiniInfo(grid, "Criado por", valueOrDash(inspection.createdByName))
        addMiniInfo(grid, "Atualizado por", valueOrDash(inspection.lastUpdatedByName))
        addMiniInfo(grid, "Ações", inspection.actions.size.toString())
        addMiniInfo(grid, "OS abertas", inspection.actions.count { it.hasWorkOrder }.toString())

        document.add(grid)

        val summary = Table(floatArrayOf(1.25f, 2.35f)).useAllAvailableWidth()
        summary.setMarginBottom(8f)

        val indicators = Cell()
            .setBorder(SolidBorder(line, 1f))
            .setBackgroundColor(softCard)
            .setPadding(9f)

        indicators.add(
            Paragraph("INDICADORES")
                .setFont(bold)
                .setFontSize(11.5f)
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
            .setPadding(9f)

        resume.add(
            Paragraph("RESUMO DA INSPEÇÃO")
                .setFont(bold)
                .setFontSize(11.5f)
                .setFontColor(purple)
                .setMarginBottom(5f)
        )

        resume.add(
            Paragraph(valueOrDash(inspection.title))
                .setFont(bold)
                .setFontSize(10.5f)
                .setFontColor(dark)
                .setMarginBottom(5f)
        )

        resume.add(
            Paragraph("Relatório consolidado da inspeção com ações registradas, evidências fotográficas, controle de OS, rastreabilidade de atualização e histórico de ações herdadas.")
                .setFont(regular)
                .setFontSize(9.2f)
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
                .setFontSize(7.5f)
                .setFontColor(purple)
                .setMarginBottom(1f)
        )

        cell.add(
            Paragraph(valueOrDash(value))
                .setFont(regular)
                .setFontSize(8.5f)
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
                        .setFontSize(8.6f)
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
                        .setFontSize(9.3f)
                        .setFontColor(purple)
                )
        )

        cell.add(table)
    }

    private fun addCompactActions(
        document: Document,
        inspection: Inspection
    ) {
        val bold = PdfFontFactory.createFont("Helvetica-Bold")
        val regular = PdfFontFactory.createFont("Helvetica")

        document.add(
            Paragraph("AÇÕES REGISTRADAS")
                .setFont(bold)
                .setFontSize(12.5f)
                .setFontColor(purple)
                .setMarginTop(2f)
                .setMarginBottom(5f)
        )

        if (inspection.actions.isEmpty()) {
            document.add(
                Paragraph("Nenhuma ação registrada.")
                    .setFont(regular)
                    .setFontSize(8.8f)
                    .setFontColor(dark)
            )
            return
        }

        val maxActionsOnPage = 3

        inspection.actions.take(maxActionsOnPage).forEachIndexed { index, action ->
            val box = Table(floatArrayOf(1f)).useAllAvailableWidth()
            box.setBorder(SolidBorder(line, 1f))
            box.setMarginBottom(5f)

            box.addCell(
                Cell()
                    .setBorder(Border.NO_BORDER)
                    .setBackgroundColor(statusColor(action.status))
                    .setPadding(6f)
                    .add(
                        Paragraph("AÇÃO ${index + 1} - ${statusLabel(action.status)}")
                            .setFont(bold)
                            .setFontSize(9.5f)
                            .setFontColor(white)
                    )
            )

            val content = Cell()
                .setBorder(Border.NO_BORDER)
                .setBackgroundColor(lightCellBg())
                .setPadding(7f)

            content.add(lineText("Risco identificado", action.unsafeCondition))
            content.add(lineText("Descrição", action.description))
            content.add(lineText("Ação imediata", action.immediateAction))

            val osText =
                if (action.hasWorkOrder) {
                    "${action.workOrderNumber ?: "N/A"} · abertura: ${action.formattedWorkOrderOpenDate()}"
                } else {
                    "Sem OS"
                }

            content.add(lineText("Ordem de Serviço", osText))

            val trace =
                "Criado por: ${valueOrDash(action.createdByName)} · Atualizado por: ${valueOrDash(action.lastUpdatedByName)}"

            content.add(
                Paragraph(trace)
                    .setFont(regular)
                    .setFontSize(7.8f)
                    .setFontColor(muted)
                    .setMarginTop(2f)
            )

            if (action.isInherited) {
                content.add(
                    Paragraph("Ação herdada · carregada ${action.carriedCount} vez(es) · ${if (action.hasChangesToday) "com atualização" else "sem modificação"}")
                        .setFont(bold)
                        .setFontSize(7.8f)
                        .setFontColor(purple)
                        .setMarginTop(2f)
                )
            }

            box.addCell(content)
            document.add(box)
        }

        if (inspection.actions.size > maxActionsOnPage) {
            document.add(
                Paragraph("Observação: esta inspeção possui ${inspection.actions.size} ações. As primeiras $maxActionsOnPage foram exibidas nesta folha executiva.")
                    .setFont(regular)
                    .setFontSize(8f)
                    .setFontColor(muted)
                    .setMarginTop(1f)
            )
        }
    }

    private fun lineText(label: String, value: String?): Paragraph {
        val bold = PdfFontFactory.createFont("Helvetica-Bold")
        val regular = PdfFontFactory.createFont("Helvetica")

        return Paragraph()
            .add(
                com.itextpdf.layout.element.Text("$label: ")
                    .setFont(bold)
                    .setFontSize(8.4f)
                    .setFontColor(purple)
            )
            .add(
                com.itextpdf.layout.element.Text(valueOrDash(value))
                    .setFont(regular)
                    .setFontSize(8.4f)
                    .setFontColor(dark)
            )
            .setMarginBottom(2f)
    }

    private fun lightCellBg(): DeviceRgb {
        return DeviceRgb(252, 249, 253)
    }

    private fun addPhotoEvidence(
        document: Document,
        inspection: Inspection
    ) {
        val bold = PdfFontFactory.createFont("Helvetica-Bold")

        val firstActionWithPhoto =
            inspection.actions.firstOrNull {
                !it.beforePhotoPath.isNullOrBlank() || !it.afterPhotoPath.isNullOrBlank()
            } ?: return

        document.add(
            Paragraph("EVIDÊNCIAS FOTOGRÁFICAS")
                .setFont(bold)
                .setFontSize(12.5f)
                .setFontColor(purple)
                .setMarginTop(4f)
                .setMarginBottom(5f)
        )

        val photos = Table(floatArrayOf(1f, 1f)).useAllAvailableWidth()
        photos.setBorder(SolidBorder(line, 1f))
        photos.setBackgroundColor(cardBg)

        val before = Cell()
            .setBorder(Border.NO_BORDER)
            .setPadding(7f)

        before.add(
            Paragraph("FOTO ANTES")
                .setFont(bold)
                .setFontSize(8.5f)
                .setFontColor(purple)
                .setMarginBottom(4f)
        )

        if (!firstActionWithPhoto.beforePhotoPath.isNullOrBlank()) {
            addImage(before, firstActionWithPhoto.beforePhotoPath, 145f)
        } else {
            addNoImage(before)
        }

        val after = Cell()
            .setBorder(Border.NO_BORDER)
            .setPadding(7f)

        after.add(
            Paragraph("FOTO DEPOIS")
                .setFont(bold)
                .setFontSize(8.5f)
                .setFontColor(purple)
                .setMarginBottom(4f)
        )

        if (!firstActionWithPhoto.afterPhotoPath.isNullOrBlank()) {
            addImage(after, firstActionWithPhoto.afterPhotoPath, 145f)
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
            val imageData =
                if (bytes != null) {
                    ImageDataFactory.create(bytes)
                } else {
                    ImageDataFactory.create(imagePath)
                }

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
            val orientation =
                exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )

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
                    Bitmap.createBitmap(
                        original,
                        0,
                        0,
                        original.width,
                        original.height,
                        matrix,
                        true
                    )
                } else {
                    original
                }

            val output = ByteArrayOutputStream()
            corrected.compress(Bitmap.CompressFormat.JPEG, 82, output)

            if (corrected != original) {
                corrected.recycle()
            }

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
                .setFontSize(7.5f)
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
}
