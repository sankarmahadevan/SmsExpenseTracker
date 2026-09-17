package com.example.smsexpensetracker

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.smsexpensetracker.ui.dashboard.DashboardScreen
import com.example.smsexpensetracker.ui.dashboard.DashboardViewModelFactory
import com.example.smsexpensetracker.ui.permission.PermissionGateScreen
import com.example.smsexpensetracker.ui.settings.SettingsScreen
import com.example.smsexpensetracker.ui.settings.SettingsViewModelFactory
import com.example.smsexpensetracker.ui.theme.SmsExpenseTheme
import com.example.smsexpensetracker.ui.transactions.AddEditTransactionScreen
import com.example.smsexpensetracker.ui.transactions.TransactionViewModelFactory
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.fragment.app.FragmentActivity
import com.example.smsexpensetracker.ui.auth.AuthGateScreen

object Routes {
    const val PERMISSION = "permission"
    const val DASHBOARD  = "dashboard"
    const val ADD_EDIT   = "add_edit/{id}"
    const val SETTINGS   = "settings"

    fun addEdit(id: Long = -1L) = "add_edit/$id"
}

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SmsExpenseTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavigation()
                }
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val context = LocalContext.current
    val app     = context.applicationContext as SmsExpenseApp
    val nav     = rememberNavController()
    val lifecycleOwner = LocalLifecycleOwner.current

    var isUnlocked by rememberSaveable { mutableStateOf(false) }

    // Re-lock whenever the app goes to background (Activity onPause/onStop)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                isUnlocked = false
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (!isUnlocked) {
        AuthGateScreen(onAuthenticated = { isUnlocked = true })
        return
    }

    val smsGranted = ContextCompat.checkSelfPermission(
        context, Manifest.permission.RECEIVE_SMS
    ) == PackageManager.PERMISSION_GRANTED

    NavHost(
        navController    = nav,
        startDestination = if (smsGranted) Routes.DASHBOARD else Routes.PERMISSION
    ) {

        composable(Routes.PERMISSION) {
            PermissionGateScreen(
                onPermissionGranted = {
                    nav.navigate(Routes.DASHBOARD) {
                        popUpTo(Routes.PERMISSION) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.DASHBOARD) {
            val vm = viewModel<com.example.smsexpensetracker.ui.dashboard.DashboardViewModel>(
                factory = DashboardViewModelFactory(app.repository)
            )
            DashboardScreen(
                viewModel         = vm,
                onAddTransaction  = { nav.navigate(Routes.addEdit()) },
                onEditTransaction = { id -> nav.navigate(Routes.addEdit(id)) },
                onSettings        = { nav.navigate(Routes.SETTINGS) }
            )
        }

        composable(
            route     = Routes.ADD_EDIT,
            arguments = listOf(navArgument("id") { type = NavType.LongType; defaultValue = -1L })
        ) { backStack ->
            val id = backStack.arguments?.getLong("id") ?: -1L
            val vm = viewModel<com.example.smsexpensetracker.ui.transactions.TransactionViewModel>(
                factory = TransactionViewModelFactory(app.repository, id)
            )
            AddEditTransactionScreen(viewModel = vm, onDone = { nav.popBackStack() })
        }

        composable(Routes.SETTINGS) {
            val vm = viewModel<com.example.smsexpensetracker.ui.settings.SettingsViewModel>(
                factory = SettingsViewModelFactory(app.repository)
            )
            SettingsScreen(viewModel = vm, onBack = { nav.popBackStack() })
        }
    }
}
