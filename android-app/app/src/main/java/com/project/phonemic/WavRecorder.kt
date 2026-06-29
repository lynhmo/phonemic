package com.project.phonemic

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import java.io.File
import java.io.RandomAccessFile

class WavRecorder(private val outputFile: File) {

    companion object {
        private const val SAMPLE_RATE = 48000
        private const val BUFFER_SIZE = SAMPLE_RATE * 20 / 1000 * 2 // 20ms packets
    }

    private var recorder: AudioRecord? = null
    private var thread: Thread? = null
    private var running = false

    fun start() {
        val minBuf = AudioRecord.getMinBufferSize(
            SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
        )
        recorder = AudioRecord(
            MediaRecorder.AudioSource.VOICE_COMMUNICATION,
            SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT, maxOf(minBuf, BUFFER_SIZE)
        )
        running = true
        thread = Thread { record() }.also { it.start() }
    }

    private fun record() {
        val buf = ByteArray(BUFFER_SIZE)
        val pcmBytes = mutableListOf<Byte>()

        recorder?.startRecording()
        while (running) {
            val read = recorder?.read(buf, 0, BUFFER_SIZE) ?: break
            if (read > 0) pcmBytes.addAll(buf.take(read))
        }
        recorder?.stop()
        writeWav(pcmBytes.toByteArray())
    }

    fun stop() {
        running = false
        recorder?.stop()
        thread?.join(1000)
        recorder?.release()
        recorder = null
    }

    private fun writeWav(pcm: ByteArray) {
        val dataSize = pcm.size
        val totalSize = 36 + dataSize
        RandomAccessFile(outputFile, "rw").use { f ->
            // RIFF header
            f.writeBytes("RIFF")
            f.writeIntLE(totalSize)
            f.writeBytes("WAVE")
            // fmt chunk
            f.writeBytes("fmt ")
            f.writeIntLE(16)           // chunk size
            f.writeShortLE(1)          // PCM
            f.writeShortLE(1)          // mono
            f.writeIntLE(SAMPLE_RATE)
            f.writeIntLE(SAMPLE_RATE * 2) // byte rate
            f.writeShortLE(2)          // block align
            f.writeShortLE(16)         // bits per sample
            // data chunk
            f.writeBytes("data")
            f.writeIntLE(dataSize)
            f.write(pcm)
        }
    }

    private fun RandomAccessFile.writeIntLE(v: Int) {
        write(v and 0xFF); write((v shr 8) and 0xFF)
        write((v shr 16) and 0xFF); write((v shr 24) and 0xFF)
    }
    private fun RandomAccessFile.writeShortLE(v: Int) {
        write(v and 0xFF); write((v shr 8) and 0xFF)
    }
}
