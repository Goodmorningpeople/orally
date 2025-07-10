package com.example.orally

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.orally.ui.theme.OrallyTheme
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OrallyTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val user = FirebaseAuth.getInstance().currentUser
                    if (user?.email != null) {
                        HomeScreen()
                    } else {
                        AuthScreen()
                    }
                }
            }
        }
    }
}

@Composable
fun AuthScreen() {
    var showLogin by remember { mutableStateOf(true) }

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = showLogin,
            transitionSpec = {
                slideInHorizontally(
                    initialOffsetX = { if (targetState) -it else it },
                    animationSpec = tween(durationMillis = 500, easing = EaseInOutCubic)
                ) + fadeIn(
                    animationSpec = tween(durationMillis = 500)
                ) togetherWith slideOutHorizontally(
                    targetOffsetX = { if (targetState) it else -it },
                    animationSpec = tween(durationMillis = 500, easing = EaseInOutCubic)
                ) + fadeOut(
                    animationSpec = tween(durationMillis = 300)
                )
            },
            label = "auth_transition"
        ) { isLogin ->
            if (isLogin) {
                LoginScreen(onSwitchToSignUp = { showLogin = false })
            } else {
                SignUpScreen(onSwitchToLogin = { showLogin = true })
            }
        }
    }
}

@Composable
fun LoginScreen(onSwitchToSignUp: () -> Unit) {
    val context = LocalContext.current
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val auth = FirebaseAuth.getInstance()

    // Animation states
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        visible = true
    }

    fun loginWithEmailAndPassword(email: String, password: String) {
        isLoading = true
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                isLoading = false
                if (task.isSuccessful) {
                    Toast.makeText(context, "Login Successful", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Login Failed", Toast.LENGTH_SHORT).show()
                }
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
            .animateContentSize(),
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
                text = "Welcome Back! 👋",
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
            TextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(
                initialOffsetY = { it / 3 },
                animationSpec = tween(durationMillis = 600, delayMillis = 300)
            ) + fadeIn(animationSpec = tween(durationMillis = 600, delayMillis = 300))
        ) {
            TextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
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
                    if (email.isNotBlank() && password.isNotBlank()) {
                        loginWithEmailAndPassword(email, password)
                    } else {
                        Toast.makeText(context, "Please enter email and password", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(),
                enabled = !isLoading
            ) {
                Text(if (isLoading) "Logging in..." else "Log In")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(
                initialOffsetY = { it / 3 },
                animationSpec = tween(durationMillis = 600, delayMillis = 500)
            ) + fadeIn(animationSpec = tween(durationMillis = 600, delayMillis = 500))
        ) {
            Text(
                text = "Don't have an account? Sign up",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.clickable { onSwitchToSignUp() }
            )
        }
    }
}

@Composable
fun SignUpScreen(onSwitchToLogin: () -> Unit) {
    val context = LocalContext.current
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val auth = FirebaseAuth.getInstance()

    // Animation states
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        visible = true
    }

    fun signUpWithEmailAndPassword(email: String, password: String) {
        isLoading = true
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                isLoading = false
                if (task.isSuccessful) {
                    Toast.makeText(context, "Sign Up Successful", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Sign Up Failed", Toast.LENGTH_SHORT).show()
                }
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
            .animateContentSize(),
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
                text = "Create Account ✨",
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
            TextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(
                initialOffsetY = { it / 3 },
                animationSpec = tween(durationMillis = 600, delayMillis = 300)
            ) + fadeIn(animationSpec = tween(durationMillis = 600, delayMillis = 300))
        ) {
            TextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
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
                    if (email.isNotBlank() && password.isNotBlank()) {
                        signUpWithEmailAndPassword(email, password)
                    } else {
                        Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(),
                enabled = !isLoading
            ) {
                Text(if (isLoading) "Signing up..." else "Sign Up")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(
                initialOffsetY = { it / 3 },
                animationSpec = tween(durationMillis = 600, delayMillis = 500)
            ) + fadeIn(animationSpec = tween(durationMillis = 600, delayMillis = 500))
        ) {
            Text(
                text = "Already have an account? Log in",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.clickable { onSwitchToLogin() }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AuthScreenPreview() {
    OrallyTheme {
        AuthScreen()
    }
}