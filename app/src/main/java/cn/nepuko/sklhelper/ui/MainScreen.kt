package cn.nepuko.sklhelper.ui

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.view.WindowManager
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import cn.nepuko.sklhelper.util.AiSettingsRepository
import cn.nepuko.sklhelper.util.UserPreferences
import kotlinx.coroutines.launch

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val items = listOf(
        Screen.Home,
        Screen.Profile,
    )
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val showBottomBar = items.any { it.route == currentDestination?.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    items.forEach { screen ->
                        NavigationBarItem(
                            icon = {
                                screen.icon?.let {
                                    Icon(it, contentDescription = null)
                                }
                            },
                            label = { Text(screen.title) },
                            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController,
            startDestination = Screen.Home.route,
            Modifier
                .imePadding()
        ) {
            composable(
                Screen.Home.route,
                enterTransition = { fadeIn(animationSpec = tween(150)) },
                exitTransition = { fadeOut(animationSpec = tween(150)) },
                popEnterTransition = { fadeIn(animationSpec = tween(150)) },
                popExitTransition = { fadeOut(animationSpec = tween(150)) }
            ) { HomeScreen(modifier = Modifier.padding(innerPadding), onNavigate = { route -> navController.navigate(route) }) }
            composable(
                Screen.Profile.route,
                enterTransition = { fadeIn(animationSpec = tween(150)) },
                exitTransition = { fadeOut(animationSpec = tween(150)) },
                popEnterTransition = { fadeIn(animationSpec = tween(150)) },
                popExitTransition = { fadeOut(animationSpec = tween(150)) }
            ) { ProfileScreen(modifier = Modifier.padding(innerPadding), onNavigate = { route -> navController.navigate(route) }) }
            composable(
                Screen.AiSettings.route,
                enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) },
                exitTransition = { slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(300)) },
                popEnterTransition = { fadeIn(animationSpec = tween(150)) },
                popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) }
            ) { AiSettingsScreen(onNavigateBack = { navController.popBackStack() }) }
            composable(
                Screen.QuestionBankSettings.route,
                enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) },
                exitTransition = { slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(300)) },
                popEnterTransition = { fadeIn(animationSpec = tween(150)) },
                popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) }
            ) { QuestionBankSettingsScreen(onNavigateBack = { navController.popBackStack() }) }
            composable(
                Screen.Login.route,
                enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) },
                exitTransition = { slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(300)) },
                popEnterTransition = { fadeIn(animationSpec = tween(150)) },
                popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) }
            ) { LoginScreen(onNavigateBack = { navController.popBackStack() }) }
        }
    }
}

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onNavigate: (String) -> Unit,
    aiSettingsViewModel: AiSettingsViewModel = viewModel(
        factory = AiSettingsViewModelFactory(AiSettingsRepository.getInstance(LocalContext.current))
    ),
    answerViewModel: AnswerViewModel = viewModel(
        factory = AnswerViewModelFactory(LocalContext.current)
    )
) {
    val context = LocalContext.current
    var targetScore by remember { mutableStateOf(UserPreferences.getTargetScore(context).toString()) }
    var targetTime by remember { mutableStateOf(UserPreferences.getTargetTime(context).toString()) }
    val modes = listOf("自测", "考试")
    var selectedMode by remember { mutableStateOf(modes[0]) }
    var aiAssistEnabled by remember { mutableStateOf(UserPreferences.getAiAssistEnabled(context)) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val aiSettings by aiSettingsViewModel.aiSettings.collectAsState()
    val username by UserPreferences.username.collectAsState()
    val answerState by answerViewModel.answerState.collectAsState()
    val logScrollState = rememberScrollState()

    // Display log based on answer state
    val answerLog = when (val state = answerState) {
        is AnswerState.Idle -> "答题日志会显示在这里"
        is AnswerState.Running -> state.log
        is AnswerState.Success -> state.log
        is AnswerState.Error -> "错误: ${state.message}"
    }

    // Auto-scroll to bottom when log changes
    LaunchedEffect(answerLog) {
        logScrollState.animateScrollTo(logScrollState.maxValue)
    }

    val isAnswering = answerState is AnswerState.Running

    // Keep screen on during answering
    val view = LocalView.current
    DisposableEffect(isAnswering) {
        val window = (view.context as? android.app.Activity)?.window
        if (isAnswering) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedTextField(
                value = targetScore,
                onValueChange = {
                    val filtered = it.filter { char -> char.isDigit() }
                    if (filtered.isEmpty() || (filtered.toIntOrNull() ?: 0) in 0..100) {
                        targetScore = filtered
                        // Save to preferences when user changes the value
                        if (filtered.isNotEmpty()) {
                            UserPreferences.saveTargetScore(context, filtered.toInt())
                        }
                    }
                },
                label = { Text("目标分数") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                enabled = !isAnswering
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = targetTime,
                onValueChange = {
                    val filtered = it.filter { char -> char.isDigit() }
                    if (filtered.isEmpty() || (filtered.toIntOrNull() ?: 0) in 0..480) {
                        targetTime = filtered
                        // Save to preferences when user changes the value
                        if (filtered.isNotEmpty()) {
                            UserPreferences.saveTargetTime(context, filtered.toInt())
                        }
                    }
                },
                label = { Text("目标时间（秒）") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                enabled = !isAnswering
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text("模式选择", style = MaterialTheme.typography.titleMedium)
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                modes.forEach { mode ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = (selectedMode == mode),
                            onClick = { selectedMode = mode },
                            enabled = !isAnswering
                        )
                        Text(
                            text = mode,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.clickable(enabled = !isAnswering) { selectedMode = mode }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !isAnswering) {
                        aiAssistEnabled = !aiAssistEnabled
                        UserPreferences.saveAiAssistEnabled(context, aiAssistEnabled)
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = aiAssistEnabled,
                    onCheckedChange = {
                        aiAssistEnabled = it
                        UserPreferences.saveAiAssistEnabled(context, it)
                    },
                    enabled = !isAnswering
                )
                Text("启用AI辅助")
            }
            Spacer(modifier = Modifier.height(16.dp))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = MaterialTheme.shapes.medium,
                tonalElevation = 1.dp
            ) {
                Text(
                    text = answerLog,
                    modifier = Modifier
                        .padding(16.dp)
                        .verticalScroll(logScrollState)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    scope.launch {
                        // Check if user is logged in
                        if (username == null) {
                            val result = snackbarHostState.showSnackbar(
                                message = "请先登录",
                                actionLabel = "去登录"
                            )
                            if (result == androidx.compose.material3.SnackbarResult.ActionPerformed) {
                                onNavigate(Screen.Login.route)
                            }
                            return@launch
                        }

                        // Validate inputs
                        if (targetScore.isBlank()) {
                            snackbarHostState.showSnackbar("请输入目标分数")
                            return@launch
                        }

                        if (targetTime.isBlank()) {
                            snackbarHostState.showSnackbar("请输入目标时间")
                            return@launch
                        }

                        if (aiAssistEnabled) {
                            if (aiSettings.apiKey.isBlank()) {
                                val result = snackbarHostState.showSnackbar(
                                    message = "请先配置AI信息",
                                    actionLabel = "去配置"
                                )
                                if (result == androidx.compose.material3.SnackbarResult.ActionPerformed) {
                                    onNavigate(Screen.AiSettings.route)
                                }
                                return@launch
                            }
                        }

                        // Get password
                        val password = UserPreferences.getPassword(context)
                        if (password == null) {
                            snackbarHostState.showSnackbar("无法获取密码，请重新登录")
                            return@launch
                        }

                        // Determine exam type
                        val examType = if (selectedMode == "自测") "0" else "1"

                        // Start answering
                        answerViewModel.startAnswering(
                            username = username!!,
                            password = password,
                            targetScore = targetScore.toInt(),
                            targetTime = targetTime.toInt(),
                            examType = examType,
                            aiAssistEnabled = aiAssistEnabled,
                            aiSettings = if (aiAssistEnabled) aiSettings else null
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isAnswering
            ) {
                Text(if (isAnswering) "答题中..." else "开始答题")
            }
        }
    }
}

@Composable
fun ProfileScreen(modifier: Modifier = Modifier, onNavigate: (String) -> Unit) {
    val username by UserPreferences.username.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        Icon(
            imageVector = Icons.Rounded.AccountCircle,
            contentDescription = "Avatar",
            modifier = Modifier
                .size(80.dp)
                .clickable { onNavigate(Screen.Login.route) }
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = username ?: "未登录", fontSize = 20.sp)
        Spacer(modifier = Modifier.height(32.dp))

        Column {
            SettingsItem(icon = Icons.AutoMirrored.Outlined.MenuBook, text = "题库设置") {
                onNavigate(Screen.QuestionBankSettings.route)
            }
            HorizontalDivider()
            SettingsItem(icon = Icons.Outlined.AutoAwesome, text = "AI设置") {
                onNavigate(Screen.AiSettings.route)
            }
        }
    }
}

@Composable
fun SettingsItem(icon: ImageVector, text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = text, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.size(16.dp))
            Text(text = text, fontSize = 16.sp)
        }
        Icon(
            imageVector = Icons.Outlined.ChevronRight,
            contentDescription = "Arrow",
            modifier = Modifier.size(24.dp)
        )
    }
}
