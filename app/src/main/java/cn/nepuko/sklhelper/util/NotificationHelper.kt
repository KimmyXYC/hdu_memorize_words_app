package cn.nepuko.sklhelper.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

/**
 * Helper class for managing notifications during the answering process
 */
class NotificationHelper(private val context: Context) {

    companion object {
        private const val CHANNEL_ID = "skl_helper_channel"
        private const val CHANNEL_NAME = "答题通知"
        private const val CHANNEL_DESCRIPTION = "显示答题进度和结果"
        private const val ONGOING_NOTIFICATION_ID = 1001
        private const val COMPLETE_NOTIFICATION_ID = 1002
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
            description = CHANNEL_DESCRIPTION
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    private fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    /**
     * Show persistent "正在答题" notification
     */
    @Suppress("MissingPermission")
    fun showOngoingNotification() {
        if (!hasNotificationPermission()) {
            return
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("正在答题")
            .setContentText("正在自动答题中...")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setOngoing(true) // Make it persistent
            .setAutoCancel(false)
            .build()

        NotificationManagerCompat.from(context).notify(ONGOING_NOTIFICATION_ID, notification)
    }

    /**
     * Cancel the ongoing notification
     */
    fun cancelOngoingNotification() {
        NotificationManagerCompat.from(context).cancel(ONGOING_NOTIFICATION_ID)
    }

    /**
     * Show completion notification
     */
    @Suppress("MissingPermission")
    fun showCompletionNotification(success: Boolean = true) {
        if (!hasNotificationPermission()) {
            return
        }

        val title = if (success) "答题完成" else "答题失败"
        val message = if (success) "自动答题已完成" else "答题过程中出现错误"

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true) // Can be dismissed
            .build()

        NotificationManagerCompat.from(context).notify(COMPLETE_NOTIFICATION_ID, notification)
    }
}
