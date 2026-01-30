package com.example.presentation.ui

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.domain.data.GenerationConfig
import com.example.presentation.theme.MyApplicationTheme
import com.example.presentation.viewmodel.TranslatorUiState
import com.example.presentation.viewmodel.TranslatorViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: TranslatorViewModel by viewModels()
    private var tts: TextToSpeech? = null

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
            }
        }

        setContent {
            MyApplicationTheme {
                Scaffold(
                    topBar = {
                        CenterAlignedTopAppBar(
                            title = {
                                Text(
                                    "Gemini AI Assistant",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.headlineSmall
                                )
                            },
                            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        TranslatorScreen(viewModel, onSpeak = { text ->
                            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
                        }, onStopSpeaking = {
                            tts?.stop()
                        })
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranslatorScreen(
    translatorViewModel: TranslatorViewModel,
    onSpeak: (String) -> Unit,
    onStopSpeaking: () -> Unit
) {
    var textToTranslate by remember { mutableStateOf("") }
    val translatorUiState by translatorViewModel.uiState.collectAsState()
    var expanded by remember { mutableStateOf(false) }
    val languages = listOf("Spanish", "French", "German", "Hindi")
    var selectedLanguage by remember { mutableStateOf(languages[0]) }

    var showAdvancedSettings by remember { mutableStateOf(false) }
    var generationConfig by remember { mutableStateOf(GenerationConfig()) }
    val context = LocalContext.current

    // Description styles
    val descriptionStyles = listOf("Short", "Detailed", "Funny", "Professional", "Accessibility-friendly")
    var selectedStyle by remember { mutableStateOf(descriptionStyles[0]) }
    var styleDropdownExpanded by remember { mutableStateOf(false) }

    // Mute state
    var isMuted by remember { mutableStateOf(false) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview(),
        onResult = { bitmap -> if (bitmap != null) translatorViewModel.describeImage(bitmap, selectedStyle) }
    )

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            uri?.let {
                val bitmap = if (Build.VERSION.SDK_INT < 28) {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, it)
                } else {
                    val source = ImageDecoder.createSource(context.contentResolver, it)
                    ImageDecoder.decodeBitmap(source) { decoder, _, _ -> decoder.isMutableRequired = true }
                }
                translatorViewModel.describeImage(bitmap, selectedStyle)
            }
        }
    )

    val pdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri -> uri?.let { translatorViewModel.summarizePdf(context, it) } }
    )

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { if (it) cameraLauncher.launch(null) }
    )

    LaunchedEffect(translatorUiState, isMuted) {
        if (translatorUiState is TranslatorUiState.Success && selectedStyle == "Accessibility-friendly" && !isMuted) {
            onSpeak((translatorUiState as TranslatorUiState.Success).translatedText)
        } else if (isMuted) {
            onStopSpeaking()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "What can I help you with?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                TextField(
                    value = textToTranslate,
                    onValueChange = { textToTranslate = it },
                    placeholder = { Text("Enter text for translation or a poem topic...") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    )
                )
            }
        }

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = selectedLanguage,
                onValueChange = {},
                readOnly = true,
                label = { Text("Target Language") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                languages.forEach { language ->
                    DropdownMenuItem(
                        text = { Text(language) },
                        onClick = {
                            selectedLanguage = language
                            expanded = false
                        }
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilledTonalButton(
                onClick = {
                    val config = generationConfig.copy(maxOutputTokens = (textToTranslate.length * 2).coerceAtLeast(100))
                    translatorViewModel.translate(textToTranslate, selectedLanguage, config)
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Translate, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Translate")
            }
            FilledTonalButton(
                onClick = { translatorViewModel.generatePoem(textToTranslate) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Poem")
            }
        }

        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Image & Document Tasks", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                
                ExposedDropdownMenuBox(
                    expanded = styleDropdownExpanded,
                    onExpandedChange = { styleDropdownExpanded = !styleDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedStyle,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Description Style") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = styleDropdownExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        textStyle = MaterialTheme.typography.bodySmall
                    )
                    ExposedDropdownMenu(expanded = styleDropdownExpanded, onDismissRequest = { styleDropdownExpanded = false }) {
                        descriptionStyles.forEach { style ->
                            DropdownMenuItem(
                                text = { Text(style) },
                                onClick = {
                                    selectedStyle = style
                                    styleDropdownExpanded = false
                                    (translatorUiState as? TranslatorUiState.Success)?.image?.let {
                                        translatorViewModel.describeImage(it, style)
                                    }
                                }
                            )
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                                cameraLauncher.launch(null)
                            } else {
                                permissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.PhotoCamera, null)
                        Spacer(Modifier.width(4.dp))
                        Text("Camera", fontSize = 12.sp)
                    }
                    Button(onClick = { galleryLauncher.launch("image/*") }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.PhotoLibrary, null)
                        Spacer(Modifier.width(4.dp))
                        Text("Gallery", fontSize = 12.sp)
                    }
                }
                
                OutlinedButton(
                    onClick = { pdfLauncher.launch("application/pdf") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.PictureAsPdf, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Summarize PDF")
                }
            }
        }

        val rotation by animateFloatAsState(if (showAdvancedSettings) 180f else 0f, label = "rotation")
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .clickable { showAdvancedSettings = !showAdvancedSettings }
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Settings, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Advanced Generation Settings", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                }
                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.rotate(rotation)
                )
            }

            AnimatedVisibility(visible = showAdvancedSettings) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    Text(text = "Temperature: ${String.format(Locale.US, "%.2f", generationConfig.temperature)}", style = MaterialTheme.typography.labelMedium)
                    Slider(value = generationConfig.temperature, onValueChange = { generationConfig = generationConfig.copy(temperature = it) })
                    
                    Text(text = "Top P: ${String.format(Locale.US, "%.2f", generationConfig.topP)}", style = MaterialTheme.typography.labelMedium)
                    Slider(value = generationConfig.topP, onValueChange = { generationConfig = generationConfig.copy(topP = it) })

                    OutlinedTextField(
                        value = generationConfig.topK.toString(),
                        onValueChange = { generationConfig = generationConfig.copy(topK = it.toIntOrNull() ?: 0) },
                        label = { Text("Top K") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 100.dp),
            contentAlignment = Alignment.Center
        ) {
            when (val state = translatorUiState) {
                is TranslatorUiState.Loading -> CircularProgressIndicator()
                is TranslatorUiState.Success -> {
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            state.image?.let {
                                Image(
                                    bitmap = it.asImageBitmap(),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .padding(bottom = 12.dp)
                                )
                            }
                            Text(
                                text = state.translatedText,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .semantics { liveRegion = LiveRegionMode.Polite }
                            )
                            IconButton(
                                onClick = { 
                                    if (isMuted) {
                                        isMuted = false
                                        onSpeak(state.translatedText)
                                    } else {
                                        isMuted = true
                                        onStopSpeaking()
                                    }
                                },
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Icon(
                                    imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                    contentDescription = if (isMuted) "Unmute" else "Mute"
                                )
                            }
                        }
                    }
                }
                is TranslatorUiState.Error -> {
                    Text(
                        text = state.errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                else -> {
                    Text(
                        "Results will appear here",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}
