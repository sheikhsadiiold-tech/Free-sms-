package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ChatRepository
import com.example.data.Contact
import com.example.data.GeminiClient
import com.example.data.Message
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ChatRepository(application)

    // Search filter state
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    // Unfiltered raw contact flows
    val rawContacts: StateFlow<List<Contact>> = repository.allContacts
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Filtered contacts based on search query
    val contacts: StateFlow<List<Contact>> = combine(rawContacts, _searchQuery) { list, query ->
        if (query.isEmpty()) {
            list
        } else {
            list.filter {
                it.name.contains(query, ignoreCase = true) || 
                it.phoneNumber.contains(query, ignoreCase = true)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Monitor isFirstLaunch and prepopulate nice contacts if empty
    init {
        viewModelScope.launch {
            // Check if database is empty, prepopulate with gorgeous Bengali virtual contacts
            rawContacts.collect { list ->
                if (list.isEmpty()) {
                    initializeDefaultContacts()
                }
            }
        }
    }

    private suspend fun initializeDefaultContacts() {
        // Pre-populating 3 realistic contacts
        repository.addContact(
            name = "মা (Mother)",
            phoneNumber = "+8801700000001",
            isVirtual = true,
            aiPrompt = "You are the loving caring mother of the user. Reply in gentle, warm, affectionate Bengali (conversational, colloquial style). Always sound loving, asking if the user has eaten, reminding them to stay safe, and expressing pure maternal warmth."
        )
        repository.addContact(
            name = "শুভ (Best Friend)",
            phoneNumber = "+8801700000002",
            isVirtual = true,
            aiPrompt = "You are a witty, energetic, best friend named Shuvo. Reply in modern conversational Banglish (mix of Bengali and English) or casual local Bengali. Use cool friendly slangs (like 'দোস্ত', 'কিরে', 'চিল', 'প্যারা নাই')."
        )
        repository.addContact(
            name = "GP Customer Support",
            phoneNumber = "121",
            isVirtual = true,
            aiPrompt = "You are an official customer representative of a top-tier telecom carrier. Reply politely and helpfully in a formal bilingual format (English and Bengali mixes). Help the user diagnose cellular connection, register internet packs, or configure SIM message gateways."
        )
    }

    // Selected contact id flow
    private val _selectedContactId = MutableStateFlow<Int?>(null)
    val selectedContactId = _selectedContactId.asStateFlow()

    // Active contract details computed from selected id
    val activeContact: StateFlow<Contact?> = combine(rawContacts, _selectedContactId) { list, id ->
        list.find { it.id == id }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    // FlatMap active messages flow based on chosen contact selection
    val activeMessages: StateFlow<List<Message>> = _selectedContactId.flatMapLatest { id ->
        if (id == null) {
            flowOf(emptyList())
        } else {
            repository.getMessagesForContact(id)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Local sending loading indicator state
    private val _isSending = MutableStateFlow(false)
    val isSending = _isSending.asStateFlow()

    // API availability state
    val isGeminiApiKeyConfigured: Boolean
        get() = GeminiClient.isApiKeyAvailable()

    fun selectContact(contactId: Int?) {
        _selectedContactId.value = contactId
        if (contactId != null) {
            viewModelScope.launch {
                repository.clearUnread(contactId)
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun createContact(name: String, phoneNumber: String, isVirtual: Boolean, aiPrompt: String) {
        viewModelScope.launch {
            repository.addContact(name, phoneNumber, isVirtual, aiPrompt)
        }
    }

    fun deleteContact(contact: Contact) {
        viewModelScope.launch {
            if (_selectedContactId.value == contact.id) {
                _selectedContactId.value = null
            }
            repository.deleteContact(contact)
        }
    }

    fun sendMessage(body: String) {
        val contact = activeContact.value ?: return
        if (body.trim().isEmpty()) return

        viewModelScope.launch {
            _isSending.value = true
            try {
                repository.sendMessage(contact, body)
            } catch (e: Exception) {
                // errors handled gracefully in repository channel logger
            } finally {
                _isSending.value = false
            }
        }
    }

    /**
     * Force on-demand SMS simulation reply
     */
    fun simulateCarrierReply(contextPrompt: String) {
        val contact = activeContact.value ?: return
        if (contextPrompt.trim().isEmpty()) return
        
        viewModelScope.launch {
            _isSending.value = true
            try {
                repository.simulateIncomingReply(contact, contextPrompt)
            } finally {
                _isSending.value = false
            }
        }
    }
}
