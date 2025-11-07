package cn.nepuko.sklhelper.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector? = null) {
    data object Home : Screen("home", "主页", Icons.Outlined.Home)
    data object Profile : Screen("profile", "我的", Icons.Rounded.AccountCircle)
    data object AiSettings : Screen("ai_settings", "AI设置")
    data object QuestionBankSettings : Screen("question_bank_settings", "题库设置", Icons.AutoMirrored.Outlined.MenuBook)
    data object Login : Screen("login", "登录")
}
