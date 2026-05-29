package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.ContactMail
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Contact
import com.example.data.Message
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    modifier: Modifier = Modifier
) {
    val contacts by viewModel.contacts.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedId by viewModel.selectedContactId.collectAsState()
    val activeContact by viewModel.activeContact.collectAsState()
    val messages by viewModel.activeMessages.collectAsState()
    val isSending by viewModel.isSending.collectAsState()
    val isApkKeyConfigured = viewModel.isGeminiApiKeyConfigured

    var showAddContactDialog by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isWideScreen = maxWidth >= 600.dp

            if (isWideScreen) {
                // Large Tablet/Desktop Layout: Show list and detail side-by-side
                Row(modifier = Modifier.fillMaxSize()) {
                    // Contact list panel on the left (fixed width of 350.dp)
                    Box(
                        modifier = Modifier
                            .width(350.dp)
                            .fillMaxHeight()
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ) {
                        ContactListPanel(
                            contacts = contacts,
                            searchQuery = searchQuery,
                            onQueryChange = { viewModel.updateSearchQuery(it) },
                            selectedId = selectedId,
                            onContactSelect = { viewModel.selectContact(it) },
                            onAddContactClick = { showAddContactDialog = true },
                            isApiKeyMissing = !isApkKeyConfigured
                        )
                    }

                    // Chat conversation panel on the right (takes up remainder space)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        if (activeContact != null) {
                            ChatConversationPanel(
                                contact = activeContact!!,
                                messages = messages,
                                isSending = isSending,
                                onSendMessage = { viewModel.sendMessage(it) },
                                onBackClick = { viewModel.selectContact(null) },
                                showBackButton = false, // on wide screen no need for back button
                                onDeleteContact = { viewModel.deleteContact(it) },
                                onSimulateReply = { viewModel.simulateCarrierReply(it) }
                            )
                        } else {
                            // Split-screen empty details pane
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Message,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "মেসেজ আদান-প্রদান করতে কন্টাক্ট সিলেক্ট করুন",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Select a contact to begin real or simulated SIM messaging",
                                    fontSize = 14.sp,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }
            } else {
                // Compact Layout (Mobile): Switch screens based on active selection
                Box(modifier = Modifier.fillMaxSize()) {
                    if (activeContact != null) {
                        ChatConversationPanel(
                            contact = activeContact!!,
                            messages = messages,
                            isSending = isSending,
                            onSendMessage = { viewModel.sendMessage(it) },
                            onBackClick = { viewModel.selectContact(null) },
                            showBackButton = true,
                            onDeleteContact = { viewModel.deleteContact(it) },
                            onSimulateReply = { viewModel.simulateCarrierReply(it) }
                        )
                    } else {
                        ContactListPanel(
                            contacts = contacts,
                            searchQuery = searchQuery,
                            onQueryChange = { viewModel.updateSearchQuery(it) },
                            selectedId = selectedId,
                            onContactSelect = { viewModel.selectContact(it) },
                            onAddContactClick = { showAddContactDialog = true },
                            isApiKeyMissing = !isApkKeyConfigured
                        )
                    }
                }
            }
        }

        // Add Contact Form Overlay Dialog
        if (showAddContactDialog) {
            AddContactDialog(
                onDismiss = { showAddContactDialog = false },
                onAdd = { name, number, isVirtual, prompt ->
                    viewModel.createContact(name, number, isVirtual, prompt)
                    showAddContactDialog = false
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactListPanel(
    contacts: List<Contact>,
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    selectedId: Int?,
    onContactSelect: (Int) -> Unit,
    onAddContactClick: () -> Unit,
    isApiKeyMissing: Boolean
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "ইন্টারনেট সিম গেটওয়ে",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "Virtual SIM & Carrier SMS Center",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                },
                navigationIcon = {
                    Icon(
                        imageVector = Icons.Default.Cloud,
                        contentDescription = "Cloud Icon",
                        modifier = Modifier
                            .padding(horizontal = 12.dp)
                            .size(28.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddContactClick,
                modifier = Modifier.testTag("add_contact_fab"),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Contact"
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Secret API key configuration alert banner
            if (isApiKeyMissing) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Warning",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "এআই রিপ্লাই নিষ্ক্রিয় (API Key নেই)",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Configure 'GEMINI_API_KEY' in AI Studio Secrets to unlock dynamic AI responses.",
                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f),
                                fontSize = 11.sp,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }
            }

            // Search Bar Input Field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .testTag("contact_search_bar"),
                placeholder = { Text("কন্টাক্ট খুঁজুন... / Search contacts") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search"
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                )
            )

            if (contacts.isEmpty()) {
                // Empty state illustration
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ContactMail,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "কোনো কন্টাক্ট পাওয়া যায়নি",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "নিচের (+) বাটনে ট্যাপ করে নতুন কন্টাক্ট তৈরি করুন।",
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            } else {
                // Feed list
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    items(contacts, key = { it.id }) { contact ->
                        ContactItemCard(
                            contact = contact,
                            isSelected = contact.id == selectedId,
                            onClick = { onContactSelect(contact.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ContactItemCard(
    contact: Contact,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .testTag("contact_card_${contact.id}")
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar profile bubble
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(contact.avatarColor)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = contact.name.trim().take(1).uppercase(Locale.getDefault()),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Contact core metadata
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = contact.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Compact Tag indicating virtual or real SIM state
                    val tagBg = if (contact.isVirtual) {
                        MaterialTheme.colorScheme.tertiaryContainer
                    } else {
                        MaterialTheme.colorScheme.secondaryContainer
                    }
                    val tagText = if (contact.isVirtual) "INTERNET AI" else "REAL SIM"
                    val tagColor = if (contact.isVirtual) {
                        MaterialTheme.colorScheme.onTertiaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(tagBg)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = tagText,
                            fontSize = 8.6.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = tagColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(3.dp))
                
                Text(
                    text = contact.phoneNumber,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Last msg summary teaser
                Text(
                    text = contact.lastMessage,
                    fontSize = 12.6.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Unread indicators
            if (contact.unreadCount > 0) {
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.error),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = contact.unreadCount.toString(),
                        color = MaterialTheme.colorScheme.onError,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatConversationPanel(
    contact: Contact,
    messages: List<Message>,
    isSending: Boolean,
    onSendMessage: (String) -> Unit,
    onBackClick: () -> Unit,
    showBackButton: Boolean,
    onDeleteContact: (Contact) -> Unit,
    onSimulateReply: (String) -> Unit
) {
    var textInput by remember { mutableStateOf("") }
    val lazyListState = rememberLazyListState()
    val keyboardController = LocalSoftwareKeyboardController.current

    // Automatically scroll to the end of messages when counts swell
    LaunchedEffect(messages.size, isSending) {
        if (messages.isNotEmpty()) {
            lazyListState.animateScrollToItem(messages.size - 1)
        }
    }

    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showSimPromptDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(contact.avatarColor)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = contact.name.trim().take(1).uppercase(Locale.getDefault()),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = contact.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "${contact.phoneNumber} • ${if (contact.isVirtual) "Virtual SIM Chat" else "Cellular Carrier Link"}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        }
                    }
                },
                navigationIcon = {
                    if (showBackButton) {
                        IconButton(
                            onClick = onBackClick,
                            modifier = Modifier.testTag("chat_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                },
                actions = {
                    // Manual simulation action for emulator/no-sim users
                    IconButton(onClick = { showSimPromptDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Simulate response",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Delete Contact bin
                    IconButton(
                        onClick = { showDeleteConfirmDialog = true },
                        modifier = Modifier.testTag("delete_contact_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Contact",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Warning header indicating status configuration helper
            if (!contact.isVirtual) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "রিয়েল সিম মোড: আপনার ফোনের টাওয়ার কানেকশন ব্যবহৃত হবে (SMS চার্জ প্রযোজ্য)।",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            lineHeight = 14.sp
                        )
                    }
                }
            } else if (contact.aiPrompt.isNotEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "এআই ব্যক্তিত্ব: ${contact.aiPrompt}",
                            fontSize = 10.6.sp,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Message Logs Stream
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (messages.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Message,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "মেসেজিং আরম্ভ করুন!",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                        Text(
                            text = "নিচে মেসেজ লিখে সেন্ড করুন।",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                    }
                } else {
                    LazyColumn(
                        state = lazyListState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp)
                    ) {
                        items(messages) { message ->
                            MessageBubble(message = message)
                        }
                    }
                }

                // AI Processing / System load Indicator
                if (isSending) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                            .align(Alignment.BottomCenter)
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.align(Alignment.Center)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "সিম টাওয়ার গেটওয়ে কানেক্ট হচ্ছে / Sending...",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // Input Send Console Box
            Surface(
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("message_input_field")
                            .heightIn(max = 120.dp),
                        placeholder = { Text("মেসেজ লিখুন... / Type message...") },
                        maxLines = 4,
                        shape = RoundedCornerShape(20.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (textInput.trim().isNotEmpty()) {
                                onSendMessage(textInput)
                                textInput = ""
                                keyboardController?.hide()
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .testTag("send_message_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }

    // Modal dialogue delete confirmation
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("কন্টাক্ট ডিলিট করুন?") },
            text = { Text("আপনি কি নিশ্চিতভাবে '${contact.name}' এবং এর সমস্ত চ্যাট ইতিহাস মুছে ফেলতে চান?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteContact(contact)
                        showDeleteConfirmDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("মুছে ফেলুন (Delete)")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("বাতিল (Cancel)")
                }
            }
        )
    }

    // Modal dialog to trigger simulated replies manually on emulators
    if (showSimPromptDialog) {
        var simPrompt by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showSimPromptDialog = false },
            title = {
                Column {
                    Text("মেসেজ রিপ্লাই এমুলেটর", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("Simulate Incoming Response", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            text = {
                Column {
                    Text(
                        text = "The physical emulator environment may lack cellular SIM service. Write a trigger here, and Gemini will reply back instantly pretending to be '${contact.name}'!",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 14.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = simPrompt,
                        onValueChange = { simPrompt = it },
                        modifier = Modifier.fillMaxWidth().testTag("emulator_prompt_input"),
                        label = { Text("মেসেজ ট্রিগার / Message Trigger") },
                        placeholder = { Text("উদা: কেমন আছো? / How are you?") },
                        maxLines = 2
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (simPrompt.trim().isNotEmpty()) {
                            onSimulateReply(simPrompt)
                            showSimPromptDialog = false
                        }
                    }
                ) {
                    Text("রিপ্লাই আনুন (Generate Reply)")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSimPromptDialog = false }) {
                    Text("বাতিল (Cancel)")
                }
            }
        )
    }
}

@Composable
fun MessageBubble(message: Message) {
    val isUser = !message.isIncoming
    val alignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    val bubbleColor = if (isUser) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = if (isUser) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val shape = if (isUser) {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 2.dp)
    } else {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 2.dp, bottomEnd = 16.dp)
    }

    val timeString = remember(message.timestamp) {
        try {
            val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
            sdf.format(Date(message.timestamp))
        } catch (e: Exception) {
            ""
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentAlignment = alignment
    ) {
        Column(
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = bubbleColor),
                shape = shape,
                elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = message.body,
                        color = contentColor,
                        fontSize = 14.sp,
                        lineHeight = 18.sp
                    )
                }
            }
            
            // Subtitle footer containing status icons
            Row(
                modifier = Modifier.padding(horizontal = 4.dp).padding(top = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = timeString,
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
                
                if (isUser) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = when (message.status) {
                            "PENDING" -> "⌛"
                            "SENT" -> "✓"
                            "FAILED" -> "✗"
                            else -> "✓"
                        },
                        fontSize = 8.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = if (message.viaRealSms) "via SIM" else "via Web",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                } else {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (message.viaRealSms) "via SIM Tower" else "via Internet AI",
                        fontSize = 8.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                }
            }
        }
    }
}

@Composable
fun AddContactDialog(
    onDismiss: () -> Unit,
    onAdd: (name: String, number: String, isVirtual: Boolean, prompt: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var isVirtual by remember { mutableStateOf(true) }
    var aiPrompt by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("নতুন কন্টাক্ট যুক্ত করুন", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("নাম (Name)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("add_contact_name_input")
                )

                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label = { Text("মোবাইল নম্বর (Phone Number)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("add_contact_phone_input")
                )

                Text(
                    text = "মেসেজ আদান-প্রদান পদ্ধতি / Gateway Mode:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isVirtual = true },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isVirtual,
                            onClick = { isVirtual = true },
                            modifier = Modifier.testTag("radio_mode_virtual")
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Column {
                            Text("ভার্চুয়াল এআই সিম (উইথ ইন্টারনেট)", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Text("Simulated via Gemini AI. Fully interactive.", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isVirtual = false },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = !isVirtual,
                            onClick = { isVirtual = false },
                            modifier = Modifier.testTag("radio_mode_real")
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Column {
                            Text("রিয়েল মোবাইল ক্যারিয়ার (উইথ সিগনাল)", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Text("Transmits SMS using physical service tower.", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                if (isVirtual) {
                    OutlinedTextField(
                        value = aiPrompt,
                        onValueChange = { aiPrompt = it },
                        label = { Text("এআই প্রম্পট ব্যক্তিত্ব (AI Prompt Persona)") },
                        placeholder = { Text("উদা: You are my dad. Reply supportively in Bengali.") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("add_contact_prompt_input")
                            .height(80.dp),
                        maxLines = 3
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.trim().isNotEmpty() && phoneNumber.trim().isNotEmpty()) {
                        onAdd(name, phoneNumber, isVirtual, aiPrompt)
                    }
                },
                modifier = Modifier.testTag("submit_contact_button")
            ) {
                Text("যুক্ত করুন (Add)")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("বাতিল (Cancel)")
            }
        }
    )
}
