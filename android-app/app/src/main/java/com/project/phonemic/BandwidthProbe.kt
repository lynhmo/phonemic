package com.project.phonemic

import java.nio.ByteBuffer

/**
 * Probe packet layout (16 bytes):
 * [0-3]  magic: 0x504D4250 ("PMBP")
 * [4-7]  sequence: Int
 * [8-15] sendTimestamp: Long (System.nanoTime())
 */
object BandwidthProbe {
    const val SIZE = 16
    private const val MAGIC = 0x504D4250.toInt()

    fun encode(seq: Int, timestamp: Long): ByteArray {
        val buf = ByteBuffer.allocate(SIZE)
        buf.putInt(MAGIC)
        buf.putInt(seq)
        buf.putLong(timestamp)
        return buf.array()
    }

    fun decode(data: ByteArray): Triple<Int, Long, Boolean> {
        if (data.size < SIZE) return Triple(0, 0L, false)
        val buf = ByteBuffer.wrap(data)
        val magic = buf.int
        if (magic != MAGIC) return Triple(0, 0L, false)
        val seq = buf.int
        val ts = buf.long
        return Triple(seq, ts, true)
    }
}
