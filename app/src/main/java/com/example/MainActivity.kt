package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.CreateVmWizardScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.VmDetailSettingsScreen
import com.example.ui.screens.VmRunnerScreen
import com.example.ui.screens.WindowsGuideScreen
import com.example.ui.theme.WinArmTheme
import com.example.ui.viewmodel.VmViewModel

const val ROUTE_HOME = "home"
const val ROUTE_CREATE = "create"
const val ROUTE_DETAIL = "detail"
const val ROUTE_RUNNER = "runner"
const val ROUTE_GUIDE = "guide"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WinArmTheme {
                WinArmApp()
            }
        }
    }
}

@Composable
fun WinArmApp(viewModel: VmViewModel = viewModel()) {
    val navController = rememberNavController()
    val selectedVmForEdit by viewModel.selectedVmForEdit.collectAsStateWithLifecycle()
    val runtimeState by viewModel.runtimeState.collectAsStateWithLifecycle()

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = ROUTE_HOME,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(ROUTE_HOME) {
                HomeScreen(
                    viewModel = viewModel,
                    onNavigateCreate = { navController.navigate(ROUTE_CREATE) },
                    onNavigateDetail = { vm ->
                        viewModel.selectVmForEdit(vm)
                        navController.navigate(ROUTE_DETAIL)
                    },
                    onNavigateRunner = { vm ->
                        navController.navigate(ROUTE_RUNNER)
                    },
                    onNavigateGuide = { navController.navigate(ROUTE_GUIDE) }
                )
            }

            composable(ROUTE_CREATE) {
                CreateVmWizardScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onVmCreated = { createdVm ->
                        navController.popBackStack()
                    }
                )
            }

            composable(ROUTE_DETAIL) {
                val vm = selectedVmForEdit
                if (vm != null) {
                    VmDetailSettingsScreen(
                        vm = vm,
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() },
                        onLaunchVm = { targetVm ->
                            viewModel.launchVm(targetVm)
                            navController.navigate(ROUTE_RUNNER)
                        }
                    )
                } else {
                    navController.popBackStack()
                }
            }

            composable(ROUTE_RUNNER) {
                val activeVm = runtimeState.vm ?: selectedVmForEdit
                if (activeVm != null) {
                    VmRunnerScreen(
                        vm = activeVm,
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() }
                    )
                } else {
                    navController.popBackStack()
                }
            }

            composable(ROUTE_GUIDE) {
                WindowsGuideScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
