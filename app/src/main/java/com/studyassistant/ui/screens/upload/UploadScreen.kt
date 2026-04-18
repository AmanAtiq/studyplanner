package com.studyassistant.ui.screens.upload

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.provider.OpenableColumns
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
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.studyassistant.ui.components.*
import com.studyassistant.viewmodel.UploadViewModel
import com.studyassistant.util.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import com.studyassistant.ui.components.ScreenBackground

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadScreen(
    onBack: () -> Unit,
    onUploadSuccess: (generatedNoteId: String?) -> Unit,
    viewModel: UploadViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // local UI hold for raw file bytes + name
    var selectedFileBytes by remember { mutableStateOf<ByteArray?>(null) }
    var selectedFileName by remember { mutableStateOf<String?>(null) }

    // NOTE: we intentionally do NOT auto-navigate on success anymore. Instead show a clear
    // success area with an explicit "Take Quiz" button so the user can choose to go to the quiz.

    // helper to read bytes from uri
    suspend fun readBytesFromUri(uri: Uri): ByteArray? = withContext(Dispatchers.IO) {
        return@withContext try {
            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // helper to get display name (filename) from uri
    fun getFileNameFromUri(uri: Uri): String? {
        var name: String? = null
        try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && nameIndex >= 0) {
                    name = cursor.getString(nameIndex)
                }
            }
        } catch (e: Exception) {
            // ignore
        }
        if (name.isNullOrBlank()) name = uri.lastPathSegment
        return name
    }

    // utility to turn filename into a safe title (strip extension)
    fun filenameToTitle(filename: String?): String? {
        if (filename == null) return null
        val dot = filename.lastIndexOf('.')
        return if (dot > 0) filename.substring(0, dot) else filename
    }

    // Launchers for picking files
    val imageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            uri?.let { selectedUri ->
                // extract text and also read bytes
                coroutineScope.launch {
                    val extracted = extractTextFromImage(context, selectedUri)
                    val bytes = readBytesFromUri(selectedUri)
                    // Update UI state on Main to ensure recomposition
                    withContext(Dispatchers.Main) {
                        if (extracted.isNotBlank()) {
                            viewModel.onContentChange(extracted.trim())
                        }
                        selectedFileBytes = bytes
                        val name = getFileNameFromUri(selectedUri) ?: "image.jpg"
                        selectedFileName = name
                        viewModel.setSelectedFile(selectedFileName, bytes?.size ?: 0)
                        // if title empty, prefill from filename
                        if (viewModel.uiState.value.title.isBlank()) {
                            filenameToTitle(name)?.let { viewModel.onTitleChange(it) }
                        }
                    }
                }
            }
        }
    )

    val pdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                selectedFileBytes = readBytesFromUri(uri)
                selectedFileName = getFileNameFromUri(uri)

                // Pass `context` explicitly to `extractTextFromPdf`
                val extractedText = extractTextFromPdf(context, uri)
                viewModel.updateNoteContent(extractedText ?: "")
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Add Note", fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // no language toggle - English-only app
                    Spacer(Modifier.width(8.dp))
                }
            )
        }
    ) { padding ->
        ScreenBackground {
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

                // Pick image/pdf buttons
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { imageLauncher.launch("image/*") }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Image, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Pick Image (OCR)")
                    }
                    OutlinedButton(onClick = { pdfLauncher.launch(arrayOf("application/pdf")) }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Pick PDF")
                    }
                }

                // Selected file info
                if (selectedFileName != null) {
                    Text("Selected: ${selectedFileName} (${selectedFileBytes?.size ?: 0} bytes)")
                }

                // Title field
                OutlinedTextField(
                    value = uiState.title,
                    onValueChange = viewModel::onTitleChange,
                    label = { Text("Note Title") },
                    placeholder = { Text("e.g. Physics Chapter 3") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Title, contentDescription = null) }
                )

                // Content field
                OutlinedTextField(
                    value = uiState.content,
                    onValueChange = viewModel::onContentChange,
                    label = { Text("Note Content") },
                    placeholder = { Text("Paste your notes here... or pick an image/pdf to extract") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    shape = RoundedCornerShape(14.dp)
                )

                // Character count (no limit)
                Text(
                    text = "${uiState.content.length} characters",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                    modifier = Modifier.align(Alignment.End)
                )

                // AI Info card
                AISummaryInfoCard()

                // Visible warning if Gemini API key not set (local fallback will be used)
                if (Constants.GEMINI_API_KEY.isBlank()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("AI key not configured — using local fallback (no external AI).", color = MaterialTheme.colorScheme.onErrorContainer)
                            Text("To enable real AI summaries/quizzes, add GEMINI_API_KEY to local.properties.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                }

                // Summary preview
                AnimatedVisibility(visible = uiState.summary.isNotEmpty()) {
                    SummaryPreviewCard(summary = uiState.summary)
                }

                // Upload button (passes selected file bytes/name)
                Button(
                    onClick = {
                        viewModel.uploadNote(selectedFileBytes, selectedFileName)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF8BD2), // Pink
                        contentColor = Color.Black
                    ),
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
                            if (uiState.isUploading) "Saving..." else "AI Summarizing..."
                        )
                    } else {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Save & Summarize", style = MaterialTheme.typography.titleSmall)
                    }
                }

                // Success area: show explicit button to open the saved summary.
                if (uiState.isSuccess) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Saved successfully", style = MaterialTheme.typography.titleMedium)
                            uiState.savedNoteId?.let { noteId ->
                                Text("Summary saved for note: ${noteId}", style = MaterialTheme.typography.bodySmall)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(onClick = { onUploadSuccess(noteId) }) {
                                        Text("Open Summary")
                                    }
                                    OutlinedButton(onClick = { viewModel.resetSuccess() }) {
                                        Text("Close")
                                    }
                                }
                            } ?: run {
                                Text("Note saved and summary generated.", style = MaterialTheme.typography.bodySmall)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(onClick = { viewModel.resetSuccess() }) {
                                        Text("Done")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

suspend fun extractTextFromImage(context: android.content.Context, uri: Uri): String = withContext(Dispatchers.IO) {
    try {
        val input = context.contentResolver.openInputStream(uri) ?: return@withContext ""
        val bitmap = android.graphics.BitmapFactory.decodeStream(input)
        input.close()
        val image = InputImage.fromBitmap(bitmap, 0)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val result = recognizer.process(image).awaitForMlKit()
        return@withContext result.text
    } catch (e: Exception) {
        e.printStackTrace()
        return@withContext ""
    }
}

// Fix unresolved reference to `context` by ensuring it is correctly passed and used
suspend fun extractTextFromPdf(context: android.content.Context, uri: Uri): String? = withContext(Dispatchers.IO) {
    val fileDescriptor = context.contentResolver.openFileDescriptor(uri, "r") ?: return@withContext null
    val pdfRenderer = PdfRenderer(fileDescriptor)
    val stringBuilder = StringBuilder()

    try {
        for (pageIndex in 0 until pdfRenderer.pageCount) {
            pdfRenderer.openPage(pageIndex).use { page ->
                val width = page.width
                val height = page.height
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                // Use OCR to extract text from the rendered bitmap
                val inputImage = InputImage.fromBitmap(bitmap, 0)
                val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                val result = recognizer.process(inputImage).awaitForMlKit()
                stringBuilder.append(result.text).append("\n")
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    } finally {
        pdfRenderer.close()
        fileDescriptor.close()
    }

    return@withContext stringBuilder.toString()
}

// helper suspend bridge for ML Kit Task using suspendCancellableCoroutine
suspend fun <T> com.google.android.gms.tasks.Task<T>.awaitForMlKit(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { result ->
        cont.resume(result)
    }
    addOnFailureListener { e ->
        cont.resumeWithException(e)
    }
    cont.invokeOnCancellation { // no-op
    }
}

// Re-add AISummaryInfoCard and SummaryPreviewCard (English-only)
@Composable
private fun AISummaryInfoCard() {
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
                    text = "AI will auto-summarize your notes",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Key points, definitions, and exam tips will be extracted",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun SummaryPreviewCard(summary: String) {
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
                    "AI Summary",
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