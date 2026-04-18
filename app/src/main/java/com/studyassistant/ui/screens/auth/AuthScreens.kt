package com.studyassistant.ui.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.studyassistant.viewmodel.AuthViewModel
import com.studyassistant.util.Constants
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Checkbox
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignInScreen(onSignedIn: () -> Unit, onNavigateToSignUp: () -> Unit, viewModel: AuthViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    LaunchedEffect(state.isSignedIn) {
        if (state.isSignedIn) onSignedIn()
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Sign In", style = MaterialTheme.typography.titleLarge)
        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Password") }, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), modifier = Modifier.fillMaxWidth())
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(onClick = { viewModel.signIn(email, password) }, modifier = Modifier.fillMaxWidth(), enabled = !state.isLoading) {
            if (state.isLoading) CircularProgressIndicator(modifier = Modifier.size(18.dp))
            Text("Sign In")
        }
        TextButton(onClick = onNavigateToSignUp, modifier = Modifier.fillMaxWidth()) {
            Text("Don't have an account? Sign up")
        }
    }
}

@Composable
fun SignUpScreen(onSignedUp: () -> Unit, onNavigateToSignIn: () -> Unit, viewModel: AuthViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    LaunchedEffect(state.isSignedIn) {
        if (state.isSignedIn) onSignedUp()
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Sign Up", style = MaterialTheme.typography.titleLarge)
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Full Name") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Password") }, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), modifier = Modifier.fillMaxWidth())
        // Exam dropdown
        val exams = Constants.SUPPORTED_EXAMS
        var expanded by remember { mutableStateOf(false) }
        var selectedExam by remember { mutableStateOf(exams.firstOrNull() ?: "Other") }

        // Stable dropdown implementation using OutlinedTextField + DropdownMenu
        OutlinedTextField(
            value = selectedExam,
            onValueChange = {},
            readOnly = true,
            label = { Text("Select Exam") },
            trailingIcon = {
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth()
        ) {
            exams.forEach { exam ->
                DropdownMenuItem(
                    text = { Text(exam) },
                    onClick = { selectedExam = exam; expanded = false }
                )
            }
        }

        // Terms checkbox
        var agreed by remember { mutableStateOf(false) }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = agreed, onCheckedChange = { agreed = it })
            Spacer(Modifier.width(8.dp))
            Text("I agree to the Terms and Conditions", style = MaterialTheme.typography.bodySmall)
        }

        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(onClick = { viewModel.signUp(name, email, password) }, modifier = Modifier.fillMaxWidth(), enabled = !state.isLoading && agreed && name.isNotBlank() && email.isNotBlank() && password.isNotBlank()) {
            if (state.isLoading) CircularProgressIndicator(modifier = Modifier.size(18.dp))
            Text("Sign Up")
        }
        TextButton(onClick = onNavigateToSignIn, modifier = Modifier.fillMaxWidth()) {
            Text("Already have an account? Sign in")
        }
    }
}
