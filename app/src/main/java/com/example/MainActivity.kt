package com.example

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoMode
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.outlined.AutoMode
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.TaskEditDialog
import com.example.ui.components.TopNavBar
import com.example.ui.components.WorkflowEditDialog
import com.example.ui.screens.ActionHubScreen
import com.example.ui.screens.AuditLogsScreen
import com.example.ui.screens.DailyTasksScreen
import com.example.ui.screens.WorkflowsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AppTab
import com.example.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                MainAppScreen(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainAppScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    var isOnline by remember { mutableStateOf(checkNetwork(context)) }

    // Listen to network connectivity changes
    DisposableEffect(context) {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                isOnline = true
            }

            override fun onLost(network: Network) {
                isOnline = false
            }
        }
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        cm.registerNetworkCallback(request, callback)

        onDispose {
            try {
                cm.unregisterNetworkCallback(callback)
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val forceOffline by viewModel.forceOfflineMode.collectAsStateWithLifecycle()
    val ttsEnabled by viewModel.speechManager.ttsEnabled.collectAsStateWithLifecycle()

    // Dialog state
    val showTaskDialog by viewModel.showTaskDialog.collectAsStateWithLifecycle()
    val editingTask by viewModel.editingTask.collectAsStateWithLifecycle()

    val showWorkflowDialog by viewModel.showWorkflowDialog.collectAsStateWithLifecycle()
    val editingWorkflow by viewModel.editingWorkflow.collectAsStateWithLifecycle()

    // Back handling: if not on Action Hub, return to Action Hub
    BackHandler(enabled = currentTab != AppTab.ACTION_HUB) {
        viewModel.selectTab(AppTab.ACTION_HUB)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopNavBar(
                isOnline = isOnline,
                forceOffline = forceOffline,
                ttsEnabled = ttsEnabled,
                onToggleTts = { viewModel.toggleTts() },
                onToggleForceOffline = { viewModel.toggleForceOffline() },
                onRunAutomations = { viewModel.runAutomationsNow() }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.testTag("bottom_nav_bar")
            ) {
                val tabs = listOf(
                    Triple(AppTab.ACTION_HUB, Icons.Filled.Bolt, Icons.Outlined.Bolt),
                    Triple(AppTab.DAILY_TASKS, Icons.Filled.Checklist, Icons.Outlined.Checklist),
                    Triple(AppTab.WORKFLOWS, Icons.Filled.AutoMode, Icons.Outlined.AutoMode),
                    Triple(AppTab.AUDIT_LOGS, Icons.Filled.History, Icons.Outlined.History)
                )

                tabs.forEach { (tab, filledIcon, outlinedIcon) ->
                    val selected = currentTab == tab
                    NavigationBarItem(
                        selected = selected,
                        onClick = { viewModel.selectTab(tab) },
                        icon = {
                            Icon(
                                imageVector = if (selected) filledIcon else outlinedIcon,
                                contentDescription = tab.title
                            )
                        },
                        label = { Text(tab.title) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.testTag("nav_item_${tab.name.lowercase()}")
                    )
                }
            }
        }
    ) { innerPadding ->
        when (currentTab) {
            AppTab.ACTION_HUB -> ActionHubScreen(
                viewModel = viewModel,
                modifier = Modifier.padding(innerPadding)
            )
            AppTab.DAILY_TASKS -> DailyTasksScreen(
                viewModel = viewModel,
                modifier = Modifier.padding(innerPadding)
            )
            AppTab.WORKFLOWS -> WorkflowsScreen(
                viewModel = viewModel,
                modifier = Modifier.padding(innerPadding)
            )
            AppTab.AUDIT_LOGS -> AuditLogsScreen(
                viewModel = viewModel,
                modifier = Modifier.padding(innerPadding)
            )
        }

        // Dialogs
        if (showTaskDialog) {
            TaskEditDialog(
                task = editingTask,
                onDismiss = { viewModel.closeTaskDialog() },
                onSave = { task -> viewModel.saveTask(task) }
            )
        }

        if (showWorkflowDialog) {
            WorkflowEditDialog(
                workflow = editingWorkflow,
                onDismiss = { viewModel.closeWorkflowDialog() },
                onSave = { workflow -> viewModel.saveWorkflow(workflow) }
            )
        }
    }
}

private fun checkNetwork(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
    val active = cm.activeNetwork ?: return false
    val caps = cm.getNetworkCapabilities(active) ?: return false
    return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}
