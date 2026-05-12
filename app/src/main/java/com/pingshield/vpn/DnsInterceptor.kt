package com.pingshield.vpn

import com.pingshield.utils.Constants
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DnsInterceptor @Inject constructor() {
    private var dnsSocket: DatagramSocket? = null

    fun interceptDns(data: ByteArray): ByteArray {
        return try {
            val socket = DatagramSocket()
            val dnsServer = InetAddress.getByName(Constants.DNS_PRIMARY)
            val packet = DatagramPacket(data, data.size, dnsServer, 53)
            socket.soTimeout = 2000
            socket.send(packet)
            val buf = ByteArray(512)
            val recv = DatagramPacket(buf, buf.size)
            socket.receive(recv)
            socket.close()
            recv.data.copyOf(recv.length)
        } catch (_: Exception) { data }
    }

    fun start() { dnsSocket = DatagramSocket() }
    fun stop() { try { dnsSocket?.close() } catch (_: Exception) {}; dnsSocket = null }
}
