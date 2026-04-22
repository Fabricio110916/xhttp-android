package com.xhttp.tunnel

import android.app.*
import android.content.Intent
import android.net.VpnService
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat
import java.io.*
import java.net.*
import javax.net.ssl.*
import kotlin.concurrent.thread

class XHttpVpnService : VpnService() {
    
    private var vpnInterface: ParcelFileDescriptor? = null
    private var tlsSocket: SSLSocket? = null
    private var isRunning = false
    
    companion object {
        private const val NOTIFICATION_ID = 999
        private const val CHANNEL_ID = "xhttp_vpn"
        var logCallback: ((String) -> Unit)? = null
    }
    
    private fun log(msg: String) {
        Log.i("XHttpVPN", msg)
        logCallback?.invoke(msg)
    }
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "STOP") {
            stopVpn()
            return START_NOT_STICKY
        }
        if (!isRunning) {
            startForeground(NOTIFICATION_ID, createNotification("VPN Iniciando..."))
            thread { startVpn() }
        }
        return START_STICKY
    }
    
    private fun startVpn() {
        isRunning = true
        
        try {
            log("[1/5] Conectando TLS...")
            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(null, arrayOf(TrustAllCerts()), java.security.SecureRandom())
            val factory = sslContext.socketFactory
            tlsSocket = factory.createSocket("168.138.147.212", 443) as SSLSocket
            tlsSocket?.startHandshake()
            log("✅ TLS: ${tlsSocket?.session?.cipherSuite}")
            
            log("[2/5] Enviando POST...")
            val writer = OutputStreamWriter(tlsSocket!!.outputStream)
            writer.write("POST /ssh HTTP/1.1\r\n")
            writer.write("Host: oracle.koom.pp.ua\r\n")
            writer.write("Content-Length: 0\r\n\r\n")
            writer.flush()
            
            val reader = BufferedReader(InputStreamReader(tlsSocket!!.inputStream))
            var line: String?
            var status = ""
            while (reader.readLine().also { line = it } != null) {
                if (line!!.startsWith("HTTP/")) status = line!!
                if (line!!.isEmpty()) break
            }
            
            if (!status.contains("200")) {
                throw Exception("HTTP $status")
            }
            log("✅ HTTP 200 OK")
            
            log("[3/5] Configurando VPN (protegendo servidor)...")
            val builder = Builder()
                .setSession("XHTTP VPN")
                .addAddress("10.8.0.2", 32)
                
                // 🔥 PROTEGER O SERVIDOR (não rotear pela VPN!)
                .addRoute("168.138.147.212", 32)
                
                // 🔥 Proteger DNS do Google
                .addRoute("8.8.8.8", 32)
                .addRoute("8.8.4.4", 32)
                
                // 🔥 Proteger redes locais
                .addRoute("10.0.0.0", 8)
                .addRoute("172.16.0.0", 12)
                .addRoute("192.168.0.0", 16)
                
                // 🔥 Rota padrão (todo o resto vai pela VPN)
                .addRoute("0.0.0.0", 0)
                
                .addDnsServer("8.8.8.8")
                .addDnsServer("8.8.4.4")
                .setMtu(1500)
            
            vpnInterface = builder.establish()
            
            if (vpnInterface == null) {
                throw Exception("VPN establish falhou")
            }
            log("✅ VPN configurada (servidor protegido!)")
            
            log("[4/5] Iniciando encaminhamento...")
            val vpnInput = FileInputStream(vpnInterface!!.fileDescriptor)
            val vpnOutput = FileOutputStream(vpnInterface!!.fileDescriptor)
            val tlsIn = tlsSocket!!.inputStream
            val tlsOut = tlsSocket!!.outputStream
            
            thread {
                try {
                    val buffer = ByteArray(32768)
                    var len: Int
                    while (isRunning) {
                        len = vpnInput.read(buffer)
                        if (len > 0) {
                            tlsOut.write(buffer, 0, len)
                            tlsOut.flush()
                        }
                    }
                } catch (e: Exception) {
                    if (isRunning) log("📤 VPN->TLS: ${e.message}")
                }
            }
            
            thread {
                try {
                    val buffer = ByteArray(32768)
                    var len: Int
                    while (isRunning) {
                        len = tlsIn.read(buffer)
                        if (len > 0) {
                            vpnOutput.write(buffer, 0, len)
                            vpnOutput.flush()
                        }
                    }
                } catch (e: Exception) {
                    if (isRunning) log("📥 TLS->VPN: ${e.message}")
                }
            }
            
            log("[5/5] 🎉 VPN ATIVA!")
            log("📍 IP: 10.8.0.2 | Servidor: 168.138.147.212")
            
            updateNotification("XHTTP VPN", "Conectado")
            
        } catch (e: Exception) {
            log("❌ ${e.message}")
            stopVpn()
        }
    }
    
    private fun stopVpn() {
        isRunning = false
        try { tlsSocket?.close() } catch (e: Exception) {}
        try { vpnInterface?.close() } catch (e: Exception) {}
        stopForeground(true)
        stopSelf()
        log("⏹ VPN parada")
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "XHTTP VPN", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(text: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("XHTTP VPN")
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
        stopVpn()
        super.onDestroy()
    }
    
    class TrustAllCerts : X509TrustManager {
        override fun checkClientTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) {}
        override fun checkServerTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) {}
        override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> = arrayOf()
    }
}
