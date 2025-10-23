package com.example.apkupdater

import android.app.*
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

class UpdateService : Service() {
    private val client = OkHttpClient()
    private val baseUrl = "http://10.0.11.150:5176"
    private var job: Job? = null
    private val CHANNEL_ID = "update_channel"
    private val NOTIFICATION_ID = 1
    private val INSTALL_NOTIFICATION_ID = 2

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification("Aguardando atualizações..."))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startMonitoring()
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Serviço de Atualização",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                setShowBadge(false)
                enableLights(false)
                enableVibration(false)
            }

            val installChannel = NotificationChannel(
                "install_channel",
                "Instalação",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                setShowBadge(true)
                enableLights(true)
                enableVibration(true)
            }

            getSystemService(NotificationManager::class.java).apply {
                createNotificationChannel(serviceChannel)
                createNotificationChannel(installChannel)
            }
        }
    }

    private fun createNotification(text: String, icon: Int = android.R.drawable.ic_dialog_info): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("APK Updater")
            .setContentText(text)
            .setSmallIcon(icon)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    private fun updateNotification(text: String, icon: Int = android.R.drawable.ic_dialog_info) {
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, createNotification(text, icon))
    }

    private fun showInstallNotification(file: File) {
        val uri = FileProvider.getUriForFile(this, "$packageName.provider", file)
        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.getActivity(this, 0, installIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        } else {
            PendingIntent.getActivity(this, 0, installIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        }

        val notification = NotificationCompat.Builder(this, "install_channel")
            .setContentTitle("✅ Atualização pronta!")
            .setContentText("Toque para instalar")
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setDefaults(Notification.DEFAULT_ALL)
            .addAction(android.R.drawable.ic_input_add, "Instalar", pendingIntent)
            .build()

        getSystemService(NotificationManager::class.java).notify(INSTALL_NOTIFICATION_ID, notification)
        startActivity(installIntent)
    }

    private fun startMonitoring() {
        job = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                try {
                    val bonusInfo = getApkInfo("bonus")
                    val updaterInfo = getApkInfo("updater")
                    val prefs = getSharedPreferences("app", MODE_PRIVATE)
                    val currentBonusModified = prefs.getLong("last_modified_bonus", 0)
                    val currentUpdaterModified = prefs.getLong("last_modified_updater", 0)

                    when {
                        currentBonusModified == 0L && currentUpdaterModified == 0L -> {
                            prefs.edit()
                                .putLong("last_modified_bonus", bonusInfo.modified)
                                .putLong("last_modified_updater", updaterInfo.modified)
                                .apply()
                        }
                        bonusInfo.modified > currentBonusModified -> {
                            updateNotification("🎉 Atualização Bonus encontrada!", android.R.drawable.stat_sys_download)
                            delay(1000)
                            updateNotification("📥 Baixando Bonus...", android.R.drawable.stat_sys_download)
                            downloadAndInstall("bonus", "Bonus.apk")
                            prefs.edit().putLong("last_modified_bonus", bonusInfo.modified).apply()
                            updateNotification("Aguardando atualizações...")
                        }
                        updaterInfo.modified > currentUpdaterModified -> {
                            updateNotification("🎉 Atualização Updater encontrada!", android.R.drawable.stat_sys_download)
                            delay(1000)
                            updateNotification("📥 Baixando Updater...", android.R.drawable.stat_sys_download)
                            downloadAndInstall("updater", "Updater.apk")
                            prefs.edit().putLong("last_modified_updater", updaterInfo.modified).apply()
                            updateNotification("Aguardando atualizações...")
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                delay(15000)
            }
        }
    }

    private suspend fun getApkInfo(apkName: String): ApkInfo = withContext(Dispatchers.IO) {
        val request = Request.Builder().url("$baseUrl/api/$apkName/info").build()
        val response = client.newCall(request).execute()
        val json = JSONObject(response.body?.string() ?: "{}")
        ApkInfo(json.optDouble("modifiedTimestamp", 0.0).toLong(), json.optLong("sizeBytes", 0))
    }

    private suspend fun downloadAndInstall(apkName: String, fileName: String) = withContext(Dispatchers.IO) {
        val request = Request.Builder().url("$baseUrl/api/$apkName").build()
        val response = client.newCall(request).execute()
        val file = File(getExternalFilesDir(null), fileName)

        response.body?.byteStream()?.use { input ->
            FileOutputStream(file).use { output ->
                input.copyTo(output)
            }
        }

        delay(500)
        withContext(Dispatchers.Main) { showInstallNotification(file) }
    }

    private data class ApkInfo(val modified: Long, val size: Long)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        job?.cancel()
    }
}