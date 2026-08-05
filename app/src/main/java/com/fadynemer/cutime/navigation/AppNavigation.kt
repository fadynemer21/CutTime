package com.fadynemer.cutime.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.fadynemer.cutime.data.BarberCatalogCache
import com.fadynemer.cutime.screens.BarberProfileScreen
import com.fadynemer.cutime.screens.AppointmentDetailScreen
import com.fadynemer.cutime.screens.BarberAvailabilityScreen
import com.fadynemer.cutime.screens.BarberGalleryManagementScreen
import com.fadynemer.cutime.screens.BarberManageProfileScreen
import com.fadynemer.cutime.screens.BarberAppointmentHistoryScreen
import com.fadynemer.cutime.screens.BarberServicesScreen
import com.fadynemer.cutime.screens.BookingScreen
import com.fadynemer.cutime.screens.DashboardScreen
import com.fadynemer.cutime.screens.HomeScreen
import com.fadynemer.cutime.screens.AppointmentsScreen
import com.fadynemer.cutime.screens.CustomerProfileScreen
import com.fadynemer.cutime.screens.LoginScreen
import com.fadynemer.cutime.screens.NotificationCenterScreen
import com.fadynemer.cutime.screens.NotificationSettingsScreen
import com.fadynemer.cutime.screens.RegisterScreen
import com.fadynemer.cutime.screens.RescheduleScreen
import com.fadynemer.cutime.screens.RatingScreen
import com.fadynemer.cutime.screens.SplashScreen
import com.fadynemer.cutime.screens.WelcomeScreen
import com.fadynemer.cutime.util.AccountModePreferences
import com.fadynemer.cutime.util.NotificationRouter
import com.google.firebase.auth.FirebaseAuth

