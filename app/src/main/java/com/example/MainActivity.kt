package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.auth.AuthScreen
import com.example.data.OfflineAuthRepository
import com.example.ui.LedgerAuthenticatedApp
import com.example.ui.theme.LedgerPulseTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private var sharedTextState by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        extractSharedText(intent)

        setContent {
            LedgerPulseTheme {
                AppContent(
                    sharedIncomingText = sharedTextState,
                    onConsumedSharedText = { sharedTextState = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        extractSharedText(intent)
    }

    private fun extractSharedText(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val incoming = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (!incoming.isNullOrBlank()) {
                sharedTextState = incoming
            }
        }
    }
}

@Composable
private fun AppContent(
    sharedIncomingText: String?,
    onConsumedSharedText: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val authRepository = remember(context) { OfflineAuthRepository(context) }

    val currentUser by authRepository.currentUserFlow.collectAsStateWithLifecycle(initialValue = null)
    val existingAccounts by authRepository.existingAccountsFlow.collectAsStateWithLifecycle(initialValue = emptyList())

    val user = currentUser
    if (user == null) {
        AuthScreen(
            authRepository = authRepository,
            existingAccounts = existingAccounts
        )
    } else {
        key(user.userId) {
            LedgerAuthenticatedApp(
                sessionUser = user,
                sharedIncomingText = sharedIncomingText,
                onConsumedSharedText = onConsumedSharedText,
                onLogout = {
                    scope.launch {
                        authRepository.logout()
                    }
                }
            )
        }
    }
}
