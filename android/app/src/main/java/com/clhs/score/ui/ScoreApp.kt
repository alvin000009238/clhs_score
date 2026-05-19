package com.clhs.score.ui

import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.clhs.score.viewmodel.GradesUiState
import com.clhs.score.viewmodel.LoginUiState

private const val GradesRoute = "grades"
private const val ScoreSimulatorRoute = "score-simulator"

@Composable
fun ScoreApp(
    loginState: LoginUiState,
    gradesState: GradesUiState,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onCaptchaChange: (String) -> Unit,
    onRefreshCaptcha: () -> Unit,
    onLogin: () -> Unit,
    onSelectYear: (String) -> Unit,
    onSelectExam: (String) -> Unit,
    onReload: () -> Unit,
    onLogout: () -> Unit,
    onToggleSubject: (String) -> Unit,
    onDismissLoginError: () -> Unit,
    onDismissGradesError: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(loginState.errorMessage) {
        val message = loginState.errorMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        onDismissLoginError()
    }
    LaunchedEffect(gradesState.errorMessage) {
        val message = gradesState.errorMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        onDismissGradesError()
    }

    if (gradesState.isLoggedIn) {
        val navController = rememberNavController()
        NavHost(navController = navController, startDestination = GradesRoute) {
            composable(GradesRoute) {
                GradesScreen(
                    state = gradesState,
                    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                    onSelectYear = onSelectYear,
                    onSelectExam = onSelectExam,
                    onReload = onReload,
                    onLogout = onLogout,
                    onToggleSubject = onToggleSubject,
                    onOpenScoreSimulator = { navController.navigate(ScoreSimulatorRoute) },
                )
            }
            composable(ScoreSimulatorRoute) {
                ScoreSimulatorScreen(
                    state = gradesState,
                    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                    onBack = { navController.popBackStack() },
                )
            }
        }
    } else {
        LoginScreen(
            state = loginState,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            onUsernameChange = onUsernameChange,
            onPasswordChange = onPasswordChange,
            onCaptchaChange = onCaptchaChange,
            onRefreshCaptcha = onRefreshCaptcha,
            onLogin = onLogin,
        )
    }
}
