package com.example.orally

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

sealed class BottomNavItem(val route: String, val emoji: String, val label: String) {
    object Home : BottomNavItem("home", "🏠", "Home")
    object Practice : BottomNavItem("practice", "▶️", "Practice")
    object Notes : BottomNavItem("notes", "\uD83D\uDCDD", "Notes")
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
                            Text(text = item.emoji, style = MaterialTheme.typography.headlineSmall)
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
                    animationSpec = tween(400, easing = EaseInOutCubic)
                ) + fadeIn(tween(400)) togetherWith
                        slideOutHorizontally(
                            targetOffsetX = { -it * direction },
                            animationSpec = tween(400, easing = EaseInOutCubic)
                        ) + fadeOut(tween(200))
            },
            label = "tab_transition"
        ) { tabIndex ->
            when (tabIndex) {
                0 -> HomeContent(
                    modifier = Modifier.padding(paddingValues),
                    onNavigateToTab = { selectedTab = it }
                )
                1 -> PracticeContent(modifier = Modifier.padding(paddingValues))
                2 -> NotesContent(modifier = Modifier.padding(paddingValues))
                3 -> RewardsContent(modifier = Modifier.padding(paddingValues))
                4 -> ProfileContent(modifier = Modifier.padding(paddingValues))
            }
        }
    }
}

@Composable
fun HomeContent(modifier: Modifier = Modifier, onNavigateToTab: (Int) -> Unit) {
    val auth = FirebaseAuth.getInstance()
    val user = auth.currentUser

    var userName by remember { mutableStateOf("User") }
    var streak by remember { mutableStateOf(0) }
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
                        userDoc.update(mapOf("streak" to 1, "lastLoginDate" to currentDate))
                    }
                    lastLoginDate == currentDate -> {
                        streak = currentStreak
                    }
                    isNextDay(lastLoginDate, currentDate) -> {
                        streak = currentStreak + 1
                        userDoc.update(mapOf("streak" to streak, "lastLoginDate" to currentDate))
                    }
                    else -> {
                        streak = 1
                        userDoc.update(mapOf("streak" to 1, "lastLoginDate" to currentDate))
                    }
                }

            } catch (e: Exception) {
                streak = 0
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AnimatedVisibility(visible) {
                Text(
                    text = "Welcome back",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.align(Alignment.Start)
                )
            }

            Spacer(Modifier.height(24.dp))

            AnimatedVisibility(visible) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(vertical = 28.dp, horizontal = 20.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text("Current Streak", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "$streak ",
                                style = MaterialTheme.typography.headlineLarge
                            )
                            Text("🔥", style = MaterialTheme.typography.headlineLarge)
                        }
                    }
                }
            }

            AnimatedVisibility(visible) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(vertical = 28.dp, horizontal = 20.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").forEach { day ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f), CircleShape)
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(day, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(visible) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToTab(1) },
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(vertical = 28.dp, horizontal = 20.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("📘", style = MaterialTheme.typography.headlineMedium)
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("Start a practice", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "Select a topic and start practicing your oral skills.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Text("➡️", style = MaterialTheme.typography.titleLarge)
                    }
                }
            }

            Spacer(Modifier.height(74.dp))
        }

        AnimatedVisibility(visible, modifier = Modifier.align(Alignment.BottomCenter).padding(24.dp)) {
            Text(
                text = "Keep up the great work!",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun getCurrentDateString(): String {
    val calendar = java.util.Calendar.getInstance()
    return String.format(
        "%04d-%02d-%02d",
        calendar.get(java.util.Calendar.YEAR),
        calendar.get(java.util.Calendar.MONTH) + 1,
        calendar.get(java.util.Calendar.DAY_OF_MONTH)
    )
}

private fun isNextDay(lastLoginDate: String, currentDate: String): Boolean {
    return try {
        val (ly, lm, ld) = lastLoginDate.split("-").map { it.toInt() }
        val (cy, cm, cd) = currentDate.split("-").map { it.toInt() }
        val lastCal = java.util.Calendar.getInstance().apply { set(ly, lm - 1, ld) }
        val currCal = java.util.Calendar.getInstance().apply { set(cy, cm - 1, cd) }
        lastCal.add(java.util.Calendar.DAY_OF_MONTH, 1)
        lastCal.get(java.util.Calendar.YEAR) == currCal.get(java.util.Calendar.YEAR) &&
                lastCal.get(java.util.Calendar.MONTH) == currCal.get(java.util.Calendar.MONTH) &&
                lastCal.get(java.util.Calendar.DAY_OF_MONTH) == currCal.get(java.util.Calendar.DAY_OF_MONTH)
    } catch (e: Exception) {
        false
    }
}