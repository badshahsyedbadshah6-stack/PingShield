package com.pingshield.vpn

import com.pingshield.utils.Constants
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PacketProcessor @Inject constructor() {

    private var input: FileInputStream? = null
    private var output: FileOutputStream? = null
    private val buffer = ByteArray(32767)
    private val whitelist = mutableSetOf(Constants.GAME_PACKAGE)

    fun setStreams(input: FileInputStream, output: FileOutputStream) {
        this.input = input
        this.output = output
    }

    fun setWhitelist(packages: Set<String>) {
        whitelist.clear()
        whitelist.addAll(packages)
    }

    fun processPacket(uid: Int, pkgName: String?): Boolean {
        val isGame = pkgName == Constants.GAME_PACKAGE
        return isGame || whitelist.contains(pkgName)
    }

    fun processAndForward(): Boolean {
        try {
            val length = input?.read(buffer) ?: return false
            if (length <= 0) return false

            val buf = ByteBuffer.wrap(buffer, 0, length)
            buf.order(ByteOrder.BIG_ENDIAN)

            val version = (buf.get(0).toInt() shr 4) and 0x0F
            if (version == 6) return true

            if (version != 4) return true

            val protocol = buf.get(9).toInt() and 0xFF
            val dstIp = String.format(
                "%d.%d.%d.%d",
                buf.get(16).toInt() and 0xFF,
                buf.get(17).toInt() and 0xFF,
                buf.get(18).toInt() and 0xFF,
                buf.get(19).toInt() and 0xFF
            )

            val ihl = (buf.get(0).toInt() and 0x0F) * 4
            val isGameTraffic = isPubgServer(dstIp) || protocol == 17

            if (isGameTraffic) {
                val originalTOS = buf.get(1)
                buf.put(1, 0xB8.toByte())

                buf.putShort(10, 0)
                val checksum = calculateChecksum(buf, 0, ihl)
                buf.putShort(10, checksum)

                if (protocol == 6) {
                    val tcpHeaderOffset = ihl
                    val flags = buf.get(tcpHeaderOffset + 13).toInt() and 0xFF
                    if ((flags and 0x08) == 0) {
                        buf.put(tcpHeaderOffset + 13, (flags or 0x08).toByte())
                        val tcpLen = length - ihl
                        val pseudoChecksum = calculatePseudoChecksum(buf, ihl, tcpLen, protocol)
                        buf.putShort(tcpHeaderOffset + 16, pseudoChecksum)
                    }
                }
            }

            output?.write(buffer, 0, length)
            output?.flush()
            return true
        } catch (e: Exception) {
            return false
        }
    }

    private fun isPubgServer(ip: String): Boolean {
        return ip.startsWith("34.") || ip.startsWith("35.") ||
               ip.startsWith("52.") || ip.startsWith("54.") ||
               ip.startsWith("99.") || ip.startsWith("108.") ||
               ip.startsWith("203.") || ip.startsWith("174.")
    }

    fun calculateChecksum(buf: ByteBuffer, offset: Int, length: Int): Short {
        var sum = 0L
        var i = offset
        while (i < offset + length - 1) {
            sum += (((buf.get(i).toInt() and 0xFF) shl 8) or
                    (buf.get(i + 1).toInt() and 0xFF))
            i += 2
        }
        if ((length and 1) == 1) {
            sum += (buf.get(offset + length - 1).toInt() and 0xFF) shl 8
        }
        while ((sum shr 16) != 0L) {
            sum = (sum and 0xFFFF) + (sum shr 16)
        }
        return ((sum.inv() and 0xFFFF).toShort())
    }

    private fun calculatePseudoChecksum(buf: ByteBuffer, offset: Int, length: Int, protocol: Int): Short {
        val pseudoBuf = ByteBuffer.allocate(12 + length)
        pseudoBuf.order(ByteOrder.BIG_ENDIAN)

        pseudoBuf.put(buf.get(12))
        pseudoBuf.put(buf.get(13))
        pseudoBuf.put(buf.get(14))
        pseudoBuf.put(buf.get(15))
        pseudoBuf.put(buf.get(16))
        pseudoBuf.put(buf.get(17))
        pseudoBuf.put(buf.get(18))
        pseudoBuf.put(buf.get(19))

        pseudoBuf.putShort(0) // zero
        pseudoBuf.put(protocol.toByte())

        pseudoBuf.putShort(length.toShort())

        buf.position(offset)
        for (j in 0 until length) {
            pseudoBuf.put(buf.get(offset + j))
        }

        pseudoBuf.flip()
        return calculateChecksum(pseudoBuf, 0, pseudoBuf.limit())
    }

    fun close() {
        try { input?.close() } catch (_: Exception) {}
        try { output?.close() } catch (_: Exception) {}
        input = null
        output = null
    }
}
