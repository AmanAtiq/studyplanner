package com.studyassistant.ui.screens.upload

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.studyassistant.domain.model.AppLanguage
import com.studyassistant.ui.components.*
import com.studyassistant.ui.theme.*
import com.studyassistant.viewmodel.UploadViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadScreen(
    onBack: () -> Unit,
    onUploadSuccess: () -> Unit,
    viewModel: UploadViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Navigate on success
    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            onUploadSuccess()
            viewModel.resetSuccess()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (uiState.selectedLanguage == AppLanguage.URDU)
                            "نوٹ شامل کریں" else "Add Note",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    LanguageToggle(
                        currentLanguage = uiState.selectedLanguage,
                        onToggle = {
                            viewModel.onLanguageChange(
                                if (uiState.selectedLanguage == AppLanguage.ENGLISH)
                                    AppLanguage.URDU else AppLanguage.ENGLISH
                            )
                        }
                    )
                    Spacer(Modifier.width(8.dp))
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Error
            uiState.error?.let { ErrorBanner(message = it, onDismiss = viewModel::clearError) }

            // Title field
            OutlinedTextField(
                value = uiState.title,
                onValueChange = viewModel::onTitleChange,
                label = {
                    Text(if (uiState.selectedLanguage == AppLanguage.URDU)
                        "عنوان" else "Note Title")
                },
                placeholder = {
                    Text(if (uiState.selectedLanguage == AppLanguage.URDU)
                        "مثلاً: فزکس باب 3" else "e.g. Physics Chapter 3")
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Title, contentDescription = null) }
            )

            // Content field
            OutlinedTextField(
                value = uiState.content,
                onValueChange = viewModel::onContentChange,
                label = {
                    Text(if (uiState.selectedLanguage == AppLanguage.URDU)
                        "نوٹ کا مواد" else "Note Content")
                },
                placeholder = {
                    Text(if (uiState.selectedLanguage == AppLanguage.URDU)
                        "یہاں اپنے نوٹس پیسٹ کریں..." else "Paste your notes here...")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
                shape = RoundedCornerShape(14.dp),
                maxLines = 20
            )

            // Character count
            Text(
                text = "${uiState.content.length} / 8000 chars",
                style = MaterialTheme.typography.labelSmall,
                color = if (uiState.content.length > 7500)
                    MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                modifier = Modifier.align(Alignment.End)
            )

            // Language selector
            LanguageSelectorRow(
                selected = uiState.selectedLanguage,
                onSelect = viewModel::onLanguageChange
            )

            // AI Info card
            AISummaryInfoCard(language = uiState.selectedLanguage)

            // Summary preview
            AnimatedVisibility(visible = uiState.summary.isNotEmpty()) {
                SummaryPreviewCard(summary = uiState.summary, language = uiState.selectedLanguage)
            }

            // Upload button
            Button(
                onClick = viewModel::uploadNote,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(14.dp),
                enabled = !uiState.isUploading && !uiState.isSummarizing
                        && uiState.title.isNotBlank() && uiState.content.isNotBlank()
            ) {
                if (uiState.isUploading || uiState.isSummarizing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        if (uiState.isUploading) "Saving..."
                        else if (uiState.selectedLanguage == AppLanguage.URDU)
                            "AI خلاصہ بنا رہا ہے..." else "AI Summarizing..."
                    )
                } else {
                    Icon(Icons.Default.CloudUpload, contentDescription = null,
                        modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (uiState.selectedLanguage == AppLanguage.URDU)
                            "محفوظ کریں اور خلاصہ بنائیں" else "Save & Summarize",
                        style = MaterialTheme.typography.titleSmall
                    )
                }
            }
        }
    }
}

@Composable
private fun LanguageSelectorRow(
    selected: AppLanguage,
    onSelect: (AppLanguage) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        AppLanguage.values().forEach { lang ->
            FilterChip(
                selected = selected == lang,
                onClick = { onSelect(lang) },
                label = {
                    Text(
                        if (lang == AppLanguage.ENGLISH) "English Summary"
                        else "اردو خلاصہ"
                    )
                },
                leadingIcon = if (selected == lang) {
                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                } else null,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun AISummaryInfoCard(language: AppLanguage) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null,
                tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = if (language == AppLanguage.URDU)
                        "AI خلاصہ خودکار بنے گا" else "AI will auto-summarize your notes",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = if (language == AppLanguage.URDU)
                        "اہم نکات، تعریفیں، اور امتحانی تجاویز شامل ہوں گی"
                    else "Key points, definitions, and exam tips will be extracted",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun SummaryPreviewCard(summary: String, language: AppLanguage) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Summarize, contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
                Text(
                    if (language == AppLanguage.URDU) "AI خلاصہ" else "AI Summary",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            Text(
                text = summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}