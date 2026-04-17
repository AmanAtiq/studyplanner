package com.studyassistant

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.material3.Scaffold
import androidx.compose.foundation.layout.padding
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.studyassistant.ui.screens.auth.SignInScreen
import com.studyassistant.ui.screens.auth.SignUpScreen
import com.studyassistant.ui.screens.home.HomeScreen
import com.studyassistant.ui.screens.home.NoteDetailScreen
import com.studyassistant.ui.screens.upload.UploadScreen
import com.studyassistant.ui.screens.quiz.QuizScreen
import com.studyassistant.ui.screens.quiz.QuizResultScreen
import com.studyassistant.ui.screens.history.LectureHistoryScreen
import com.studyassistant.ui.screens.planner.PlannerScreen
import com.studyassistant.ui.screens.profile.ProfileScreen
import com.studyassistant.ui.screens.quizzes.QuizzesListScreen
import com.studyassistant.ui.components.StudyBottomBar
import com.studyassistant.viewmodel.AuthViewModel
import androidx.hilt.navigation.compose.hiltViewModel

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Upload : Screen("upload")
    object LectureHistory : Screen("lectures")
    object QuizzesList : Screen("quizzes")
    object Settings : Screen("settings")
    object NoteDetail : Screen("note/{noteId}") {
        fun createRoute(noteId: String) = "note/$noteId"
    }
    object Quiz : Screen("quiz/{noteId}/{forceRefresh}") {
        fun createRoute(noteId: String, forceRefresh: Boolean = false) = "quiz/$noteId/$forceRefresh"
    }
    object QuizResult : Screen("quiz_result/{score}/{total}") {
        fun createRoute(score: Int, total: Int) = "quiz_result/$score/$total"
    }
    object Planner : Screen("planner")
}

@Composable
fun NavGraph(navController: NavHostController = rememberNavController()) {
    val authViewModel: AuthViewModel = hiltViewModel()
    val authState by authViewModel.uiState.collectAsState()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute in setOf(
        Screen.Home.route,
        Screen.LectureHistory.route,
        Screen.QuizzesList.route,
        Screen.Settings.route
    )

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                StudyBottomBar(navController = navController, currentRoute = currentRoute)
            }
        }
    ) { scaffoldPadding ->
        NavHost(
            navController = navController,
            startDestination = if (authState.isSignedIn) Screen.Home.route else "signin",
            modifier = androidx.compose.ui.Modifier.padding(scaffoldPadding)
        ) {

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
                    onNavigateToProfile = { navController.navigate(Screen.Settings.route) },
                    onNavigateToNoteDetail = { noteId -> navController.navigate(Screen.NoteDetail.createRoute(noteId)) },
                    onNavigateToQuiz = { noteId -> navController.navigate(Screen.Quiz.createRoute(noteId, true)) }
                )
            }

            composable(Screen.Upload.route) {
                UploadScreen(
                    onBack = { navController.popBackStack() },
                    onUploadSuccess = { noteId ->
                        if (!noteId.isNullOrEmpty()) {
                            navController.navigate(Screen.NoteDetail.createRoute(noteId))
                        } else {
                            navController.popBackStack()
                        }
                    }
                )
            }

            composable(Screen.LectureHistory.route) {
                LectureHistoryScreen(
                    onBack = { navController.popBackStack() },
                    onOpenNote = { noteId -> navController.navigate(Screen.NoteDetail.createRoute(noteId)) }
                )
            }

            composable(Screen.QuizzesList.route) {
                QuizzesListScreen(
                    onBack = { navController.popBackStack() },
                    onOpenQuiz = { noteId, forceRefresh -> navController.navigate(Screen.Quiz.createRoute(noteId, forceRefresh)) }
                )
            }

            composable(Screen.NoteDetail.route) { backStackEntry ->
                val noteId = backStackEntry.arguments?.getString("noteId") ?: ""
                NoteDetailScreen(
                    noteId = noteId,
                    onBack = { navController.popBackStack() },
                    onTakeQuiz = { quizNoteId -> navController.navigate(Screen.Quiz.createRoute(quizNoteId, true)) }
                )
            }

            composable(Screen.Quiz.route) { backStackEntry ->
                val noteId = backStackEntry.arguments?.getString("noteId") ?: ""
                val forceRefresh = backStackEntry.arguments?.getString("forceRefresh")?.toBooleanStrictOrNull() ?: false
                QuizScreen(
                    noteId = noteId,
                    forceRefresh = forceRefresh,
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

            composable(Screen.Settings.route) {
                ProfileScreen(onBack = { navController.popBackStack() }, onSignOut = {
                    navController.navigate("signin") { popUpTo(0) }
                })
            }
        }
    }
}