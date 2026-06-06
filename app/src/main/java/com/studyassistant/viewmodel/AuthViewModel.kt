package com.studyassistant.viewmodel

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.studyassistant.BuildConfig
import com.studyassistant.domain.model.User
import com.studyassistant.repository.FirebaseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

data class AuthUiState(
    val isSignedIn: Boolean = false,
    val currentUser: User? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val firebaseRepository: FirebaseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        val user = firebaseRepository.getCurrentUser()
        _uiState.value = _uiState.value.copy(isSignedIn = user != null, currentUser = user)
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            // guard against hanging network calls
            val res = withTimeoutOrNull(30_000) { firebaseRepository.signIn(email.trim(), password) }
            if (res == null) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = "Sign in timed out. Check your network and try again.")
                return@launch
            }
            res.fold(
                onSuccess = { user ->
                    _uiState.value = _uiState.value.copy(isLoading = false, isSignedIn = true, currentUser = user)
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
                }
            )
        }
    }

    fun signUp(name: String, email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val res = withTimeoutOrNull(30_000) { firebaseRepository.signUp(name.trim(), email.trim(), password) }
            if (res == null) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = "Sign up timed out. Check your network and try again.")
                return@launch
            }
            res.fold(
                onSuccess = { user ->
                    _uiState.value = _uiState.value.copy(isLoading = false, isSignedIn = true, currentUser = user)
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
                }
            )
        }
    }

    fun signOut() {
        viewModelScope.launch {
            firebaseRepository.signOut()
            _uiState.value = _uiState.value.copy(isSignedIn = false, currentUser = null)
        }
    }
    // ── Google Sign In (NEW) ──────────────────────────────────────────────
    fun signInWithGoogle(activityContext: Context) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                // 1. Show Google account picker
                val credentialManager = CredentialManager.create(activityContext)
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
                    .setAutoSelectEnabled(false)
                    .build()
                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()
                val result = credentialManager.getCredential(
                    request = request,
                    context = activityContext
                )

                // 2. Extract Google ID token
                val googleIdTokenCredential = GoogleIdTokenCredential
                    .createFrom(result.credential.data)
                val idToken = googleIdTokenCredential.idToken

                // 3. Pass token to your FirebaseRepository
                val res = withTimeoutOrNull(30_000) {
                    firebaseRepository.signInWithGoogle(idToken)
                }
                if (res == null) {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = "Google Sign-In timed out.")
                    return@launch
                }
                res.fold(
                    onSuccess = { user ->
                        _uiState.value = _uiState.value.copy(isLoading = false, isSignedIn = true, currentUser = user)
                    },
                    onFailure = { e ->
                        _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
                    }
                )

            } catch (e: GetCredentialException) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Google Sign-In cancelled or failed"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

}
