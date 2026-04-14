package com.studyassistant

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.studyassistant.ui.screens.home.HomeScreen
import com.studyassistant.ui.screens.upload.UploadScreen
import com.studyassistant.ui.screens.quiz.QuizScreen
import com.studyassistant.ui.screens.quiz.QuizResultScreen
import com.studyassistant.ui.screens.planner.PlannerScreen
import com.studyassistant.ui.screens.profile.ProfileScreen

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Upload : Screen("upload")
    object Quiz : Screen("quiz/{noteId}") {
        fun createRoute(noteId: String) = "quiz/$noteId"
    }
    object QuizResult : Screen("quiz_result/{score}/{total}") {
        fun createRoute(score: Int, total: Int) = "quiz_result/$score/$total"
    }
    object Planner : Screen("planner")
    object Profile : Screen("profile")
}

@Composable
fun NavGraph(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Screen.Home.route) {

        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToUpload = { navController.navigate(Screen.Upload.route) },
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
                onUploadSuccess = { navController.popBackStack() }
            )
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
            ProfileScreen(onBack = { navController.popBackStack() })
        }
    }
}