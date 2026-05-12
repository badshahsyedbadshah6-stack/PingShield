package com.pingshield.vpn

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import com.pingshield.PingShieldApplication
import com.pingshield.R
import com.pingshield.core.DnsManager
import com.pingshield.core.PacketLossDetector
import com.pingshield.core.PingEngine
import com.pingshield.core.StabilityEngine
import com.pingshield.ui.MainActivity
import com.pingshield.utils.Constants
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject

@AndroidEntryPoint
class PingShieldVpn : VpnService() {

    @Inject lateinit var pingEngine: PingEngine
    @Inject lateinit var stabilityEngine: StabilityEngine
    @Inject lateinit var dnsManager: DnsManager
    @Inject lateinit var packetLossDetector: PacketLossDetector
    @Inject lateinit var trafficController: TrafficController
    @Inject lateinit var dnsInterceptor: DnsInterceptor

    private var vpnInterface: ParcelFileDescriptor? = null
    private var scope: CoroutineScope? = null
    private var isRunning = false

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIFICATION_ID, createNotification(0, false))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopVpn()
            return START_NOT_STICKY
        }

        if (!isRunning) {
            startVpnInternal()
        }
        return START_STICKY
    }

    private fun startVpnInternal() {
        try {
            val builder = Builder()
            builder.setName(Constants.VPN_SESSION)
            builder.setMtu(Constants.VPN_MTU)
            builder.addAddress(Constants.VPN_ADDRESS, Constants.VPN_PREFIX_LENGTH)
            builder.addRoute("0.0.0.0", 0)
            builder.addDnsServer(Constants.DNS_PRIMARY)
            builder.setBlocking(true)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                builder.setMetered(false)
            }

            vpnInterface = builder.establish() ?: throw Exception("VPN establish failed")
            isRunning = true

            trafficController.loadWhitelist()
            dnsInterceptor.start()
            stabilityEngine.start()

            scope = CoroutineScope(Dispatchers.IO + Job())
            startPacketLoop()
            startNotificationUpdater()
        } catch (e: Exception) {
            e.printStackTrace()
            stopVpn()
        }
    }

    private fun startPacketLoop() {
        scope?.launch {
            val input = FileInputStream(vpnInterface?.fileDescriptor)
            val output = FileOutputStream(vpnInterface?.fileDescriptor)
            val buffer = ByteBuffer.allocate(32767)

            while (isActive) {
                try {
                    buffer.clear()
                    val length = input.read(buffer.array())
                    if (length > 0) {
                        buffer.limit(length)
                        val packet = ByteArray(length)
                        buffer.get(packet)

                        val processed = processPacket(packet)
                        if (processed != null) {
                            output.write(processed)
                            output.flush()
                        }
                    }
                } catch (e: Exception) {
                    if (isActive) {
                        delay(100)
                    }
                }
            }

            try {
                input.close()
                output.close()
            } catch (_: Exception) {}
        }
    }

    private fun processPacket(data: ByteArray): ByteArray? {
        if (data.size < 20) return null
        val version = (data[0].toInt() shr 4) and 0x0F
        if (version != 4 && version != 6) return null

        val protocol = data[9].toInt() and 0xFF
        if (protocol == 17) {
            return dnsInterceptor.interceptDns(data)
        }

        return data
    }

    private fun startNotificationUpdater() {
        scope?.launch {
            while (isActive) {
                val ping = pingEngine.currentPing.first()
                val notification = createNotification(ping, true)
                val manager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
                manager.notify(NOTIFICATION_ID, notification)
                delay(1000)
            }
        }
    }

    private fun createNotification(ping: Int, isActive: Boolean): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, PingShieldVpn::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val statusText = if (isActive) "Ping: ${ping}ms" else "Initializing..."

        return NotificationCompat.Builder(this, PingShieldApplication.VPN_CHANNEL_ID)
            .setContentTitle("PingShield Active")
            .setContentText(statusText)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .addAction(android.R.drawable.ic_media_pause, "STOP", stopPendingIntent)
            .build()
    }

    private fun stopVpn() {
        isRunning = false
        try {
            vpnInterface?.close()
        } catch (_: Exception) {}
        vpnInterface = null
        stabilityEngine.stop()
        dnsInterceptor.stop()
        scope?.cancel()
        scope = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onRevoke() {
        stopVpn()
    }

    override fun onDestroy() {
        stopVpn()
        super.onDestroy()
    }

    companion object {
        const val ACTION_STOP = "com.pingshield.STOP_VPN"
        private const val NOTIFICATION_ID = 1001

        fun prepare(context: Context): Intent? {
            return VpnService.prepare(context)
        }

        fun startVpn(context: Context) {
            val intent = Intent(context, PingShieldVpn::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopVpn(context: Context) {
            val intent = Intent(context, PingShieldVpn::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
