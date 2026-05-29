package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "contacts")
data class Contact(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val phoneNumber: String,
    val isVirtual: Boolean, // true if simulated over internet via Gemini, false if actual phone number that we want to send real SMS to
    val aiPrompt: String = "", // custom instruction (e.g. "You are an affectionate sister replying in Bengali.")
    val avatarColor: Long = 0xFF4A90E2L, // color prefix
    val unreadCount: Int = 0,
    val lastMessage: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "messages")
data class Message(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val contactId: Int,
    val senderNumber: String,
    val receiverNumber: String,
    val body: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isIncoming: Boolean, // true if received, false if sent by the user
    val status: String = "SENT", // "PENDING", "SENT", "FAILED", "RECEIVED"
    val viaRealSms: Boolean = false // true if sent/received via actual SMS Manager, false if simulated over the internet with Gemini
)

@Dao
interface ChatDao {
    @Query("SELECT * FROM contacts ORDER BY timestamp DESC")
    fun getAllContacts(): Flow<List<Contact>>

    @Query("SELECT * FROM contacts WHERE id = :id LIMIT 1")
    suspend fun getContactById(id: Int): Contact?

    @Query("SELECT * FROM contacts WHERE phoneNumber = :phone LIMIT 1")
    suspend fun getContactByPhone(phone: String): Contact?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: Contact): Long

    @Update
    suspend fun updateContact(contact: Contact)

    @Delete
    suspend fun deleteContact(contact: Contact)

    @Query("SELECT * FROM messages WHERE contactId = :contactId ORDER BY timestamp ASC")
    fun getMessagesForContact(contactId: Int): Flow<List<Message>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: Message): Long

    @Update
    suspend fun updateMessage(message: Message)

    @Query("DELETE FROM messages WHERE contactId = :contactId")
    suspend fun deleteMessagesForContact(contactId: Int)

    @Query("UPDATE contacts SET lastMessage = :lastMsg, timestamp = :time WHERE id = :contactId")
    suspend fun updateContactLastMessage(contactId: Int, lastMsg: String, time: Long)

    @Query("UPDATE contacts SET unreadCount = unreadCount + 1 WHERE id = :contactId")
    suspend fun incrementUnreadCount(contactId: Int)

    @Query("UPDATE contacts SET unreadCount = 0 WHERE id = :contactId")
    suspend fun clearUnreadCount(contactId: Int)
}

@Database(entities = [Contact::class, Message::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "sim_chat_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
