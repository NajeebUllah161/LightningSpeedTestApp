package com.example.internetspeedtestapp

import androidx.appcompat.app.AppCompatActivity
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.view.animation.RotateAnimation
import org.achartengine.renderer.XYSeriesRenderer
import org.achartengine.renderer.XYSeriesRenderer.FillOutsideLine
import org.achartengine.renderer.XYMultipleSeriesRenderer
import com.example.internetspeedtestapp.test.PingTest
import com.example.internetspeedtestapp.test.HttpDownloadTest
import com.example.internetspeedtestapp.test.HttpUploadTest
import org.achartengine.model.XYSeries
import org.achartengine.model.XYMultipleSeriesDataset
import org.achartengine.ChartFactory
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.widget.*
import java.text.DecimalFormat
import java.util.ArrayList
import java.util.HashSet

class MainActivity : AppCompatActivity() {
    var getSpeedTestHostsHandler: GetSpeedTestHostsHandler? = null
    var tempBlackList: HashSet<String>? = null
    public override fun onResume() {
        super.onResume()
        getSpeedTestHostsHandler = GetSpeedTestHostsHandler()
        getSpeedTestHostsHandler!!.start()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val startButton = findViewById<Button>(R.id.startButton)
        val dec = DecimalFormat("#.##")
        startButton.text = getString(R.string.begin_text)
        tempBlackList = HashSet()
        getSpeedTestHostsHandler = GetSpeedTestHostsHandler()
        getSpeedTestHostsHandler!!.start()
        startButton.setOnClickListener {
            startButton.isEnabled = false

            //Restart test
            if (getSpeedTestHostsHandler == null) {
                getSpeedTestHostsHandler = GetSpeedTestHostsHandler()
                getSpeedTestHostsHandler!!.start()
            }
            Thread(object : Runnable {
                var rotate: RotateAnimation? = null
                var barImageView = findViewById<ImageView>(R.id.barImageView)
                var pingTextView = findViewById<TextView>(R.id.pingTextView)
                var downloadTextView = findViewById<TextView>(R.id.downloadTextView)
                var uploadTextView = findViewById<TextView>(R.id.uploadTextView)
                override fun run() {
                    runOnUiThread { startButton.text = getString(R.string.ping_waiting_text) }

                    //Get hosts
                    var timeCount = 600 //1min
                    while (!getSpeedTestHostsHandler!!.isFinished) {
                        timeCount--
                        try {
                            Thread.sleep(100)
                        } catch (e: InterruptedException) {
                        }
                        if (timeCount <= 0) {
                            runOnUiThread {
                                Toast.makeText(
                                    applicationContext,
                                    "No Connection...",
                                    Toast.LENGTH_LONG
                                ).show()
                                startButton.isEnabled = true
                                startButton.textSize = 16f
                                startButton.text = getString(R.string.restart_btn_txt)
                            }
                            getSpeedTestHostsHandler = null
                            return
                        }
                    }

                    //Find closest server
                    val mapKey = getSpeedTestHostsHandler!!.mapKey
                    val mapValue = getSpeedTestHostsHandler!!.mapValue
                    val selfLat = getSpeedTestHostsHandler!!.selfLat
                    val selfLon = getSpeedTestHostsHandler!!.selfLon
                    var tmp = 19349458.0
                    var dist = 0.0
                    var findServerIndex = 0
                    for (index in mapKey.keys) {
                        if (tempBlackList!!.contains(mapValue[index]!![5])) {
                            continue
                        }
                        val source = Location("Source")
                        source.latitude = selfLat
                        source.longitude = selfLon
                        val ls = mapValue[index]!!
                        val dest = Location("Dest")
                        dest.latitude = ls[0].toDouble()
                        dest.longitude = ls[1].toDouble()
                        val distance = source.distanceTo(dest).toDouble()
                        if (tmp > distance) {
                            tmp = distance
                            dist = distance
                            findServerIndex = index
                        }
                    }
                    val testAddr = mapKey[findServerIndex]!!
                        .replace("http://", "https://")
                    val info = mapValue[findServerIndex]
                    val distance = dist
                    if (info == null) {
                        runOnUiThread {
                            startButton.textSize = 12f
                            startButton.text =
                                getString(R.string.host_err_try_again_txt)
                        }
                        return
                    }
                    runOnUiThread {
                        startButton.textSize = 13f
                        startButton.text = String.format(
                            "Host Location: %s [Distance: %s km]",
                            info[2],
                            DecimalFormat("#.##").format(distance / 1000)
                        )
                    }

                    //Init Ping graphic
                    val chartPing = findViewById<LinearLayout>(R.id.chartPing)
                    val pingRenderer = XYSeriesRenderer()
                    val pingFill = FillOutsideLine(FillOutsideLine.Type.BOUNDS_ALL)
                    pingFill.color = Color.parseColor("#4d5a6a")
                    pingRenderer.addFillOutsideLine(pingFill)
                    pingRenderer.isDisplayChartValues = false
                    pingRenderer.isShowLegendItem = false
                    pingRenderer.color = Color.parseColor("#4d5a6a")
                    pingRenderer.lineWidth = 5f
                    val multiPingRenderer = XYMultipleSeriesRenderer()
                    multiPingRenderer.xLabels = 0
                    multiPingRenderer.yLabels = 0
                    multiPingRenderer.isZoomEnabled = false
                    multiPingRenderer.xAxisColor = Color.parseColor("#647488")
                    multiPingRenderer.yAxisColor = Color.parseColor("#2F3C4C")
                    multiPingRenderer.setPanEnabled(true, true)
                    multiPingRenderer.isZoomButtonsVisible = false
                    multiPingRenderer.marginsColor = Color.argb(0x00, 0xff, 0x00, 0x00)
                    multiPingRenderer.addSeriesRenderer(pingRenderer)

                    //Init Download graphic
                    val chartDownload = findViewById<LinearLayout>(R.id.chartDownload)
                    val downloadRenderer = XYSeriesRenderer()
                    val downloadFill = FillOutsideLine(FillOutsideLine.Type.BOUNDS_ALL)
                    downloadFill.color = Color.parseColor("#4d5a6a")
                    downloadRenderer.addFillOutsideLine(downloadFill)
                    downloadRenderer.isDisplayChartValues = false
                    downloadRenderer.color = Color.parseColor("#4d5a6a")
                    downloadRenderer.isShowLegendItem = false
                    downloadRenderer.lineWidth = 5f
                    val multiDownloadRenderer = XYMultipleSeriesRenderer()
                    multiDownloadRenderer.xLabels = 0
                    multiDownloadRenderer.yLabels = 0
                    multiDownloadRenderer.isZoomEnabled = false
                    multiDownloadRenderer.xAxisColor = Color.parseColor("#647488")
                    multiDownloadRenderer.yAxisColor = Color.parseColor("#2F3C4C")
                    multiDownloadRenderer.setPanEnabled(false, false)
                    multiDownloadRenderer.isZoomButtonsVisible = false
                    multiDownloadRenderer.marginsColor = Color.argb(0x00, 0xff, 0x00, 0x00)
                    multiDownloadRenderer.addSeriesRenderer(downloadRenderer)

                    //Init Upload graphic
                    val chartUpload = findViewById<LinearLayout>(R.id.chartUpload)
                    val uploadRenderer = XYSeriesRenderer()
                    val uploadFill = FillOutsideLine(FillOutsideLine.Type.BOUNDS_ALL)
                    uploadFill.color = Color.parseColor("#4d5a6a")
                    uploadRenderer.addFillOutsideLine(uploadFill)
                    uploadRenderer.isDisplayChartValues = false
                    uploadRenderer.color = Color.parseColor("#4d5a6a")
                    uploadRenderer.isShowLegendItem = false
                    uploadRenderer.lineWidth = 5f
                    val multiUploadRenderer = XYMultipleSeriesRenderer()
                    multiUploadRenderer.xLabels = 0
                    multiUploadRenderer.yLabels = 0
                    multiUploadRenderer.isZoomEnabled = false
                    multiUploadRenderer.xAxisColor = Color.parseColor("#647488")
                    multiUploadRenderer.yAxisColor = Color.parseColor("#2F3C4C")
                    multiUploadRenderer.setPanEnabled(false, false)
                    multiUploadRenderer.isZoomButtonsVisible = false
                    multiUploadRenderer.marginsColor = Color.argb(0x00, 0xff, 0x00, 0x00)
                    multiUploadRenderer.addSeriesRenderer(uploadRenderer)

                    //Reset value, graphics
                    runOnUiThread {
                        pingTextView.text = getString(R.string.ping_init_val)
                        chartPing.removeAllViews()
                        downloadTextView.text = getString(R.string.download_init_val)
                        chartDownload.removeAllViews()
                        uploadTextView.text = getString(R.string.upload_init_val)
                        chartUpload.removeAllViews()
                    }
                    val pingRateList: MutableList<Double> = ArrayList()
                    val downloadRateList: MutableList<Double> = ArrayList()
                    val uploadRateList: MutableList<Double> = ArrayList()
                    var pingTestStarted = false
                    var pingTestFinished = false
                    var downloadTestStarted = false
                    var downloadTestFinished = false
                    var uploadTestStarted = false
                    var uploadTestFinished = false

                    //Init Test
                    val pingTest = PingTest(info[6].replace(":8080", ""), 3)
                    val downloadTest = HttpDownloadTest(
                        testAddr.replace(
                            testAddr.split("/").toTypedArray()[testAddr.split("/")
                                .toTypedArray().size - 1], ""
                        )
                    )
                    val uploadTest = HttpUploadTest(testAddr)


                    //Tests
                    while (true) {
                        if (!pingTestStarted) {
                            pingTest.start()
                            pingTestStarted = true
                        }
                        if (pingTestFinished && !downloadTestStarted) {
                            downloadTest.start()
                            downloadTestStarted = true
                        }
                        if (downloadTestFinished && !uploadTestStarted) {
                            uploadTest.start()
                            uploadTestStarted = true
                        }


                        //Ping Test
                        if (pingTestFinished) {
                            //Failure
                            if (pingTest.avgRtt == 0.0) {
                                println("Ping error...")
                            } else {
                                //Success
                                runOnUiThread {
                                    pingTextView.text = dec.format(pingTest.avgRtt) + " ms"
                                }
                            }
                        } else {
                            pingRateList.add(pingTest.instantRtt)
                            runOnUiThread {
                                pingTextView.text = dec.format(pingTest.instantRtt) + " ms"
                            }

                            //Update chart
                            runOnUiThread { // Creating an  XYSeries for Income
                                val pingSeries = XYSeries("")
                                pingSeries.title = ""
                                var count = 0
                                val tmpLs: List<Double> = ArrayList(pingRateList)
                                for (`val` in tmpLs) {
                                    pingSeries.add(count++.toDouble(), `val`)
                                }
                                val dataset = XYMultipleSeriesDataset()
                                dataset.addSeries(pingSeries)
                                val chartView = ChartFactory.getLineChartView(
                                    baseContext,
                                    dataset,
                                    multiPingRenderer
                                )
                                chartPing.addView(chartView, 0)
                            }
                        }


                        //Download Test
                        if (pingTestFinished) {
                            if (downloadTestFinished) {
                                //Failure
                                if (downloadTest.finalDownloadRate == 0.0) {
                                    println("Download error...")
                                } else {
                                    //Success
                                    runOnUiThread {
                                        downloadTextView.text =
                                            dec.format(downloadTest.finalDownloadRate) + " Mbps"
                                    }
                                }
                            } else {
                                //Calc position
                                val downloadRate = downloadTest.instantDownloadRate
                                downloadRateList.add(downloadRate)
                                position = getPositionByRate(downloadRate)
                                runOnUiThread {
                                    rotate = RotateAnimation(
                                        lastPosition.toFloat(),
                                        position.toFloat(),
                                        Animation.RELATIVE_TO_SELF,
                                        0.5f,
                                        Animation.RELATIVE_TO_SELF,
                                        0.5f
                                    )
                                    rotate!!.interpolator = LinearInterpolator()
                                    rotate!!.duration = 100
                                    barImageView.startAnimation(rotate)
                                    downloadTextView.text =
                                        dec.format(downloadTest.instantDownloadRate) + " Mbps"
                                }
                                lastPosition = position

                                //Update chart
                                runOnUiThread { // Creating an  XYSeries for Income
                                    val downloadSeries = XYSeries("")
                                    downloadSeries.title = ""
                                    val tmpLs: List<Double> = ArrayList(downloadRateList)
                                    var count = 0
                                    for (`val` in tmpLs) {
                                        downloadSeries.add(count++.toDouble(), `val`)
                                    }
                                    val dataset = XYMultipleSeriesDataset()
                                    dataset.addSeries(downloadSeries)
                                    val chartView = ChartFactory.getLineChartView(
                                        baseContext, dataset, multiDownloadRenderer
                                    )
                                    chartDownload.addView(chartView, 0)
                                }
                            }
                        }


                        //Upload Test
                        if (downloadTestFinished) {
                            if (uploadTestFinished) {
                                //Failure
                                if (uploadTest.finalUploadRate == 0.0) {
                                    println("Upload error...")
                                } else {
                                    //Success
                                    runOnUiThread {
                                        uploadTextView.text =
                                            dec.format(uploadTest.finalUploadRate) + " Mbps"
                                    }
                                }
                            } else {
                                //Calc position
                                val uploadRate = uploadTest.instantUploadRate
                                uploadRateList.add(uploadRate)
                                position = getPositionByRate(uploadRate)
                                runOnUiThread {
                                    rotate = RotateAnimation(
                                        lastPosition.toFloat(),
                                        position.toFloat(),
                                        Animation.RELATIVE_TO_SELF,
                                        0.5f,
                                        Animation.RELATIVE_TO_SELF,
                                        0.5f
                                    )
                                    rotate!!.interpolator = LinearInterpolator()
                                    rotate!!.duration = 100
                                    barImageView.startAnimation(rotate)
                                    uploadTextView.text =
                                        dec.format(uploadTest.instantUploadRate) + " Mbps"
                                }
                                lastPosition = position

                                //Update chart
                                runOnUiThread { // Creating an  XYSeries for Income
                                    val uploadSeries = XYSeries("")
                                    uploadSeries.title = ""
                                    var count = 0
                                    val tmpLs: List<Double> = ArrayList(uploadRateList)
                                    for (`val` in tmpLs) {
                                        if (count == 0) {

                                            uploadSeries.add(count++.toDouble(), 0.0)
                                            continue
                                        }
                                        uploadSeries.add(count++.toDouble(), `val`)
                                    }
                                    val dataset = XYMultipleSeriesDataset()
                                    dataset.addSeries(uploadSeries)
                                    val chartView = ChartFactory.getLineChartView(
                                        baseContext, dataset, multiUploadRenderer
                                    )
                                    chartUpload.addView(chartView, 0)
                                }
                            }
                        }

                        //Test afterwards check
                        if (pingTestFinished && downloadTestFinished && uploadTest.isFinished) {
                            break
                        }
                        if (pingTest.isFinished) {
                            pingTestFinished = true
                        }
                        if (downloadTest.isFinished) {
                            downloadTestFinished = true
                        }
                        if (uploadTest.isFinished) {
                            uploadTestFinished = true
                        }
                        if (pingTestStarted && !pingTestFinished) {
                            try {
                                Thread.sleep(300)
                            } catch (e: InterruptedException) {
                            }
                        } else {
                            try {
                                Thread.sleep(100)
                            } catch (e: InterruptedException) {
                            }
                        }
                    }

                    //Thread button property set
                    runOnUiThread {
                        startButton.isEnabled = true
                        startButton.textSize = 16f
                        startButton.text = getString(R.string.restart_btn_txt)
                    }
                }
            }).start()
        }
    }

    fun getPositionByRate(rate: Double): Int {
        when {
            rate <= 1 -> {
                return (rate * 30).toInt()
            }
            rate <= 10 -> {
                return (rate * 6).toInt() + 30
            }
            rate <= 30 -> {
                return ((rate - 10) * 3).toInt() + 90
            }
            rate <= 50 -> {
                return ((rate - 30) * 1.5).toInt() + 150
            }
            rate <= 100 -> {
                return ((rate - 50) * 1.2).toInt() + 180
            }
            else -> return 0
        }
    }

    companion object {
        var position = 0
        var lastPosition = 0
    }
}