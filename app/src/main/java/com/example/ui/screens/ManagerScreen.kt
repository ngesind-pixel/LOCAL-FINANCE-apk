package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Pending
import androidx.compose.material.icons.filled.PersonAdd
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
import com.example.data.model.ApprovalStatus
import com.example.data.model.UserRole
import com.example.data.repository.LocalFinanceRepository
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
fun ManagerScreen(repository: LocalFinanceRepository) {
    val context = LocalContext.current
    val currentCompany by repository.currentCompany.collectAsState()
    val currentProfile by repository.currentProfile.collectAsState()
    val staffList by repository.staffList.collectAsState()
    val customers by repository.customers.collectAsState()
    val dailyBooks by repository.dailyBooks.collectAsState()
    val collections by repository.collections.collectAsState()

    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Cash Reconciliation", "Customer Deposits", "Customer Onboard", "Create Daily Book", "Create Executive")
    var activePassbookSummary by remember { mutableStateOf<CustomerDepositSummary?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Manager Operations",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Manager: ${currentProfile.name} • ${currentCompany?.name}",
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
            0 -> ManagerCashReconciliationSection(repository, currentProfile.id, collections, staffList)
            1 -> ManagerCustomerDepositsSection(
                repository = repository,
                currentCompany = currentCompany,
                onSelectPassbook = { activePassbookSummary = it }
            )
            2 -> ManagerCustomerOnboardingSection(repository)
            3 -> ManagerDailyBookCreationSection(repository, customers, dailyBooks)
            4 -> ManagerCreateExecutiveSection(repository, currentProfile.id)
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
private fun ManagerCashReconciliationSection(
    repository: LocalFinanceRepository,
    managerId: String,
    collections: List<com.example.data.model.DailyCollectionRecord>,
    staffList: List<com.example.data.model.UserProfile>
) {
    val context = LocalContext.current
    // Filter collections relevant to executives under this manager or general collections
    val myExecutives = staffList.filter { it.assignedManagerId == managerId }.map { it.id }.toSet()
    val relevantCollections = collections.filter {
        it.managerId == managerId || it.collectedByExecutiveId in myExecutives
    }

    // Group day-wise and executive-wise
    val groupedByDay = relevantCollections.groupBy { it.collectedDate }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Cash Handover Verification (Marker Collect)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "Verify and tap 'Mark Collected' when field executives deposit physical collected cash to you.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }

        if (groupedByDay.isEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No collections assigned to your executives yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        for ((day, recordsForDay) in groupedByDay) {
            item {
                val dayTotal = recordsForDay.sumOf { it.depositAmount }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Date: $day", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    Text("Day Total: Rs. $dayTotal", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }

            // Group by executive under this day
            val groupedByExec = recordsForDay.groupBy { it.collectedByExecutiveName }
            for ((execName, execRecords) in groupedByExec) {
                val execPending = execRecords.filter { !it.managerCollectedMarker }.sumOf { it.depositAmount }
                val execTotal = execRecords.sumOf { it.depositAmount }
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(execName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                                Text("Total: Rs. $execTotal | Pending: Rs. $execPending", style = MaterialTheme.typography.bodySmall, color = if (execPending > 0) Color(0xFFF59E0B) else Color(0xFF10B981))
                            }

                            for (rec in execRecords) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.surface)
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(rec.customerName, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                        Text("Receipt: ${rec.receiptNumber} • CIF: ${rec.cif} • Book: ${rec.bookNumber}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("Rs. ${rec.depositAmount}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    }

                                    if (rec.managerCollectedMarker) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Collected", color = Color(0xFF10B981), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                    } else {
                                        Button(
                                            onClick = {
                                                repository.markCollectionCollectedByManager(rec.id)
                                                Toast.makeText(context, "Marked collected for ${rec.customerName}!", Toast.LENGTH_SHORT).show()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                            modifier = Modifier.testTag("marker_collect_${rec.receiptNumber}")
                                        ) {
                                            Text("Marker Collect", fontSize = 11.sp)
                                        }
                                    }
                                }
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
}

@Composable
fun ManagerCustomerOnboardingSection(repository: LocalFinanceRepository) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var businessName by remember { mutableStateOf("") }
    var contact by remember { mutableStateOf("") }
    var aadhar by remember { mutableStateOf("") }
    var pan by remember { mutableStateOf("") }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Customer Onboarding Form (Auto 5-Digit CIF)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Customer Full Name (Must)") },
                        modifier = Modifier.fillMaxWidth().testTag("cust_name_input"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = businessName,
                        onValueChange = { businessName = it },
                        label = { Text("Business Name / Shop Name") },
                        modifier = Modifier.fillMaxWidth().testTag("cust_business_input"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = contact,
                        onValueChange = { contact = it },
                        label = { Text("Contact Number (Must)") },
                        modifier = Modifier.fillMaxWidth().testTag("cust_contact_input"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = aadhar,
                        onValueChange = { aadhar = it },
                        label = { Text("Aadhar Number (Must)") },
                        modifier = Modifier.fillMaxWidth().testTag("cust_aadhar_input"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = pan,
                        onValueChange = { pan = it },
                        label = { Text("PAN Number") },
                        modifier = Modifier.fillMaxWidth().testTag("cust_pan_input"),
                        singleLine = true
                    )

                    Button(
                        onClick = {
                            if (name.isBlank() || contact.isBlank() || aadhar.isBlank()) {
                                Toast.makeText(context, "Name, Contact, and Aadhar are mandatory!", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            val c = repository.onboardCustomer(
                                name = name.trim(),
                                businessName = businessName.trim(),
                                contact = contact.trim(),
                                aadhar = aadhar.trim(),
                                pan = pan.trim().uppercase()
                            )
                            Toast.makeText(context, "Customer onboarded! Auto-Generated CIF: ${c.cif}", Toast.LENGTH_LONG).show()
                            name = ""
                            businessName = ""
                            contact = ""
                            aadhar = ""
                            pan = ""
                        },
                        modifier = Modifier.fillMaxWidth().testTag("submit_onboard_customer_button")
                    ) {
                        Text("Generate CIF & Register Customer")
                    }
                }
            }
        }
    }
}

@Composable
private fun ManagerDailyBookCreationSection(
    repository: LocalFinanceRepository,
    customers: List<com.example.data.model.Customer>,
    dailyBooks: List<com.example.data.model.DailyDepositBook>
) {
    val context = LocalContext.current
    var selectedCif by remember { mutableStateOf("") }

    val approvedCustomers = customers.filter { it.status == ApprovalStatus.APPROVED.name }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Daily Recurring Deposit Book Creation", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text("Create a recurring collection book under a specific Customer CIF (Auto 6-Digit Book Number).", style = MaterialTheme.typography.bodySmall)

                    OutlinedTextField(
                        value = selectedCif,
                        onValueChange = { selectedCif = it },
                        label = { Text("Customer 5-Digit CIF Number") },
                        modifier = Modifier.fillMaxWidth().testTag("book_cif_input"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )

                    if (approvedCustomers.isNotEmpty()) {
                        Text("Quick Select Customer CIF:", style = MaterialTheme.typography.labelSmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            for (cust in approvedCustomers.take(4)) {
                                OutlinedButton(
                                    onClick = { selectedCif = cust.cif },
                                    modifier = Modifier.testTag("quick_cif_${cust.cif}")
                                ) {
                                    Text(cust.cif)
                                }
                            }
                        }
                    }

                    Button(
                        onClick = {
                            if (selectedCif.isBlank()) {
                                Toast.makeText(context, "Enter a valid CIF number", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            try {
                                val b = repository.createDailyDepositBook(selectedCif.trim())
                                Toast.makeText(context, "Daily Book #${b.bookNumber} created for CIF ${b.customerCif}!", Toast.LENGTH_LONG).show()
                                selectedCif = ""
                            } catch (e: Exception) {
                                Toast.makeText(context, e.message ?: "Failed to create book", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("submit_create_book_button")
                    ) {
                        Text("Generate Book Number & Open Book")
                    }
                }
            }
        }

        item {
            Text("Active Daily Books (${dailyBooks.size})", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
        }

        items(dailyBooks) { book ->
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Book, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Book #${book.bookNumber}", fontWeight = FontWeight.Bold)
                        Text("${book.customerName} (CIF: ${book.customerCif})", style = MaterialTheme.typography.bodySmall)
                        Text("Cumulative Deposit: Rs. ${book.cumulativeDeposit}", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@Composable
private fun ManagerCreateExecutiveSection(
    repository: LocalFinanceRepository,
    managerId: String
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var contact by remember { mutableStateOf("") }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Create Collection Executive Credential", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text("As Manager, you can create credentials for field Collection Executives who report directly to you.", style = MaterialTheme.typography.bodySmall)

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Executive Name (Must)") },
                        modifier = Modifier.fillMaxWidth().testTag("exec_name_input"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email Address (Must)") },
                        modifier = Modifier.fillMaxWidth().testTag("exec_email_input"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = contact,
                        onValueChange = { contact = it },
                        label = { Text("Contact Phone (Must)") },
                        modifier = Modifier.fillMaxWidth().testTag("exec_contact_input"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true
                    )

                    Button(
                        onClick = {
                            if (name.isBlank() || email.isBlank() || contact.isBlank()) {
                                Toast.makeText(context, "All fields are required!", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            repository.createStaffCredential(
                                name = name.trim(),
                                email = email.trim(),
                                contact = contact.trim(),
                                role = UserRole.COLLECTION_EXECUTIVE,
                                assignedManagerId = managerId
                            )
                            Toast.makeText(context, "Executive credential created under you!", Toast.LENGTH_SHORT).show()
                            name = ""
                            email = ""
                            contact = ""
                        },
                        modifier = Modifier.fillMaxWidth().testTag("submit_create_exec_button")
                    ) {
                        Text("Create Executive Account")
                    }
                }
            }
        }
    }
}

@Composable
private fun ManagerCustomerDepositsSection(
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
                    Text("Customer-Wise Cumulative Deposits", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "Inspect individual customer passbooks, active daily recurring deposit books, and verified deposit balances.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
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
                                                "Manager View"
                                            )
                                            Toast.makeText(context, "Customer Summary PDF generated!", Toast.LENGTH_SHORT).show()
                                            ReportExporter.shareFile(context, pdfFile, "application/pdf")
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Export error: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f).testTag("manager_download_all_customers_pdf_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Summary PDF", fontSize = 11.sp)
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
                            modifier = Modifier.weight(1f).testTag("manager_download_all_customers_excel_button")
                        ) {
                            Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Excel / CSV", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search by Customer Name, CIF, or Business") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().testTag("manager_customer_search_input"),
                singleLine = true
            )
        }

        item {
            val totalDeposit = customerSummaries.sumOf { it.totalDepositAmount }
            val totalPayments = customerSummaries.sumOf { it.collectionCount }
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("${customerSummaries.size} Customers • $totalPayments Collections Recorded", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = "Cumulative Balance: Rs. $totalDeposit",
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        if (customerSummaries.isEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No matching customer deposit records found.", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                            Text("Contact: ${sum.customer.contact} • Aadhar: ${sum.customer.aadharNumber}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

                    // Passbook & individual export actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { onSelectPassbook(sum) },
                            modifier = Modifier.weight(1f).testTag("manager_view_passbook_${sum.customer.cif}"),
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

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