@Composable
fun AppNavigation(
    externalRoute: String? = null,
    onExternalRouteConsumed: () -> Unit = {}
) {

    val navController = rememberNavController()
    val currentEntry by
        navController.currentBackStackEntryAsState()
    val currentRoute = currentEntry?.destination?.route
    val context = LocalContext.current
    val currentUserId =
        FirebaseAuth.getInstance().currentUser?.uid
    val canCurrentAccountRate =
        currentUserId != null &&
            !AccountModePreferences.isCustomerMode(
                context,
                currentUserId
            )

    LaunchedEffect(externalRoute, currentRoute) {
        val route = externalRoute ?: return@LaunchedEffect
        if (!AppRoutePolicy.isAllowed(route)) {
            onExternalRouteConsumed()
            return@LaunchedEffect
        }
        val authenticationRoutes = setOf(
            AppRoute.Splash.pattern,
            AppRoute.Welcome.pattern,
            AppRoute.Login.pattern,
            AppRoute.Register.pattern
        )

        if (
            currentRoute != null &&
            currentRoute !in authenticationRoutes
        ) {
            navController.navigate(route) {
                launchSingleTop = true
            }
            onExternalRouteConsumed()
        }
    }

    NavHost(
        navController = navController,
        startDestination = AppRoute.Splash.pattern
    ) {
        composable(AppRoute.Splash.pattern) {
            SplashScreen(navController = navController)
        }

        composable(AppRoute.Welcome.pattern) {
            WelcomeScreen(navController = navController)
        }

        composable(AppRoute.Login.pattern) {
            LoginScreen(navController = navController)
        }

        composable(AppRoute.Register.pattern) {
            RegisterScreen(navController = navController)
        }

        composable(AppRoute.CustomerHome.pattern) {
            HomeScreen(navController = navController)
        }

        composable(AppRoute.CustomerAppointments.pattern) {
            AppointmentsScreen(
                onHomeSelected = {
                    navController.navigate(AppRoute.CustomerHome.pattern) {
                        popUpTo(AppRoute.CustomerHome.pattern) {
                            inclusive = false
                        }
                        launchSingleTop = true
                    }
                },
                onBrowseBarbers = {
                    navController.navigate(AppRoute.CustomerHome.pattern) {
                        popUpTo(AppRoute.CustomerHome.pattern) {
                            inclusive = false
                        }
                        launchSingleTop = true
                    }
                },
                onProfileSelected = {
                    navController.navigate(AppRoute.CustomerProfile.pattern) {
                        launchSingleTop = true
                    }
                },
                onAppointmentSelected = { appointmentId ->
                    navController.navigate(
                        AppRoute.CustomerAppointmentDetail.create(
                            appointmentId
                        )
                    )
                },
                onRate = { appointmentId ->
                    navController.navigate(
                        AppRoute.Rating.create(appointmentId)
                    )
                },
                canRate = canCurrentAccountRate
            )
        }

        composable(AppRoute.CustomerProfile.pattern) {
            CustomerProfileScreen(
                onHomeSelected = {
                    navController.navigate(AppRoute.CustomerHome.pattern) {
                        popUpTo(AppRoute.CustomerHome.pattern) {
                            inclusive = false
                        }
                        launchSingleTop = true
                    }
                },
                onAppointmentsSelected = {
                    navController.navigate(
                        AppRoute.CustomerAppointments.pattern
                    ) {
                        launchSingleTop = true
                    }
                },
                onReturnToBarberMode = {
                    navController.navigate(AppRoute.BarberDashboard.pattern) {
                        popUpTo(AppRoute.CustomerHome.pattern) {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                },
            )
        }

        composable(
            route = AppRoute.BarberProfile.pattern,
            arguments = listOf(
                navArgument("barberId") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val barberId =
                backStackEntry.arguments?.getString("barberId").orEmpty()

            BarberProfileScreen(
                barberShop = BarberCatalogCache.find(barberId),
                onBack = navController::navigateUp,
                onBookAppointment = {
                    navController.navigate(
                        AppRoute.Booking.create(barberId)
                    )
                }
            )
        }

        composable(
            route = AppRoute.Booking.pattern,
            arguments = listOf(
                navArgument("barberId") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val barberId =
                backStackEntry.arguments?.getString("barberId").orEmpty()

            BookingScreen(
                barberShop = BarberCatalogCache.find(barberId),
                onBack = navController::navigateUp,
                onViewAppointments = {
                    navController.navigate(
                        AppRoute.CustomerAppointments.pattern
                    ) {
                        popUpTo(AppRoute.Booking.create(barberId)) {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(
            route = AppRoute.CustomerAppointmentDetail.pattern,
            arguments = listOf(
                navArgument(RouteArguments.APPOINTMENT_ID) {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val appointmentId = backStackEntry.arguments
                ?.getString(RouteArguments.APPOINTMENT_ID)
                .orEmpty()
            AppointmentDetailScreen(
                appointmentId = appointmentId,
                isBarberView = false,
                canRate = canCurrentAccountRate,
                onBack = navController::navigateUp,
                onReschedule = {
                    navController.navigate(
                        AppRoute.Reschedule.create(it)
                    )
                },
                onRate = {
                    navController.navigate(AppRoute.Rating.create(it))
                }
            )
        }

        composable(
            route = AppRoute.BarberAppointmentDetail.pattern,
            arguments = listOf(
                navArgument(RouteArguments.APPOINTMENT_ID) {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            AppointmentDetailScreen(
                appointmentId = backStackEntry.arguments
                    ?.getString(RouteArguments.APPOINTMENT_ID)
                    .orEmpty(),
                isBarberView = true,
                onBack = navController::navigateUp,
                onReschedule = {},
                onRate = {}
            )
        }

        composable(
            route = AppRoute.Reschedule.pattern,
            arguments = listOf(
                navArgument(RouteArguments.APPOINTMENT_ID) {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val appointmentId = backStackEntry.arguments
                ?.getString(RouteArguments.APPOINTMENT_ID)
                .orEmpty()
            RescheduleScreen(
                appointmentId = appointmentId,
                onBack = navController::navigateUp,
                onFinished = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = AppRoute.Rating.pattern,
            arguments = listOf(
                navArgument(RouteArguments.APPOINTMENT_ID) {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val appointmentId = backStackEntry.arguments
                ?.getString(RouteArguments.APPOINTMENT_ID)
                .orEmpty()
            RatingScreen(
                appointmentId = appointmentId,
                canRate = canCurrentAccountRate,
                onBack = navController::navigateUp,
                onFinished = navController::navigateUp
            )
        }

        composable(AppRoute.BarberDashboard.pattern) {
            DashboardScreen(navController = navController)
        }

        composable(AppRoute.BarberServices.pattern) {
            BarberServicesScreen(navController = navController)
        }

        composable(AppRoute.BarberAvailability.pattern) {
            BarberAvailabilityScreen(
                navController = navController
            )
        }

        composable(AppRoute.BarberManageProfile.pattern) {
            BarberManageProfileScreen(
                navController = navController
            )
        }

        composable(AppRoute.BarberAppointmentHistory.pattern) {
            BarberAppointmentHistoryScreen(
                onBack = navController::navigateUp,
                onAppointmentSelected = { appointmentId ->
                    navController.navigate(
                        AppRoute.BarberAppointmentDetail.create(
                            appointmentId
                        )
                    )
                }
            )
        }

        composable(AppRoute.BarberGallery.pattern) {
            BarberGalleryManagementScreen(
                navController = navController
            )
        }

        composable(
            route = AppRoute.Notifications.pattern,
            arguments = listOf(
                navArgument(RouteArguments.MODE) {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val isBarberMode = backStackEntry.arguments
                ?.getString(RouteArguments.MODE) == "barber"
            NotificationCenterScreen(
                isBarberMode = isBarberMode,
                onBack = navController::navigateUp,
                onOpenSettings = { barberMode ->
                    navController.navigate(
                        AppRoute.NotificationSettings.create(barberMode)
                    )
                },
                onNotificationSelected = {
                        notification,
                        barberMode ->
                    navController.navigate(
                        NotificationRouter.destination(
                            notification = notification,
                            isBarberMode = barberMode
                        )
                    )
                }
            )
        }

        composable(
            route = AppRoute.NotificationSettings.pattern,
            arguments = listOf(
                navArgument(RouteArguments.MODE) {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            NotificationSettingsScreen(
                isBarberMode = backStackEntry.arguments
                    ?.getString(RouteArguments.MODE) == "barber",
                onBack = navController::navigateUp
            )
        }
    }
}
