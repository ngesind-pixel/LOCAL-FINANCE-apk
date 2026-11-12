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
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.LocalFinanceRepository
import java.text.NumberFormat
import java.util.Locale

@Composable
fun DeveloperScreen(repository: LocalFinanceRepository) {
    val context = LocalContext.current
    val companies by repository.companies.collectAsState()
    val currentCompany by repository.currentCompany.collectAsState()
    val staffList by repository.staffList.collectAsState()
    val partners by repository.partners.collectAsState()
    val partnerDeposits by repository.partnerDeposits.collectAsState()
    val customers by repository.customers.collectAsState()
    val dailyBooks by repository.dailyBooks.collectAsState()
    val collections by repository.collections.collectAsState()

    var showCreateForm by remember { mutableStateOf(false) }
    var isSyncing by remember { mutableStateOf(false) }

    var compName by remember { mutableStateOf("") }
    var compAddress by remember { mutableStateOf("") }
    var compContact by remember { mutableStateOf("") }
    var pinStart by remember { mutableStateOf("1001") }
    var cifStart by remember { mutableStateOf("20001") }
    var bookStart by remember { mutableStateOf("500001") }

    val totalCapitalAcrossCompanies = partnerDeposits.sumOf { it.amount }
    val totalCollectionsAcrossCompanies = collections.sumOf { it.depositAmount }

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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Terminal,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Developer Backend Console",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Text(
                        text = "Master Root Access • Multi-Company Provisioning",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = { showCreateForm = !showCreateForm },
                    modifier = Modifier.testTag("toggle_create_company_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (showCreateForm) "Hide Form" else "New Company")
                }
            }
        }

        // 1. GLOBAL SYSTEM METRICS (Across all companies in backend)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "GLOBAL BACKEND DATABASE TOTALS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        MetricBox(
                            label = "COMPANIES",
                            value = "${companies.size}",
                            modifier = Modifier.weight(1f)
                        )
                        MetricBox(
                            label = "CUSTOMERS",
                            value = "${customers.size}",
                            modifier = Modifier.weight(1f)
                        )
                        MetricBox(
                            label = "STAFF",
                            value = "${staffList.size}",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        MetricBox(
                            label = "TOTAL CAPITAL INVESTED",
                            value = NumberFormat.getCurrencyInstance(Locale("en", "IN")).format(totalCapitalAcrossCompanies).replace("₹", "Rs. "),
                            modifier = Modifier.weight(1f)
                        )
                        MetricBox(
                            label = "ALL-TIME COLLECTIONS",
                            value = NumberFormat.getCurrencyInstance(Locale("en", "IN")).format(totalCollectionsAcrossCompanies).replace("₹", "Rs. "),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // 2. CLOUD DATABASE SYNC ACTION
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Dns, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Firestore Cloud Sync", fontWeight = FontWeight.Bold)
                        }
                        Text(
                            text = "Push companies, staff, and collection ledger to Firestore backend.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = {
                            isSyncing = true
                            repository.syncAllToFirestore { success, msg ->
                                isSyncing = false
                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                            }
                        },
                        enabled = !isSyncing,
                        modifier = Modifier.testTag("sync_firestore_backend_button")
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Sync Cloud")
                        }
                    }
                }
            }
        }

        // 3. CREATE NEW COMPANY FORM
        if (showCreateForm) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Register New Collection Company (Backend)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )

                        OutlinedTextField(
                            value = compName,
                            onValueChange = { compName = it },
                            label = { Text("Company Name (Mandatory)") },
                            modifier = Modifier.fillMaxWidth().testTag("company_name_input"),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = compAddress,
                            onValueChange = { compAddress = it },
                            label = { Text("Company Address") },
                            modifier = Modifier.fillMaxWidth().testTag("company_address_input"),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = compContact,
                            onValueChange = { compContact = it },
                            label = { Text("Company Phone / Helpline") },
                            modifier = Modifier.fillMaxWidth().testTag("company_contact_input"),
                            singleLine = true
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = pinStart,
                                onValueChange = { pinStart = it },
                                label = { Text("PIN Start (4-dig)") },
                                modifier = Modifier.weight(1f).testTag("pin_start_input"),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = cifStart,
                                onValueChange = { cifStart = it },
                                label = { Text("CIF Start (5-dig)") },
                                modifier = Modifier.weight(1f).testTag("cif_start_input"),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = bookStart,
                                onValueChange = { bookStart = it },
                                label = { Text("Book Start (6-dig)") },
                                modifier = Modifier.weight(1.2f).testTag("book_start_input"),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true
                            )
                        }

                        Button(
                            onClick = {
                                if (compName.isBlank()) {
                                    Toast.makeText(context, "Company name is required", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                repository.createCompany(
                                    name = compName.trim(),
                                    address = compAddress.trim(),
                                    contact = compContact.trim(),
                                    pinStart = pinStart.toLongOrNull() ?: 1001L,
                                    cifStart = cifStart.toLongOrNull() ?: 20001L,
                                    bookStart = bookStart.toLongOrNull() ?: 500001L
                                )
                                compName = ""
                                compAddress = ""
                                compContact = ""
                                showCreateForm = false
                                Toast.makeText(context, "Company provisioned in backend!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth().testTag("submit_company_button")
                        ) {
                            Text("Provision Company in Backend")
                        }
                    }
                }
            }
        }

        // 4. REGISTERED COMPANIES MANAGEMENT
        item {
            Text(
                text = "Backend Managed Companies (${companies.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        items(companies) { comp ->
            val isCurrent = comp.id == currentCompany?.id
            val isSuspended = comp.status == "SUSPENDED"

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { repository.selectCompany(comp.id) }
                    .border(
                        width = if (isCurrent) 2.dp else 1.dp,
                        color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                        shape = RoundedCornerShape(18.dp)
                    ),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isCurrent)
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                    else
                        MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSuspended) Color(0xFFEF4444).copy(alpha = 0.15f)
                                    else if (isCurrent) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Business,
                                contentDescription = null,
                                tint = if (isSuspended) Color(0xFFEF4444)
                                else if (isCurrent) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = comp.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = comp.status,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSuspended) Color(0xFFEF4444) else Color(0xFF10B981),
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (isSuspended) Color(0xFFEF4444).copy(alpha = 0.15f) else Color(0xFF10B981).copy(alpha = 0.15f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }

                            if (comp.address.isNotBlank()) {
                                Text(
                                    text = comp.address,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (isCurrent) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "Active Selection",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "PIN: ${comp.pinStart} • CIF: ${comp.cifStart} • Book: ${comp.bookStart}",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    repository.toggleCompanyStatus(comp.id)
                                    Toast.makeText(context, "Company status updated to ${if (isSuspended) "ACTIVE" else "SUSPENDED"}", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.testTag("toggle_status_${comp.id}")
                            ) {
                                Icon(
                                    if (isSuspended) Icons.Default.PlayCircle else Icons.Default.PauseCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (isSuspended) "Activate" else "Suspend", fontSize = 11.sp)
                            }

                            if (!isCurrent) {
                                Button(
                                    onClick = { repository.selectCompany(comp.id) },
                                    modifier = Modifier.testTag("select_company_${comp.id}")
                                ) {
                                    Text("Switch", fontSize = 11.sp)
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
private fun MetricBox(label: String, value: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(12.dp)
    ) {
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}
