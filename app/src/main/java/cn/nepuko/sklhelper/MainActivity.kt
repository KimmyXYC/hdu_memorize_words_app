package cn.nepuko.sklhelper

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import cn.nepuko.sklhelper.data.QuestionBankRepository
import cn.nepuko.sklhelper.ui.MainScreen
import cn.nepuko.sklhelper.ui.theme.SklHelperTheme
import cn.nepuko.sklhelper.util.UserPreferences
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        // 权限结果处理（可选）
        if (isGranted) {
            // 用户授予了通知权限
        } else {
            // 用户拒绝了通知权限
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 初始化用户偏好设置
        UserPreferences.init(this)

        // 自动更新题库（如果启用）
        checkAndUpdateQuestionBank()

        // 请求通知权限（仅适用于 Android 13+）
        requestNotificationPermissionIfNeeded()

        setContent {
            SklHelperTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen()
                }
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasPermission) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun checkAndUpdateQuestionBank() {
        if (UserPreferences.getAutoUpdateQuestionBank(this)) {
            lifecycleScope.launch {
                try {
                    val repository = QuestionBankRepository(applicationContext)
                    repository.updateQuestionBankFromOnline()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}
