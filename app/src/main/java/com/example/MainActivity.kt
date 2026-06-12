package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.ui.chat.ChatScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.dashboard.DashboardScreen
import com.example.ui.dashboard.DashboardViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        val viewModel: DashboardViewModel = hiltViewModel()
        var currentDestination by rememberSaveable() { mutableStateOf(ChatNavDestination.HOME) }

        val backgroundLight = remember { Color(0xFFF3F4F9) }
        val cardSurface = remember { Color(0xFFFDFBFF) }
        val cardBorder = remember { Color(0xFFC4C6D0) }
        val textPrimary = remember { Color(0xFF1A1C1E) }
        val textSecondary = remember { Color(0xFF44474E) }
        val textMuted = remember { Color(0xFF74777F) }
        val accentContainer = remember { Color(0xFFD6E3FF) }
        val accentOnContainer = remember { Color(0xFF001B3E) }
        val greenCredit = remember { Color(0xFF116D34) }
        val redDebit = remember { Color(0xFFBA1A1A) }

        NavigationSuiteScaffold(
          navigationSuiteItems = {
            ChatNavDestination.entries.forEach { destination ->
              item(
                icon = {
                  Icon(
                    painter = painterResource(destination.icon),
                    contentDescription = destination.label,
                    modifier = Modifier.background(accentContainer)
                  )
                },
                modifier = Modifier.background(accentContainer),
                label = { Text(destination.label) },
                selected = destination == currentDestination,
                onClick = { currentDestination = destination },
              )
            }
          }
        ) {
          Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
              when (currentDestination) {
                ChatNavDestination.HOME -> DashboardScreen(
                  viewModel = viewModel,
                  modifier = Modifier.padding(innerPadding))
                ChatNavDestination.CHAT -> ChatScreen(
                  modifier =  Modifier.padding(innerPadding))

              }
            }
          }
        }
      }
    }
  }
}

enum class ChatNavDestination(
  val label: String,
  val icon: Int,
) {
  HOME("Home", R.drawable.ic_home),
  CHAT("Chat", R.drawable.ic_home)
}