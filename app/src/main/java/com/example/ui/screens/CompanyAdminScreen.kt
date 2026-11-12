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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
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
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
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
import com.example.data.model.ApprovalStatus
import com.example.data.model.UserRole
import com.example.data.repository.LocalFinanceRepository
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CompanyAdminScreen(repository: LocalFinanceRepository) {
    val context = LocalContext.current
    val currentCompany by repository.currentCompany.collectAsState()
    val staffList by repository.staffList.collectAsState()
    val partners by repository.partners.collectAsState()
    val partnerDeposits by repository.partnerDeposits.collectAsState()
    val customers by repository.customers.collectAsState()
    val dailyBooks by repository.dailyBooks.collectAsState()

    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Company Info", "Partners & Deposits", "Staff Credentials", "Approvals")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Company Administration",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = currentCompany?.name ?: "No Company Selected",
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
            0 -> CompanyInfoSection(repository, currentCompany)
            1 -> PartnersAndDepositsSection(repository, partners, partnerDeposits)
            2 -> StaffCredentialsSection(repository, staffList)
            3 -> ApprovalsSection(repository, customers, dailyBooks)
        }
    }
}

@Composable
private fun CompanyInfoSection(
    repository: LocalFinanceRepository,
    currentCompany: com.example.data.model.Company?
) {
    val context = LocalContext.current
    var editMode by remember { mutableStateOf(false) }
    var name by remember(currentCompany) { mutableStateOf(currentCompany?.name ?: "") }
    var address by remember(currentCompany) { mutableStateOf(currentCompany?.address ?: "") }
    var contact by remember(currentCompany) { mutableStateOf(currentCompany?.contact ?: "") }
    var logoUrl by remember(currentCompany) { mutableStateOf(currentCompany?.logoUrl ?: "") }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Company Profile",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { editMode = !editMode }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Company")
                        }
                    }

                    if (editMode) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Company Name (Editable)") },
                            modifier = Modifier.fillMaxWidth().testTag("admin_company_name_input"),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = address,
                            onValueChange = { address = it },
                            label = { Text("Company Address (Editable)") },
                            modifier = Modifier.fillMaxWidth().testTag("admin_company_address_input")
                        )
                        OutlinedTextField(
                            value = contact,
                            onValueChange = { contact = it },
                            label = { Text("Contact Number (Editable)") },
                            modifier = Modifier.fillMaxWidth().testTag("admin_company_contact_input"),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = logoUrl,
                            onValueChange = { logoUrl = it },
                            label = { Text("Company Logo URL / Asset Name") },
                            modifier = Modifier.fillMaxWidth().testTag("admin_company_logo_input"),
                            singleLine = true
                        )

                        Button(
                            onClick = {
                                repository.updateCompanyDetails(name.trim(), address.trim(), contact.trim(), logoUrl.trim())
                                editMode = false
                                Toast.makeText(context, "Company profile saved!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth().testTag("save_company_profile_button")
                        ) {
                            Text("Save Changes")
                        }
                    } else {
                        ProfileAttribute("Name", currentCompany?.name ?: "")
                        ProfileAttribute("Address", currentCompany?.address ?: "No address set")
                        ProfileAttribute("Contact", currentCompany?.contact ?: "No contact set")
                        ProfileAttribute("Sequence Ranges", "PIN Start: ${currentCompany?.pinStart} | CIF Start: ${currentCompany?.cifStart} | Book Start: ${currentCompany?.bookStart}")
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileAttribute(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun PartnersAndDepositsSection(
    repository: LocalFinanceRepository,
    partners: List<com.example.data.model.Partner>,
    deposits: List<com.example.data.model.PartnerDeposit>
) {
    val context = LocalContext.current
    var showPartnerForm by remember { mutableStateOf(false) }
    var showDepositForm by remember { mutableStateOf(false) }

    // Partner form state
    var partnerName by remember { mutableStateOf("") }
    var partnerContact by remember { mutableStateOf("") }
    var partnerAadhar by remember { mutableStateOf("") }

    // Deposit form state
    var depositPin by remember { mutableStateOf("") }
    var depositAmount by remember { mutableStateOf("") }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { showPartnerForm = !showPartnerForm },
                    modifier = Modifier.weight(1f).testTag("toggle_add_partner_button")
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (showPartnerForm) "Close" else "Add Partner")
                }
                OutlinedButton(
                    onClick = { showDepositForm = !showDepositForm },
                    modifier = Modifier.weight(1.1f).testTag("toggle_partner_deposit_button")
                ) {
                    Icon(Icons.Default.CreditCard, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (showDepositForm) "Close" else "Partner Deposit")
                }
            }
        }

        // Onboard Partner Form
        if (showPartnerForm) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Partner Onboarding (Auto 4-Digit PIN)", fontWeight = FontWeight.Bold)
                        OutlinedTextField(
                            value = partnerName,
                            onValueChange = { partnerName = it },
                            label = { Text("Name (Must)") },
                            modifier = Modifier.fillMaxWidth().testTag("partner_name_input"),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = partnerContact,
                            onValueChange = { partnerContact = it },
                            label = { Text("Contact Number (Must)") },
                            modifier = Modifier.fillMaxWidth().testTag("partner_contact_input"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = partnerAadhar,
                            onValueChange = { partnerAadhar = it },
                            label = { Text("Aadhar Number (Must)") },
                            modifier = Modifier.fillMaxWidth().testTag("partner_aadhar_input"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                        Button(
                            onClick = {
                                if (partnerName.isBlank() || partnerContact.isBlank() || partnerAadhar.isBlank()) {
                                    Toast.makeText(context, "All fields are required", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                val p = repository.onboardPartner(partnerName.trim(), partnerContact.trim(), partnerAadhar.trim())
                                Toast.makeText(context, "Partner created! Assigned PIN: ${p.pin}", Toast.LENGTH_LONG).show()
                                partnerName = ""
                                partnerContact = ""
                                partnerAadhar = ""
                                showPartnerForm = false
                            },
                            modifier = Modifier.fillMaxWidth().testTag("submit_partner_button")
                        ) {
                            Text("Generate PIN & Save Partner")
                        }
                    }
                }
            }
        }

        // Partner Deposit Form
        if (showDepositForm) {
            item {
                val liveDateTime = remember {
                    SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault()).format(Date())
                }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Partner Deposit Form", fontWeight = FontWeight.Bold)
                        OutlinedTextField(
                            value = depositPin,
                            onValueChange = { depositPin = it },
                            label = { Text("Enter 4-Digit PIN") },
                            modifier = Modifier.fillMaxWidth().testTag("deposit_pin_input"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = depositAmount,
                            onValueChange = { depositAmount = it },
                            label = { Text("Amount (INR)") },
                            modifier = Modifier.fillMaxWidth().testTag("deposit_amount_input"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                        Text(
                            text = "Live Date & Time: $liveDateTime (Auto Selected)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = {
                                val amt = depositAmount.toDoubleOrNull()
                                if (depositPin.isBlank() || amt == null || amt <= 0) {
                                    Toast.makeText(context, "Enter valid PIN and amount", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                try {
                                    val dep = repository.depositPartnerEquity(depositPin.trim(), amt)
                                    Toast.makeText(context, "Deposit of Rs. ${dep.amount} logged for ${dep.partnerName}!", Toast.LENGTH_LONG).show()
                                    depositPin = ""
                                    depositAmount = ""
                                    showDepositForm = false
                                } catch (e: Exception) {
                                    Toast.makeText(context, e.message ?: "Deposit failed", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth().testTag("submit_deposit_button")
                        ) {
                            Text("Record Deposit")
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "Registered Partners (${partners.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        items(partners) { p ->
            val totalDeposited = deposits.filter { it.partnerPin == p.pin }.sumOf { it.amount }
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("PIN\n${p.pin}", fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(p.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                        Text("Ph: ${p.contact} • Aadhar: ${p.aadharNumber}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = "Total Investment: Rs. $totalDeposited",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun StaffCredentialsSection(
    repository: LocalFinanceRepository,
    staffList: List<com.example.data.model.UserProfile>
) {
    val context = LocalContext.current
    var showForm by remember { mutableStateOf(false) }
    var staffName by remember { mutableStateOf("") }
    var staffEmail by remember { mutableStateOf("") }
    var staffContact by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf(UserRole.COLLECTION_EXECUTIVE) }
    var selectedManagerId by remember { mutableStateOf("") }

    val managers = staffList.filter { it.role == UserRole.MANAGER.name }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Staff Roster & Credentials",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Button(
                    onClick = { showForm = !showForm },
                    modifier = Modifier.testTag("toggle_create_staff_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (showForm) "Close" else "New Staff")
                }
            }
        }

        if (showForm) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Create Staff Credentials", fontWeight = FontWeight.Bold)

                        OutlinedTextField(
                            value = staffName,
                            onValueChange = { staffName = it },
                            label = { Text("Staff Full Name") },
                            modifier = Modifier.fillMaxWidth().testTag("staff_name_input"),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = staffEmail,
                            onValueChange = { staffEmail = it },
                            label = { Text("Email Address") },
                            modifier = Modifier.fillMaxWidth().testTag("staff_email_input"),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = staffContact,
                            onValueChange = { staffContact = it },
                            label = { Text("Phone Number") },
                            modifier = Modifier.fillMaxWidth().testTag("staff_contact_input"),
                            singleLine = true
                        )

                        // Role Selector
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(
                                onClick = { selectedRole = UserRole.MANAGER },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (selectedRole == UserRole.MANAGER) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                                )
                            ) {
                                Text("Manager")
                            }
                            OutlinedButton(
                                onClick = { selectedRole = UserRole.COLLECTION_EXECUTIVE },
                                modifier = Modifier.weight(1.3f),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (selectedRole == UserRole.COLLECTION_EXECUTIVE) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                                )
                            ) {
                                Text("Collection Executive")
                            }
                        }

                        // Mandatory Manager Selection for Collection Executive
                        if (selectedRole == UserRole.COLLECTION_EXECUTIVE) {
                            Text(
                                text = "Select Assigned Manager (Mandatory)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (managers.isEmpty()) {
                                Text(
                                    text = "No Manager available. Please create a Manager credential first.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    for (mgr in managers) {
                                        val isSelected = selectedManagerId == mgr.id
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(10.dp))
                                                .clickable { selectedManagerId = mgr.id }
                                                .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface)
                                                .border(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "${mgr.name} (${mgr.contact})",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Button(
                            onClick = {
                                if (staffName.isBlank() || staffEmail.isBlank()) {
                                    Toast.makeText(context, "Name and Email are required", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                if (selectedRole == UserRole.COLLECTION_EXECUTIVE && selectedManagerId.isBlank()) {
                                    Toast.makeText(context, "Must select an assigned Manager for Collection Executive!", Toast.LENGTH_LONG).show()
                                    return@Button
                                }
                                repository.createStaffCredential(
                                    name = staffName.trim(),
                                    email = staffEmail.trim(),
                                    contact = staffContact.trim(),
                                    role = selectedRole,
                                    assignedManagerId = selectedManagerId
                                )
                                staffName = ""
                                staffEmail = ""
                                staffContact = ""
                                selectedManagerId = ""
                                showForm = false
                                Toast.makeText(context, "Staff credential created successfully!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth().testTag("submit_staff_credential_button")
                        ) {
                            Text("Create Staff Account")
                        }
                    }
                }
            }
        }

        items(staffList) { staff ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(staff.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = staff.role.replace("_", " "),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Text("${staff.email} • ${staff.contact}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (staff.assignedManagerName.isNotBlank()) {
                            Text("Manager: ${staff.assignedManagerName}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun ApprovalsSection(
    repository: LocalFinanceRepository,
    customers: List<com.example.data.model.Customer>,
    dailyBooks: List<com.example.data.model.DailyDepositBook>
) {
    val pendingCustomers = customers.filter { it.status == ApprovalStatus.PENDING.name }
    val pendingBooks = dailyBooks.filter { it.status == ApprovalStatus.PENDING.name }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text(
                text = "Pending Approvals",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        if (pendingCustomers.isEmpty() && pendingBooks.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("All customers and daily books are approved!", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        if (pendingCustomers.isNotEmpty()) {
            item {
                Text("Pending Customers (${pendingCustomers.size})", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
            }
            items(pendingCustomers) { cust ->
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(cust.name, fontWeight = FontWeight.Bold)
                            Text("CIF: ${cust.cif} • Biz: ${cust.businessName}", style = MaterialTheme.typography.bodySmall)
                            Text("Onboarded by: ${cust.onboardedByName} (${cust.onboardedByRole})", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Button(
                            onClick = { repository.approveCustomer(cust.id) },
                            modifier = Modifier.testTag("approve_customer_${cust.cif}")
                        ) {
                            Text("Approve")
                        }
                    }
                }
            }
        }

        if (pendingBooks.isNotEmpty()) {
            item {
                Text("Pending Daily Books (${pendingBooks.size})", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.secondary)
            }
            items(pendingBooks) { book ->
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Book #${book.bookNumber}", fontWeight = FontWeight.Bold)
                            Text("Customer: ${book.customerName} (CIF: ${book.customerCif})", style = MaterialTheme.typography.bodySmall)
                            Text("Created by Manager: ${book.createdByManagerName}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Button(
                            onClick = { repository.approveDailyBook(book.id) },
                            modifier = Modifier.testTag("approve_book_${book.bookNumber}")
                        ) {
                            Text("Approve")
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
