package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.R
import com.example.auth.FirebaseAuthManager
import com.example.data.model.ApprovalStatus
import com.example.data.model.Company
import com.example.data.model.Customer
import com.example.data.model.CustomerDepositSummary
import com.example.data.model.DailyCollectionRecord
import com.example.data.model.DailyDepositBook
import com.example.data.model.Partner
import com.example.data.model.PartnerDeposit
import com.example.data.model.UserProfile
import com.example.data.model.UserRole
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

object FirestoreCollections {
    const val COMPANIES = "Companies"
    const val USERS = "Users"
    const val CUSTOMERS = "Customers"
    const val PARTNERS = "Partners"
    const val DAILY_BOOKS = "DailyBooks"
    const val PARTNER_DEPOSITS = "PartnerDeposits"
    const val DAILY_COLLECTIONS = "DailyCollections"
}

class LocalFinanceRepository(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.IO)
    private val auth = Firebase.auth

    // Resolve custom Firestore database from firebase_applet_config.xml
    private val db: FirebaseFirestore by lazy {
        try {
            val dbId = context.getString(R.string.firestore_database_id)
            FirebaseFirestore.getInstance(dbId)
        } catch (e: Exception) {
            Log.e("LocalFinanceRepo", "Falling back to default Firestore: ${e.message}")
            FirebaseFirestore.getInstance()
        }
    }

    private suspend fun writeCompany(comp: Company) {
        try {
            db.collection(FirestoreCollections.COMPANIES).document(comp.id).set(comp).await()
            db.collection("companies").document(comp.id).set(comp).await()
        } catch (_: Exception) {}
    }

    private suspend fun writeUser(user: UserProfile) {
        try {
            db.collection(FirestoreCollections.USERS).document(user.id).set(user).await()
            db.collection("users").document(user.id).set(user).await()
        } catch (_: Exception) {}
    }

    private suspend fun writePartner(partner: Partner) {
        try {
            db.collection(FirestoreCollections.PARTNERS).document(partner.id).set(partner).await()
            db.collection("partners").document(partner.id).set(partner).await()
        } catch (_: Exception) {}
    }

    private suspend fun writePartnerDeposit(deposit: PartnerDeposit) {
        try {
            db.collection(FirestoreCollections.PARTNER_DEPOSITS).document(deposit.id).set(deposit).await()
            db.collection("partner_deposits").document(deposit.id).set(deposit).await()
        } catch (_: Exception) {}
    }

    private suspend fun writeCustomer(customer: Customer) {
        try {
            db.collection(FirestoreCollections.CUSTOMERS).document(customer.id).set(customer).await()
            db.collection("customers").document(customer.id).set(customer).await()
        } catch (_: Exception) {}
    }

    private suspend fun writeDailyBook(book: DailyDepositBook) {
        try {
            db.collection(FirestoreCollections.DAILY_BOOKS).document(book.id).set(book).await()
            db.collection("daily_deposit_books").document(book.id).set(book).await()
        } catch (_: Exception) {}
    }

    private suspend fun writeDailyCollection(col: DailyCollectionRecord) {
        try {
            db.collection(FirestoreCollections.DAILY_COLLECTIONS).document(col.id).set(col).await()
            db.collection("daily_collections").document(col.id).set(col).await()
        } catch (_: Exception) {}
    }

    // --- In-Memory Reactive State with Seed Data ---

    private val _companies = MutableStateFlow<List<Company>>(emptyList())
    val companies: StateFlow<List<Company>> = _companies.asStateFlow()

    private val _currentCompany = MutableStateFlow<Company?>(null)
    val currentCompany: StateFlow<Company?> = _currentCompany.asStateFlow()

    val authManager: FirebaseAuthManager = FirebaseAuthManager(context)
    val currentProfile: StateFlow<UserProfile> = authManager.currentProfile

    private val _staffList = MutableStateFlow<List<UserProfile>>(emptyList())
    val staffList: StateFlow<List<UserProfile>> = _staffList.asStateFlow()

    private val _partners = MutableStateFlow<List<Partner>>(emptyList())
    val partners: StateFlow<List<Partner>> = _partners.asStateFlow()

    private val _partnerDeposits = MutableStateFlow<List<PartnerDeposit>>(emptyList())
    val partnerDeposits: StateFlow<List<PartnerDeposit>> = _partnerDeposits.asStateFlow()

    private val _customers = MutableStateFlow<List<Customer>>(emptyList())
    val customers: StateFlow<List<Customer>> = _customers.asStateFlow()

    private val _dailyBooks = MutableStateFlow<List<DailyDepositBook>>(emptyList())
    val dailyBooks: StateFlow<List<DailyDepositBook>> = _dailyBooks.asStateFlow()

    private val _collections = MutableStateFlow<List<DailyCollectionRecord>>(emptyList())
    val collections: StateFlow<List<DailyCollectionRecord>> = _collections.asStateFlow()

    init {
        seedInitialData()
        startFirestoreSyncIfAuthenticated()
    }

    private fun seedInitialData() {
        val defaultCompany = Company(
            id = "comp_1",
            name = "BHARAT MICRO FINANCE",
            address = "Shop 14, Main Market, MG Road, Pune",
            contact = "+91 20 2567 8900",
            logoUrl = "",
            pinStart = 1001L,
            cifStart = 20001L,
            bookStart = 500001L,
            managerCanApproveCustomer = true,
            managerCanApproveBook = true,
            createdBy = "dev_1"
        )
        val secondCompany = Company(
            id = "comp_2",
            name = "SWADESHI DAILY CREDIT CO.",
            address = "Plot 88, Commercial Complex, Surat",
            contact = "+91 261 245 7800",
            logoUrl = "",
            pinStart = 1101L,
            cifStart = 21001L,
            bookStart = 510001L,
            managerCanApproveCustomer = true,
            managerCanApproveBook = true,
            createdBy = "dev_1"
        )
        _companies.value = listOf(defaultCompany, secondCompany)
        _currentCompany.value = defaultCompany

        val mgr1 = UserProfile(
            id = "mgr_1",
            userId = "mgr_1",
            email = "manager.pune@localfinance.com",
            name = "Amit Deshmukh",
            role = UserRole.MANAGER.name,
            companyId = defaultCompany.id,
            contact = "+91 94220 11223"
        )
        val exec1 = UserProfile(
            id = "exec_1",
            userId = "exec_1",
            email = "vikram.field@localfinance.com",
            name = "Vikram Patil",
            role = UserRole.COLLECTION_EXECUTIVE.name,
            companyId = defaultCompany.id,
            contact = "+91 98230 44556",
            assignedManagerId = "mgr_1",
            assignedManagerName = "Amit Deshmukh"
        )
        val exec2 = UserProfile(
            id = "exec_2",
            userId = "exec_2",
            email = "rahul.field@localfinance.com",
            name = "Rahul Kulkarni",
            role = UserRole.COLLECTION_EXECUTIVE.name,
            companyId = defaultCompany.id,
            contact = "+91 97654 33221",
            assignedManagerId = "mgr_1",
            assignedManagerName = "Amit Deshmukh"
        )
        _staffList.value = listOf(
            currentProfile.value,
            mgr1,
            exec1,
            exec2
        )

        // Seed Partners
        val p1 = Partner(
            id = "p_1",
            companyId = defaultCompany.id,
            name = "Suresh Agarwal",
            contact = "+91 98900 12345",
            aadharNumber = "4589 1234 5678",
            pin = "1001"
        )
        val p2 = Partner(
            id = "p_2",
            companyId = defaultCompany.id,
            name = "Mahesh Shinde",
            contact = "+91 98901 67890",
            aadharNumber = "7812 3456 9012",
            pin = "1002"
        )
        _partners.value = listOf(p1, p2)

        val dep1 = PartnerDeposit(
            id = "dep_1",
            companyId = defaultCompany.id,
            partnerPin = "1001",
            partnerName = "Suresh Agarwal",
            amount = 500000.0,
            depositDate = "2026-11-01",
            depositTime = "10:30 AM"
        )
        val dep2 = PartnerDeposit(
            id = "dep_2",
            companyId = defaultCompany.id,
            partnerPin = "1002",
            partnerName = "Mahesh Shinde",
            amount = 350000.0,
            depositDate = "2026-11-03",
            depositTime = "02:15 PM"
        )
        _partnerDeposits.value = listOf(dep1, dep2)

        // Seed Customers
        val c1 = Customer(
            id = "cust_1",
            companyId = defaultCompany.id,
            name = "Ganesh General Stores",
            businessName = "Ganesh Kirana",
            contact = "+91 91234 56789",
            aadharNumber = "2345 6789 0123",
            panNumber = "ABCDE1234F",
            cif = "20001",
            status = ApprovalStatus.APPROVED.name,
            onboardedByRole = UserRole.COLLECTION_EXECUTIVE.name,
            onboardedById = "exec_1",
            onboardedByName = "Vikram Patil",
            approvedBy = "Amit Deshmukh (Manager)"
        )
        val c2 = Customer(
            id = "cust_2",
            companyId = defaultCompany.id,
            name = "Sai Garments & Hosiery",
            businessName = "Sai Textiles",
            contact = "+91 92345 67890",
            aadharNumber = "3456 7890 1234",
            panNumber = "FGHIJ5678K",
            cif = "20002",
            status = ApprovalStatus.APPROVED.name,
            onboardedByRole = UserRole.COLLECTION_EXECUTIVE.name,
            onboardedById = "exec_1",
            onboardedByName = "Vikram Patil",
            approvedBy = "Amit Deshmukh (Manager)"
        )
        val c3 = Customer(
            id = "cust_3",
            companyId = defaultCompany.id,
            name = "Laxmi Sweets & Dairy",
            businessName = "Laxmi Dairy",
            contact = "+91 93456 78901",
            aadharNumber = "4567 8901 2345",
            panNumber = "KLMNO9012P",
            cif = "20003",
            status = ApprovalStatus.APPROVED.name,
            onboardedByRole = UserRole.MANAGER.name,
            onboardedById = "mgr_1",
            onboardedByName = "Amit Deshmukh",
            approvedBy = "Rajesh Sharma (Admin)"
        )
        _customers.value = listOf(c1, c2, c3)

        // Seed Daily Deposit Books
        val b1 = DailyDepositBook(
            id = "book_1",
            companyId = defaultCompany.id,
            customerCif = "20001",
            customerName = "Ganesh General Stores",
            customerContact = "+91 91234 56789",
            bookNumber = "500001",
            createdByManagerId = "mgr_1",
            createdByManagerName = "Amit Deshmukh",
            status = ApprovalStatus.APPROVED.name,
            cumulativeDeposit = 15400.0
        )
        val b2 = DailyDepositBook(
            id = "book_2",
            companyId = defaultCompany.id,
            customerCif = "20002",
            customerName = "Sai Garments & Hosiery",
            customerContact = "+91 92345 67890",
            bookNumber = "500002",
            createdByManagerId = "mgr_1",
            createdByManagerName = "Amit Deshmukh",
            status = ApprovalStatus.APPROVED.name,
            cumulativeDeposit = 9800.0
        )
        val b3 = DailyDepositBook(
            id = "book_3",
            companyId = defaultCompany.id,
            customerCif = "20003",
            customerName = "Laxmi Sweets & Dairy",
            customerContact = "+91 93456 78901",
            bookNumber = "500003",
            createdByManagerId = "mgr_1",
            createdByManagerName = "Amit Deshmukh",
            status = ApprovalStatus.APPROVED.name,
            cumulativeDeposit = 22600.0
        )
        _dailyBooks.value = listOf(b1, b2, b3)

        // Seed Daily Collections
        val yesterdayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(
            Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000L)
        )
        val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        val col1 = DailyCollectionRecord(
            id = "col_1",
            companyId = defaultCompany.id,
            receiptNumber = "RCP-20261105-0101",
            customerName = "Ganesh General Stores",
            cif = "20001",
            bookNumber = "500001",
            depositAmount = 500.0,
            cumulativeTotal = 14900.0,
            collectedByExecutiveId = "exec_1",
            collectedByExecutiveName = "Vikram Patil",
            managerId = "mgr_1",
            managerCollectedMarker = true,
            collectedDate = yesterdayDate,
            collectedTime = "11:20 AM"
        )
        val col2 = DailyCollectionRecord(
            id = "col_2",
            companyId = defaultCompany.id,
            receiptNumber = "RCP-20261105-0102",
            customerName = "Sai Garments & Hosiery",
            cif = "20002",
            bookNumber = "500002",
            depositAmount = 800.0,
            cumulativeTotal = 9500.0,
            collectedByExecutiveId = "exec_1",
            collectedByExecutiveName = "Vikram Patil",
            managerId = "mgr_1",
            managerCollectedMarker = true,
            collectedDate = yesterdayDate,
            collectedTime = "12:10 PM"
        )
        val col3 = DailyCollectionRecord(
            id = "col_3",
            companyId = defaultCompany.id,
            receiptNumber = "RCP-20261105-0103",
            customerName = "Laxmi Sweets & Dairy",
            cif = "20003",
            bookNumber = "500003",
            depositAmount = 1200.0,
            cumulativeTotal = 21400.0,
            collectedByExecutiveId = "exec_2",
            collectedByExecutiveName = "Rahul Kulkarni",
            managerId = "mgr_1",
            managerCollectedMarker = true,
            collectedDate = yesterdayDate,
            collectedTime = "04:30 PM"
        )
        val col4 = DailyCollectionRecord(
            id = "col_4",
            companyId = defaultCompany.id,
            receiptNumber = "RCP-20261106-0201",
            customerName = "Ganesh General Stores",
            cif = "20001",
            bookNumber = "500001",
            depositAmount = 500.0,
            cumulativeTotal = 15400.0,
            collectedByExecutiveId = "exec_1",
            collectedByExecutiveName = "Vikram Patil",
            managerId = "mgr_1",
            managerCollectedMarker = false, // Not yet handed over to manager
            collectedDate = todayDate,
            collectedTime = "09:45 AM"
        )
        _collections.value = listOf(col1, col2, col3, col4)
    }

    private fun startFirestoreSyncIfAuthenticated() {
        if (auth.currentUser == null) return
        scope.launch {
            try {
                // Fetch companies from primary Companies collection with fallback to companies
                var compDocs = db.collection(FirestoreCollections.COMPANIES).get().await()
                if (compDocs.isEmpty) {
                    compDocs = db.collection("companies").get().await()
                }
                if (!compDocs.isEmpty) {
                    val list = compDocs.toObjects(Company::class.java)
                    _companies.value = list
                    if (_currentCompany.value == null || !list.any { it.id == _currentCompany.value?.id }) {
                        _currentCompany.value = list.firstOrNull()
                    }
                }
            } catch (e: Exception) {
                Log.w("LocalFinanceRepo", "Firestore sync initial load notice: ${e.message}")
            }
        }
    }

    // --- Role Switching & Profile Selection ---

    fun switchProfile(role: UserRole, targetUser: UserProfile? = null) {
        val companyId = targetUser?.companyId ?: _currentCompany.value?.id ?: "comp_1"
        authManager.switchRoleAndCompany(role, companyId)
        val match = _staffList.value.firstOrNull { it.role == role.name && it.companyId == companyId }
        if (match == null) {
            val updated = currentProfile.value
            _staffList.value = _staffList.value + updated
        }
    }

    fun selectCompany(companyId: String) {
        val found = _companies.value.firstOrNull { it.id == companyId }
        if (found != null) {
            _currentCompany.value = found
            val currentRole = try {
                UserRole.valueOf(currentProfile.value.role)
            } catch (_: Exception) {
                UserRole.COMPANY_ADMIN
            }
            authManager.switchRoleAndCompany(currentRole, found.id)
        }
    }

    // --- Developer Actions ---

    fun createCompany(
        name: String,
        address: String,
        contact: String,
        pinStart: Long,
        cifStart: Long,
        bookStart: Long
    ): Company {
        val newComp = Company(
            id = "comp_" + UUID.randomUUID().toString().take(8),
            name = name,
            address = address,
            contact = contact,
            pinStart = pinStart,
            cifStart = cifStart,
            bookStart = bookStart,
            createdBy = currentProfile.value.userId
        )
        _companies.value = _companies.value + newComp
        if (_currentCompany.value == null) {
            _currentCompany.value = newComp
        }
        // Write to Firestore asynchronously
        scope.launch {
            writeCompany(newComp)
        }
        return newComp
    }

    // --- Company Admin Actions ---

    fun updateCompanyDetails(name: String, address: String, contact: String, logoUrl: String) {
        val current = _currentCompany.value ?: return
        val updated = current.copy(
            name = name,
            address = address,
            contact = contact,
            logoUrl = logoUrl
        )
        _currentCompany.value = updated
        _companies.value = _companies.value.map { if (it.id == updated.id) updated else it }
        scope.launch {
            writeCompany(updated)
        }
    }

    fun createStaffCredential(
        name: String,
        email: String,
        contact: String,
        role: UserRole,
        assignedManagerId: String = ""
    ): UserProfile {
        val companyId = _currentCompany.value?.id ?: "comp_1"
        val mgrName = _staffList.value.firstOrNull { it.id == assignedManagerId }?.name ?: ""
        val newStaff = UserProfile(
            id = "user_" + UUID.randomUUID().toString().take(8),
            userId = "user_" + UUID.randomUUID().toString().take(8),
            email = email,
            name = name,
            role = role.name,
            companyId = companyId,
            contact = contact,
            assignedManagerId = assignedManagerId,
            assignedManagerName = mgrName
        )
        _staffList.value = _staffList.value + newStaff
        scope.launch {
            writeUser(newStaff)
        }
        return newStaff
    }

    fun onboardPartner(name: String, contact: String, aadhar: String): Partner {
        val company = _currentCompany.value ?: error("No active company")
        val existingPins = _partners.value
            .filter { it.companyId == company.id }
            .mapNotNull { it.pin.toLongOrNull() }
        val nextPinNumber = if (existingPins.isEmpty()) company.pinStart else (existingPins.maxOrNull()!! + 1)
        val pinStr = String.format(Locale.US, "%04d", nextPinNumber)

        val newPartner = Partner(
            id = "partner_" + UUID.randomUUID().toString().take(8),
            companyId = company.id,
            name = name,
            contact = contact,
            aadharNumber = aadhar,
            pin = pinStr
        )
        _partners.value = _partners.value + newPartner
        scope.launch {
            writePartner(newPartner)
        }
        return newPartner
    }

    fun depositPartnerEquity(partnerPin: String, amount: Double): PartnerDeposit {
        val company = _currentCompany.value ?: error("No active company")
        val partner = _partners.value.firstOrNull { it.companyId == company.id && it.pin == partnerPin }
            ?: error("Partner with PIN $partnerPin not found")

        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val timeStr = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())

        val deposit = PartnerDeposit(
            id = "pdep_" + UUID.randomUUID().toString().take(8),
            companyId = company.id,
            partnerPin = partnerPin,
            partnerName = partner.name,
            amount = amount,
            depositDate = dateStr,
            depositTime = timeStr
        )
        _partnerDeposits.value = _partnerDeposits.value + deposit
        scope.launch {
            writePartnerDeposit(deposit)
        }
        return deposit
    }

    // --- Customer & Daily Book Onboarding ---

    fun onboardCustomer(
        name: String,
        businessName: String,
        contact: String,
        aadhar: String,
        pan: String
    ): Customer {
        val company = _currentCompany.value ?: error("No active company")
        val existingCifs = _customers.value
            .filter { it.companyId == company.id }
            .mapNotNull { it.cif.toLongOrNull() }
        val nextCifNumber = if (existingCifs.isEmpty()) company.cifStart else (existingCifs.maxOrNull()!! + 1)
        val cifStr = String.format(Locale.US, "%05d", nextCifNumber)

        val profile = currentProfile.value
        val autoApprove = profile.role == UserRole.COMPANY_ADMIN.name ||
                (profile.role == UserRole.MANAGER.name && company.managerCanApproveCustomer)

        val customer = Customer(
            id = "cust_" + UUID.randomUUID().toString().take(8),
            companyId = company.id,
            name = name,
            businessName = businessName,
            contact = contact,
            aadharNumber = aadhar,
            panNumber = pan,
            cif = cifStr,
            status = if (autoApprove) ApprovalStatus.APPROVED.name else ApprovalStatus.PENDING.name,
            onboardedByRole = profile.role,
            onboardedById = profile.id,
            onboardedByName = profile.name,
            approvedBy = if (autoApprove) "${profile.name} (${profile.role})" else ""
        )
        _customers.value = _customers.value + customer
        scope.launch {
            writeCustomer(customer)
        }
        return customer
    }

    fun approveCustomer(customerId: String) {
        val approver = currentProfile.value
        _customers.value = _customers.value.map {
            if (it.id == customerId) {
                it.copy(
                    status = ApprovalStatus.APPROVED.name,
                    approvedBy = "${approver.name} (${approver.role})"
                )
            } else it
        }
        scope.launch {
            try {
                val updates = mapOf(
                    "status" to ApprovalStatus.APPROVED.name,
                    "approvedBy" to "${approver.name} (${approver.role})"
                )
                db.collection(FirestoreCollections.CUSTOMERS).document(customerId).update(updates).await()
                db.collection("customers").document(customerId).update(updates).await()
            } catch (_: Exception) {}
        }
    }

    fun createDailyDepositBook(cif: String): DailyDepositBook {
        val company = _currentCompany.value ?: error("No active company")
        val customer = _customers.value.firstOrNull { it.companyId == company.id && it.cif == cif }
            ?: error("Customer with CIF $cif not found")

        val existingBooks = _dailyBooks.value
            .filter { it.companyId == company.id }
            .mapNotNull { it.bookNumber.toLongOrNull() }
        val nextBookNumber = if (existingBooks.isEmpty()) company.bookStart else (existingBooks.maxOrNull()!! + 1)
        val bookStr = String.format(Locale.US, "%06d", nextBookNumber)

        val manager = currentProfile.value
        val autoApprove = manager.role == UserRole.COMPANY_ADMIN.name ||
                (manager.role == UserRole.MANAGER.name && company.managerCanApproveBook)

        val book = DailyDepositBook(
            id = "book_" + UUID.randomUUID().toString().take(8),
            companyId = company.id,
            customerCif = cif,
            customerName = customer.name,
            customerContact = customer.contact,
            bookNumber = bookStr,
            createdByManagerId = manager.id,
            createdByManagerName = manager.name,
            status = if (autoApprove) ApprovalStatus.APPROVED.name else ApprovalStatus.PENDING.name,
            cumulativeDeposit = 0.0
        )
        _dailyBooks.value = _dailyBooks.value + book
        scope.launch {
            writeDailyBook(book)
        }
        return book
    }

    fun approveDailyBook(bookId: String) {
        _dailyBooks.value = _dailyBooks.value.map {
            if (it.id == bookId) it.copy(status = ApprovalStatus.APPROVED.name) else it
        }
        scope.launch {
            try {
                db.collection(FirestoreCollections.DAILY_BOOKS).document(bookId)
                    .update("status", ApprovalStatus.APPROVED.name).await()
                db.collection("daily_deposit_books").document(bookId)
                    .update("status", ApprovalStatus.APPROVED.name).await()
            } catch (_: Exception) {}
        }
    }

    // --- Daily Collection Submission & Reconciliation ---

    fun submitDailyCollection(bookNumber: String, amount: Double): DailyCollectionRecord {
        val company = _currentCompany.value ?: error("No active company")
        val book = _dailyBooks.value.firstOrNull { it.companyId == company.id && it.bookNumber == bookNumber }
            ?: error("Deposit Book #$bookNumber not found")

        val executive = currentProfile.value
        val todayStr = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
        val dateReadable = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val timeReadable = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())

        val dayCollectionCount = _collections.value.count { it.collectedDate == dateReadable } + 1
        val receiptNumber = "RCP-$todayStr-${String.format(Locale.US, "%04d", dayCollectionCount)}"

        val newCumulativeTotal = book.cumulativeDeposit + amount

        val record = DailyCollectionRecord(
            id = "col_" + UUID.randomUUID().toString().take(8),
            companyId = company.id,
            receiptNumber = receiptNumber,
            customerName = book.customerName,
            cif = book.customerCif,
            bookNumber = book.bookNumber,
            depositAmount = amount,
            cumulativeTotal = newCumulativeTotal,
            collectedByExecutiveId = executive.id,
            collectedByExecutiveName = executive.name,
            managerId = executive.assignedManagerId.ifBlank { book.createdByManagerId },
            managerCollectedMarker = false,
            collectedDate = dateReadable,
            collectedTime = timeReadable
        )

        // Update book cumulative deposit
        _dailyBooks.value = _dailyBooks.value.map {
            if (it.id == book.id) it.copy(cumulativeDeposit = newCumulativeTotal) else it
        }
        _collections.value = _collections.value + record

        scope.launch {
            writeDailyCollection(record)
            try {
                db.collection(FirestoreCollections.DAILY_BOOKS).document(book.id)
                    .update("cumulativeDeposit", FieldValue.increment(amount)).await()
                db.collection("daily_deposit_books").document(book.id)
                    .update("cumulativeDeposit", FieldValue.increment(amount)).await()
            } catch (_: Exception) {}
        }

        return record
    }

    fun markCollectionCollectedByManager(collectionId: String) {
        _collections.value = _collections.value.map {
            if (it.id == collectionId) it.copy(managerCollectedMarker = true) else it
        }
        scope.launch {
            try {
                db.collection(FirestoreCollections.DAILY_COLLECTIONS).document(collectionId)
                    .update("managerCollectedMarker", true).await()
                db.collection("daily_collections").document(collectionId)
                    .update("managerCollectedMarker", true).await()
            } catch (_: Exception) {}
        }
    }

    // --- Developer Backend Operations ---

    fun authenticateDeveloper(accessKey: String): Boolean {
        val ok = authManager.loginAsDeveloper(accessKey)
        if (ok) {
            val devUser = currentProfile.value
            if (!_staffList.value.any { it.id == devUser.id }) {
                _staffList.value = listOf(devUser) + _staffList.value
            }
        }
        return ok
    }

    fun toggleCompanyStatus(companyId: String) {
        _companies.value = _companies.value.map { comp ->
            if (comp.id == companyId) {
                val newStatus = if (comp.status == "ACTIVE") "SUSPENDED" else "ACTIVE"
                comp.copy(status = newStatus)
            } else comp
        }
        val target = _companies.value.firstOrNull { it.id == companyId } ?: return
        if (_currentCompany.value?.id == companyId) {
            _currentCompany.value = target
        }
        scope.launch {
            try {
                db.collection(FirestoreCollections.COMPANIES).document(companyId)
                    .update("status", target.status).await()
                db.collection("companies").document(companyId)
                    .update("status", target.status).await()
            } catch (_: Exception) {}
        }
    }

    fun syncAllToFirestore(onResult: (Boolean, String) -> Unit) {
        scope.launch {
            try {
                // Batch write companies to Companies and companies
                for (comp in _companies.value) {
                    writeCompany(comp)
                }
                // Write staff to Users and users
                for (staff in _staffList.value) {
                    writeUser(staff)
                }
                // Write partners & deposits to Partners and PartnerDeposits
                for (partner in _partners.value) {
                    writePartner(partner)
                }
                for (dep in _partnerDeposits.value) {
                    writePartnerDeposit(dep)
                }
                // Write customers to Customers and customers
                for (cust in _customers.value) {
                    writeCustomer(cust)
                }
                // Write books to DailyBooks and daily_deposit_books
                for (book in _dailyBooks.value) {
                    writeDailyBook(book)
                }
                // Write collections to DailyCollections and daily_collections
                for (col in _collections.value) {
                    writeDailyCollection(col)
                }
                withContext(Dispatchers.Main) {
                    onResult(true, "All collections (Companies, Users, Customers, Partners, DailyBooks) synced to Firestore!")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onResult(false, e.localizedMessage ?: "Sync error")
                }
            }
        }
    }

    // --- Customer-Wise Deposit Data Extraction ---

    fun getCustomerDepositSummaries(companyId: String? = null): List<CustomerDepositSummary> {
        val compId = companyId ?: _currentCompany.value?.id ?: ""
        val compCustomers = _customers.value.filter { compId.isBlank() || it.companyId == compId }
        val compBooks = _dailyBooks.value.filter { compId.isBlank() || it.companyId == compId }
        val compCollections = _collections.value.filter { compId.isBlank() || it.companyId == compId }

        return compCustomers.map { cust ->
            val custBooks = compBooks.filter { it.customerCif == cust.cif }
            val custCollections = compCollections.filter { it.cif == cust.cif }
                .sortedByDescending { "${it.collectedDate} ${it.collectedTime}" }

            val totalAmount = if (custCollections.isNotEmpty()) {
                custCollections.sumOf { it.depositAmount }
            } else {
                custBooks.sumOf { it.cumulativeDeposit }
            }
            val count = custCollections.size
            val lastDate = custCollections.firstOrNull()?.collectedDate ?: ""
            val lastTime = custCollections.firstOrNull()?.collectedTime ?: ""
            val avg = if (count > 0) totalAmount / count else 0.0

            CustomerDepositSummary(
                customer = cust,
                books = custBooks,
                collections = custCollections,
                totalDepositAmount = totalAmount,
                collectionCount = count,
                lastDepositDate = lastDate,
                lastDepositTime = lastTime,
                averageDepositAmount = avg
            )
        }.sortedByDescending { it.totalDepositAmount }
    }
}
