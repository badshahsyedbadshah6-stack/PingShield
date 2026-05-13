package com.pingshield.vpn

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.os.Process
import androidx.core.app.NotificationCompat
import com.pingshield.PingShieldApp
import com.pingshield.R
import com.pingshield.core.AdaptiveResponseEngine
import com.pingshield.core.DnsManager
import com.pingshield.core.JitterAnalyzer
import com.pingshield.core.PacketLossAnalyzer
import com.pingshield.core.PingEngine
import com.pingshield.core.StabilityEngine
import com.pingshield.killer.AppKiller
import com.pingshield.monitor.NetworkSwitcher
import com.pingshield.ui.MainActivity
import com.pingshield.utils.Constants
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.FileInputStream
import java.io.FileOutputStream
import javax.inject.Inject

@AndroidEntryPoint
class PingShieldVpn : VpnService() {

    @Inject lateinit var pingEngine: PingEngine
    @Inject lateinit var stabilityEngine: StabilityEngine
    @Inject lateinit var dnsManager: DnsManager
    @Inject lateinit var appKiller: AppKiller
    @Inject lateinit var networkSwitcher: NetworkSwitcher
    @Inject lateinit var packetProcessor: PacketProcessor
    @Inject lateinit var trafficController: TrafficController
    @Inject lateinit var jitterAnalyzer: JitterAnalyzer
    @Inject lateinit var adaptiveEngine: AdaptiveResponseEngine
    @Inject lateinit var packetLossAnalyzer: PacketLossAnalyzer
    @Inject lateinit var dnsInterceptor: DnsInterceptor

    private var vpnInterface: ParcelFileDescriptor? = null
    private var scope: CoroutineScope? = null
    private var isRunning = false

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIFICATION_ID, createNotification("Starting..."))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopVpn()
            return START_NOT_STICKY
        }
        if (!isRunning) startVpnInternal()
        return START_STICKY
    }

    private fun startVpnInternal() {
        try {
            val builder = Builder()
            builder.setSession(Constants.VPN_SESSION)
            builder.addAddress(Constants.VPN_ADDRESS, Constants.VPN_PREFIX_LENGTH)
            builder.addRoute("0.0.0.0", 0)
            builder.addDnsServer(java.net.InetAddress.getByName(Constants.DNS_PRIMARY))
            builder.setMtu(Constants.VPN_MTU)
            builder.setBlocking(false)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                builder.setMetered(false)
            }

            appKiller.proactiveKill()
            for (pkg in appKiller.getBlockedPackages()) {
                try { builder.addDisallowedApplication(pkg) } catch (_: Exception) {}
            }

            vpnInterface = builder.establish()
            if (vpnInterface == null) {
                stopSelf()
                return
            }

            isRunning = true

            networkSwitcher.setVpnService(this)
            trafficController.loadWhitelist()
            stabilityEngine.start()
            dnsInterceptor.start()

            val input = FileInputStream(vpnInterface?.fileDescriptor)
            val output = FileOutputStream(vpnInterface?.fileDescriptor)
            packetProcessor.setStreams(input, output)
            packetProcessor.setWhitelist(trafficController.getWhitelist())

            val blockedIps = appKiller.getBlockedIps()
            if (blockedIps.isNotEmpty()) {
                packetProcessor.setBlockedIps(blockedIps)
            }

            scope = CoroutineScope(Dispatchers.IO + Job())
            scope?.launch {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    try {
                        val hintMgr = this@PingShieldVpn.getSystemService(Context.PERFORMANCE_HINT_SERVICE)
                        if (hintMgr != null) {
                            val cls = hintMgr.javaClass
                            val method = cls.getMethod("createHintSession", IntArray::class.java, Long::class.java)
                            method.invoke(hintMgr, intArrayOf(Process.myTid()), Constants.TARGET_FRAME_NANOS)
                        }
                    } catch (_: Exception) {}
                }
                while (isActive) {
                    try {
                        packetProcessor.processAndForward()
                    } catch (_: Exception) {
                        delay(10)
                    }
                }
            }

            scope?.launch {
                while (isActive) {
                    val status = buildStatusText()
                    val notification = createNotification(status)
                    val manager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
                    manager.notify(NOTIFICATION_ID, notification)
                    delay(1000)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            stopVpn()
        }
    }

    private fun buildStatusText(): String {
        val ping = pingEngine.currentPing.value
        val jitter = jitterAnalyzer.ipdv.value
        val score = adaptiveEngine.networkScore.value
        val loss = packetLossAnalyzer.lossPercent.value
        return "Ping ${ping}ms | Jitter ${"%.1f".format(jitter)}ms | Score $score | Loss ${"%.1f".format(loss)}%"
    }

    private fun createNotification(status: String): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = Intent(this, PingShieldVpn::class.java).apply { action = ACTION_STOP }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val ping = pingEngine.currentPing.value

        return NotificationCompat.Builder(this, PingShieldApp.VPN_CHANNEL_ID)
            .setContentTitle("PingShield \u25CF ${ping}ms")
            .setContentText(status)
            .setStyle(NotificationCompat.BigTextStyle().bigText(status))
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .addAction(android.R.drawable.ic_media_pause, "STOP", stopPendingIntent)
            .build()
    }

    private fun stopVpn() {
        isRunning = false
        stabilityEngine.stop()
        dnsInterceptor.stop()
        packetProcessor.close()
        try { vpnInterface?.close() } catch (_: Exception) {}
        vpnInterface = null
        scope?.cancel()
        scope = null
        networkSwitcher.setVpnService(null)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onRevoke() { stopVpn() }
    override fun onDestroy() { stopVpn(); super.onDestroy() }

    companion object {
        const val ACTION_STOP = "com.pingshield.STOP_VPN"
        private const val NOTIFICATION_ID = 1001

        fun prepare(context: Context): Intent? = VpnService.prepare(context)

        fun startVpn(context: Context) {
            val intent = Intent(context, PingShieldVpn::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                context.startForegroundService(intent)
            else
                context.startService(intent)
        }

        fun stopVpn(context: Context) {
            Intent(context, PingShieldVpn::class.java).apply {
                action = ACTION_STOP
                context.startService(this)
            }
        }
    }
}
