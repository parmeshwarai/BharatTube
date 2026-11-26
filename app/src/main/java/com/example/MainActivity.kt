package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.firebase.BharatTubeRepository
import com.example.data.firebase.GeminiAiService
import com.example.data.firebase.attemptAutoSignIn
import com.example.data.firebase.authStateFlow
import com.example.data.firebase.configureAppCheck
import com.example.data.firebase.signOutUser
import com.example.ui.BharatTubeApp
import com.example.ui.screens.AuthScreen
import com.example.ui.theme.BharatTubeTheme
import com.example.viewmodel.BharatTubeViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureAppCheck(this, intent)
        enableEdgeToEdge()

        setContent {
            val scope = rememberCoroutineScope()
            val credentialManager = remember(this) { CredentialManager.create(this) }
            val currentUser by Firebase.auth.authStateFlow()
                .collectAsStateWithLifecycle(initialValue = Firebase.auth.currentUser)

            var autoSignInChecked by remember { mutableStateOf(Firebase.auth.currentUser != null) }

            LaunchedEffect(Unit) {
                if (Firebase.auth.currentUser == null) {
                    attemptAutoSignIn(
                        context = this@MainActivity,
                        credentialManager = credentialManager,
                        onAuthSuccess = { autoSignInChecked = true },
                        onUnauthenticated = { autoSignInChecked = true },
                        scope = scope
                    )
                }
            }

            if (currentUser == null) {
                BharatTubeTheme(themeMode = "DARK") {
                    if (!autoSignInChecked) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.background),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else {
                        AuthScreen(onAuthSuccess = { autoSignInChecked = true })
                    }
                }
            } else {
                val uid = currentUser!!.uid
                key(uid) {
                    val vm: BharatTubeViewModel = viewModel(
                        key = uid,
                        factory = object : ViewModelProvider.Factory {
                            @Suppress("UNCHECKED_CAST")
                            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                                return BharatTubeViewModel(
                                    repository = BharatTubeRepository(applicationContext),
                                    geminiAiService = GeminiAiService(),
                                    currentUserId = uid
                                ) as T
                            }
                        }
                    )
                    val userProfile by vm.userProfile.collectAsStateWithLifecycle()

                    BharatTubeTheme(
                        themeMode = userProfile.darkThemeMode,
                        highContrast = userProfile.highContrastMode,
                        fontScale = userProfile.fontScaleMultiplier.toFloat().coerceIn(0.85f, 1.35f)
                    ) {
                        BharatTubeApp(
                            viewModel = vm,
                            onSignOut = {
                                signOutUser(
                                    credentialManager = credentialManager,
                                    scope = scope
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}
