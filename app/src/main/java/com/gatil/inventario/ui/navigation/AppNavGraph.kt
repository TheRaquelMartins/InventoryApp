package com.gatil.inventario.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.gatil.inventario.ui.ced.CEDListScreen
import com.gatil.inventario.ui.ced.LoanHistoryScreen
import com.gatil.inventario.ui.dashboard.DashboardScreen
import com.gatil.inventario.ui.cats.CatsListScreen
import com.gatil.inventario.ui.donations.DonationsListScreen
import com.gatil.inventario.ui.login.LoginScreen
import com.gatil.inventario.ui.partners.PartnersListScreen
import com.gatil.inventario.ui.scanner.BarcodeScannerScreen
import com.gatil.inventario.ui.scanner.ScannerViewModel
import com.gatil.inventario.ui.session.SessionViewModel
import com.gatil.inventario.ui.vet.VetAppointmentsListScreen
import com.gatil.inventario.ui.vet.VetInvoicesScreen
import com.google.firebase.auth.FirebaseAuth

object Routes {
    const val LOGIN = "login"
    const val DASHBOARD = "dashboard"
    const val DONATIONS = "donations"
    const val CED = "ced"
    const val CED_LOAN_HISTORY = "ced_loan_history"
    const val PARTNERS = "partners"
    const val CATS = "cats"
    const val VET_APPOINTMENTS = "vet_appointments"
    const val VET_INVOICES = "vet_invoices"
    const val BARCODE_SCANNER = "barcode_scanner"
}

@Composable
fun AppNavGraph(navController: NavHostController = rememberNavController()) {
    // Instâncias únicas partilhadas por toda a app.
    val session: SessionViewModel = viewModel()
    val scanner: ScannerViewModel = viewModel()

    // Verifica se já existe uma sessão ativa no arranque.
    LaunchedEffect(Unit) {
        if (FirebaseAuth.getInstance().currentUser != null) {
            session.loadCurrentUser()
            navController.navigate(Routes.DASHBOARD) {
                popUpTo(Routes.LOGIN) { inclusive = true }
            }
        }
    }

    NavHost(navController = navController, startDestination = Routes.LOGIN) {
        composable(Routes.LOGIN) {
            LoginScreen(
                onLoginSuccess = {
                    session.loadCurrentUser()
                    navController.navigate(Routes.DASHBOARD) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.DASHBOARD) {
            DashboardScreen(
                isAdmin = session.isAdmin,
                onNavigateDonations = { navController.navigate(Routes.DONATIONS) },
                onNavigateCED = { navController.navigate(Routes.CED) },
                onNavigatePartners = { navController.navigate(Routes.PARTNERS) },
                onNavigateCats = { navController.navigate(Routes.CATS) },
                onNavigateVetAppointments = { navController.navigate(Routes.VET_APPOINTMENTS) },
                onNavigateVetInvoices = { navController.navigate(Routes.VET_INVOICES) },
                onLogout = {
                    session.clear()
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0)
                    }
                }
            )
        }
        composable(Routes.DONATIONS) {
            DonationsListScreen(
                isAdmin = session.isAdmin,
                onBack = { navController.popBackStack() },
                onScanRequest = {
                    scanner.requestScan(ScannerViewModel.TARGET_DONATIONS)
                    navController.navigate(Routes.BARCODE_SCANNER)
                },
                scannedCode = scanner.pendingCode.takeIf { scanner.target == ScannerViewModel.TARGET_DONATIONS },
                onScanConsumed = { scanner.consume() }
            )
        }
        composable(Routes.CED) {
            CEDListScreen(
                isAdmin = session.isAdmin,
                onBack = { navController.popBackStack() },
                onNavigateHistory = { navController.navigate(Routes.CED_LOAN_HISTORY) },
                onScanRequest = {
                    scanner.requestScan(ScannerViewModel.TARGET_CED)
                    navController.navigate(Routes.BARCODE_SCANNER)
                },
                scannedCode = scanner.pendingCode.takeIf { scanner.target == ScannerViewModel.TARGET_CED },
                onScanConsumed = { scanner.consume() }
            )
        }
        composable(Routes.CED_LOAN_HISTORY) {
            LoanHistoryScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.PARTNERS) {
            PartnersListScreen(isAdmin = session.isAdmin, onBack = { navController.popBackStack() })
        }
        composable(Routes.CATS) {
            CatsListScreen(isAdmin = session.isAdmin, onBack = { navController.popBackStack() })
        }
        composable(Routes.VET_APPOINTMENTS) {
            VetAppointmentsListScreen(isAdmin = session.isAdmin, onBack = { navController.popBackStack() })
        }
        composable(Routes.VET_INVOICES) {
            VetInvoicesScreen(isAdmin = session.isAdmin, onBack = { navController.popBackStack() })
        }
        composable(Routes.BARCODE_SCANNER) {
            BarcodeScannerScreen(
                onScanned = { code ->
                    scanner.onScanned(code)
                    navController.popBackStack()
                },
                onCancel = { navController.popBackStack() }
            )
        }
    }
}
