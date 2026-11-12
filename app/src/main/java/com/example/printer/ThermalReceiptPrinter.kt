package com.example.printer

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import com.example.data.model.Company
import com.example.data.model.DailyCollectionRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.text.NumberFormat
import java.util.Locale
import java.util.UUID

data class ReceiptData(
    val receiptNumber: String,
    val dateTime: String,
    val companyName: String,
    val companyAddress: String,
    val customerName: String,
    val cifNo: String,
    val bookNo: String,
    val depositAmount: Double,
    val totalDeposit: Double,
    val executiveName: String
)

object ThermalReceiptPrinter {

    // Standard Bluetooth Serial Port Profile (SPP) UUID
    private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private const val LINE_WIDTH = 32 // Standard 58mm thermal paper width in characters

    fun createReceiptData(
        record: DailyCollectionRecord,
        company: Company
    ): ReceiptData {
        val dateStr = if (record.collectedDate.isNotBlank()) "${record.collectedDate} ${record.collectedTime}" else "N/A"
        return ReceiptData(
            receiptNumber = record.receiptNumber,
            dateTime = dateStr,
            companyName = company.name.ifBlank { "LOCAL FINANCE CO." },
            companyAddress = company.address.ifBlank { "Main Branch" },
            customerName = record.customerName,
            cifNo = record.cif,
            bookNo = record.bookNumber,
            depositAmount = record.depositAmount,
            totalDeposit = record.cumulativeTotal,
            executiveName = record.collectedByExecutiveName
        )
    }

    private fun formatCurrency(amount: Double): String {
        return NumberFormat.getCurrencyInstance(Locale("en", "IN")).format(amount)
            .replace("₹", "Rs. ")
    }

    private fun centerText(text: String, width: Int = LINE_WIDTH): String {
        if (text.length >= width) return text.take(width)
        val padding = (width - text.length) / 2
        return " ".repeat(padding) + text
    }

    private fun twoColumns(left: String, right: String, width: Int = LINE_WIDTH): String {
        val totalSpace = width - left.length - right.length
        return if (totalSpace > 0) {
            left + " ".repeat(totalSpace) + right
        } else {
            "$left $right"
        }
    }

    fun generateReceiptText(receipt: ReceiptData): String {
        val sb = StringBuilder()
        val div = "-".repeat(LINE_WIDTH)
        val doubleDiv = "=".repeat(LINE_WIDTH)

        sb.appendLine(centerText(receipt.companyName))
        if (receipt.companyAddress.isNotBlank()) {
            sb.appendLine(centerText(receipt.companyAddress))
        }
        sb.appendLine(centerText("DAILY COLLECTION RECEIPT"))
        sb.appendLine(doubleDiv)

        sb.appendLine(twoColumns("Receipt No:", receipt.receiptNumber))
        sb.appendLine(twoColumns("Date & Time:", receipt.dateTime))
        sb.appendLine(div)

        sb.appendLine(twoColumns("Customer Name:", receipt.customerName.take(17)))
        sb.appendLine(twoColumns("CIF NO.:", receipt.cifNo))
        sb.appendLine(twoColumns("BOOK NO.:", receipt.bookNo))
        sb.appendLine(div)

        sb.appendLine(twoColumns("Deposit:", formatCurrency(receipt.depositAmount)))
        sb.appendLine(twoColumns("Total Deposit:", formatCurrency(receipt.totalDeposit)))
        sb.appendLine(doubleDiv)

        sb.appendLine(twoColumns("Collected By:", receipt.executiveName.take(18)))
        sb.appendLine(centerText("Thank you for your business!"))
        sb.appendLine(centerText("*** Computer Generated ***"))
        sb.appendLine("\n\n")

        return sb.toString()
    }

