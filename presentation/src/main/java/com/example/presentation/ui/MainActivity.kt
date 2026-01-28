package com.example.presentation.ui

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.api.GenerationConfig
import com.example.presentation.theme.MyApplicationTheme
import com.example.presentation.viewmodel.TranslatorUiState
import com.example.presentation.viewmodel.TranslatorViewModel
import dagger.hilt.android.AndroidEntryPoint
import androidx.activity.viewModels
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: TranslatorViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                TranslatorScreen(viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranslatorScreen(translatorViewModel: TranslatorViewModel) {
    var textToTranslate by remember { mutableStateOf("") }
    val translatorUiState by translatorViewModel.uiState.collectAsState()
    var expanded by remember { mutableStateOf(false) }
    val languages = listOf("Spanish", "French", "German", "Hindi")
    var selectedLanguage by remember { mutableStateOf(languages[0]) }

    // State for advanced settings
    var showAdvancedSettings by remember { mutableStateOf(false) }

    var generationConfig by remember { mutableStateOf(GenerationConfig()) }
    val context = LocalContext.current

    // Description styles
    val descriptionStyles = listOf("Short", "Detailed", "Funny", "Professional", "Accessibility-friendly")
    var selectedStyle by remember { mutableStateOf(descriptionStyles[0]) }
    var styleDropdownExpanded by remember { mutableStateOf(false) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview(),
        onResult = { bitmap: Bitmap? ->
            if (bitmap != null) {
                translatorViewModel.describeImage(bitmap, selectedStyle)
            }
        }
    )

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            uri?.let {
                val bitmap = if (Build.VERSION.SDK_INT < 28) {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, it)
                } else {
                    val source = ImageDecoder.createSource(context.contentResolver, it)
                    ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                        decoder.isMutableRequired = true
                    }
                }
                translatorViewModel.describeImage(bitmap, selectedStyle)
            }
        }
    )

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted: Boolean ->
            if (isGranted) {
                cameraLauncher.launch(null)
            }
        }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TextField(
            value = textToTranslate,
            onValueChange = { textToTranslate = it },
            label = { Text("Enter text for translation or a poem topic") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            TextField(
                value = selectedLanguage,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
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

        Spacer(modifier = Modifier.height(16.dp))

        // Style dropdown for image description
        ExposedDropdownMenuBox(
            expanded = styleDropdownExpanded,
            onExpandedChange = { styleDropdownExpanded = !styleDropdownExpanded }
        ) {
            TextField(
                value = selectedStyle,
                onValueChange = {},
                readOnly = true,
                label = { Text("Image Description Style") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = styleDropdownExpanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )

            ExposedDropdownMenu(
                expanded = styleDropdownExpanded,
                onDismissRequest = { styleDropdownExpanded = false }
            ) {
                descriptionStyles.forEach { style ->
                    DropdownMenuItem(
                        text = { Text(style) },
                        onClick = {
                            selectedStyle = style
                            styleDropdownExpanded = false

                            // Automatically re-describe the image if one is already present
                            (translatorUiState as? TranslatorUiState.Success)?.image?.let { existingImage ->
                                translatorViewModel.describeImage(existingImage, style)
                            }
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- Advanced Settings Section ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showAdvancedSettings = !showAdvancedSettings }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Advanced Settings")
            Icon(
                imageVector = if (showAdvancedSettings) Icons.Default.ArrowDropDown else Icons.Default.ArrowDropDown,
                contentDescription = "Toggle Advanced Settings"
            )
        }

        AnimatedVisibility(visible = showAdvancedSettings) {
            Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp)) {
                Text(text = "Temperature: %.2f".format(generationConfig.temperature))
                Slider(
                    value = generationConfig.temperature,
                    onValueChange = { generationConfig = generationConfig.copy(temperature = it) },
                    valueRange = 0f..1f
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(text = "Top P: %.2f".format(generationConfig.topP))
                Slider(
                    value = generationConfig.topP,
                    onValueChange = { generationConfig = generationConfig.copy(topP = it) },
                    valueRange = 0f..1f
                )

                Spacer(modifier = Modifier.height(16.dp))

                TextField(
                    value = generationConfig.topK.toString(),
                    onValueChange = { value ->
                        generationConfig = generationConfig.copy(topK = value.toIntOrNull() ?: 0)
                    },
                    label = { Text("Top K") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
            Button(onClick = {
                val config = generationConfig.copy(maxOutputTokens = (textToTranslate.length * 2).coerceAtLeast(100))
              //  translatorViewModel.translate(textToTranslate, selectedLanguage, config)
            }) {
                Text("Translate")
            }
            Button(onClick = { translatorViewModel.generatePoem(textToTranslate) }) {
                Text("Generate Poem")
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
            Button(onClick = {
                when (PackageManager.PERMISSION_GRANTED) {
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.CAMERA
                    ) -> {
                        cameraLauncher.launch(null)
                    }
                    else -> {
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                }
            }) {
                Text("From Camera")
            }
            
            Button(onClick = {
                galleryLauncher.launch("image/*")
            }) {
                Text("From Gallery")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (val state = translatorUiState) {
            is TranslatorUiState.Initial -> { /* Do nothing */ }
            is TranslatorUiState.Loading -> {
                CircularProgressIndicator()
            }
            is TranslatorUiState.Success -> {
                state.image?.let {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = "Captured/Selected Image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .padding(bottom = 16.dp)
                    )
                }
                Text(text = state.translatedText, modifier = Modifier.padding(bottom=16.dp))
            }
            is TranslatorUiState.Error -> {
                Text(text = state.errorMessage, color = Color.Red, modifier = Modifier.padding(bottom=16.dp))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TranslatorScreenPreview() {
    MyApplicationTheme {
       // TranslatorScreen()
    }
}
