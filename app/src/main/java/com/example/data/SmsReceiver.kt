package com.example.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "SmsReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            for (sms in messages) {
                val sender = sms.displayOriginatingAddress ?: continue
                val body = sms.displayMessageBody ?: continue
                val timestamp = sms.timestampMillis

                Log.d(TAG, "Received SMS from $sender: $body")

                val appContext = context.applicationContext
                val db = AppDatabase.getDatabase(appContext)
                val dao = db.chatDao()

                // Insert into Room database in IO thread
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        // Standardize matching by cleaning phone string
                        val cleanSender = sender.replace("\\s".toRegex(), "").replace("-", "")
                        
                        // Find match in our saved contacts
                        var contact = dao.getContactByPhone(cleanSender)
                        if (contact == null) {
                            // Secondary fallback matching via substring of number to bypass country codes
                            val allContacts = db.chatDao().getAllContacts()
                            // Note: we fetch the list to find matching tails (e.g. +88017... matches 017...)
                            // For simplicity, we create a new contact profile
                            contact = Contact(
                                name = sender,
                                phoneNumber = cleanSender,
                                isVirtual = false,
                                lastMessage = body,
                                timestamp = timestamp,
                                avatarColor = 0xFF2ECC71L, // Green accent for real Sim contacts
                                unreadCount = 1
                            )
                            val insertedId = dao.insertContact(contact)
                            contact = contact.copy(id = insertedId.toInt())
                        } else {
                            // Update existing contact metadata
                            dao.updateContactLastMessage(contact.id, body, timestamp)
                            dao.incrementUnreadCount(contact.id)
                        }

                        // Insert the incoming SMS text record
                        val messageEntity = Message(
                            contactId = contact.id,
                            senderNumber = sender,
                            receiverNumber = "Self",
                            body = body,
                            timestamp = timestamp,
                            isIncoming = true,
                            status = "RECEIVED",
                            viaRealSms = true
                        )
                        dao.insertMessage(messageEntity)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing incoming SMS block list", e)
                    }
                }
            }
        }
    }
}
