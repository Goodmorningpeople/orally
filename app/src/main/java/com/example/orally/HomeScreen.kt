package com.example.orally

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import com.google.firebase.firestore.DocumentSnapshot

sealed class BottomNavItem(
    val route: String,
    val emoji: String,
    val label: String
) {
    object Home : BottomNavItem("home", "🏠", "Home")
    object Practice : BottomNavItem("practice", "▶️", "Practice")
    object Notes: BottomNavItem("notes", "\uD83D\uDCDD", "Notes")
    object Rewards : BottomNavItem("rewards", "\uD83E\uDD47", "Rewards")
    object Profile : BottomNavItem("profile", "👤", "Profile")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    var selectedTab by remember { mutableStateOf(0) }
    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Practice,
        BottomNavItem.Notes,
        BottomNavItem.Rewards,
        BottomNavItem.Profile
    )

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                items.forEachIndexed { index, item ->
                    NavigationBarItem(
                        icon = {
                            Text(
                                text = item.emoji,
                                style = MaterialTheme.typography.headlineSmall
                            )
                        },
                        label = { Text(item.label) },
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        AnimatedContent(
            targetState = selectedTab,
            transitionSpec = {
                val direction = if (targetState > initialState) 1 else -1
                slideInHorizontally(
                    initialOffsetX = { it * direction },
                    animationSpec = tween(durationMillis = 400, easing = EaseInOutCubic)
                ) + fadeIn(
                    animationSpec = tween(durationMillis = 400)
                ) togetherWith slideOutHorizontally(
                    targetOffsetX = { -it * direction },
                    animationSpec = tween(durationMillis = 400, easing = EaseInOutCubic)
                ) + fadeOut(
                    animationSpec = tween(durationMillis = 200)
                )
            },
            label = "tab_transition"
        ) { tabIndex ->
            when (tabIndex) {
                0 -> HomeContent(modifier = Modifier.padding(paddingValues))
                1 -> PracticeContent(modifier = Modifier.padding(paddingValues))
                2 -> NotesContent(modifier = Modifier.padding(paddingValues))
                3 -> ProgressContent(modifier = Modifier.padding(paddingValues))
                4 -> ProfileContent(modifier = Modifier.padding(paddingValues))
            }
        }
    }
}