    fun generateEscPosBytes(receipt: ReceiptData): ByteArray {
        val stream = ByteArrayOutputStream()

        // ESC @ - Initialize printer
        stream.write(byteArrayOf(0x1B, 0x40))

        // ESC t 0 - Set Character code table PC437
        stream.write(byteArrayOf(0x1B, 0x74, 0x00))

        // Center Align & Double height for Company Name
        stream.write(byteArrayOf(0x1B, 0x61, 0x01)) // Center
        stream.write(byteArrayOf(0x1D, 0x21, 0x11)) // Double size
        stream.write(byteArrayOf(0x1B, 0x45, 0x01)) // Bold on
        stream.write("${receipt.companyName}\n".toByteArray(Charsets.US_ASCII))

        // Normal text
        stream.write(byteArrayOf(0x1D, 0x21, 0x00)) // Normal
        stream.write(byteArrayOf(0x1B, 0x45, 0x00)) // Bold off
        if (receipt.companyAddress.isNotBlank()) {
            stream.write("${receipt.companyAddress}\n".toByteArray(Charsets.US_ASCII))
        }
        stream.write("DAILY COLLECTION RECEIPT\n".toByteArray(Charsets.US_ASCII))
        stream.write("${"=".repeat(LINE_WIDTH)}\n".toByteArray(Charsets.US_ASCII))

        // Left Align
        stream.write(byteArrayOf(0x1B, 0x61, 0x00))
        val bodyLines = listOf(
            twoColumns("Receipt No:", receipt.receiptNumber),
            twoColumns("Date & Time:", receipt.dateTime),
            "-".repeat(LINE_WIDTH),
            twoColumns("Customer Name:", receipt.customerName),
            twoColumns("CIF NO.:", receipt.cifNo),
            twoColumns("BOOK NO.:", receipt.bookNo),
            "-".repeat(LINE_WIDTH)
        )
        for (line in bodyLines) {
            stream.write("$line\n".toByteArray(Charsets.US_ASCII))
        }

        // Bold Deposit Details
        stream.write(byteArrayOf(0x1B, 0x45, 0x01))
        stream.write("${twoColumns("Deposit:", formatCurrency(receipt.depositAmount))}\n".toByteArray(Charsets.US_ASCII))
        stream.write("${twoColumns("Total Deposit:", formatCurrency(receipt.totalDeposit))}\n".toByteArray(Charsets.US_ASCII))
        stream.write(byteArrayOf(0x1B, 0x45, 0x00))

        stream.write("${"=".repeat(LINE_WIDTH)}\n".toByteArray(Charsets.US_ASCII))
        stream.write("${twoColumns("Collected By:", receipt.executiveName)}\n".toByteArray(Charsets.US_ASCII))

        // Center align footer
        stream.write(byteArrayOf(0x1B, 0x61, 0x01))
        stream.write("Thank you for your business!\n".toByteArray(Charsets.US_ASCII))
        stream.write("*** Computer Generated ***\n".toByteArray(Charsets.US_ASCII))

        // Feed 4 lines and partial cut (GS V 66 0)
        stream.write(byteArrayOf(0x1B, 0x64, 0x04))
        stream.write(byteArrayOf(0x1D, 0x56, 0x42, 0x00))

        return stream.toByteArray()
    }

    @SuppressLint("MissingPermission")
    fun getPairedBluetoothPrinters(context: Context): List<BluetoothDevice> {
        val adapter = BluetoothAdapter.getDefaultAdapter() ?: return emptyList()
        return try {
            adapter.bondedDevices?.filter {
                // Return paired devices, favoring printers / imaging / audio/peripheral
                true
            } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun printToBluetoothDevice(
        device: BluetoothDevice,
        receiptBytes: ByteArray
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        var socket: BluetoothSocket? = null
        var outputStream: OutputStream? = null
        try {
            socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
            socket.connect()
            outputStream = socket.outputStream
            outputStream.write(receiptBytes)
            outputStream.flush()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            try {
                outputStream?.close()
                socket?.close()
            } catch (_: Exception) {}
        }
    }
}
