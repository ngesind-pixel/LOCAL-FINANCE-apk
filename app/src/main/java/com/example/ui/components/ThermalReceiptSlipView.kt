package com.example.ui.components

import android.bluetooth.BluetoothDevice
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.Company
import com.example.data.model.DailyCollectionRecord
import com.example.export.ReportExporter
import com.example.printer.ReceiptData
import com.example.printer.ThermalReceiptPrinter
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

@Composable
fun ThermalReceiptSlipDialog(
    record: DailyCollectionRecord,
    company: Company,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val receiptData = remember(record, company) {
        ThermalReceiptPrinter.createReceiptData(record, company)
    }

    var isPrinting by remember { mutableStateOf(false) }
    var showPrinterPicker by remember { mutableStateOf(false) }
    var pairedPrinters by remember { mutableStateOf<List<BluetoothDevice>>(emptyList()) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .shadow(16.dp, RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "58mm Thermal Receipt",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "READY TO PRINT",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF10B981),
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF10B981).copy(alpha = 0.12f))
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Realistic 58mm Thermal Paper Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(4.dp, RoundedCornerShape(8.dp))
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFFCFDFD))
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Header
                        Text(
                            text = receiptData.companyName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF0F172A),
                            textAlign = TextAlign.Center
                        )
                        if (receiptData.companyAddress.isNotBlank()) {
                            Text(
                                text = receiptData.companyAddress,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF475569),
                                textAlign = TextAlign.Center
                            )
                        }
                        Text(
                            text = "DAILY COLLECTION RECEIPT",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A),
                            modifier = Modifier.padding(top = 4.dp)
                        )

                        Divider(
                            modifier = Modifier.padding(vertical = 10.dp),
                            color = Color(0xFF0F172A),
                            thickness = 1.dp
                        )

                        ReceiptRowMonospace("Receipt No:", receiptData.receiptNumber)
                        ReceiptRowMonospace("Date & Time:", receiptData.dateTime)

                        Divider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = Color(0xFFCBD5E1),
                            thickness = 1.dp
                        )

                        ReceiptRowMonospace("Customer:", receiptData.customerName)
                        ReceiptRowMonospace("CIF NO.:", receiptData.cifNo)
                        ReceiptRowMonospace("BOOK NO.:", receiptData.bookNo)

                        Divider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = Color(0xFFCBD5E1),
                            thickness = 1.dp
                        )

                        // Highlighted Financials
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Deposit:",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
                                    .format(receiptData.depositAmount).replace("₹", "Rs. "),
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                color = Color(0xFF0F172A)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Total Deposit:",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = Color(0xFF475569)
                            )
                            Text(
                                text = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
                                    .format(receiptData.totalDeposit).replace("₹", "Rs. "),
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color(0xFF475569)
                            )
                        }

                        Divider(
                            modifier = Modifier.padding(vertical = 10.dp),
                            color = Color(0xFF0F172A),
                            thickness = 1.dp
                        )

                        ReceiptRowMonospace("Collected By:", receiptData.executiveName)

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Thank you for banking locally",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = Color(0xFF64748B),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "*** 58mm Thermal Slip ***",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = Color(0xFF94A3B8),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            val printers = ThermalReceiptPrinter.getPairedBluetoothPrinters(context)
                            if (printers.isEmpty()) {
                                Toast.makeText(
                                    context,
                                    "No paired Bluetooth printers found. Pair your 58mm printer in Android Settings.",
                                    Toast.LENGTH_LONG
                                ).show()
                            } else {
                                pairedPrinters = printers
                                showPrinterPicker = true
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("bluetooth_print_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        if (isPrinting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Bluetooth, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("BT Print")
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                try {
                                    val pdfFile = ReportExporter.exportCollectionsToPdf(
                                        context,
                                        company,
                                        listOf(record),
                                        "Single Receipt ${record.receiptNumber}"
                                    )
                                    ReportExporter.shareFile(context, pdfFile, "application/pdf")
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Export error: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("share_receipt_pdf_button")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share PDF")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().testTag("close_receipt_dialog_button")
                ) {
                    Text("Close")
                }
            }
        }
    }

    if (showPrinterPicker) {
        AlertDialog(
            onDismissRequest = { showPrinterPicker = false },
            title = { Text("Select 58mm Thermal Printer") },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    for (printer in pairedPrinters) {
                        OutlinedButton(
                            onClick = {
                                showPrinterPicker = false
                                isPrinting = true
                                scope.launch {
                                    val bytes = ThermalReceiptPrinter.generateEscPosBytes(receiptData)
                                    val res = ThermalReceiptPrinter.printToBluetoothDevice(printer, bytes)
                                    isPrinting = false
                                    if (res.isSuccess) {
                                        Toast.makeText(context, "Receipt printed successfully!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(
                                            context,
                                            "Print error: ${res.exceptionOrNull()?.message}",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Print, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(printer.name ?: "Unknown Printer")
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showPrinterPicker = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ReceiptRowMonospace(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            color = Color(0xFF475569)
        )
        Text(
            text = value,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            color = Color(0xFF0F172A)
        )
    }
}
