package com.project.phonemic

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.MotionEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.appbar.MaterialToolbar
import java.io.File

class MainActivity : AppCompatActivity() {

    private var streaming = false
    private var wavRecorder: WavRecorder? = null
    private var recording = false
    private lateinit var ipHistory: IpHistoryStore
    private lateinit var etIp: MaterialAutoCompleteTextView

    private val requestPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) toggleStream() }

    private val requestPermissionForWav = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) toggleWavRecord() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        ipHistory = IpHistoryStore(this)
        etIp = findViewById(R.id.etIp)
        refreshIpAdapter()
        ipHistory.getAll().firstOrNull()?.let { etIp.setText(it) }

        // hide keyboard on Done
        etIp.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                hideKeyboard(); true
            } else false
        }

        findViewById<MaterialButton>(R.id.btnToggle).setOnClickListener {
            if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                toggleStream()
            } else {
                requestPermission.launch(Manifest.permission.RECORD_AUDIO)
            }
        }

        findViewById<MaterialButton>(R.id.btnRecord).setOnClickListener {
            if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                toggleWavRecord()
            } else {
                requestPermissionForWav.launch(Manifest.permission.RECORD_AUDIO)
            }
        }

        findViewById<MaterialButton>(R.id.btnBandwidth).setOnClickListener {
            val ip = etIp.text.toString().trim()
            startActivity(Intent(this, BandwidthActivity::class.java).putExtra("host", ip))
        }
    }

    // touch outside EditText → hide keyboard
    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (ev.action == MotionEvent.ACTION_DOWN) {
            val focused = currentFocus
            if (focused is MaterialAutoCompleteTextView) {
                val rect = android.graphics.Rect()
                focused.getGlobalVisibleRect(rect)
                if (!rect.contains(ev.rawX.toInt(), ev.rawY.toInt())) {
                    hideKeyboard()
                    focused.clearFocus()
                }
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        currentFocus?.let { imm.hideSoftInputFromWindow(it.windowToken, 0) }
    }

    private fun refreshIpAdapter() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, ipHistory.getAll())
        etIp.setAdapter(adapter)
    }

    private fun getIp() = etIp.text.toString().trim()

    private fun toggleStream() {
        val ip = getIp()
        val status = findViewById<TextView>(R.id.tvStatus)
        val btn = findViewById<MaterialButton>(R.id.btnToggle)

        if (!streaming) {
            if (ip.isEmpty()) { status.text = "Nhập IP máy Windows trước"; return }
            ipHistory.save(ip)
            refreshIpAdapter()
            val intent = Intent(this, AudioCaptureService::class.java).apply {
                putExtra(AudioCaptureService.EXTRA_HOST, ip)
                putExtra(AudioCaptureService.EXTRA_PORT, 5005)
            }
            startForegroundService(intent)
            streaming = true
            btn.text = "Stop Stream"
            status.text = "Đang stream tới $ip:5005"
        } else {
            stopService(Intent(this, AudioCaptureService::class.java))
            streaming = false
            btn.text = "Start Stream"
            status.text = "Đã dừng"
        }
    }

    private fun toggleWavRecord() {
        val status = findViewById<TextView>(R.id.tvStatus)
        val btn = findViewById<MaterialButton>(R.id.btnRecord)

        if (!recording) {
            val outFile = File(getExternalFilesDir(null), "test_audio.wav")
            wavRecorder = WavRecorder(outFile).also { it.start() }
            recording = true
            btn.text = "Stop Recording"
            status.text = "Đang ghi... lưu tại: ${outFile.absolutePath}"
            btn.postDelayed({ if (recording) toggleWavRecord() }, 10_000)
        } else {
            wavRecorder?.stop()
            wavRecorder = null
            recording = false
            btn.text = "Record WAV (10s)"
            status.text = "Đã lưu: ${File(getExternalFilesDir(null), "test_audio.wav").absolutePath}"
        }
    }
}
