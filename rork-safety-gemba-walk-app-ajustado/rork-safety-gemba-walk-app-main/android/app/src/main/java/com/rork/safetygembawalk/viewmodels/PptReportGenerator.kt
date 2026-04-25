package com.rork.safetygembawalk.viewmodels

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.rork.safetygembawalk.data.Inspection
import com.rork.safetygembawalk.data.formattedDate
import org.apache.poi.sl.usermodel.PictureData
import org.apache.poi.xslf.usermodel.SlideLayout
import org.apache.poi.xslf.usermodel.XMLSlideShow
import org.apache.poi.xslf.usermodel.XSLFSlide
import org.apache.poi.xslf.usermodel.XSLFTextShape
import org.apache.poi.xslf.usermodel.XSLFTextParagraph
import org.apache.poi.xslf.usermodel.XSLFTextRun
import org.apache.poi.sl.usermodel.ColorStyle
import org.apache.poi.sl.usermodel.PaintStyle
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PptReportGenerator(private val context: Context) {

    fun generateReport(inspections: List<Inspection>): String {
        val fileName = "Safety_Gemba_Walk_${SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())}.pptx"
        val file = File(context.getExternalFilesDir(null), fileName)

        XMLSlideShow().use { ppt ->
            // Create title slide
            createTitleSlide(ppt)

            // Create summary slide
            createSummarySlide(ppt, inspections)

            // Create inspection slides
            inspections.forEachIndexed { index, inspection ->
                createInspectionSlide(ppt, inspection, index + 1)
            }

            // Save the presentation
            FileOutputStream(file).use { out ->
                ppt.write(out)
            }
        }

        return file.absolutePath
    }

    private fun createTitleSlide(ppt: XMLSlideShow) {
        val layout = ppt.slideMasters[0].getLayout(SlideLayout.TITLE)
        val slide = ppt.createSlide(layout)

        val titleShape = slide.getPlaceholder(0) as XSLFTextShape
        titleShape.clearText()
        val titlePara = titleShape.addNewTextParagraph()
        val titleRun = titlePara.addNewTextRun()
        titleRun.setText("SAFETY GEMBA WALK")
        titleRun.setFontSize(44.0)
        titleRun.isBold = true

        val subtitleShape = slide.getPlaceholder(1) as XSLFTextShape
        subtitleShape.clearText()
        val subPara = subtitleShape.addNewTextParagraph()
        val subRun = subPara.addNewTextRun()
        subRun.setText("Relatório de Inspeções de Segurança")
        subRun.setFontSize(24.0)
        
        val subRun2 = subPara.addNewTextRun()
        subRun2.setText("\nAhlstrom")
        subRun2.setFontSize(28.0)
        subRun2.isBold = true
    }

    private fun createSummarySlide(ppt: XMLSlideShow, inspections: List<Inspection>) {
        val layout = ppt.slideMasters[0].getLayout(SlideLayout.TITLE_AND_CONTENT)
        val slide = ppt.createSlide(layout)

        val titleShape = slide.getPlaceholder(0) as XSLFTextShape
        titleShape.clearText()
        val titlePara = titleShape.addNewTextParagraph()
        val titleRun = titlePara.addNewTextRun()
        titleRun.setText("Resumo das Inspeções")
        titleRun.setFontSize(32.0)
        titleRun.isBold = true

        val total = inspections.size
        val immediate = inspections.count { it.isImmediateAction }
        val withWorkOrder = inspections.count { it.hasWorkOrder }
        val completed = inspections.count { it.status.name == "COMPLETED" }

        val contentShape = slide.getPlaceholder(1) as XSLFTextShape
        contentShape.clearText()

        val p1 = contentShape.addNewTextParagraph()
        val r1 = p1.addNewTextRun()
        r1.setText("Total de Inspeções: $total")
        r1.setFontSize(24.0)
        r1.isBold = true

        val p2 = contentShape.addNewTextParagraph()
        val r2 = p2.addNewTextRun()
        r2.setText("Ações Imediatas: $immediate")
        r2.setFontSize(20.0)

        val p3 = contentShape.addNewTextParagraph()
        val r3 = p3.addNewTextRun()
        r3.setText("Ordens de Serviço: $withWorkOrder")
        r3.setFontSize(20.0)

        val p4 = contentShape.addNewTextParagraph()
        val r4 = p4.addNewTextRun()
        r4.setText("Concluídas: $completed")
        r4.setFontSize(20.0)

        val p5 = contentShape.addNewTextParagraph()
        val r5 = p5.addNewTextRun()
        r5.setText("\nAhlstrom - Safety Gemba Walk")
        r5.setFontSize(14.0)
        r5.isBold = true
        
        val p6 = contentShape.addNewTextParagraph()
        val r6 = p6.addNewTextRun()
        r6.setText("Data: ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())}")
        r6.setFontSize(12.0)
    }

    private fun createInspectionSlide(ppt: XMLSlideShow, inspection: Inspection, number: Int) {
        val layout = ppt.slideMasters[0].getLayout(SlideLayout.TITLE_AND_CONTENT)
        val slide = ppt.createSlide(layout)

        val titleShape = slide.getPlaceholder(0) as XSLFTextShape
        titleShape.clearText()
        val titlePara = titleShape.addNewTextParagraph()
        val titleRun = titlePara.addNewTextRun()
        titleRun.setText("Inspeção #$number - ${inspection.formattedDate()}")
        titleRun.setFontSize(28.0)
        titleRun.isBold = true

        val contentShape = slide.getPlaceholder(1) as XSLFTextShape
        contentShape.clearText()

        val p1 = contentShape.addNewTextParagraph()
        val r1 = p1.addNewTextRun()
        r1.setText("Condição Insegura:")
        r1.setFontSize(14.0)
        r1.isBold = true
        
        val r1b = p1.addNewTextRun()
        r1b.setText(" ${inspection.unsafeCondition}")
        r1b.setFontSize(16.0)
        r1b.isBold = true

        val p2 = contentShape.addNewTextParagraph()
        val r2 = p2.addNewTextRun()
        r2.setText("Descrição: ${inspection.description}")
        r2.setFontSize(14.0)

        val p3 = contentShape.addNewTextParagraph()
        val r3 = p3.addNewTextRun()
        r3.setText("Ação Imediata: ${inspection.immediateAction}")
        r3.setFontSize(14.0)

        val p4 = contentShape.addNewTextParagraph()
        val r4 = p4.addNewTextRun()
        r4.setText("Local: ${inspection.location}")
        r4.setFontSize(14.0)
        
        val p4b = contentShape.addNewTextParagraph()
        val r4b = p4b.addNewTextRun()
        r4b.setText("Inspetor: ${inspection.inspectorName}")
        r4b.setFontSize(12.0)

        if (inspection.hasWorkOrder) {
            val p5 = contentShape.addNewTextParagraph()
            val r5 = p5.addNewTextRun()
            r5.setText("O.S. Nº: ${inspection.workOrderNumber ?: "N/A"}")
            r5.setFontSize(14.0)
            r5.isBold = true
        }
        
        if (inspection.isImmediateAction) {
            val p6 = contentShape.addNewTextParagraph()
            val r6 = p6.addNewTextRun()
            r6.setText("✓ Ação Imediata Realizada")
            r6.setFontSize(14.0)
            r6.isBold = true
        }
        
        // Footer with Ahlstrom branding
        val pFooter = contentShape.addNewTextParagraph()
        pFooter.setLineSpacing(2.0)
        val rFooter = pFooter.addNewTextRun()
        rFooter.setText("\nAhlstrom - Safety Gemba Walk")
        rFooter.setFontSize(10.0)
    }
}
