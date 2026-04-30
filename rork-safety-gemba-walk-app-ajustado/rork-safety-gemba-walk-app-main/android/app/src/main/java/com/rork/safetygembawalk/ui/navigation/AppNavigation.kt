package com.rork.safetygembawalk.ui.navigation

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.rork.safetygembawalk.ui.screens.HomeScreen
import com.rork.safetygembawalk.ui.screens.InspectionDetailScreen
import com.rork.safetygembawalk.ui.screens.LoginScreen
import com.rork.safetygembawalk.ui.screens.NewInspectionScreen
import com.rork.safetygembawalk.ui.screens.RegisterScreen
import com.rork.safetygembawalk.ui.screens.ReportsScreen
import com.rork.safetygembawalk.viewmodels.AuthViewModel
import com.rork.safetygembawalk.viewmodels.HomeViewModel
import com.rork.safetygembawalk.viewmodels.InspectionViewModel
import com.rork.safetygembawalk.viewmodels.ReportViewModel

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = viewModel()
    val authState by authViewModel.authState.collectAsState()

    val startDestination = if (authState.isAuthenticated) "home" else "login"

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable("login") {
            LoginScreen(navController = navController)
        }

        composable("register") {
            RegisterScreen(navController = navController)
        }

        composable("home") {
            HomeScreen(navController = navController)
        }

        composable("new_inspection") {
            NewInspectionScreen(
                navController = navController,
                parentInspectionId = 0L,
                actionId = 0L
            )
        }

        composable(
            route = "add_action/{inspectionId}",
            arguments = listOf(
                navArgument("inspectionId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val inspectionId = backStackEntry.arguments?.getLong("inspectionId") ?: 0L

            NewInspectionScreen(
                navController = navController,
                parentInspectionId = inspectionId,
                actionId = 0L
            )
        }

        composable(
            route = "edit_action/{inspectionId}/{actionId}",
            arguments = listOf(
                navArgument("inspectionId") { type = NavType.LongType },
                navArgument("actionId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val inspectionId = backStackEntry.arguments?.getLong("inspectionId") ?: 0L
            val actionId = backStackEntry.arguments?.getLong("actionId") ?: 0L

            NewInspectionScreen(
                navController = navController,
                parentInspectionId = inspectionId,
                actionId = actionId
            )
        }

        composable(
            route = "inspection_detail/{inspectionId}",
            arguments = listOf(
                navArgument("inspectionId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val inspectionId = backStackEntry.arguments?.getLong("inspectionId") ?: 0L
            InspectionDetailScreen(
                navController = navController,
                inspectionId = inspectionId
            )
        }

        composable(
            route = "reports/{reportType}",
            arguments = listOf(
                navArgument("reportType") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val reportType = backStackEntry.arguments?.getString("reportType") ?: "pdf"
            ReportsScreen(
                navController = navController,
                reportType = reportType
            )
        }
    }
}

fun provideHomeViewModelFactory(application: Application): ViewModelProvider.Factory {
    return object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HomeViewModel(application) as T
        }
    }
}

fun provideInspectionViewModelFactory(application: Application): ViewModelProvider.Factory {
    return object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return InspectionViewModel(application) as T
        }
    }
}

fun provideReportViewModelFactory(application: Application): ViewModelProvider.Factory {
    return object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ReportViewModel(application) as T
        }
    }
}
