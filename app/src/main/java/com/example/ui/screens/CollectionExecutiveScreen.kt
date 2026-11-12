package com.example.ui.screens

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Pending
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DailyCollectionRecord
import com.example.data.repository.LocalFinanceRepository
import com.example.ui.components.ThermalReceiptSlipDialog
import java.text.NumberFormat
import java.util.Locale

import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.IconButton
import androidx.compose.runtime.rememberCoroutineScope
import com.example.data.model.Company
import com.example.data.model.CustomerDepositSummary
import com.example.export.ReportExporter
import com.example.ui.components.CustomerPassbookDialog
import kotlinx.coroutines.launch

@Composable
fun CollectionExecutiveScreen(repository: LocalFinanceRepository) {
    val context = LocalContext.current
    val currentCompany by repository.currentCompany.collectAsState()
    val currentProfile by repository.currentProfile.collectAsState()
    val customers by repository.customers.collectAsState()
    val dailyBooks by repository.dailyBooks.collectAsState()
    val collections by repository.collections.collectAsState()

    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("New Collection", "Customer Passbooks", "Day-Wise Collections", "Onboard Customer")

    var activeReceiptRecord by remember { mutableStateOf<DailyCollectionRecord?>(null) }
    var activePassbookSummary by remember { mutableStateOf<CustomerDepositSummary?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Field Collection Desk",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Executive: ${currentProfile.name} • Manager: ${currentProfile.assignedManagerName.ifBlank { "Assigned Manager" }}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(12.dp))

        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            edgePadding = 0.dp,
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal) }
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        when (selectedTab) {
            0 -> ExecutiveCollectionFlowSection(
                repository = repository,
                customers = customers,
                dailyBooks = dailyBooks,
                onCollectionSuccess = { record ->
                    activeReceiptRecord = record
                }
            )
            1 -> ExecutiveCustomerPassbooksSection(
                repository = repository,
                currentCompany = currentCompany,
                onSelectPassbook = { activePassbookSummary = it }
            )
            2 -> ExecutiveDayWiseViewSection(
                executiveId = currentProfile.id,
                collections = collections,
                onViewReceipt = { record ->
                    activeReceiptRecord = record
                }
            )
            3 -> ManagerCustomerOnboardingSection(repository)
        }
    }

    // Auto-pop receipt dialog ready for 58mm Bluetooth print
    activeReceiptRecord?.let { rec ->
        currentCompany?.let { comp ->
            ThermalReceiptSlipDialog(
                record = rec,
                company = comp,
                onDismiss = { activeReceiptRecord = null }
            )
        }
    }

    // Customer Passbook Modal
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

