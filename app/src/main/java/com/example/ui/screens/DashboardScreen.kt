package com.example.ui.screens

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import com.example.data.model.CustomerDepositSummary
import com.example.data.model.UserRole
import com.example.data.repository.LocalFinanceRepository
import com.example.export.ReportExporter
import com.example.ui.components.ChartPalette
import com.example.ui.components.ChartSlice
import com.example.ui.components.CustomerPassbookDialog
import com.example.ui.components.FinancialDoughnutChart
import com.example.ui.components.FinancialPieChart
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class TimeFilterType(val label: String) {
    YESTERDAY("Yesterday"),
    TODAY("Today"),
    THIS_MONTH("This Month"),
    THIS_YEAR("This Year"),
    CUSTOM_RANGE("Custom Range")
}

enum class TargetFilterType(val label: String) {
    ALL_COMPANY("Company Total"),
    BY_MANAGER("Manager"),
    BY_EXECUTIVE("Collection Executive"),
    BY_CUSTOMER_CIF("Customer CIF")
}

@Composable
fun DashboardScreen(repository: LocalFinanceRepository) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val currentCompany by repository.currentCompany.collectAsState()
    val currentProfile by repository.currentProfile.collectAsState()
    val staffList by repository.staffList.collectAsState()
    val partners by repository.partners.collectAsState()
    val partnerDeposits by repository.partnerDeposits.collectAsState()
    val collections by repository.collections.collectAsState()
    val customers by repository.customers.collectAsState()
    val dailyBooks by repository.dailyBooks.collectAsState()

    // Determine default time filter based on role instructions:
    // Company/Manager: "BY DEFAULT SHOW TOTAL YESTERDAY COLLECTION"
    // Executive: "BY DEFAULT SHOW TOTAL TODAY COLLECTION BY HIM"
    val defaultTimeFilter = if (currentProfile.role == UserRole.COLLECTION_EXECUTIVE.name) {
        TimeFilterType.TODAY
    } else {
        TimeFilterType.YESTERDAY
    }

    var chartTypeIsDoughnut by remember { mutableStateOf(true) }
    var selectedTimeFilter by remember(currentProfile.role) { mutableStateOf(defaultTimeFilter) }
    var selectedTargetFilter by remember { mutableStateOf(TargetFilterType.ALL_COMPANY) }

    var selectedStaffFilterId by remember { mutableStateOf("") }
    var customerCifQuery by remember { mutableStateOf("") }

    // Customer-wise deposit view states
    var viewModeIsCustomerWise by remember { mutableStateOf(false) }
    var customerSearchQuery by remember { mutableStateOf("") }
    var activePassbookSummary by remember { mutableStateOf<CustomerDepositSummary?>(null) }

    // Date range
    val todayCalendar = Calendar.getInstance()
    var startDateStr by remember {
        val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time))
    }
    var endDateStr by remember {
        mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()))
    }

    val yesterdayStr = remember {
        val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
    }
    val todayStr = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }
    val currentMonthPrefix = remember {
        SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
    }
    val currentYearPrefix = remember {
        SimpleDateFormat("yyyy", Locale.getDefault()).format(Date())
    }

    // Role-scoped base collections
    val roleScopedCollections = when (currentProfile.role) {
        UserRole.COLLECTION_EXECUTIVE.name -> collections.filter { it.collectedByExecutiveId == currentProfile.id }
        UserRole.MANAGER.name -> {
            val myExecIds = staffList.filter { it.assignedManagerId == currentProfile.id }.map { it.id }.toSet()
            collections.filter { it.managerId == currentProfile.id || it.collectedByExecutiveId in myExecIds }
        }
        else -> collections // Admin / Developer sees company collections
    }

    // Filter by Time
    val timeFilteredCollections = roleScopedCollections.filter { rec ->
        when (selectedTimeFilter) {
            TimeFilterType.YESTERDAY -> rec.collectedDate == yesterdayStr
            TimeFilterType.TODAY -> rec.collectedDate == todayStr
            TimeFilterType.THIS_MONTH -> rec.collectedDate.startsWith(currentMonthPrefix)
            TimeFilterType.THIS_YEAR -> rec.collectedDate.startsWith(currentYearPrefix)
            TimeFilterType.CUSTOM_RANGE -> rec.collectedDate >= startDateStr && rec.collectedDate <= endDateStr
        }
    }

    // Filter by Target Hierarchy
    val finalCollections = timeFilteredCollections.filter { rec ->
        when (selectedTargetFilter) {
            TargetFilterType.ALL_COMPANY -> true
            TargetFilterType.BY_MANAGER -> {
                if (selectedStaffFilterId.isNotBlank()) rec.managerId == selectedStaffFilterId else true
            }
            TargetFilterType.BY_EXECUTIVE -> {
                if (selectedStaffFilterId.isNotBlank()) rec.collectedByExecutiveId == selectedStaffFilterId else true
            }
            TargetFilterType.BY_CUSTOMER_CIF -> {
                if (customerCifQuery.isNotBlank()) rec.cif == customerCifQuery.trim() else true
            }
        }
    }

    // Build Chart Slices
    val chartSlices = remember(finalCollections) {
        if (finalCollections.isEmpty()) {
            emptyList()
        } else {
            // Group by Customer Name or Executive Name depending on volume
            finalCollections.groupBy { it.customerName }
                .map { (name, recs) -> name to recs.sumOf { it.depositAmount } }
                .sortedByDescending { it.second }
                .take(6)
                .mapIndexed { index, (label, value) ->
                    ChartSlice(
                        label = label.take(15),
                        value = value,
                        color = ChartPalette[index % ChartPalette.size]
                    )
                }
        }
    }

    val totalAmount = finalCollections.sumOf { it.depositAmount }
    val totalTransactions = finalCollections.size
    val totalPartnerInvestment = partnerDeposits.sumOf { it.amount }

    val customerSummaries = remember(customers, dailyBooks, collections, customerSearchQuery) {
        val all = repository.getCustomerDepositSummaries()
        if (customerSearchQuery.isBlank()) {
            all
        } else {
            val q = customerSearchQuery.trim().lowercase()
            all.filter {
                it.customer.name.lowercase().contains(q) ||
                it.customer.cif.contains(q) ||
                it.customer.businessName.lowercase().contains(q) ||
                it.customer.contact.contains(q) ||
                it.books.any { b -> b.bookNumber.contains(q) }
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = when (currentProfile.role) {
                            UserRole.COLLECTION_EXECUTIVE.name -> "Executive Dashboard"
                            UserRole.MANAGER.name -> "Manager Dashboard"
                            else -> "Company Dashboard"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "${currentCompany?.name} • Live Reports & Visual Analytics",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Chart type switcher (Pie & Doughnut)
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(3.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (chartTypeIsDoughnut) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable { chartTypeIsDoughnut = true }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Doughnut",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (chartTypeIsDoughnut) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (!chartTypeIsDoughnut) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable { chartTypeIsDoughnut = false }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Pie",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (!chartTypeIsDoughnut) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // 1. PARTNER INVESTMENT SECTION (Visible for Company Admin / Developer)
        if (currentProfile.role == UserRole.COMPANY_ADMIN.name || currentProfile.role == UserRole.DEVELOPER.name) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("TOTAL INVESTMENT BY ALL PARTNERS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = NumberFormat.getCurrencyInstance(Locale("en", "IN")).format(totalPartnerInvestment).replace("₹", "Rs. "),
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            IconButton(onClick = {
                                currentCompany?.let { comp ->
                                    scope.launch {
                                        val csvFile = ReportExporter.exportPartnersToCsv(context, comp, partnerDeposits)
                                        ReportExporter.shareFile(context, csvFile, "text/csv")
                                    }
                                }
                            }) {
                                Icon(Icons.Default.Download, contentDescription = "Download Partner Report")
                            }
                        }

                        Text("Partner-Wise Investment Timeline:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        for (dep in partnerDeposits.take(4)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("${dep.partnerName} (PIN: ${dep.partnerPin})", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Text("${dep.depositDate} • Rs. ${dep.amount}", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // 2. CHART VIEW (Pie or Doughnut)
        item {
            if (chartTypeIsDoughnut) {
                FinancialDoughnutChart(
                    title = "Collection Distribution",
                    slices = chartSlices,
                    centerPrimaryText = NumberFormat.getCurrencyInstance(Locale("en", "IN")).format(totalAmount).replace("₹", "Rs. "),
                    centerSecondaryText = "Total Collection"
                )
            } else {
                FinancialPieChart(
                    title = "Collection Distribution",
                    slices = chartSlices
                )
            }
        }

        // 3. FILTER SECTION
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.FilterList, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Report Filters & Criteria", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    }

                    // Time Filter Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        TimeFilterType.values().forEach { filter ->
                            val isSelected = selectedTimeFilter == filter
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
                                    .clickable { selectedTimeFilter = filter }
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = filter.label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    // Custom Date Range Picker Trigger
                    if (selectedTimeFilter == TimeFilterType.CUSTOM_RANGE) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val c = Calendar.getInstance()
                                    DatePickerDialog(
                                        context,
                                        { _, y, m, d ->
                                            startDateStr = String.format(Locale.US, "%04d-%02d-%02d", y, m + 1, d)
                                        },
                                        c.get(Calendar.YEAR),
                                        c.get(Calendar.MONTH),
                                        c.get(Calendar.DAY_OF_MONTH)
                                    ).show()
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("From: $startDateStr", fontSize = 11.sp)
                            }

                            OutlinedButton(
                                onClick = {
                                    val c = Calendar.getInstance()
                                    DatePickerDialog(
                                        context,
                                        { _, y, m, d ->
                                            endDateStr = String.format(Locale.US, "%04d-%02d-%02d", y, m + 1, d)
                                        },
                                        c.get(Calendar.YEAR),
                                        c.get(Calendar.MONTH),
                                        c.get(Calendar.DAY_OF_MONTH)
                                    ).show()
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("To: $endDateStr", fontSize = 11.sp)
                            }
                        }
                    }

                    // Hierarchy Target Selector
                    if (currentProfile.role != UserRole.COLLECTION_EXECUTIVE.name) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            TargetFilterType.values().forEach { target ->
                                val isSelected = selectedTargetFilter == target
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surface)
                                        .clickable { selectedTargetFilter = target }
                                        .padding(horizontal = 8.dp, vertical = 5.dp)
                                ) {
                                    Text(
                                        text = target.label,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    if (selectedTargetFilter == TargetFilterType.BY_CUSTOMER_CIF) {
                        OutlinedTextField(
                            value = customerCifQuery,
                            onValueChange = { customerCifQuery = it },
                            label = { Text("Enter Customer 5-Digit CIF Number") },
                            modifier = Modifier.fillMaxWidth().testTag("filter_cif_input"),
                            singleLine = true
                        )
                    }
                }
            }
        }

        // 4. VIEW SELECTOR: TRANSACTIONS vs CUSTOMER-WISE DEPOSITS
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (!viewModeIsCustomerWise) MaterialTheme.colorScheme.primary else Color.Transparent)
                        .clickable { viewModeIsCustomerWise = false }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.ReceiptLong,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (!viewModeIsCustomerWise) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Transactions (${finalCollections.size})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (!viewModeIsCustomerWise) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (viewModeIsCustomerWise) MaterialTheme.colorScheme.primary else Color.Transparent)
                        .clickable { viewModeIsCustomerWise = true }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.People,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (viewModeIsCustomerWise) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        val count = repository.getCustomerDepositSummaries().size
                        Text(
                            text = "Customer-Wise ($count)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (viewModeIsCustomerWise) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        if (!viewModeIsCustomerWise) {
            // --- TRANSACTIONS LOG VIEW ---
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                currentCompany?.let { comp ->
                                    scope.launch {
                                        try {
                                            val pdfFile = ReportExporter.exportCollectionsToPdf(
                                                context,
                                                comp,
                                                finalCollections,
                                                "${selectedTimeFilter.label} (${selectedTargetFilter.label})"
                                            )
                                            Toast.makeText(context, "PDF Report generated!", Toast.LENGTH_SHORT).show()
                                            ReportExporter.shareFile(context, pdfFile, "application/pdf")
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "PDF error: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f).testTag("download_pdf_report_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("PDF Report", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                currentCompany?.let { comp ->
                                    scope.launch {
                                        try {
                                            val csvFile = ReportExporter.exportCollectionsToCsv(
                                                context,
                                                comp,
                                                finalCollections,
                                                "${selectedTimeFilter.label} (${selectedTargetFilter.label})"
                                            )
                                            Toast.makeText(context, "Excel/CSV Report generated!", Toast.LENGTH_SHORT).show()
                                            ReportExporter.shareFile(context, csvFile, "text/csv")
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Export error: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f).testTag("download_excel_report_button")
                        ) {
                            Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Excel / CSV", fontSize = 12.sp)
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Collection Records (${finalCollections.size} Items • Total: Rs. $totalAmount)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            items(finalCollections) { rec ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(rec.customerName, fontWeight = FontWeight.Bold)
                            Text("Receipt #${rec.receiptNumber} • CIF: ${rec.cif} • Book: ${rec.bookNumber}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Date: ${rec.collectedDate} ${rec.collectedTime} • Exec: ${rec.collectedByExecutiveName}", style = MaterialTheme.typography.labelSmall)
                        }
                        Text(
                            text = "Rs. ${rec.depositAmount}",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        } else {
            // --- CUSTOMER-WISE DEPOSITS VIEW ---
            item {
                OutlinedTextField(
                    value = customerSearchQuery,
                    onValueChange = { customerSearchQuery = it },
                    label = { Text("Search by Customer Name, CIF, or Business") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (customerSearchQuery.isNotBlank()) {
                            IconButton(onClick = { customerSearchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("customer_search_input"),
                    singleLine = true
                )
            }

            // Export All Customers Deposit Summaries (PDF & Excel)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f))
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Customer Deposit Reports (Consolidated)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = {
                                    currentCompany?.let { comp ->
                                        scope.launch {
                                            try {
                                                val pdfFile = ReportExporter.exportAllCustomersDepositSummaryPdf(
                                                    context,
                                                    comp,
                                                    customerSummaries,
                                                    if (customerSearchQuery.isBlank()) "All Customers" else "Search: $customerSearchQuery"
                                                )
                                                Toast.makeText(context, "Customer Summary PDF generated!", Toast.LENGTH_SHORT).show()
                                                ReportExporter.shareFile(context, pdfFile, "application/pdf")
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "Export error: ${e.message}", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f).testTag("download_all_customers_pdf_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("All Customers PDF", fontSize = 11.sp)
                            }

                            OutlinedButton(
                                onClick = {
                                    currentCompany?.let { comp ->
                                        scope.launch {
                                            try {
                                                val csvFile = ReportExporter.exportAllCustomersDepositSummaryCsv(
                                                    context,
                                                    comp,
                                                    customerSummaries
                                                )
                                                Toast.makeText(context, "Customer Summary Excel generated!", Toast.LENGTH_SHORT).show()
                                                ReportExporter.shareFile(context, csvFile, "text/csv")
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "Export error: ${e.message}", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f).testTag("download_all_customers_excel_button")
                            ) {
                                Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("All Customers Excel", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            // Summary Totals Card
            item {
                val totalCustDeposit = customerSummaries.sumOf { it.totalDepositAmount }
                val totalCustPayments = customerSummaries.sumOf { it.collectionCount }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Customers: ${customerSummaries.size} • $totalCustPayments Total Payments", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = "Cumulative: Rs. $totalCustDeposit",
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Customer List
            if (customerSummaries.isEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("No customers found matching search.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            items(customerSummaries) { sum ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = sum.customer.name,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "CIF: ${sum.customer.cif}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                if (sum.customer.businessName.isNotBlank()) {
                                    Text(
                                        text = "Shop: ${sum.customer.businessName}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = "Contact: ${sum.customer.contact} • Aadhar: ${sum.customer.aadharNumber}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Rs. ${sum.totalDepositAmount}",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 17.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "${sum.collectionCount} Collections",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Daily Books and Last Payment Info
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val booksText = sum.books.joinToString(", ") { "Book #${it.bookNumber}" }.ifBlank { "No Book" }
                            Text(booksText, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            val lastPaymentText = if (sum.lastDepositDate.isNotBlank()) "Last: ${sum.lastDepositDate}" else "No deposits"
                            Text(lastPaymentText, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        // Actions Row: View Passbook, PDF, Excel
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { activePassbookSummary = sum },
                                modifier = Modifier.weight(1f).testTag("view_passbook_${sum.customer.cif}"),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Passbook & Ledger", fontSize = 11.sp)
                            }

                            IconButton(
                                onClick = {
                                    currentCompany?.let { comp ->
                                        scope.launch {
                                            try {
                                                val pdf = ReportExporter.exportCustomerPassbookPdf(context, comp, sum)
                                                ReportExporter.shareFile(context, pdf, "application/pdf")
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.size(38.dp)
                            ) {
                                Icon(Icons.Default.Description, contentDescription = "PDF Passbook", tint = MaterialTheme.colorScheme.primary)
                            }

                            IconButton(
                                onClick = {
                                    currentCompany?.let { comp ->
                                        scope.launch {
                                            try {
                                                val csv = ReportExporter.exportCustomerPassbookCsv(context, comp, sum)
                                                ReportExporter.shareFile(context, csv, "text/csv")
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.size(38.dp)
                            ) {
                                Icon(Icons.Default.TableChart, contentDescription = "Excel Passbook", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Interactive Customer Passbook Dialog
    activePassbookSummary?.let { summary ->
        currentCompany?.let { comp ->
            CustomerPassbookDialog(
                summary = summary,
                company = comp,
                onDismiss = { activePassbookSummary = null }
            )
        }
    }
}
