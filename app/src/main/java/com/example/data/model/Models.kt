package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class UserRole(val displayName: String) {
    DEVELOPER("Developer"),
    COMPANY_ADMIN("Company Admin"),
    MANAGER("Manager"),
    COLLECTION_EXECUTIVE("Collection Executive")
}

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val email: String = "",
    val name: String = "",
    val role: String = UserRole.COLLECTION_EXECUTIVE.name,
    val companyId: String = "",
    val contact: String = "",
    val assignedManagerId: String = "",
    val assignedManagerName: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Company(
    @DocumentId val id: String = "",
    val name: String = "",
    val address: String = "",
    val contact: String = "",
    val logoUrl: String = "",
    val pinStart: Long = 1001L,
    val cifStart: Long = 20001L,
    val bookStart: Long = 500001L,
    val managerCanApproveCustomer: Boolean = true,
    val managerCanApproveBook: Boolean = true,
    val status: String = "ACTIVE",
    val createdBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Partner(
    @DocumentId val id: String = "",
    val companyId: String = "",
    val name: String = "",
    val contact: String = "",
    val aadharNumber: String = "",
    val pin: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class PartnerDeposit(
    @DocumentId val id: String = "",
    val companyId: String = "",
    val partnerPin: String = "",
    val partnerName: String = "",
    val amount: Double = 0.0,
    val depositDate: String = "",
    val depositTime: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

enum class ApprovalStatus {
    PENDING,
    APPROVED,
    REJECTED
}

@IgnoreExtraProperties
data class Customer(
    @DocumentId val id: String = "",
    val companyId: String = "",
    val name: String = "",
    val businessName: String = "",
    val contact: String = "",
    val aadharNumber: String = "",
    val panNumber: String = "",
    val cif: String = "",
    val status: String = ApprovalStatus.PENDING.name,
    val onboardedByRole: String = "",
    val onboardedById: String = "",
    val onboardedByName: String = "",
    val approvedBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class DailyDepositBook(
    @DocumentId val id: String = "",
    val companyId: String = "",
    val customerCif: String = "",
    val customerName: String = "",
    val customerContact: String = "",
    val bookNumber: String = "",
    val createdByManagerId: String = "",
    val createdByManagerName: String = "",
    val status: String = ApprovalStatus.PENDING.name,
    val cumulativeDeposit: Double = 0.0,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class DailyCollectionRecord(
    @DocumentId val id: String = "",
    val companyId: String = "",
    val receiptNumber: String = "",
    val customerName: String = "",
    val cif: String = "",
    val bookNumber: String = "",
    val depositAmount: Double = 0.0,
    val cumulativeTotal: Double = 0.0,
    val collectedByExecutiveId: String = "",
    val collectedByExecutiveName: String = "",
    val managerId: String = "",
    val managerCollectedMarker: Boolean = false,
    val collectedDate: String = "",
    val collectedTime: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

data class CustomerDepositSummary(
    val customer: Customer = Customer(),
    val books: List<DailyDepositBook> = emptyList(),
    val collections: List<DailyCollectionRecord> = emptyList(),
    val totalDepositAmount: Double = 0.0,
    val collectionCount: Int = 0,
    val lastDepositDate: String = "",
    val lastDepositTime: String = "",
    val averageDepositAmount: Double = 0.0
)
