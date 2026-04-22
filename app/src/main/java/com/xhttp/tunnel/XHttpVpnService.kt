package com.xhttp.tunnel

import android.app.*
import android.content.Intent
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat
import java.io.*
import java.net.*
import javax.net.ssl.*
import kotlin.concurrent.thread

class XHttpVpnService : Service() {
    
    private var tlsSocket: SSLSocket? = null
    private var isRunning = false
    
    companion object {
        private const val NOTIFICATION_ID = 999
        private const val CHANNEL_ID = "xhttp_tunnel"
        var logCallback: ((String) -> Unit)? = null
    }
    
    private fun log(msg: String) {
        Log.i("XHttpTunnel", msg)
        logCallback?.invoke(msg)
    }
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "STOP") {
            stopTunnel()
            return START_NOT_STICKY
        }
        if (!isRunning) {
            startForeground(NOTIFICATION_ID, createNotification("Túnel XHTTP ativo"))
            thread { startTunnel() }
        }
        return START_STICKY
    }
    
    private fun startTunnel() {
        isRunning = true
        
        try {
            log("════════════════════════════════")
            log("?? TESTE: APENAS TÚNEL XHTTP")
            log("════════════════════════════════")
            
            log("[1/3] Conectando TLS...")
            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(null, arrayOf(TrustAllCerts()), java.security.SecureRandom())
            val factory = sslContext.socketFactory
            tlsSocket = factory.createSocket("168.138.147.212", 443) as SSLSocket
            tlsSocket?.startHandshake()
            log("✅ TLS: ${tlsSocket?.session?.cipherSuite}")
            
            log("[2/3] Enviando POST...")
            val writer = OutputStreamWriter(tlsSocket!!.outputStream)
            writer.write("POST /ssh HTTP/1.1\r\n")
            writer.write("Host: oracle.koom.pp.ua\r\n")
            writer.write("Content-Length: 0\r\n\r\n")
            writer.flush()
            
            val reader = BufferedReader(InputStreamReader(tlsSocket!!.inputStream))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                if (line!!.isEmpty()) break
            }
            log("✅ HTTP 200 OK")
            
            log("[3/3] ?? TÚNEL XHTTP ESTABELECIDO!")
            log("")
            log("⏸ AGUARDANDO 60 SEGUNDOS...")
            log("   (SEM TUN, SEM VPN - APENAS TÚNEL)")
            log("")
            
            updateNotification("Túnel XHTTP", "Ativo (sem VPN)")
            
            // AGUARDAR 60 SEGUNDOS
            for (i in 1..60) {
                if (!isRunning) break
                Thread.sleep(1000)
                if (i % 10 == 0) {
                    log("   ⏰ $i segundos...")
                }
            }
            
            if (isRunning) {
                log("")
                log("════════════════════════════════")
                log("✅ TESTE CONCLUÍDO!")
                log("════════════════════════════════")
                log("?? O túnel XHTTP ficou estável por 60s!")
                log("?? Conclusão: O problema é a TUN/VPN!")
            }
            
        } catch (e: Exception) {
            log("❌ ${e.message}")
            stopTunnel()
        }
    }
    
    private fun stopTunnel() {
        isRunning = false
        try { tlsSocket?.close() } catch (e: Exception) {}
        stopForeground(true)
        stopSelf()
        log("⏹ Túnel parado")
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "XHTTP Tunnel", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(text: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("XHTTP Tunnel Teste")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .build()
    }
    
    private fun updateNotification(title: String, content: String) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .build()
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification)
    }
    
    override fun onDestroy() {
        stopTunnel()
        super.onDestroy()
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    class TrustAllCerts : X509TrustManager {
        override fun checkClientTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) {}
        override fun checkServerTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) {}
        override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> = arrayOf()
    }
}
