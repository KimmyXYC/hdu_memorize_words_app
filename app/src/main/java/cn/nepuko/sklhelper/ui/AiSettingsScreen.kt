package cn.nepuko.sklhelper.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import cn.nepuko.sklhelper.util.AiSettings
import cn.nepuko.sklhelper.util.AiSettingsRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: AiSettingsViewModel = viewModel(
        factory = AiSettingsViewModelFactory(AiSettingsRepository.getInstance(LocalContext.current))
    )
) {
    val context = LocalContext.current
    val currentSettings by viewModel.aiSettings.collectAsState()

    var apiUrl by remember { mutableStateOf("") }
    var apiKey by remember { mutableStateOf("") }
    var modelName by remember { mutableStateOf("") }
    var temperature by remember { mutableStateOf("") }
    var timeout by remember { mutableStateOf("") }
    var retries by remember { mutableStateOf("") }

    LaunchedEffect(currentSettings) {
        apiUrl = currentSettings.apiUrl
        apiKey = currentSettings.apiKey
        modelName = currentSettings.modelName
        temperature = currentSettings.temperature
        timeout = currentSettings.timeout
        retries = currentSettings.retries
    }

    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI设置") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = apiUrl,
                onValueChange = { apiUrl = it },
                label = { Text("API地址") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it },
                label = { Text("API密钥") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = modelName,
                onValueChange = { modelName = it },
                label = { Text("模型名称") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = temperature,
                onValueChange = { newValue ->
                    // Allow empty string, digits, and one decimal point
                    if (newValue.isEmpty()) {
                        temperature = newValue
                    } else if (newValue.matches(Regex("^[0-1]?(\\.\\d*)?$"))) {
                        // Check if the value is valid (0-1 range)
                        val floatValue = newValue.toFloatOrNull()
                        if (floatValue == null || floatValue in 0f..1f) {
                            temperature = newValue
                        }
                    }
                },
                label = { Text("温度 (0-1)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = timeout,
                onValueChange = { timeout = it },
                label = { Text("超时（毫秒）") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = retries,
                onValueChange = { retries = it },
                label = { Text("重试次数") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    val newSettings = AiSettings(
                        apiUrl = apiUrl,
                        apiKey = apiKey,
                        modelName = modelName,
                        temperature = temperature,
                        timeout = timeout,
                        retries = retries
                    )
                    coroutineScope.launch {
                        viewModel.saveAiSettings(newSettings)
                        Toast.makeText(context, "保存成功", Toast.LENGTH_SHORT).show()
                        onNavigateBack()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("保存")
            }
        }
    }
}
