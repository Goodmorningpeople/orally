package com.example.orally

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

                tip = if (savedTipDate != currentDate) {
                    tips.random().also {
                        userDoc.update(mapOf("dailyTip" to it, "tipDate" to currentDate))
                    }
                } else {
                    savedTip ?: tips.random()
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
        AnimatedVisibility(visible) {
            Text("Hello, $userName 👋", style = MaterialTheme.typography.headlineMedium)
        }

        Spacer(Modifier.height(24.dp))

        AnimatedVisibility(visible) {
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
                        Text("Streak", style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(8.dp))
                        Text("$streak", style = MaterialTheme.typography.displayMedium)
                        Text("🔥", style = MaterialTheme.typography.headlineLarge)
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
                        Text("Today", style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(8.dp))
                        if (streak > 0) {
                            Text("✅", style = MaterialTheme.typography.displayMedium)
                            Text("Done!", style = MaterialTheme.typography.titleMedium)
                        } else {
                            Text("⏰", style = MaterialTheme.typography.displayMedium)
                            Text("Pending", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        AnimatedVisibility(visible) {
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
                    Text("🦷 Oral Tip Of The Day", style = MaterialTheme.typography.titleLarge)
                    Text(tip, style = MaterialTheme.typography.bodyLarge)
                    Text("💡", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.align(Alignment.CenterHorizontally))
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        AnimatedVisibility(visible) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .height(100.dp)
                        .clickable { onNavigateToTab(1) },
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Text("▶️ Go to Practice", style = MaterialTheme.typography.titleMedium)
                    }
                }

                Card(
                    modifier = Modifier
                        .weight(1f)
                        .height(100.dp)
                        .clickable { onNavigateToTab(3) },
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Text("🏆 View Rewards", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }

        Spacer(Modifier.height(74.dp))

        AnimatedVisibility(visible) {
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