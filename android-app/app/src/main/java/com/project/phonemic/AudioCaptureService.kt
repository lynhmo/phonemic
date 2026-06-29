package com.project.phonemic

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.IBinder

class AudioCaptureService : Service() {

    companion object {
        const val EXTRA_HOST = "host"
        const val EXTRA_PORT = "port"
        private const val CHANNEL_ID = "mic_stream"
        private const val SAMPLE_RATE = 48000
        private const val PACKET_MS = 20
        private const val BUFFER_SIZE = SAMPLE_RATE * PACKET_MS / 1000 * 2
    }

    private var recorder: AudioRecord? = null
    private var streamer: UdpStreamer? = null
    private var running = false
    private var thread: Thread? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val host = intent?.getStringExtra(EXTRA_HOST) ?: return START_NOT_STICKY
        val port = intent.getIntExtra(EXTRA_PORT, 5005)

        startForeground(1, buildNotification())
        startStreaming(host, port)
        return START_STICKY
    }

    private fun startStreaming(host: String, port: Int) {
        running = true
        streamer = UdpStreamer(host, port).also { it.start() }

        val minBuf = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        recorder = AudioRecord(
            MediaRecorder.AudioSource.VOICE_COMMUNICATION,
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            maxOf(minBuf, BUFFER_SIZE)
        )

        thread = Thread {
            val buf = ByteArray(BUFFER_SIZE)
            recorder?.startRecording()
            while (running) {
                val read = recorder?.read(buf, 0, BUFFER_SIZE) ?: break
                if (read > 0) streamer?.send(buf, read)
            }
            recorder?.stop()
        }.also { it.start() }
    }

    override fun onDestroy() {
        running = false
        recorder?.stop()   // unblocks blocking read() call in thread
        thread?.join(500)
        recorder?.release()
        streamer?.stop()
        super.onDestroy()
    }

    private fun buildNotification(): Notification =
        Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("Phone Mic")
            .setContentText("Đang stream microphone...")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .build()

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Microphone Stream",
            NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}
