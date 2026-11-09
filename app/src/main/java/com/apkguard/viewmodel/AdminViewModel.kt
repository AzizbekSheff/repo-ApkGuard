package com.apkguard.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

data class PaymentRequest(
    val id: String = "",
    val userId: String = "",
    val email: String = "",
    val planId: String = "",
    val receiptUrl: String = "",
    val status: String = "pending",
    val timestamp: com.google.firebase.Timestamp? = null
)

@HiltViewModel
class AdminViewModel @Inject constructor() : ViewModel() {
    private val db = FirebaseFirestore.getInstance()

    private val _requests = MutableStateFlow<List<PaymentRequest>>(emptyList())
    val requests = _requests.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    init {
        loadRequests()
    }

    fun loadRequests() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val snapshot = db.collection("payment_requests")
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .get()
                    .await()
                
                _requests.value = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(PaymentRequest::class.java)?.copy(id = doc.id)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun approveRequest(request: PaymentRequest) {
        viewModelScope.launch {
            try {
                // 1. Update request status
                db.collection("payment_requests").document(request.id).update("status", "approved").await()
                
                // 2. Grant Premium status to user
                db.collection("users").document(request.userId).update("premium", true).await()
                
                // Refresh list
                loadRequests()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun rejectRequest(requestId: String) {
        viewModelScope.launch {
            try {
                db.collection("payment_requests").document(requestId).update("status", "rejected").await()
                loadRequests()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
