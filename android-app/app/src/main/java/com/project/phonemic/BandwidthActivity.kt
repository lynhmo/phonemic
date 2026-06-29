package com.project.phonemic

import android.app.AlertDialog
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class BandwidthActivity : AppCompatActivity() {

    private var tester: BandwidthTester? = null
    private var testing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bandwidth)

        // status bar inset → extra top padding
        val root = findViewById<LinearLayout>(R.id.rootLayout)
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            val pad = (16 * resources.displayMetrics.density).toInt()
            v.setPadding(pad, top + pad, pad, pad)
            insets
        }

        val host = intent.getStringExtra("host") ?: ""
        val graph = findViewById<BandwidthGraphView>(R.id.graphView)
        val btnStart = findViewById<Button>(R.id.btnStartTest)
        val tvPing = findViewById<TextView>(R.id.tvPing)
        val tvJitter = findViewById<TextView>(R.id.tvJitter)
        val tvLoss = findViewById<TextView>(R.id.tvLoss)
        val tvThroughput = findViewById<TextView>(R.id.tvThroughput)

        btnStart.setOnClickListener {
            if (!testing) {
                if (host.isEmpty()) {
                    AlertDialog.Builder(this)
                        .setTitle("Chưa có IP")
                        .setMessage("Vui lòng nhập IP máy Windows ở màn hình chính trước khi đo.")
                        .setPositiveButton("OK", null)
                        .show()
                    return@setOnClickListener
                }
                testing = true
                btnStart.text = "Dừng"
                graph.clear()
                tester = BandwidthTester(host, 5006).also {
                    it.start(object : BandwidthTester.Callback {
                        override fun onSample(sample: BandwidthTester.Sample) {
                            runOnUiThread {
                                tvPing.text = "%.1f".format(sample.rttMs)
                                tvJitter.text = "%.1f".format(sample.jitterMs)
                                tvLoss.text = "%.1f".format(sample.lossPercent)
                                tvThroughput.text = "%.2f".format(sample.throughputMbps)
                                graph.addPoint(sample.rttMs)
                            }
                        }
                        override fun onError(msg: String) {
                            runOnUiThread {
                                tester = null
                                testing = false
                                btnStart.text = "Bắt đầu đo"
                                AlertDialog.Builder(this@BandwidthActivity)
                                    .setTitle("Kết nối thất bại")
                                    .setMessage("Không thể kết nối tới $host.\n\nLỗi: $msg\n\nKiểm tra:\n• IP đúng chưa?\n• PowerShell echo server đang chạy chưa?\n• Firewall có chặn UDP 5006 không?")
                                    .setPositiveButton("OK", null)
                                    .show()
                            }
                        }
                    })
                }
            } else {
                tester?.stop()
                tester = null
                testing = false
                btnStart.text = "Bắt đầu đo"
            }
        }
    }

    override fun onDestroy() {
        tester?.stop()
        super.onDestroy()
    }
}
