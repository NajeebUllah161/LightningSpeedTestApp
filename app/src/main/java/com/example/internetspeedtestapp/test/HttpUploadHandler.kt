package com.example.internetspeedtestapp.test

import java.io.DataOutputStream
import java.lang.Exception
import java.net.URL
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLSocketFactory


internal class HandlerUpload(private var url: URL) : Thread() {
    override fun run() {
        val buffer = ByteArray(150 * 1024)
        val startTime = System.currentTimeMillis()
        val timeout = 8
        while (true) {
            try {
                var conn: HttpsURLConnection?
                conn = url.openConnection() as HttpsURLConnection
                conn.doOutput = true
                conn.requestMethod = "POST"
                conn.setRequestProperty("Connection", "Keep-Alive")
                conn.sslSocketFactory = SSLSocketFactory.getDefault() as SSLSocketFactory
                conn.hostnameVerifier = HostnameVerifier { hostname, session -> true }
                conn.connect()
                val dos = DataOutputStream(conn.outputStream)
                dos.write(buffer, 0, buffer.size)
                dos.flush()
                conn.responseCode
                HttpUploadTest.uploadedKByte += (buffer.size / 1024.0).toInt()
                val endTime = System.currentTimeMillis()
                val uploadElapsedTime = (endTime - startTime) / 1000.0
                if (uploadElapsedTime >= timeout) {
                    break
                }
                dos.close()
                conn.disconnect()
            } catch (ex: Exception) {
                ex.printStackTrace()
                break
            }
        }
    }
}
