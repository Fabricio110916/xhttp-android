package com.xhttp.tunnel

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.io.*
import java.net.*
import javax.net.ssl.*

class MainActivity : AppCompatActivity() {
    
    private lateinit var startButton: Button
    private lateinit var stopButton: Button
    private lateinit var statusText: TextView
    private lateinit var logText: TextView
    private lateinit var scrollView: ScrollView
    private lateinit var progressBar: ProgressBar
    
    private val handler = Handler(Looper.getMainLooper())
    private val VPN_REQUEST_CODE = 100
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        startButton = findViewById(R.id.startButton)
        stopButton = findViewById(R.id.stopButton)
        statusText = findViewById(R.id.statusText)
        logText = findViewById(R.id.terminalText)
        scrollView = findViewById(R.id.terminalScroll)
        progressBar = findViewById(R.id.progressBar)
        
        XHttpVpnService.logCallback = { msg ->
            handler.post {
                logText.append("$msg\n")
                scrollView.post { scrollView.fullScroll(android.view.View.FOCUS_DOWN) }
            }
        }
        
        startButton.setOnClickListener {
            val intent = VpnService.prepare(this)
            if (intent != null) {
                startActivityForResult(intent, VPN_REQUEST_CODE)
            } else {
                startVpn()
            }
        }
        
        stopButton.setOnClickListener {
            stopVpn()
        }
        
        log("?? XHTTP VPN")
        log("?? Servidor: 209.74.85.241:443")
        log("")
    }
    
    private fun log(msg: String) {
        handler.post {
            logText.append("$msg\n")
            scrollView.post { scrollView.fullScroll(android.view.View.FOCUS_DOWN) }
        }
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == VPN_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            startVpn()
        }
    }
    
    private fun startVpn() {
        val intent = Intent(this, XHttpVpnService::class.java)
        startService(intent)
        
        startButton.isEnabled = false
        stopButton.isEnabled = true
        progressBar.visibility = android.view.View.VISIBLE
        statusText.text = "VPN ATIVA"
        log("▶ VPN iniciada")
    }
    
    private fun stopVpn() {
        val intent = Intent(this, XHttpVpnService::class.java).apply { action = "STOP" }
        startService(intent)
        
        startButton.isEnabled = true
        stopButton.isEnabled = false
        progressBar.visibility = android.view.View.GONE
        statusText.text = "Parado"
        log("⏹ VPN parada")
    }
    
    class TrustAllCerts : X509TrustManager {
        override fun checkClientTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) {}
        override fun checkServerTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) {}
        override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> = arrayOf()
    }
}
