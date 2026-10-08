package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import com.example.data.repository.TodoRepository
import com.example.ui.AuthScreen
import com.example.ui.TodoListScreen
import com.example.ui.TodoViewModel
import com.example.ui.attemptAutoSignIn
import com.example.ui.signOut
import com.example.ui.theme.PrioriTaskTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PrioriTaskTheme {
                AppRootContent()
            }
        }
    }
}

@Composable
fun AppRootContent() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }
    val credentialManager = remember { CredentialManager.create(context) }

    // Listen to Auth state changes
    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose {
            Firebase.auth.removeAuthStateListener(listener)
        }
    }

    // Attempt silent auto-sign in on launch if not currently authenticated
    LaunchedEffect(Unit) {
        if (currentUser == null) {
            attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = {
                    currentUser = Firebase.auth.currentUser
                },
                onUnauthenticated = {
                    // Ready for interactive sign in
                },
                scope = scope
            )
        }
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        val user = currentUser
        if (user == null) {
            AuthScreen(
                onAuthSuccess = {
                    currentUser = Firebase.auth.currentUser
                }
            )
        } else {
            val repository = remember(user.uid) {
                TodoRepository(context)
            }
            val viewModel = remember(user.uid) {
                TodoViewModel(
                    repository = repository,
                    userId = user.uid,
                    userEmail = user.email ?: "",
                    userDisplayName = user.displayName ?: ""
                )
            }

            TodoListScreen(
                viewModel = viewModel,
                onSignOut = {
                    signOut(
                        context = context,
                        credentialManager = credentialManager,
                        onSignOutComplete = {
                            currentUser = null
                        },
                        scope = scope
                    )
                }
            )
        }
    }
}
