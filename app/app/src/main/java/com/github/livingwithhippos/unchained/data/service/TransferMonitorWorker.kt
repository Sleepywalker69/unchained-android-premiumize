package com.github.livingwithhippos.unchained.data.service

import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.data.model.domain.TransferStatus
import com.github.livingwithhippos.unchained.data.repository.ProviderManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber

@HiltWorker
class TransferMonitorWorker
@AssistedInject
constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val providerManager: ProviderManager,
    private val preferences: SharedPreferences,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val repository = providerManager.getRepository()
            val transfers = repository.getTransferList()

            val activeTransfers =
                transfers.filter {
                    it.status == TransferStatus.DOWNLOADING ||
                        it.status == TransferStatus.QUEUED ||
                        it.status == TransferStatus.MAGNET_CONVERSION ||
                        it.status == TransferStatus.COMPRESSING ||
                        it.status == TransferStatus.UPLOADING
                }

            val completedTransfers =
                transfers.filter { it.status == TransferStatus.COMPLETED }

            completedTransfers.forEach { transfer ->
                val notificationManager =
                    applicationContext.getSystemService(Context.NOTIFICATION_SERVICE)
                        as NotificationManager
                val notification =
                    NotificationCompat.Builder(applicationContext, TORRENT_CHANNEL_ID)
                        .setSmallIcon(R.drawable.icon_transfer)
                        .setContentTitle(transfer.name)
                        .setContentText(
                            applicationContext.getString(R.string.download_complete)
                        )
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        .setAutoCancel(true)
                        .build()
                notificationManager.notify(transfer.id.hashCode(), notification)
            }

            if (activeTransfers.isNotEmpty()) {
                Timber.d(
                    "TransferMonitorWorker: ${activeTransfers.size} active transfers remaining"
                )
            }

            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "TransferMonitorWorker failed")
            Result.retry()
        }
    }

    companion object {
        const val TORRENT_CHANNEL_ID = "unchained_torrent_channel"
        const val WORK_NAME = "transfer_monitor_worker"
    }
}
