package com.example.data

import android.content.Context
import android.telephony.SmsManager
import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class ChatRepository(private val context: Context) {
    private val database = AppDatabase.getDatabase(context)
    private val chatDao = database.chatDao()

    val allContacts: Flow<List<Contact>> = chatDao.getAllContacts()

    fun getMessagesForContact(contactId: Int): Flow<List<Message>> {
        return chatDao.getMessagesForContact(contactId)
    }

    suspend fun clearUnread(contactId: Int) {
        chatDao.clearUnreadCount(contactId)
    }

    suspend fun addContact(name: String, phoneNumber: String, isVirtual: Boolean, aiPrompt: String) {
        val cleanPhone = phoneNumber.replace("\\s".toRegex(), "").replace("-", "")
        val contact = Contact(
            name = name,
            phoneNumber = cleanPhone,
            isVirtual = isVirtual,
            aiPrompt = aiPrompt,
            avatarColor = if (isVirtual) 0xFF8E44ADL else 0xFF27AE60L, // dynamic avatar backgrounds (purple/green)
            lastMessage = if (isVirtual) "Tap to open AI SIM connection" else "Tap to send network SMS",
            timestamp = System.currentTimeMillis()
        )
        chatDao.insertContact(contact)
    }

    suspend fun deleteContact(contact: Contact) {
        chatDao.deleteMessagesForContact(contact.id)
        chatDao.deleteContact(contact)
    }

    suspend fun sendMessage(contact: Contact, body: String): Message {
        val timestamp = System.currentTimeMillis()
        
        // 1. Log outgoing message record in local DB as PENDING
        val userMessage = Message(
            contactId = contact.id,
            senderNumber = "Self",
            receiverNumber = contact.phoneNumber,
            body = body,
            timestamp = timestamp,
            isIncoming = false,
            status = "PENDING",
            viaRealSms = !contact.isVirtual
        )
        val msgId = chatDao.insertMessage(userMessage)
        val savedUserMessage = userMessage.copy(id = msgId)

        // 2. Refresh active contact's listing metadata
        chatDao.updateContactLastMessage(contact.id, body, timestamp)

        if (contact.isVirtual) {
            // --- VIRTUAL AI CONNECTION MODE (GEMINI SMS SIMULATION) ---
            try {
                val history = chatDao.getMessagesForContact(contact.id).first()
                
                val replyText = GeminiClient.getSimulatedReply(
                    contactName = contact.name,
                    contactPhone = contact.phoneNumber,
                    personaPrompt = contact.aiPrompt,
                    chatHistory = history.filter { it.id != msgId }, // ignore unsaved duplicate reference
                    newMessageBody = body
                )

                // Update outgoing status to SUCCESS
                chatDao.updateMessage(savedUserMessage.copy(status = "SENT"))

                // Save virtual contact's reply record
                val responseStamp = System.currentTimeMillis()
                val replyMessage = Message(
                    contactId = contact.id,
                    senderNumber = contact.phoneNumber,
                    receiverNumber = "Self",
                    body = replyText,
                    timestamp = responseStamp,
                    isIncoming = true,
                    status = "RECEIVED",
                    viaRealSms = false
                )
                chatDao.insertMessage(replyMessage)
                chatDao.updateContactLastMessage(contact.id, replyText, responseStamp)
                chatDao.incrementUnreadCount(contact.id)

            } catch (e: Exception) {
                Log.e("ChatRepository", "Error requesting AI response", e)
                chatDao.updateMessage(savedUserMessage.copy(status = "FAILED"))
                
                // Log clear system failure notice
                val systemFailMsg = Message(
                    contactId = contact.id,
                    senderNumber = contact.phoneNumber,
                    receiverNumber = "Self",
                    body = "Connection Error! Check internet or Secret panel API key. (${e.localizedMessage})",
                    timestamp = System.currentTimeMillis(),
                    isIncoming = true,
                    status = "FAILED",
                    viaRealSms = false
                )
                chatDao.insertMessage(systemFailMsg)
            }
        } else {
            // --- SYSTEM SIM OVER-THE-AIR SMS MODE ---
            try {
                val smsManager = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    context.getSystemService(SmsManager::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    SmsManager.getDefault()
                }

                smsManager.sendTextMessage(contact.phoneNumber, null, body, null, null)
                chatDao.updateMessage(savedUserMessage.copy(status = "SENT"))
            } catch (e: Exception) {
                Log.e("ChatRepository", "Real Cellular SIM transmission failed", e)
                chatDao.updateMessage(savedUserMessage.copy(status = "FAILED"))
            }
        }

        return savedUserMessage
    }

    /**
     * Request simulated reply on-demand for manual testing or emulator users.
     */
    suspend fun simulateIncomingReply(contact: Contact, triggerPrompt: String) {
        try {
            val history = chatDao.getMessagesForContact(contact.id).first()
            val promptStyle = if (contact.isVirtual) contact.aiPrompt else "A typical standard SMS reply from someone's phone number."

            val replyText = GeminiClient.getSimulatedReply(
                contactName = contact.name,
                contactPhone = contact.phoneNumber,
                personaPrompt = promptStyle,
                chatHistory = history,
                newMessageBody = triggerPrompt
            )

            val replyStamp = System.currentTimeMillis()
            val replyMessage = Message(
                contactId = contact.id,
                senderNumber = contact.phoneNumber,
                receiverNumber = "Self",
                body = replyText,
                timestamp = replyStamp,
                isIncoming = true,
                status = "RECEIVED",
                viaRealSms = !contact.isVirtual
            )
            chatDao.insertMessage(replyMessage)
            chatDao.updateContactLastMessage(contact.id, replyText, replyStamp)
            chatDao.incrementUnreadCount(contact.id)
        } catch (e: Exception) {
            Log.e("ChatRepository", "Simulate reply failed", e)
        }
    }
}