@Composable
fun HomeContent(modifier: Modifier = Modifier) {
    val auth = FirebaseAuth.getInstance()
    val user = auth.currentUser

    var userName by remember { mutableStateOf("User") }
    var streak by remember { mutableStateOf(0) }
    var tip by remember { mutableStateOf("Loading...") }
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        visible = true
    }

    LaunchedEffect(user?.uid) {
        user?.let {
            userName = it.displayName ?: it.email?.substringBefore("@") ?: "Anonymous"

            val db = FirebaseFirestore.getInstance()
            try {
                val userDoc = db.collection("users").document(it.uid)
                val doc: DocumentSnapshot = userDoc.get().await()

                val currentDate = getCurrentDateString()
                val lastLoginDate = doc.getString("lastLoginDate")
                val currentStreak = doc.getLong("streak")?.toInt() ?: 0

                when {
                    lastLoginDate == null -> {
                        streak = 1
                        userDoc.update(mapOf(
                            "streak" to 1,
                            "lastLoginDate" to currentDate
                        ))
                    }
                    lastLoginDate == currentDate -> {
                        streak = currentStreak
                    }
                    isNextDay(lastLoginDate, currentDate) -> {
                        streak = currentStreak + 1
                        userDoc.update(mapOf(
                            "streak" to streak,
                            "lastLoginDate" to currentDate
                        ))
                    }
                    else -> {
                        streak = 1
                        userDoc.update(mapOf(
                            "streak" to 1,
                            "lastLoginDate" to currentDate
                        ))
                    }
                }

                val tips = listOf(
                    "Brush your teeth for at least 2 minutes twice daily",
                    "Don't forget to floss daily to remove plaque between teeth",
                    "Use fluoride toothpaste to strengthen your enamel",
                    "Replace your toothbrush every 3-4 months",
                    "Rinse with mouthwash after brushing and flossing",
                    "Limit sugary and acidic foods and drinks",
                    "Visit your dentist regularly for check-ups",
                    "Don't brush immediately after eating acidic foods",
                    "Use a soft-bristled toothbrush to protect your gums",
                    "Stay hydrated - water helps wash away bacteria"
                )

                val savedTip = doc.getString("dailyTip")
                val savedTipDate = doc.getString("tipDate")

                if (savedTipDate != currentDate) {
                    tip = tips.random()
                    userDoc.update(mapOf(
                        "dailyTip" to tip,
                        "tipDate" to currentDate
                    ))
                } else {
                    tip = savedTip ?: tips.random()
                }

            } catch (e: Exception) {
                tip = "Stay consistent and brush twice a day!"
                streak = 0
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(
                initialOffsetY = { -it / 3 },
                animationSpec = tween(durationMillis = 600, delayMillis = 100)
            ) + fadeIn(animationSpec = tween(durationMillis = 600, delayMillis = 100))
        ) {
            Text(
                text = "Hello, $userName 👋",
                style = MaterialTheme.typography.headlineMedium
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(
                initialOffsetY = { it / 3 },
                animationSpec = tween(durationMillis = 600, delayMillis = 200)
            ) + fadeIn(animationSpec = tween(durationMillis = 600, delayMillis = 200))
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Streak",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "$streak",
                            style = MaterialTheme.typography.displayMedium
                        )
                        Text(
                            text = "🔥",
                            style = MaterialTheme.typography.headlineLarge
                        )
                    }
                }

                Card(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Today",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        if (streak > 0) {
                            Text(
                                text = "✅",
                                style = MaterialTheme.typography.displayMedium
                            )
                            Text(
                                text = "Done!",
                                style = MaterialTheme.typography.titleMedium
                            )
                        } else {
                            Text(
                                text = "⏰",
                                style = MaterialTheme.typography.displayMedium
                            )
                            Text(
                                text = "Pending",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(
                initialOffsetY = { it / 3 },
                animationSpec = tween(durationMillis = 600, delayMillis = 300)
            ) + fadeIn(animationSpec = tween(durationMillis = 600, delayMillis = 300))
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                shape = RoundedCornerShape(28.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "🦷 Oral Tip Of The Day",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = tip,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "💡",
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(
                initialOffsetY = { it / 3 },
                animationSpec = tween(durationMillis = 600, delayMillis = 400)
            ) + fadeIn(animationSpec = tween(durationMillis = 600, delayMillis = 400))
        ) {
            Text(
                text = "Keep up the great work!",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }
    }
}

private fun getCurrentDateString(): String {
    val calendar = java.util.Calendar.getInstance()
    val year = calendar.get(java.util.Calendar.YEAR)
    val month = calendar.get(java.util.Calendar.MONTH) + 1
    val day = calendar.get(java.util.Calendar.DAY_OF_MONTH)
    return String.format("%04d-%02d-%02d", year, month, day)
}

private fun isNextDay(lastLoginDate: String, currentDate: String): Boolean {
    return try {
        val lastParts = lastLoginDate.split("-")
        val currentParts = currentDate.split("-")

        if (lastParts.size != 3 || currentParts.size != 3) return false

        val lastYear = lastParts[0].toInt()
        val lastMonth = lastParts[1].toInt()
        val lastDay = lastParts[2].toInt()

        val currentYear = currentParts[0].toInt()
        val currentMonth = currentParts[1].toInt()
        val currentDay = currentParts[2].toInt()

        val lastCalendar = java.util.Calendar.getInstance().apply {
            set(lastYear, lastMonth - 1, lastDay)
        }
        val currentCalendar = java.util.Calendar.getInstance().apply {
            set(currentYear, currentMonth - 1, currentDay)
        }

        lastCalendar.add(java.util.Calendar.DAY_OF_MONTH, 1)

        return lastCalendar.get(java.util.Calendar.YEAR) == currentCalendar.get(java.util.Calendar.YEAR) &&
                lastCalendar.get(java.util.Calendar.MONTH) == currentCalendar.get(java.util.Calendar.MONTH) &&
                lastCalendar.get(java.util.Calendar.DAY_OF_MONTH) == currentCalendar.get(java.util.Calendar.DAY_OF_MONTH)
    } catch (e: Exception) {
        false
    }
}

@Composable
fun PracticeContent(modifier: Modifier = Modifier) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        visible = true
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(
                initialOffsetY = { -it / 3 },
                animationSpec = tween(durationMillis = 600, delayMillis = 100)
            ) + fadeIn(animationSpec = tween(durationMillis = 600, delayMillis = 100))
        ) {
            Text(
                text = "▶️",
                style = MaterialTheme.typography.displayLarge
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(
                initialOffsetY = { it / 3 },
                animationSpec = tween(durationMillis = 600, delayMillis = 200)
            ) + fadeIn(animationSpec = tween(durationMillis = 600, delayMillis = 200))
        ) {
            Text(
                text = "Practice Your Oral Care",
                style = MaterialTheme.typography.headlineMedium
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(
                initialOffsetY = { it / 3 },
                animationSpec = tween(durationMillis = 600, delayMillis = 300)
            ) + fadeIn(animationSpec = tween(durationMillis = 600, delayMillis = 300))
        ) {
            Text(
                text = "Start your daily oral care routine",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(
                initialOffsetY = { it / 3 },
                animationSpec = tween(durationMillis = 600, delayMillis = 400)
            ) + fadeIn(animationSpec = tween(durationMillis = 600, delayMillis = 400))
        ) {
            Button(
                onClick = { /* TODO: Implement practice */ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Start Practice Session")
            }
        }
    }
}

@Composable
fun ProgressContent(modifier: Modifier = Modifier) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        visible = true
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(
                initialOffsetY = { -it / 3 },
                animationSpec = tween(durationMillis = 600, delayMillis = 100)
            ) + fadeIn(animationSpec = tween(durationMillis = 600, delayMillis = 100))
        ) {
            Text(
                text = "\uD83E\uDD47",
                style = MaterialTheme.typography.displayLarge
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(
                initialOffsetY = { it / 3 },
                animationSpec = tween(durationMillis = 600, delayMillis = 200)
            ) + fadeIn(animationSpec = tween(durationMillis = 600, delayMillis = 200))
        ) {
            Text(
                text = "Your rewards",
                style = MaterialTheme.typography.headlineMedium
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(
                initialOffsetY = { it / 3 },
                animationSpec = tween(durationMillis = 600, delayMillis = 300)
            ) + fadeIn(animationSpec = tween(durationMillis = 600, delayMillis = 300))
        ) {
            Text(
                text = "Get rewards and recognition for your efforts",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ProfileContent(modifier: Modifier = Modifier) {
    val auth = FirebaseAuth.getInstance()
    val user = auth.currentUser
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        visible = true
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(
                initialOffsetY = { -it / 3 },
                animationSpec = tween(durationMillis = 600, delayMillis = 100)
            ) + fadeIn(animationSpec = tween(durationMillis = 600, delayMillis = 100))
        ) {
            Text(
                text = "👤",
                style = MaterialTheme.typography.displayLarge
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(
                initialOffsetY = { it / 3 },
                animationSpec = tween(durationMillis = 600, delayMillis = 200)
            ) + fadeIn(animationSpec = tween(durationMillis = 600, delayMillis = 200))
        ) {
            Text(
                text = "Profile",
                style = MaterialTheme.typography.headlineMedium
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(
                initialOffsetY = { it / 3 },
                animationSpec = tween(durationMillis = 600, delayMillis = 300)
            ) + fadeIn(animationSpec = tween(durationMillis = 600, delayMillis = 300))
        ) {
            Text(
                text = user?.email ?: "No email",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(
                initialOffsetY = { it / 3 },
                animationSpec = tween(durationMillis = 600, delayMillis = 400)
            ) + fadeIn(animationSpec = tween(durationMillis = 600, delayMillis = 400))
        ) {
            Button(
                onClick = {
                    auth.signOut()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Sign Out")
            }
        }
    }
}

@Composable
fun NotesContent(modifier: Modifier = Modifier) {
    var visible by remember { mutableStateOf(false) }
    var notes by remember { mutableStateOf<List<DocumentSnapshot>>(emptyList()) }
    var selectedNote by remember { mutableStateOf<DocumentSnapshot?>(null) }
    var showEditor by remember { mutableStateOf(false) }

    val db = FirebaseFirestore.getInstance()
    val user = FirebaseAuth.getInstance().currentUser
    val uid = user?.uid

    LaunchedEffect(Unit) {
        visible = true
        if (uid != null) {
            db.collection("users").document(uid).collection("notes")
                .addSnapshotListener { value, _ ->
                    if (value != null) notes = value.documents
                }
        }
    }

    AnimatedContent(
        targetState = showEditor,
        transitionSpec = {
            slideInHorizontally(
                initialOffsetX = { if (targetState) it else -it },
                animationSpec = tween(500, easing = EaseInOutCubic)
            ) + fadeIn(animationSpec = tween(500)) togetherWith
                    slideOutHorizontally(
                        targetOffsetX = { if (targetState) -it else it },
                        animationSpec = tween(500, easing = EaseInOutCubic)
                    ) + fadeOut(animationSpec = tween(300))
        },
        label = "notes_transition"
    ) { isEditing ->
        if (isEditing) {
            NoteEditor(
                note = selectedNote,
                onSave = { title, content ->
                    if (uid != null) {
                        val collection = db.collection("users").document(uid).collection("notes")
                        if (selectedNote == null) {
                            collection.add(mapOf("title" to title, "content" to content))
                        } else {
                            collection.document(selectedNote!!.id).update(
                                mapOf("title" to title, "content" to content)
                            )
                        }
                    }
                    selectedNote = null
                    showEditor = false
                },
                onCancel = {
                    selectedNote = null
                    showEditor = false
                }
            )
        } else {
            NotesListView(
                notes = notes,
                visible = visible,
                onAddNote = {
                    selectedNote = null
                    showEditor = true
                },
                onEditNote = {
                    selectedNote = it
                    showEditor = true
                },
                onDeleteNote = { note ->
                    uid?.let {
                        db.collection("users").document(it)
                            .collection("notes").document(note.id)
                            .delete()
                    }
                },
                modifier = modifier
            )
        }
    }
}

@Composable
fun NoteEditor(
    note: DocumentSnapshot?,
    onSave: (String, String) -> Unit,
    onCancel: () -> Unit
) {
    var title by remember { mutableStateOf(note?.getString("title") ?: "") }
    var content by remember { mutableStateOf(note?.getString("content") ?: "") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (note == null) "New Note 📝" else "Edit Note 🖊️",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Title") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = content,
            onValueChange = { content = it },
            label = { Text("Content") },
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            maxLines = 10,
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(Modifier.height(24.dp))

        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedButton(onClick = onCancel) {
                Text("Cancel")
            }
            Button(onClick = {
                if (title.isNotBlank() || content.isNotBlank()) {
                    onSave(title.trim(), content.trim())
                }
            }) {
                Text("Save")
            }
        }
    }
}

@Composable
fun NotesListView(
    notes: List<DocumentSnapshot>,
    visible: Boolean,
    onAddNote: () -> Unit,
    onEditNote: (DocumentSnapshot) -> Unit,
    onDeleteNote: (DocumentSnapshot) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AnimatedVisibility(visible) {
            Text("📝", style = MaterialTheme.typography.displayLarge)
        }

        Spacer(Modifier.height(16.dp))

        AnimatedVisibility(visible) {
            Text("Your Notes", style = MaterialTheme.typography.headlineMedium)
        }

        Spacer(Modifier.height(24.dp))

        notes.forEach { note ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                ) {
                    Text(text = note.getString("title") ?: "Untitled", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(text = note.getString("content") ?: "", style = MaterialTheme.typography.bodyMedium)

                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                        TextButton(onClick = { onEditNote(note) }) {
                            Text("Edit")
                        }
                        Spacer(Modifier.width(8.dp))
                        TextButton(onClick = { onDeleteNote(note) }) {
                            Text("Delete", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f))

        AnimatedVisibility(visible) {
            Button(
                onClick = onAddNote,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("➕ New Note")
            }
        }
    }
}