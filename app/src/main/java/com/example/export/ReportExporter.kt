package com.example.export

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.data.model.Company
import com.example.data.model.CustomerDepositSummary
import com.example.data.model.DailyCollectionRecord
import com.example.data.model.PartnerDeposit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ReportExporter {

    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

    suspend fun exportCollectionsToCsv(
        context: Context,
        company: Company,
        records: List<DailyCollectionRecord>,
        filterDescription: String
    ): File = withContext(Dispatchers.IO) {
        val fileName = "Collection_Report_${System.currentTimeMillis()}.csv"
        val file = File(context.cacheDir, fileName)
        val writer = file.bufferedWriter()

        writer.write("Company,${sanitize(company.name)}\n")
        writer.write("Address,${sanitize(company.address)}\n")
        writer.write("Filter,${sanitize(filterDescription)}\n")
        writer.write("Generated On,${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}\n")
        writer.write("\n")
        writer.write("Receipt No,Date,Time,Customer Name,CIF No,Book No,Deposit Amount,Cumulative Total,Executive Name,Manager Marker\n")

        for (rec in records) {
            val line = listOf(
                sanitize(rec.receiptNumber),
                sanitize(rec.collectedDate),
                sanitize(rec.collectedTime),
                sanitize(rec.customerName),
                sanitize(rec.cif),
                sanitize(rec.bookNumber),
                rec.depositAmount.toString(),
                rec.cumulativeTotal.toString(),
                sanitize(rec.collectedByExecutiveName),
                if (rec.managerCollectedMarker) "Collected" else "Pending"
            ).joinToString(",")
            writer.write("$line\n")
        }

        val totalAmount = records.sumOf { it.depositAmount }
        writer.write(",,,,Total,Rs. $totalAmount,,,,\n")
        writer.flush()
        writer.close()
        file
    }

    suspend fun exportPartnersToCsv(
        context: Context,
        company: Company,
        deposits: List<PartnerDeposit>
    ): File = withContext(Dispatchers.IO) {
        val fileName = "Partner_Investment_Report_${System.currentTimeMillis()}.csv"
        val file = File(context.cacheDir, fileName)
        val writer = file.bufferedWriter()

        writer.write("Company,${sanitize(company.name)}\n")
        writer.write("Report,Partner Equity Deposits\n")
        writer.write("Generated On,${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}\n\n")
        writer.write("Partner PIN,Partner Name,Deposit Date,Deposit Time,Amount (INR)\n")

        for (dep in deposits) {
            val line = listOf(
                sanitize(dep.partnerPin),
                sanitize(dep.partnerName),
                sanitize(dep.depositDate),
                sanitize(dep.depositTime),
                dep.amount.toString()
            ).joinToString(",")
            writer.write("$line\n")
        }
        val total = deposits.sumOf { it.amount }
        writer.write(",,,Total Investment,Rs. $total\n")
        writer.flush()
        writer.close()
        file
    }

    suspend fun exportCollectionsToPdf(
        context: Context,
        company: Company,
        records: List<DailyCollectionRecord>,
        filterDescription: String
    ): File = withContext(Dispatchers.IO) {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 portrait
        val page = document.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val paint = Paint().apply { isAntiAlias = true }

        // Header Background
        paint.color = Color.rgb(15, 23, 42) // Dark Navy
        canvas.drawRect(0f, 0f, 595f, 90f, paint)

        // Company Name
        paint.color = Color.WHITE
        paint.textSize = 18f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(company.name.ifBlank { "LOCAL FINANCE" }, 30f, 40f, paint)

        // Subtitle & Filter
        paint.textSize = 10f
        paint.typeface = Typeface.DEFAULT
        paint.color = Color.rgb(203, 213, 225)
        canvas.drawText("${company.address.ifBlank { "Daily Collection Ledger" }} | Filter: $filterDescription", 30f, 60f, paint)
        canvas.drawText("Generated: ${SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault()).format(Date())}", 30f, 75f, paint)

        // Summary Metric Cards
        val totalCollected = records.sumOf { it.depositAmount }
        paint.color = Color.rgb(241, 245, 249)
        canvas.drawRoundRect(30f, 105f, 280f, 155f, 8f, 8f, paint)
        canvas.drawRoundRect(310f, 105f, 565f, 155f, 8f, 8f, paint)

        paint.color = Color.rgb(71, 85, 105)
        paint.textSize = 10f
        canvas.drawText("TOTAL COLLECTIONS", 45f, 125f, paint)
        canvas.drawText("TOTAL TRANSACTIONS", 325f, 125f, paint)

        paint.color = Color.rgb(15, 23, 42)
        paint.textSize = 16f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(currencyFormat.format(totalCollected), 45f, 146f, paint)
        canvas.drawText("${records.size} Receipts", 325f, 146f, paint)

        // Table Header
        paint.color = Color.rgb(30, 41, 59)
        canvas.drawRect(30f, 175f, 565f, 198f, paint)

        paint.color = Color.WHITE
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("RECEIPT", 36f, 190f, paint)
        canvas.drawText("TIME", 110f, 190f, paint)
        canvas.drawText("CUSTOMER", 160f, 190f, paint)
        canvas.drawText("CIF / BOOK", 270f, 190f, paint)
        canvas.drawText("EXECUTIVE", 360f, 190f, paint)
        canvas.drawText("STATUS", 440f, 190f, paint)
        canvas.drawText("AMOUNT", 505f, 190f, paint)

        // Rows
        var y = 215f
        paint.typeface = Typeface.DEFAULT
        for ((index, rec) in records.take(28).withIndex()) {
            if (index % 2 == 0) {
                paint.color = Color.rgb(248, 250, 252)
                canvas.drawRect(30f, y - 13f, 565f, y + 5f, paint)
            }
            paint.color = Color.rgb(30, 41, 59)
            paint.textSize = 8.5f
            canvas.drawText(rec.receiptNumber.takeLast(10), 36f, y, paint)
            canvas.drawText("${rec.collectedDate.takeLast(5)} ${rec.collectedTime.take(5)}", 110f, y, paint)
            canvas.drawText(rec.customerName.take(18), 160f, y, paint)
            canvas.drawText("${rec.cif}/${rec.bookNumber.takeLast(4)}", 270f, y, paint)
            canvas.drawText(rec.collectedByExecutiveName.take(12), 360f, y, paint)

            paint.color = if (rec.managerCollectedMarker) Color.rgb(16, 185, 129) else Color.rgb(245, 158, 11)
            canvas.drawText(if (rec.managerCollectedMarker) "Collected" else "Pending", 440f, y, paint)

            paint.color = Color.rgb(15, 23, 42)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val amtStr = currencyFormat.format(rec.depositAmount)
            canvas.drawText(amtStr, 505f, y, paint)
            paint.typeface = Typeface.DEFAULT

            y += 20f
        }

        // Footer
        paint.color = Color.rgb(148, 163, 184)
        paint.textSize = 8f
        canvas.drawText("LOCAL FINANCE Official Financial Statement • Powered by Android Local Daily Collection System", 30f, 815f, paint)
        canvas.drawText("Page 1 of 1", 520f, 815f, paint)

        document.finishPage(page)
        val file = File(context.cacheDir, "Local_Finance_Statement_${System.currentTimeMillis()}.pdf")
        val out = FileOutputStream(file)
        document.writeTo(out)
        out.close()
        document.close()
        file
    }

    suspend fun exportCustomerPassbookPdf(
        context: Context,
        company: Company,
        summary: CustomerDepositSummary
    ): File = withContext(Dispatchers.IO) {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = document.startPage(pageInfo)
        val canvas: Canvas = page.canvas
        val paint = Paint().apply { isAntiAlias = true }

        // Navy Header
        paint.color = Color.rgb(15, 23, 42)
        canvas.drawRect(0f, 0f, 595f, 105f, paint)

        paint.color = Color.WHITE
        paint.textSize = 20f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(company.name.ifBlank { "LOCAL FINANCE" }, 30f, 40f, paint)

        paint.textSize = 10f
        paint.typeface = Typeface.DEFAULT
        canvas.drawText(company.address.ifBlank { "Local Branch Office" }, 30f, 58f, paint)

        paint.textSize = 12f
        paint.color = Color.rgb(52, 211, 153)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("CUSTOMER DEPOSIT PASSBOOK & STATEMENT", 30f, 85f, paint)

        // Customer Details Box
        paint.color = Color.rgb(241, 245, 249)
        canvas.drawRoundRect(30f, 120f, 565f, 220f, 12f, 12f, paint)

        paint.color = Color.rgb(15, 23, 42)
        paint.textSize = 13f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Customer: ${summary.customer.name}", 45f, 145f, paint)

        paint.textSize = 10f
        paint.typeface = Typeface.DEFAULT
        paint.color = Color.rgb(71, 85, 105)
        canvas.drawText("Business: ${summary.customer.businessName.ifBlank { "Retail / Merchant" }}", 45f, 165f, paint)
        canvas.drawText("Contact: ${summary.customer.contact}", 45f, 185f, paint)
        canvas.drawText("Aadhar: ${summary.customer.aadharNumber} • PAN: ${summary.customer.panNumber.ifBlank { "N/A" }}", 45f, 205f, paint)

        canvas.drawText("CIF Number: ${summary.customer.cif}", 340f, 145f, paint)
        val booksStr = summary.books.joinToString(", ") { it.bookNumber }.ifBlank { "None" }
        canvas.drawText("Active Books: $booksStr", 340f, 165f, paint)
        canvas.drawText("Total Collections: ${summary.collectionCount} installments", 340f, 185f, paint)
        paint.color = Color.rgb(16, 185, 129)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Total Deposited: ${currencyFormat.format(summary.totalDepositAmount)}", 340f, 205f, paint)

        // Transactions Table Header
        paint.color = Color.rgb(30, 41, 59)
        canvas.drawRect(30f, 235f, 565f, 260f, paint)

        paint.color = Color.WHITE
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Date & Time", 36f, 252f, paint)
        canvas.drawText("Receipt No.", 145f, 252f, paint)
        canvas.drawText("Book No.", 265f, 252f, paint)
        canvas.drawText("Collected By", 340f, 252f, paint)
        canvas.drawText("Amount (Cr)", 440f, 252f, paint)
        canvas.drawText("Balance", 515f, 252f, paint)

        var y = 280f
        paint.typeface = Typeface.DEFAULT
        paint.textSize = 9f

        val displayRecords = summary.collections.take(24)
        for ((idx, rec) in displayRecords.withIndex()) {
            if (idx % 2 == 1) {
                paint.color = Color.rgb(248, 250, 252)
                canvas.drawRect(30f, y - 14f, 565f, y + 6f, paint)
            }
            paint.color = Color.rgb(30, 41, 59)
            canvas.drawText("${rec.collectedDate} ${rec.collectedTime}", 36f, y, paint)
            canvas.drawText(rec.receiptNumber, 145f, y, paint)
            canvas.drawText(rec.bookNumber, 265f, y, paint)
            canvas.drawText(rec.collectedByExecutiveName.take(14), 340f, y, paint)

            paint.color = Color.rgb(16, 185, 129)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(currencyFormat.format(rec.depositAmount), 440f, y, paint)

            paint.color = Color.rgb(30, 41, 59)
            paint.typeface = Typeface.DEFAULT
            canvas.drawText(currencyFormat.format(rec.cumulativeTotal), 515f, y, paint)

            y += 21f
        }

        // Footer
        paint.color = Color.rgb(148, 163, 184)
        paint.textSize = 8f
        canvas.drawText("LOCAL FINANCE Customer Passbook • Verified by Authorized Branch Manager", 30f, 815f, paint)
        canvas.drawText("Page 1 of 1", 520f, 815f, paint)

        document.finishPage(page)
        val file = File(context.cacheDir, "Passbook_${summary.customer.cif}_${System.currentTimeMillis()}.pdf")
        val out = FileOutputStream(file)
        document.writeTo(out)
        out.close()
        document.close()
        file
    }

    suspend fun exportCustomerPassbookCsv(
        context: Context,
        company: Company,
        summary: CustomerDepositSummary
    ): File = withContext(Dispatchers.IO) {
        val fileName = "Passbook_${summary.customer.cif}_${System.currentTimeMillis()}.csv"
        val file = File(context.cacheDir, fileName)
        val writer = file.bufferedWriter()

        writer.write("Company,${sanitize(company.name)}\n")
        writer.write("Customer Name,${sanitize(summary.customer.name)}\n")
        writer.write("CIF Number,${sanitize(summary.customer.cif)}\n")
        writer.write("Business,${sanitize(summary.customer.businessName)}\n")
        writer.write("Total Deposited,${summary.totalDepositAmount}\n")
        writer.write("Installment Count,${summary.collectionCount}\n")
        writer.write("Generated On,${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}\n\n")

        writer.write("Receipt No,Date,Time,Book No,Deposit Amount,Running Total,Collected By,Manager Verified\n")
        for (rec in summary.collections) {
            val line = listOf(
                sanitize(rec.receiptNumber),
                sanitize(rec.collectedDate),
                sanitize(rec.collectedTime),
                sanitize(rec.bookNumber),
                rec.depositAmount.toString(),
                rec.cumulativeTotal.toString(),
                sanitize(rec.collectedByExecutiveName),
                if (rec.managerCollectedMarker) "Verified" else "Pending"
            ).joinToString(",")
            writer.write("$line\n")
        }
        writer.flush()
        writer.close()
        file
    }

    suspend fun exportAllCustomersDepositSummaryPdf(
        context: Context,
        company: Company,
        summaries: List<CustomerDepositSummary>,
        filterDescription: String = "All Registered Customers"
    ): File = withContext(Dispatchers.IO) {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = document.startPage(pageInfo)
        val canvas: Canvas = page.canvas
        val paint = Paint().apply { isAntiAlias = true }

        // Navy Header
        paint.color = Color.rgb(15, 23, 42)
        canvas.drawRect(0f, 0f, 595f, 95f, paint)

        paint.color = Color.WHITE
        paint.textSize = 18f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(company.name.ifBlank { "LOCAL FINANCE" }, 30f, 38f, paint)

        paint.textSize = 10f
        paint.typeface = Typeface.DEFAULT
        canvas.drawText("CUSTOMER-WISE DEPOSIT SUMMARY REPORT • $filterDescription", 30f, 58f, paint)
        canvas.drawText("Generated: ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())}", 30f, 75f, paint)

        // Summary KPI Bar
        val grandTotal = summaries.sumOf { it.totalDepositAmount }
        paint.color = Color.rgb(241, 245, 249)
        canvas.drawRect(0f, 95f, 595f, 130f, paint)
        paint.color = Color.rgb(15, 23, 42)
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Total Customers: ${summaries.size}", 30f, 116f, paint)
        canvas.drawText("Cumulative Balance: ${currencyFormat.format(grandTotal)}", 350f, 116f, paint)

        // Table Header
        paint.color = Color.rgb(30, 41, 59)
        canvas.drawRect(30f, 140f, 565f, 165f, paint)

        paint.color = Color.WHITE
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("CIF", 36f, 157f, paint)
        canvas.drawText("Customer Name", 90f, 157f, paint)
        canvas.drawText("Business", 220f, 157f, paint)
        canvas.drawText("Book No.", 325f, 157f, paint)
        canvas.drawText("Txns", 390f, 157f, paint)
        canvas.drawText("Last Date", 435f, 157f, paint)
        canvas.drawText("Total Deposit", 500f, 157f, paint)

        var y = 185f
        paint.typeface = Typeface.DEFAULT
        paint.textSize = 9f

        for ((idx, sum) in summaries.take(28).withIndex()) {
            if (idx % 2 == 1) {
                paint.color = Color.rgb(248, 250, 252)
                canvas.drawRect(30f, y - 14f, 565f, y + 6f, paint)
            }
            paint.color = Color.rgb(30, 41, 59)
            canvas.drawText(sum.customer.cif, 36f, y, paint)
            canvas.drawText(sum.customer.name.take(18), 90f, y, paint)
            canvas.drawText(sum.customer.businessName.ifBlank { "-" }.take(16), 220f, y, paint)
            val bookStr = sum.books.firstOrNull()?.bookNumber ?: "-"
            canvas.drawText(bookStr, 325f, y, paint)
            canvas.drawText(sum.collectionCount.toString(), 390f, y, paint)
            canvas.drawText(sum.lastDepositDate.ifBlank { "-" }, 435f, y, paint)

            paint.color = Color.rgb(16, 185, 129)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(currencyFormat.format(sum.totalDepositAmount), 500f, y, paint)
            paint.typeface = Typeface.DEFAULT

            y += 20f
        }

        // Footer
        paint.color = Color.rgb(148, 163, 184)
        paint.textSize = 8f
        canvas.drawText("LOCAL FINANCE Official Financial Statement • Powered by Android Local Daily Collection System", 30f, 815f, paint)
        canvas.drawText("Page 1 of 1", 520f, 815f, paint)

        document.finishPage(page)
        val file = File(context.cacheDir, "Customer_Wise_Deposit_Report_${System.currentTimeMillis()}.pdf")
        val out = FileOutputStream(file)
        document.writeTo(out)
        out.close()
        document.close()
        file
    }

    suspend fun exportAllCustomersDepositSummaryCsv(
        context: Context,
        company: Company,
        summaries: List<CustomerDepositSummary>
    ): File = withContext(Dispatchers.IO) {
        val fileName = "Customer_Wise_Deposit_Report_${System.currentTimeMillis()}.csv"
        val file = File(context.cacheDir, fileName)
        val writer = file.bufferedWriter()

        writer.write("Company,${sanitize(company.name)}\n")
        writer.write("Report,Customer-Wise Cumulative Deposits Summary\n")
        writer.write("Generated On,${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}\n")
        writer.write("Total Customers,${summaries.size}\n")
        writer.write("Total Cumulative Balance,${summaries.sumOf { it.totalDepositAmount }}\n\n")

        writer.write("CIF No,Customer Name,Business Name,Contact,Aadhar Number,PAN Number,Book Numbers,Installments Paid,Last Deposit Date,Last Deposit Time,Total Deposited (INR)\n")
        for (sum in summaries) {
            val booksStr = sum.books.joinToString(";") { it.bookNumber }
            val line = listOf(
                sanitize(sum.customer.cif),
                sanitize(sum.customer.name),
                sanitize(sum.customer.businessName),
                sanitize(sum.customer.contact),
                sanitize(sum.customer.aadharNumber),
                sanitize(sum.customer.panNumber),
                sanitize(booksStr),
                sum.collectionCount.toString(),
                sanitize(sum.lastDepositDate),
                sanitize(sum.lastDepositTime),
                sum.totalDepositAmount.toString()
            ).joinToString(",")
            writer.write("$line\n")
        }
        writer.flush()
        writer.close()
        file
    }

    fun shareFile(context: Context, file: File, mimeType: String) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, file.name)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, "Share Report via").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    private fun sanitize(value: String): String {
        return "\"${value.replace("\"", "\"\"")}\""
    }
}
