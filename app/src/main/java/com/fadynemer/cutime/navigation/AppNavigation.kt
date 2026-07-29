package com.fadynemer.cutime.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.fadynemer.cutime.data.SampleBarberData
import com.fadynemer.cutime.screens.BarberProfileScreen
import com.fadynemer.cutime.screens.BookingScreen
import com.fadynemer.cutime.screens.DashboardScreen
import com.fadynemer.cutime.screens.HomeScreen
import com.fadynemer.cutime.screens.LoginScreen
import com.fadynemer.cutime.screens.RegisterScreen
import com.fadynemer.cutime.screens.SplashScreen
import com.fadynemer.cutime.screens.WelcomeScreen

@Composable
fun AppNavigation() {

    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "splash"
    ) {
        composable("splash") {
            SplashScreen(navController = navController)
        }

        composable("welcome") {
            WelcomeScreen(navController = navController)
        }

        composable("login") {
            LoginScreen(navController = navController)
        }

        composable("register") {
            RegisterScreen(navController = navController)
        }

        composable("home") {
            HomeScreen(navController = navController)
        }

        composable(
            route = "barber_profile/{barberId}",
            arguments = listOf(
                navArgument("barberId") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val barberId =
                backStackEntry.arguments?.getString("barberId").orEmpty()

            BarberProfileScreen(
                barberShop = SampleBarberData.findById(barberId),
                onBack = navController::navigateUp,
                onBookAppointment = {
                    navController.navigate("booking/$barberId")
                }
            )
        }

        composable(
            route = "booking/{barberId}",
            arguments = listOf(
                navArgument("barberId") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val barberId =
                backStackEntry.arguments?.getString("barberId").orEmpty()

            BookingScreen(
                barberShop = SampleBarberData.findById(barberId),
                onBack = navController::navigateUp
            )
        }

        composable("dashboard") {
            DashboardScreen(navController = navController)
        }
    }
}
