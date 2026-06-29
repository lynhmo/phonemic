package com.project.phonemic

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Gửi UDP probe packets tới host:port, nhận echo từ server,
 * tính RTT, jitter, packet loss, throughput mỗi giây.
 *
 * ponytail: throughput chỉ đo tx-side (bytes gửi/s).
 * Add khi Phase 4: nhận ACK từ server để tính rx throughput thực.
 */
class BandwidthTester(private val host: String, private val port: Int) {

    companion object {
        private const val PROBE_PORT = 5006       // port riêng, không đụng audio 5005
        private const val PROBE_INTERVAL_MS = 50L // 20 probes/s
        private const val ECHO_TIMEOUT_MS = 2000
        private const val HISTORY = 60            // 60 điểm = 60 giây
    }

    data class Sample(
        val rttMs: Double,
        val lossPercent: Double,
        val jitterMs: Double,
        val throughputMbps: Double,
        val quality: Quality
    )

    enum class Quality(val label: String, val colorHex: Int) {
        EXCELLENT("Xuất sắc", 0xFF4CAF50.toInt()),
        GOOD     ("Tốt",      0xFF2196F3.toInt()),
        FAIR     ("Trung bình",0xFFFF9800.toInt()),
        POOR     ("Kém",      0xFFF44336.toInt())
    }

    interface Callback {
        fun onSample(sample: Sample)
        fun onError(msg: String)
    }

    private var running = false
    private var sendThread: Thread? = null
    private var recvThread: Thread? = null
    private var socket: DatagramSocket? = null

    // per-second accumulators
    private val pendingRtts = mutableMapOf<Int, Long>() // seq → sendNano
    private val rttHistory = ArrayDeque<Double>(HISTORY)
    private var seqCounter = 0
    private var sentThisSec = 0
    private var receivedThisSec = 0
    private var bytesSentThisSec = 0

    fun start(callback: Callback) {
        if (running) return
        running = true

        val addr = try { InetAddress.getByName(host) }
        catch (e: Exception) { callback.onError("Không resolve được $host"); running = false; return }

        socket = DatagramSocket().also { it.soTimeout = ECHO_TIMEOUT_MS }

        // receive thread — đọc echo packets
        recvThread = Thread {
            val buf = ByteArray(BandwidthProbe.SIZE)
            val pkt = DatagramPacket(buf, buf.size)
            while (running) {
                try {
                    socket?.receive(pkt)
                    val now = System.nanoTime()
                    val (seq, _, ok) = BandwidthProbe.decode(buf)
                    if (!ok) continue
                    synchronized(pendingRtts) {
                        pendingRtts.remove(seq)?.let { sentNano ->
                            val rttMs = (now - sentNano) / 1_000_000.0
                            rttHistory.addLast(rttMs)
                            if (rttHistory.size > HISTORY) rttHistory.removeFirst()
                            receivedThisSec++
                        }
                    }
                } catch (_: Exception) {}
            }
        }.also { it.isDaemon = true; it.start() }

        // send thread — gửi probe + report mỗi giây
        sendThread = Thread {
            var lastReport = System.currentTimeMillis()
            while (running) {
                val seq = seqCounter++
                val now = System.nanoTime()
                val payload = BandwidthProbe.encode(seq, now)
                synchronized(pendingRtts) { pendingRtts[seq] = now }
                try {
                    socket?.send(DatagramPacket(payload, payload.size, addr, PROBE_PORT))
                    sentThisSec++
                    bytesSentThisSec += payload.size
                } catch (e: Exception) { callback.onError(e.message ?: "send error"); break }

                val elapsed = System.currentTimeMillis() - lastReport
                if (elapsed >= 1000) {
                    val sample = buildSample()
                    callback.onSample(sample)
                    sentThisSec = 0; receivedThisSec = 0; bytesSentThisSec = 0
                    lastReport = System.currentTimeMillis()
                }
                Thread.sleep(PROBE_INTERVAL_MS)
            }
        }.also { it.isDaemon = true; it.start() }
    }

    private fun buildSample(): Sample {
        val sent = sentThisSec.coerceAtLeast(1)
        val loss = ((sent - receivedThisSec).coerceAtLeast(0) * 100.0 / sent)
        val recentRtts = synchronized(pendingRtts) { rttHistory.takeLast(20) }
        val avgRtt = if (recentRtts.isEmpty()) 0.0 else recentRtts.average()
        val jitter = if (recentRtts.size < 2) 0.0 else {
            val diffs = recentRtts.zipWithNext { a, b -> abs(b - a) }
            diffs.average()
        }
        val throughput = bytesSentThisSec * 8.0 / 1_000_000.0

        val quality = when {
            avgRtt < 5 && loss < 0.5 && throughput > 5.0  -> Quality.EXCELLENT
            avgRtt < 20 && loss < 1.0 && throughput > 2.0 -> Quality.GOOD
            avgRtt < 50 && loss < 5.0 && throughput > 0.5 -> Quality.FAIR
            else                                            -> Quality.POOR
        }
        return Sample(avgRtt, loss, jitter, throughput, quality)
    }

    fun stop() {
        running = false
        socket?.close()
        sendThread?.join(500)
        recvThread?.join(500)
        socket = null
    }
}
