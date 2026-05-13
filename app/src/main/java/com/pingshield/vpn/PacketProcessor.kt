package com.pingshield.vpn

import com.pingshield.core.DnsManager
import com.pingshield.utils.Constants
import java.io.FileInputStream
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PacketProcessor @Inject constructor(
    private val dnsManager: DnsManager
) {
    private var input: FileInputStream? = null
    private var output: FileOutputStream? = null
    private val buffer = ByteArray(32767)
    private val blockedIps = mutableSetOf<String>()
    private val whitelist = mutableSetOf(Constants.GAME_PACKAGE)

    private val dnsResponseBuf = ByteArray(512)
    private val dnsLabelBuf = ByteArray(256)

    fun setStreams(input: FileInputStream, output: FileOutputStream) {
        this.input = input
        this.output = output
    }

    fun setWhitelist(packages: Set<String>) {
        whitelist.clear()
        whitelist.addAll(packages)
    }

    fun setBlockedIps(ips: Set<String>) {
        blockedIps.clear()
        blockedIps.addAll(ips)
    }

    fun addBlockedIp(ip: String) { blockedIps.add(ip) }
    fun removeBlockedIp(ip: String) { blockedIps.remove(ip) }
    fun getBlockedIps(): Set<String> = blockedIps.toSet()

    fun processAndForward(): Boolean {
        val length = input?.read(buffer) ?: return false
        if (length <= 0) return false

        val version = (buffer[0].toInt() shr 4) and 0x0F
        if (version != 4) return true

        val ihl = (buffer[0].toInt() and 0x0F) * 4
        val protocol = buffer[9].toInt() and 0xFF

        if (blockedIps.isNotEmpty() && isDstIpBlocked(buffer)) return true

        if (protocol == 17 && isDnsQuery(buffer, ihl, length)) {
            if (tryInjectDnsResponse(buffer, ihl, length)) return true
        }

        val isGameTraffic = isPubgServerFast(buffer) || protocol == 17

        if (isGameTraffic || (protocol == 6 && isTcpAckOnly(buffer, ihl, length))) {
            val oldTos = buffer[1]
            buffer[1] = 0xB8.toByte()
            updateIpChecksumIncremental(buffer, oldTos, buffer[1])

            if (protocol == 6 && isGameTraffic) {
                val tcpOffset = ihl
                val flags = buffer[tcpOffset + 13].toInt() and 0xFF
                if ((flags and 0x08) == 0) {
                    val oldFlags = buffer[tcpOffset + 13]
                    buffer[tcpOffset + 13] = (flags or 0x08).toByte()
                    updateTcpChecksumIncremental(buffer, ihl, length - ihl, protocol, oldFlags, buffer[tcpOffset + 13])
                }
            }
        }

        return writeOutput(length)
    }

    private fun isTcpAckOnly(buffer: ByteArray, ipHdrLen: Int, pktLen: Int): Boolean {
        val tcpDataLen = pktLen - ipHdrLen
        if (tcpDataLen < 20) return false
        val tcpOffset = ipHdrLen
        val dataOffset = ((buffer[tcpOffset + 12].toInt() and 0xF0) shr 2)
        val tcpHeaderLen = dataOffset.coerceAtLeast(20)
        val payloadLen = tcpDataLen - tcpHeaderLen
        if (payloadLen != 0) return false
        val flags = buffer[tcpOffset + 13].toInt() and 0xFF
        return (flags and 0x10) != 0 && tcpDataLen < 100
    }

    private fun isDnsQuery(buffer: ByteArray, ipHdrLen: Int, pktLen: Int): Boolean {
        if (pktLen < ipHdrLen + 8) return false
        val udpLen = ((buffer[ipHdrLen + 4].toInt() and 0xFF) shl 8) or
                     (buffer[ipHdrLen + 5].toInt() and 0xFF)
        if (udpLen < 12) return false
        val dstPort = ((buffer[ipHdrLen + 2].toInt() and 0xFF) shl 8) or
                      (buffer[ipHdrLen + 3].toInt() and 0xFF)
        return dstPort == 53
    }

    private fun tryInjectDnsResponse(buffer: ByteArray, ipHdrLen: Int, pktLen: Int): Boolean {
        val udpOffset = ipHdrLen
        val dnsOffset = udpOffset + 8
        val dnsLen = pktLen - dnsOffset
        if (dnsLen < 12) return false

        val flags = ((buffer[dnsOffset + 2].toInt() and 0xFF) shl 8) or
                    (buffer[dnsOffset + 3].toInt() and 0xFF)
        if ((flags and 0x8000) != 0) return false
        val qdCount = ((buffer[dnsOffset + 4].toInt() and 0xFF) shl 8) or
                      (buffer[dnsOffset + 5].toInt() and 0xFF)
        if (qdCount != 1) return false

        var nameStart = dnsOffset + 12
        var nameEnd = nameStart
        while (nameEnd < dnsOffset + dnsLen) {
            val labelLen = buffer[nameEnd].toInt() and 0xFF
            if (labelLen == 0) { nameEnd++; break }
            if ((labelLen and 0xC0) != 0) { nameEnd += 2; break }
            nameEnd += 1 + labelLen
        }
        if (nameEnd >= dnsOffset + dnsLen) return false

        val qtypeOffset = nameEnd
        var qnameLen = 0
        var scanPos = nameStart
        while (scanPos < qtypeOffset) {
            val ll = buffer[scanPos].toInt() and 0xFF
            if (ll == 0) { qnameLen++; break }
            scanPos += 1 + ll
            qnameLen += 1 + ll
        }

        val domain = extractDnsName(buffer, nameStart, dnsLen) ?: return false

        val cachedDomains = dnsManager.resolvedDomains.value
        val cachedIp = cachedDomains[domain] ?: return false
        if (cachedIp == "failed" || cachedIp == "unresolved") return false

        if (qtypeOffset + 2 > dnsOffset + dnsLen) return false
        val qtype = ((buffer[qtypeOffset].toInt() and 0xFF) shl 8) or (buffer[qtypeOffset + 1].toInt() and 0xFF)
        if (qtype != 1) return false

        val txId = ((buffer[dnsOffset].toInt() and 0xFF) shl 8) or (buffer[dnsOffset + 1].toInt() and 0xFF)

        var pos = 0
        dnsResponseBuf[pos++] = (txId shr 8).toByte()
        dnsResponseBuf[pos++] = (txId and 0xFF).toByte()
        dnsResponseBuf[pos++] = (0x81).toByte()
        dnsResponseBuf[pos++] = (0x80).toByte()
        dnsResponseBuf[pos++] = (0x00).toByte()
        dnsResponseBuf[pos++] = (0x01).toByte()
        dnsResponseBuf[pos++] = (0x00).toByte()
        dnsResponseBuf[pos++] = (0x01).toByte()
        dnsResponseBuf[pos++] = (0x00).toByte()
        dnsResponseBuf[pos++] = (0x00).toByte()
        dnsResponseBuf[pos++] = (0x00).toByte()
        dnsResponseBuf[pos++] = (0x00).toByte()

        val qLen = qnameLen + 4
        for (i in 0 until qLen) {
            dnsResponseBuf[pos++] = buffer[nameStart + i]
        }

        dnsResponseBuf[pos++] = (0xC0).toByte()
        dnsResponseBuf[pos++] = (0x0C).toByte()
        dnsResponseBuf[pos++] = (0x00).toByte()
        dnsResponseBuf[pos++] = (0x01).toByte()
        dnsResponseBuf[pos++] = (0x00).toByte()
        dnsResponseBuf[pos++] = (0x01).toByte()

        val ttlSeconds = 300
        dnsResponseBuf[pos++] = ((ttlSeconds shr 24) and 0xFF).toByte()
        dnsResponseBuf[pos++] = ((ttlSeconds shr 16) and 0xFF).toByte()
        dnsResponseBuf[pos++] = ((ttlSeconds shr 8) and 0xFF).toByte()
        dnsResponseBuf[pos++] = (ttlSeconds and 0xFF).toByte()

        dnsResponseBuf[pos++] = (0x00).toByte()
        dnsResponseBuf[pos++] = (0x04).toByte()

        val ipParts = cachedIp.split(".")
        if (ipParts.size != 4) return false
        for (part in ipParts) {
            dnsResponseBuf[pos++] = (part.toIntOrNull() ?: return false).toByte()
        }

        val responseLen = pos

        val srcIp0 = buffer[12]; val srcIp1 = buffer[13]; val srcIp2 = buffer[14]; val srcIp3 = buffer[15]
        val dstIp0 = buffer[16]; val dstIp1 = buffer[17]; val dstIp2 = buffer[18]; val dstIp3 = buffer[19]

        val origSrcPort = buffer[udpOffset]; val origSrcPort2 = buffer[udpOffset + 1]
        buffer[udpOffset] = buffer[udpOffset + 2]
        buffer[udpOffset + 1] = buffer[udpOffset + 3]
        buffer[udpOffset + 2] = origSrcPort
        buffer[udpOffset + 3] = origSrcPort2

        buffer[12] = dstIp0; buffer[13] = dstIp1; buffer[14] = dstIp2; buffer[15] = dstIp3
        buffer[16] = srcIp0; buffer[17] = srcIp1; buffer[18] = srcIp2; buffer[19] = srcIp3

        System.arraycopy(dnsResponseBuf, 0, buffer, udpOffset + 8, responseLen)

        val newTotalLen = udpOffset + 8 + responseLen
        val newUdpLen = 8 + responseLen
        buffer[2] = ((newTotalLen shr 8) and 0xFF).toByte()
        buffer[3] = (newTotalLen and 0xFF).toByte()

        buffer[udpOffset + 4] = ((newUdpLen shr 8) and 0xFF).toByte()
        buffer[udpOffset + 5] = (newUdpLen and 0xFF).toByte()

        var oldTos = buffer[1]
        buffer[1] = 0xB8.toByte()
        updateIpChecksumIncremental(buffer, oldTos, buffer[1])

        buffer[udpOffset + 6] = 0
        buffer[udpOffset + 7] = 0
        val udpCksum = computeUdpChecksum(buffer, udpOffset, newUdpLen, 17)
        buffer[udpOffset + 6] = (udpCksum shr 8).toByte()
        buffer[udpOffset + 7] = (udpCksum and 0xFF).toByte()

        return writeOutput(newTotalLen)
    }

    private fun extractDnsName(buffer: ByteArray, offset: Int, maxLen: Int): String? {
        var pos = offset
        var len = 0
        while (pos < offset + maxLen) {
            val labelLen = buffer[pos].toInt() and 0xFF
            if (labelLen == 0) break
            if ((labelLen and 0xC0) != 0) return null
            if (pos + labelLen + 1 > offset + maxLen) return null
            if (len > 0) dnsLabelBuf[len++] = '.'.code.toByte()
            for (i in 1..labelLen) {
                dnsLabelBuf[len++] = buffer[pos + i]
            }
            pos += 1 + labelLen
            if (len > 250) return null
        }
        if (len == 0) return null
        return String(dnsLabelBuf, 0, len)
    }

    private fun isDstIpBlocked(buffer: ByteArray): Boolean {
        val b16 = buffer[16].toInt() and 0xFF
        val b17 = buffer[17].toInt() and 0xFF
        val b18 = buffer[18].toInt() and 0xFF
        val b19 = buffer[19].toInt() and 0xFF
        val sb = StringBuilder(15)
        sb.append(b16).append('.').append(b17).append('.').append(b18).append('.').append(b19)
        return blockedIps.contains(sb.toString())
    }

    private fun isPubgServerFast(buffer: ByteArray): Boolean {
        val first = buffer[16].toInt() and 0xFF
        return first == 34 || first == 35 || first == 52 || first == 54 ||
               first == 99 || first == 108 || first == 174 || first == 203
    }

    private fun isPubgServer(ip: String): Boolean {
        return ip.startsWith("34.") || ip.startsWith("35.") ||
               ip.startsWith("52.") || ip.startsWith("54.") ||
               ip.startsWith("99.") || ip.startsWith("108.") ||
               ip.startsWith("203.") || ip.startsWith("174.")
    }

    private fun updateIpChecksumIncremental(buffer: ByteArray, oldTos: Byte, newTos: Byte) {
        val oldCksum = ((buffer[10].toInt() and 0xFF) shl 8) or (buffer[11].toInt() and 0xFF)
        if (oldCksum == 0) return
        val oldWord = ((buffer[0].toInt() and 0xFF) shl 8) or (oldTos.toInt() and 0xFF)
        val newWord = ((buffer[0].toInt() and 0xFF) shl 8) or (newTos.toInt() and 0xFF)

        var acc = ((oldCksum xor 0xFFFF).toLong() and 0xFFFF) +
                  ((oldWord xor 0xFFFF).toLong() and 0xFFFF) +
                  (newWord.toLong() and 0xFFFF)
        acc = (acc and 0xFFFF) + (acc shr 16)
        acc = (acc and 0xFFFF) + (acc shr 16)
        val newCksum = ((acc xor 0xFFFF).toLong() and 0xFFFF).toInt()
        buffer[10] = (newCksum shr 8).toByte()
        buffer[11] = (newCksum and 0xFF).toByte()
    }

    private fun updateTcpChecksumIncremental(
        buffer: ByteArray, ipHdrLen: Int, tcpLen: Int, protocol: Int,
        oldFlags: Byte, newFlags: Byte
    ) {
        val tcpOffset = ipHdrLen
        val oldTcpCksum = ((buffer[tcpOffset + 16].toInt() and 0xFF) shl 8) or
                          (buffer[tcpOffset + 17].toInt() and 0xFF)
        if (oldTcpCksum == 0) return
        val oldWord = ((buffer[tcpOffset + 12].toInt() and 0xFF) shl 8) or (oldFlags.toInt() and 0xFF)
        val newWord = ((buffer[tcpOffset + 12].toInt() and 0xFF) shl 8) or (newFlags.toInt() and 0xFF)

        var acc = ((oldTcpCksum xor 0xFFFF).toLong() and 0xFFFF) +
                  ((oldWord xor 0xFFFF).toLong() and 0xFFFF) +
                  (newWord.toLong() and 0xFFFF)
        acc = (acc and 0xFFFF) + (acc shr 16)
        acc = (acc and 0xFFFF) + (acc shr 16)
        val newCksum = ((acc xor 0xFFFF).toLong() and 0xFFFF).toInt()
        buffer[tcpOffset + 16] = (newCksum shr 8).toByte()
        buffer[tcpOffset + 17] = (newCksum and 0xFF).toByte()
    }

    private fun computeUdpChecksum(buffer: ByteArray, udpOffset: Int, udpLen: Int, protocol: Int): Int {
        var sum = 0L
        sum += ((buffer[12].toInt() and 0xFF) shl 8) or (buffer[13].toInt() and 0xFF)
        sum += ((buffer[14].toInt() and 0xFF) shl 8) or (buffer[15].toInt() and 0xFF)
        sum += ((buffer[16].toInt() and 0xFF) shl 8) or (buffer[17].toInt() and 0xFF)
        sum += ((buffer[18].toInt() and 0xFF) shl 8) or (buffer[19].toInt() and 0xFF)
        sum += 0x0000 or (protocol and 0xFF)
        sum += udpLen

        var i = udpOffset
        val end = udpOffset + udpLen - 1
        while (i < end) {
            sum += (((buffer[i].toInt() and 0xFF) shl 8) or (buffer[i + 1].toInt() and 0xFF))
            i += 2
        }
        if ((udpLen and 1) == 1) {
            sum += (buffer[udpOffset + udpLen - 1].toInt() and 0xFF) shl 8
        }
        while ((sum shr 16) != 0L) {
            sum = (sum and 0xFFFF) + (sum shr 16)
        }
        return (sum xor 0xFFFF).toInt() and 0xFFFF
    }

    private fun writeOutput(length: Int): Boolean {
        try {
            output?.write(buffer, 0, length)
            return true
        } catch (_: Exception) {
            return false
        }
    }

    fun close() {
        try { input?.close() } catch (_: Exception) {}
        try { output?.close() } catch (_: Exception) {}
        input = null
        output = null
    }
}
