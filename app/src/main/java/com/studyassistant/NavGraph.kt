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
import com.studyassistant.ui.screens.grades.GradesScreen
import com.studyassistant.ui.screens.flashcards.FlashcardsScreen
import com.studyassistant.ui.screens.chat.ChatScreen
import com.studyassistant.ui.screens.progress.ProgressTimelineScreen
import com.studyassistant.ui.screens.analytics.PerformanceAnalyticsScreen
import com.studyassistant.ui.screens.leaderboard.LeaderboardScreen
import com.studyassistant.ui.screens.studygroup.CreateGroupScreen
import com.studyassistant.ui.screens.studygroup.GroupChatScreen
import com.studyassistant.ui.screens.studygroup.StudyGroupsScreen
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
    object Grades : Screen("grades")
    object Flashcards : Screen("flashcards/{noteId}") {
        fun createRoute(noteId: String) = "flashcards/$noteId"
    }
    object Chat : Screen("chat/{noteId}") {
        fun createRoute(noteId: String) = "chat/$noteId"
    }
    object ProgressTimeline : Screen("progress")
    object PerformanceAnalytics : Screen("analytics")
    object Leaderboard : Screen("leaderboard")
    object StudyGroups : Screen("study_groups")
    object CreateGroup : Screen("study_groups/create")
    object GroupChat : Screen("study_groups/chat/{groupId}") {
        fun createRoute(groupId: String) = "study_groups/chat/$groupId"
    }
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
        Screen.Settings.route,
        Screen.Grades.route,
        Screen.PerformanceAnalytics.route,
        Screen.Leaderboard.route,
        Screen.StudyGroups.route
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
                    onNavigateToGrades = { navController.navigate(Screen.Grades.route) },
                    onNavigateToAnalytics = { navController.navigate(Screen.PerformanceAnalytics.route) },
                    onNavigateToLeaderboard = { navController.navigate(Screen.Leaderboard.route) },
                    onNavigateToStudyGroups = { navController.navigate(Screen.StudyGroups.route) },
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
                    onTakeQuiz = { quizNoteId -> navController.navigate(Screen.Quiz.createRoute(quizNoteId, true)) },
                    onFlashcards = { fNoteId -> navController.navigate(Screen.Flashcards.createRoute(fNoteId)) },
                    onAskAI = { cNoteId -> navController.navigate(Screen.Chat.createRoute(cNoteId)) }
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

            composable(Screen.Grades.route) {
                GradesScreen(
                    onBack = { navController.popBackStack() },
                    onNavigateToTimeline = { navController.navigate(Screen.ProgressTimeline.route) }
                )
            }

            composable(Screen.Flashcards.route) { backStackEntry ->
                val noteId = backStackEntry.arguments?.getString("noteId") ?: ""
                FlashcardsScreen(noteId = noteId, onBack = { navController.popBackStack() })
            }

            composable(Screen.Chat.route) { backStackEntry ->
                val noteId = backStackEntry.arguments?.getString("noteId") ?: ""
                ChatScreen(noteId = noteId, onBack = { navController.popBackStack() })
            }

            composable(Screen.ProgressTimeline.route) {
                ProgressTimelineScreen(onBack = { navController.popBackStack() })
            }

            composable(Screen.PerformanceAnalytics.route) {
                PerformanceAnalyticsScreen(onBack = { navController.popBackStack() })
            }

            composable(Screen.Leaderboard.route) {
                LeaderboardScreen(onBack = { navController.popBackStack() })
            }

            composable(Screen.StudyGroups.route) {
                StudyGroupsScreen(
                    onBack = { navController.popBackStack() },
                    onSelectGroup = { group -> navController.navigate(Screen.GroupChat.createRoute(group.id)) },
                    onNavigateToCreateGroup = { navController.navigate(Screen.CreateGroup.route) }
                )
            }

            composable(Screen.CreateGroup.route) {
                CreateGroupScreen(
                    onBack = { navController.popBackStack() },
                    onGroupCreated = {
                        navController.navigate(Screen.StudyGroups.route) {
                            popUpTo(Screen.StudyGroups.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.GroupChat.route) { backStackEntry ->
                val groupId = backStackEntry.arguments?.getString("groupId") ?: ""
                GroupChatScreen(
                    groupId = groupId,
                    onBack = { navController.popBackStack() },
                    onShowMembers = { }
                )
            }
        }
    }
}
