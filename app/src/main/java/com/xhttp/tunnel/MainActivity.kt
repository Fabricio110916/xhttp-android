package com.xhttp.tunnel

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    
    private lateinit var startButton: Button
    private lateinit var stopButton: Button
    private lateinit var statusText: TextView
    private lateinit var logText: TextView
    private lateinit var scrollView: ScrollView
    
    private val handler = Handler(Looper.getMainLooper())
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        startButton = findViewById(R.id.startButton)
        stopButton = findViewById(R.id.stopButton)
        statusText = findViewById(R.id.statusText)
        logText = findViewById(R.id.terminalText)
        scrollView = findViewById(R.id.terminalScroll)
        
        XHttpVpnService.logCallback = { msg ->
            handler.post {
                logText.append("$msg\n")
                scrollView.post { scrollView.fullScroll(android.view.View.FOCUS_DOWN) }
            }
        }
        
        startButton.setOnClickListener {
            val intent = Intent(this, XHttpVpnService::class.java)
            startService(intent)
            startButton.isEnabled = false
            stopButton.isEnabled = true
            statusText.text = "Túnel Ativo"
        }
        
        stopButton.setOnClickListener {
            val intent = Intent(this, XHttpVpnService::class.java).apply { action = "STOP" }
            startService(intent)
            startButton.isEnabled = true
            stopButton.isEnabled = false
            statusText.text = "Parado"
        }
        
        logText.text = "🚀 Teste: Apenas Túnel XHTTP (sem VPN)\n📍 168.138.147.212:443\n\n"
    }
}
