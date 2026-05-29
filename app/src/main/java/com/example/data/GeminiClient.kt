package com.example.data

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiClient {
    private const val TAG = "GeminiClient"
    private const val MODEL_NAME = "gemini-3.5-flash"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL_NAME:generateContent"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * Checks if the Gemini API Key is configured.
     */
    fun isApiKeyAvailable(): Boolean {
        // BuildConfig.GEMINI_API_KEY is injected by secrets gradle plugin from .env / Secrets panel
        val key = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }
        return key.isNotEmpty() && key != "MY_GEMINI_API_KEY" && !key.startsWith("placeholder", true)
    }

    /**
     * Sends a message to the Gemini API and returns the simulated contact's text reply.
     * Includes chat history for rich contextual replies.
     */
    suspend fun getSimulatedReply(
        contactName: String,
        contactPhone: String,
        personaPrompt: String,
        chatHistory: List<Message>,
        newMessageBody: String
    ): String = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }

        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "Error: Gemini API Key is missing. Please add your key to the 'Secrets' panel in AI Studio or .env file as GEMINI_API_KEY."
        }

        val url = "$BASE_URL?key=$apiKey"

        // Build system instruction to formulate the contact's perspective
        val systemInstructionText = """
            You are simulating an SMS/text chat on a mobile phone. 
            The user is texting a contact named '$contactName' (Phone Number: $contactPhone).
            You MUST reply strictly as '$contactName'. 
            The contact's personality/instruction is: '$personaPrompt'. 
            
            Strict Guidelines:
            1. Respond naturally like a human texting. Keep it authentic, human, and conversational.
            2. Match the language, slang, and style of the user's incoming message (e.g., reply in Bengali if they write in Bengali, or English if english).
            3. Keep replies relatively concise, fitting a mobile text exchange (no long essays unless requested).
            4. Do not include system metadata or write like an AI bot.
            5. Your contact identity is: '$contactName'. Stay in character at all costs.
        """.trimIndent()

        try {
            // Build request json
            val requestJson = JSONObject()
            
            // System instructions
            val sysParts = JSONObject().put("text", systemInstructionText)
            val sysInstruction = JSONObject().put("parts", JSONArray().put(sysParts))
            requestJson.put("systemInstruction", sysInstruction)

            // Contents (including context conversation history wrapper)
            val contentsArray = JSONArray()

            // Format previous context (limit to last 10 messages for speed and context limits)
            val limitedHistory = chatHistory.takeLast(10)
            for (msg in limitedHistory) {
                val turn = JSONObject()
                // Role must be 'user' for user, 'model' for the simulated contact response
                turn.put("role", if (msg.isIncoming) "model" else "user")
                
                val part = JSONObject().put("text", msg.body)
                turn.put("parts", JSONArray().put(part))
                contentsArray.put(turn)
            }

            // Append the latest user message
            val currentTurn = JSONObject().put("role", "user")
            val currentPart = JSONObject().put("text", newMessageBody)
            currentTurn.put("parts", JSONArray().put(currentPart))
            contentsArray.put(currentTurn)

            requestJson.put("contents", contentsArray)

            // Generation config
            val genConfig = JSONObject()
            genConfig.put("temperature", 0.8)
            requestJson.put("generationConfig", genConfig)

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = requestJson.toString().toRequestBody(mediaType)

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val code = response.code
                    val errorMsg = response.body?.string() ?: ""
                    Log.e(TAG, "Gemini Request Failed: Code $code - $errorMsg")
                    return@withContext "Error: SIM Gateway Connection failed (Code $code). $errorMsg"
                }

                val responseStr = response.body?.string() ?: return@withContext "Error: Empty response from SIM gateway."
                val responseJson = JSONObject(responseStr)
                
                val candidates = responseJson.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val firstCandidate = candidates.getJSONObject(0)
                    val contentObj = firstCandidate.optJSONObject("content")
                    if (contentObj != null) {
                        val partsArr = contentObj.optJSONArray("parts")
                        if (partsArr != null && partsArr.length() > 0) {
                            return@withContext partsArr.getJSONObject(0).optString("text", "No message content.")
                        }
                    }
                }
                
                return@withContext "Error: Failed to extract message content from SIM gateway response."
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during Gemini Call", e)
            return@withContext "Network Error: Could not connect to SIM gateway. ${e.localizedMessage}"
        }
    }
}
