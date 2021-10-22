package com.example.internetspeedtestapp.test

import java.lang.Exception
import java.math.BigDecimal
import java.math.RoundingMode
import java.net.URL
import java.util.concurrent.Executors


class HttpUploadTest(fileURL: String) : Thread() {
    private var fileURL = ""
    private var uploadElapsedTime = 0.0
    var isFinished = false
    private var elapsedTime = 0.0
    var finalUploadRate = 0.0
    private var startTime: Long = 0
    private fun round(value: Double, places: Int): Double {
        require(value = places >= 0)
        var bd: BigDecimal = try {
            BigDecimal(value)
        } catch (ex: Exception) {
            return 0.0
        }
        bd = bd.setScale(places, RoundingMode.HALF_UP)
        return bd.toDouble()
    }

    val instantUploadRate: Double
        get() {
            try {
                BigDecimal(uploadedKByte)
            } catch (ex: Exception) {
                return 0.0
            }
            return if (uploadedKByte >= 0) {
                val now = System.currentTimeMillis()
                elapsedTime = (now - startTime) / 1000.0
                round((uploadedKByte / 1000.0 * 8 / elapsedTime), 2)
            } else {
                0.0
            }
        }

    override fun run() {
        try {
            val url = URL(fileURL)
            uploadedKByte = 0
            startTime = System.currentTimeMillis()
            val executor = Executors.newFixedThreadPool(4)
            for (i in 0..3) {
                executor.execute(HandlerUpload(url))
            }
            executor.shutdown()
            while (!executor.isTerminated) {
                try {
                    sleep(100)
                } catch (ex: InterruptedException) {
                }
            }
            val now = System.currentTimeMillis()
            uploadElapsedTime = (now - startTime) / 1000.0
            finalUploadRate = (uploadedKByte / 1000.0 * 8 / uploadElapsedTime)
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        isFinished = true
    }

    companion object {
        var uploadedKByte = 0
    }

    init {
        this.fileURL = fileURL
    }
}