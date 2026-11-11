package com.aurviz.personalagent.domain.model

data class PendingAction(
    val id: Long = 0,
    val toolName: String,
    val arguments: String,
    val status: PendingActionStatus = PendingActionStatus.AWAITING_APPROVAL,
    val expiresAt: Long = System.currentTimeMillis() + 5 * 60 * 1000 // 5 min default
)

enum class PendingActionStatus {
    AWAITING_APPROVAL,
    APPROVED,
    REJECTED,
    EXPIRED,
    EXECUTED
}
