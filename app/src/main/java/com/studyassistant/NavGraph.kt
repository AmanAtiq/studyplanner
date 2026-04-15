package com.studyassistant

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.studyassistant.ui.screens.auth.SignInScreen
import com.studyassistant.ui.screens.auth.SignUpScreen
import com.studyassistant.ui.screens.home.HomeScreen
import com.studyassistant.ui.screens.upload.UploadScreen
import com.studyassistant.ui.screens.quiz.QuizScreen
import com.studyassistant.ui.screens.quiz.QuizResultScreen
import com.studyassistant.ui.screens.planner.PlannerScreen
import com.studyassistant.ui.screens.profile.ProfileScreen
import com.studyassistant.ui.screens.quizzes.QuizzesListScreen
import com.studyassistant.viewmodel.AuthViewModel
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Upload : Screen("upload")
    object QuizzesList : Screen("quizzes")
    object Quiz : Screen("quiz/{noteId}") {
        fun createRoute(noteId: String) = "quiz/$noteId"
    }
    object QuizResult : Screen("quiz_result/{score}/{total}") {
        fun createRoute(score: Int, total: Int) = "quiz/$score/$total"
    }
    object Planner : Screen("planner")
    object Profile : Screen("profile")
}

@Composable
fun NavGraph(navController: NavHostController = rememberNavController()) {
    val authViewModel: AuthViewModel = hiltViewModel()
    val authState by authViewModel.uiState.collectAsState()

    NavHost(navController = navController, startDestination = if (authState.isSignedIn) Screen.Home.route else "signin") {

        composable("signin") {
            SignInScreen(
                onSignedIn = { navController.navigate(Screen.Home.route) { popUpTo("signin") { inclusive = true } } },
                onNavigateToSignUp = { navController.navigate("signup") }
            )
        }

        composable("signup") {
            SignUpScreen(
                onSignedUp = { navController.navigate(Screen.Home.route) { popUpTo("signup") { inclusive = true } } },
                onNavigateToSignIn = { navController.navigate("signin") }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToUpload = { navController.navigate(Screen.Upload.route) },
                onNavigateToQuizzes = { navController.navigate(Screen.QuizzesList.route) },
                onNavigateToPlanner = { navController.navigate(Screen.Planner.route) },
                onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                onNavigateToQuiz = { noteId ->
                    navController.navigate(Screen.Quiz.createRoute(noteId))
                }
            )
        }

        composable(Screen.Upload.route) {
            UploadScreen(
                onBack = { navController.popBackStack() },
                onUploadSuccess = { generatedNoteId ->
                    if (!generatedNoteId.isNullOrEmpty()) {
                        navController.navigate(Screen.Quiz.createRoute(generatedNoteId))
                    } else {
                        navController.popBackStack()
                    }
                }
            )
        }

        composable(Screen.QuizzesList.route) {
            QuizzesListScreen(onBack = { navController.popBackStack() }, onOpenQuiz = { noteId ->
                navController.navigate(Screen.Quiz.createRoute(noteId))
            })
        }

        composable(Screen.Quiz.route) { backStackEntry ->
            val noteId = backStackEntry.arguments?.getString("noteId") ?: ""
            QuizScreen(
                noteId = noteId,
                onBack = { navController.popBackStack() },
                onFinish = { score, total ->
                    navController.navigate(Screen.QuizResult.createRoute(score, total)) {
                        popUpTo(Screen.Quiz.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.QuizResult.route) { backStackEntry ->
            val score = backStackEntry.arguments?.getString("score")?.toIntOrNull() ?: 0
            val total = backStackEntry.arguments?.getString("total")?.toIntOrNull() ?: 0
            QuizResultScreen(
                score = score,
                total = total,
                onBack = { navController.navigate(Screen.Home.route) { popUpTo(0) } }
            )
        }


        composable(Screen.Planner.route) {
            PlannerScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.Profile.route) {
            ProfileScreen(onBack = { navController.popBackStack() }, onSignOut = {
                navController.navigate("signin") { popUpTo(0) }
            })
        }
    }
}