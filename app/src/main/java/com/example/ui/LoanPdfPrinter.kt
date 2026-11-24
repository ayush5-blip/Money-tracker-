package com.example.ui

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import com.example.model.LoanCalculatorEngine
import com.example.model.LoanDisbursementComputed
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToLong

object LoanPdfPrinter {

    fun printOrSavePdf(context: Context, sheet: LoanDisbursementComputed, currencyCode: String) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
        val customerTitle = sheet.input.customerName.trim().ifEmpty { "Customer" }
        val jobName = "Disbursement_Sheet_${customerTitle.replace(" ", "_")}"

        val adapter = object : PrintDocumentAdapter() {
            override fun onLayout(
                oldAttributes: PrintAttributes?,
                newAttributes: PrintAttributes?,
                cancellationSignal: CancellationSignal?,
                callback: LayoutResultCallback?,
                extras: Bundle?
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback?.onLayoutCancelled()
                    return
                }
                val info = PrintDocumentInfo.Builder("$jobName.pdf")
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(1)
                    .build()
                callback?.onLayoutFinished(info, true)
            }

            override fun onWrite(
                pages: Array<out PageRange>?,
                destination: ParcelFileDescriptor?,
                cancellationSignal: CancellationSignal?,
                callback: WriteResultCallback?
            ) {
                if (destination == null) {
                    callback?.onWriteFailed("Missing output descriptor")
                    return
                }
                val pdf = PdfDocument()
                val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
                val page = pdf.startPage(pageInfo)
                drawSheetOnCanvas(page.canvas, sheet, currencyCode)
                pdf.finishPage(page)

                try {
                    FileOutputStream(destination.fileDescriptor).use { out ->
                        pdf.writeTo(out)
                    }
                    callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                } catch (e: Exception) {
                    callback?.onWriteFailed(e.message)
                } finally {
                    pdf.close()
                }
            }
        }

        printManager.print(
            jobName,
            adapter,
            PrintAttributes.Builder()
                .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                .build()
        )
    }

    private fun drawSheetOnCanvas(
        canvas: android.graphics.Canvas,
        sheet: LoanDisbursementComputed,
        currencyCode: String
    ) {
        val margin = 36f
        val pageWidth = 595f
        val contentWidth = pageWidth - margin * 2f

        val bgHeaderPaint = Paint().apply { color = Color.rgb(18, 22, 30) }
        val goldPaint = Paint().apply {
            color = Color.rgb(184, 134, 11)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 14f
        }
        val headerTitlePaint = Paint().apply {
            color = Color.rgb(245, 196, 81)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 16f
        }
        val headerSubPaint = Paint().apply {
            color = Color.WHITE
            textSize = 11f
        }
        val sectionBgPaint = Paint().apply { color = Color.rgb(248, 240, 214) }
        val altRowPaint = Paint().apply { color = Color.rgb(247, 249, 252) }
        val borderPaint = Paint().apply {
            color = Color.rgb(203, 213, 225)
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        val labelPaint = Paint().apply {
            color = Color.rgb(15, 23, 42)
            textSize = 10.5f
        }
        val labelBoldPaint = Paint().apply {
            color = Color.rgb(15, 23, 42)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 10.5f
        }
        val valuePaint = Paint().apply {
            color = Color.rgb(15, 23, 42)
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textSize = 10.5f
            textAlign = Paint.Align.RIGHT
        }
        val notePaint = Paint().apply {
            color = Color.rgb(30, 41, 59)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            textSize = 9.5f
        }

        var y = 32f
        val customer = sheet.input.customerName.trim().ifEmpty { "Customer" }
        val dateStr = formatDateOnly(sheet.input.disbursementDateMillis)

        // Header banner
        canvas.drawRect(margin, y, margin + contentWidth, y + 50f, bgHeaderPaint)
        canvas.drawText("CUSTOMER: ${customer.uppercase(Locale.getDefault())}", margin + 12f, y + 22f, headerTitlePaint)
        canvas.drawText("Scheme: ${sheet.input.scheme.fullName}  |  Disbursement: $dateStr", margin + 12f, y + 39f, headerSubPaint)
        y += 58f

        fun fmt(v: Double): String = formatMoney(v, currencyCode)

        val rows: List<Triple<String, String, Int>> = listOf(
            // type: 0 normal, 1 bold highlight, 2 section header
            Triple("Loan Amount", fmt(sheet.input.loanAmount), 0),
            Triple("Loan Insurance (SK)", fmt(sheet.input.loanInsuranceSk), 0),
            Triple("Total Loan", fmt(sheet.totalLoan), 1),
            Triple("Loan Amount", fmt(sheet.input.loanAmount), 0),
            Triple("Bank Charges", fmt(sheet.bankCharges), 0),
            Triple("Disbursment Amount", fmt(sheet.disbursementAmount), 1),
            Triple("NOC Hold", fmt(sheet.nocHold), 0),
            Triple("Forclose", fmt(sheet.forcloseAmount), 0),
            Triple("Inhand", fmt(sheet.inHand), 1),
            Triple("RTO Cash", fmt(sheet.input.rtoCash), 0),
            Triple("PAYMENT TERM", "", 2),
            Triple("First Payment by Bank", fmt(sheet.firstPaymentByBank), 0),
            Triple("NOC Hold Payment", fmt(sheet.nocHold), 0),
            Triple("Forclose", fmt(sheet.forcloseAmount), 0),
            Triple("In Hand", fmt(sheet.inHand), 1),
            Triple("EMI", fmt(sheet.emiReducing), 1),
            Triple("Month", "${sheet.input.tenureMonths}", 0),
            Triple("Reducing Rate", LoanCalculatorEngine.formatPercent(sheet.input.reducingRateIt), 0),
            Triple("Flat Rate", LoanCalculatorEngine.formatPercent(sheet.flatRate), 0),
            Triple("Per Lac Per Year", fmt(sheet.perLacPerYear), 0),
            Triple("Per Month", fmt(sheet.perMonth), 0),
            Triple("Per Month Interest", fmt(sheet.perMonthInterest), 0),
            Triple("EMI Start Date", formatDateOnly(sheet.emiStartDateMillis), 0),
            Triple("RC Collect", sheet.input.rcCollect.label, 0),
            Triple("Insurance PUC", sheet.input.insurancePuc.label, 0),
            Triple("Lightbill", sheet.input.lightBill.label, 0),
            Triple("Cheques", sheet.input.cheques, 0)
        )

        val rowH = 22.5f
        rows.forEachIndexed { idx, (label, value, kind) ->
            if (kind == 2) {
                canvas.drawRect(margin, y, margin + contentWidth, y + rowH, sectionBgPaint)
                canvas.drawRect(margin, y, margin + contentWidth, y + rowH, borderPaint)
                canvas.drawText(label, margin + 10f, y + 15f, goldPaint)
            } else {
                if (idx % 2 == 1) {
                    canvas.drawRect(margin, y, margin + contentWidth, y + rowH, altRowPaint)
                }
                canvas.drawRect(margin, y, margin + contentWidth, y + rowH, borderPaint)
                canvas.drawText(label, margin + 10f, y + 15f, if (kind == 1) labelBoldPaint else labelPaint)
                canvas.drawText(value, margin + contentWidth - 10f, y + 15f, valuePaint)
            }
            y += rowH
        }

        y += 16f
        val noteLine1 = "Dear Sir, your file will go for disbursement with above mentioned details..."
        val noteLine2 = "OK For Disbursement. AGAR RTO CHALLAN HOGA KO CUSTOMER"
        canvas.drawText(noteLine1, margin, y, notePaint)
        canvas.drawText(noteLine2, margin, y + 14f, labelBoldPaint)
    }

    private fun formatMoney(amount: Double, currencyCode: String): String {
        val cents = (amount * 100.0).roundToLong()
        val absVal = kotlin.math.abs(cents) / 100.0
        val prefix = if (cents < 0) "-" else ""
        return try {
            val nf = NumberFormat.getCurrencyInstance(Locale.getDefault())
            nf.currency = java.util.Currency.getInstance(currencyCode.uppercase(Locale.ROOT))
            prefix + nf.format(absVal)
        } catch (_: Exception) {
            "$prefix$currencyCode ${String.format(Locale.US, "%,.2f", absVal)}"
        }
    }

    private fun formatDateOnly(millis: Long): String {
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        return sdf.format(Date(millis))
    }
}