@Composable
private fun ExecutiveCollectionFlowSection(
    repository: LocalFinanceRepository,
    customers: List<com.example.data.model.Customer>,
    dailyBooks: List<com.example.data.model.DailyDepositBook>,
    onCollectionSuccess: (DailyCollectionRecord) -> Unit
) {
    val context = LocalContext.current
    var searchInput by remember { mutableStateOf("") }
    var selectedBookNumber by remember { mutableStateOf("") }
    var depositAmountText by remember { mutableStateOf("") }

    // Resolve book and customer matches
    val matchedBooks = dailyBooks.filter {
        it.bookNumber.contains(searchInput.trim(), ignoreCase = true) ||
                it.customerCif.contains(searchInput.trim(), ignoreCase = true) ||
                it.customerName.contains(searchInput.trim(), ignoreCase = true)
    }

    val activeSelectedBook = dailyBooks.firstOrNull { it.bookNumber == selectedBookNumber }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Collect Daily Deposit",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Enter BOOK NUMBER or Customer CIF NUMBER, input deposit amount, and submit. The 58mm Bluetooth thermal receipt will pop up automatically.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = searchInput,
                        onValueChange = {
                            searchInput = it
                            if (it.length == 6 && dailyBooks.any { b -> b.bookNumber == it }) {
                                selectedBookNumber = it
                            }
                        },
                        label = { Text("Enter BOOK NUMBER / CIF NUMBER") },
                        modifier = Modifier.fillMaxWidth().testTag("collection_search_input"),
                        singleLine = true
                    )

                    if (matchedBooks.isNotEmpty() && selectedBookNumber.isBlank()) {
                        Text("Select Matching Daily Book:", style = MaterialTheme.typography.labelSmall)
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            for (b in matchedBooks.take(3)) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { selectedBookNumber = b.bookNumber }
                                        .background(MaterialTheme.colorScheme.surface)
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Book #${b.bookNumber}", fontWeight = FontWeight.Bold)
                                        Text("${b.customerName} (CIF: ${b.customerCif})", style = MaterialTheme.typography.bodySmall)
                                    }
                                    Text("Select", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    activeSelectedBook?.let { book ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Selected Book: #${book.bookNumber}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Text("Customer: ${book.customerName} • CIF: ${book.customerCif}", style = MaterialTheme.typography.bodySmall)
                                Text("Current Cumulative Total: Rs. ${book.cumulativeDeposit}", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = depositAmountText,
                        onValueChange = { depositAmountText = it },
                        label = { Text("Deposit Amount (INR)") },
                        modifier = Modifier.fillMaxWidth().testTag("collection_amount_input"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )

                    Button(
                        onClick = {
                            val amt = depositAmountText.toDoubleOrNull()
                            val targetBook = selectedBookNumber.ifBlank { searchInput.trim() }
                            if (targetBook.isBlank() || amt == null || amt <= 0) {
                                Toast.makeText(context, "Select a valid Book and enter deposit amount", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            try {
                                val rec = repository.submitDailyCollection(targetBook, amt)
                                Toast.makeText(context, "Collection submitted! Printing receipt...", Toast.LENGTH_SHORT).show()
                                onCollectionSuccess(rec)
                                depositAmountText = ""
                                searchInput = ""
                                selectedBookNumber = ""
                            } catch (e: Exception) {
                                Toast.makeText(context, e.message ?: "Collection failed", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("submit_collection_button")
                    ) {
                        Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Submit & Print 58mm Receipt")
                    }
                }
            }
        }
    }
}

@Composable
private fun ExecutiveDayWiseViewSection(
    executiveId: String,
    collections: List<DailyCollectionRecord>,
    onViewReceipt: (DailyCollectionRecord) -> Unit
) {
    // Filter collections by this executive
    val myCollections = collections.filter { it.collectedByExecutiveId == executiveId }
    val groupedByDay = myCollections.groupBy { it.collectedDate }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Day-Wise Collection & Cash Handover Status", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "Real-time sync of collections and manager cash verification status. Green check indicates manager has physically received your cash.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (groupedByDay.isEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No collections recorded yet by you.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        for ((day, records) in groupedByDay) {
            val dayTotal = records.sumOf { it.depositAmount }
            val handedOver = records.filter { it.managerCollectedMarker }.sumOf { it.depositAmount }
            val pendingHandover = records.filter { !it.managerCollectedMarker }.sumOf { it.depositAmount }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Date: $day", fontWeight = FontWeight.Bold)
                    Text("Total: Rs. $dayTotal", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }

            items(records) { rec ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onViewReceipt(rec) },
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(if (rec.managerCollectedMarker) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFFF59E0B).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                if (rec.managerCollectedMarker) Icons.Default.CheckCircle else Icons.Default.Pending,
                                contentDescription = null,
                                tint = if (rec.managerCollectedMarker) Color(0xFF10B981) else Color(0xFFF59E0B),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(rec.customerName, fontWeight = FontWeight.Bold)
                            Text("Book #${rec.bookNumber} • CIF: ${rec.cif}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = if (rec.managerCollectedMarker) "Cash Deposited to Manager" else "Cash in Hand (Pending Manager)",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (rec.managerCollectedMarker) Color(0xFF10B981) else Color(0xFFF59E0B),
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Rs. ${rec.depositAmount}",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(rec.collectedTime, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ExecutiveCustomerPassbooksSection(
    repository: LocalFinanceRepository,
    currentCompany: Company?,
    onSelectPassbook: (CustomerDepositSummary) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val customers by repository.customers.collectAsState()
    val dailyBooks by repository.dailyBooks.collectAsState()
    val collections by repository.collections.collectAsState()

    var searchQuery by remember { mutableStateOf("") }

    val customerSummaries = remember(customers, dailyBooks, collections, searchQuery) {
        val all = repository.getCustomerDepositSummaries()
        if (searchQuery.isBlank()) {
            all
        } else {
            val q = searchQuery.trim().lowercase()
            all.filter {
                it.customer.name.lowercase().contains(q) ||
                it.customer.cif.contains(q) ||
                it.customer.businessName.lowercase().contains(q) ||
                it.customer.contact.contains(q) ||
                it.books.any { b -> b.bookNumber.contains(q) }
            }
        }
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Customer Deposit Passbooks", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "Look up customer cumulative deposits, recurring deposit books, and generate live digital passbooks in the field.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search Customer Name, CIF, or Business") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().testTag("exec_customer_search_input"),
                singleLine = true
            )
        }

        if (customerSummaries.isEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No customer records found matching search.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        items(customerSummaries) { sum ->
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(sum.customer.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("CIF: ${sum.customer.cif}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                            if (sum.customer.businessName.isNotBlank()) {
                                Text("Shop: ${sum.customer.businessName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text("Contact: ${sum.customer.contact}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Rs. ${sum.totalDepositAmount}",
                                fontWeight = FontWeight.Black,
                                fontSize = 17.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text("${sum.collectionCount} Collections", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    // Daily books list
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

                    // Passbook button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { onSelectPassbook(sum) },
                            modifier = Modifier.weight(1f).testTag("exec_view_passbook_${sum.customer.cif}"),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("View Passbook & Statement", fontSize = 11.sp)
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
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
