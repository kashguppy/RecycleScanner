package com.example.recyclescanner

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.recyclescanner.R
import com.google.common.util.concurrent.ListenableFuture
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var executor: ExecutorService
    private lateinit var previewView: PreviewView
    private lateinit var scanButton: Button
    private lateinit var scanResult: TextView
    private lateinit var disposalInstructions: TextView
    private lateinit var scannedItemsList: ListView

    // Views for home and scanner pages
    private lateinit var homePageView: View
    private lateinit var scannerPageView: View
    private lateinit var scannedItemsPageView: View

    private val scannedItems = ArrayList<String>()
    private val disposalMethods = ArrayList<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Get references to home and scanner page views
        homePageView = findViewById(R.id.homePageView)
        scannerPageView = findViewById(R.id.scannerPageView)
        scannedItemsPageView = findViewById(R.id.scannedItemsPageView)

        // Initialize scanned items list view
        scannedItemsList = findViewById(R.id.scannedItemsList)

        // Initially hide the scanner page and scanned items page
        scannerPageView.visibility = View.GONE
        scannedItemsPageView.visibility = View.GONE

        // Set up home page content
        val statisticsTextView = findViewById<TextView>(R.id.statisticsTextView)
        statisticsTextView.text = "Did you know that incorrectly recycling or trashing items can cause significant harm to the environment? In fact, it's estimated that over 8 million tons of plastic waste enter our oceans every year, harming marine life and contaminating the food chain. By using this app, you can help reduce waste and make a positive impact on the environment."

        // Set up scan button
        scanButton = findViewById(R.id.scanButton)
        scanButton.setOnClickListener {
            // Show scanner page and hide home page
            homePageView.visibility = View.GONE
            scannerPageView.visibility = View.VISIBLE
            scannedItemsPageView.visibility = View.GONE

            // Initialize scanner components
            previewView = findViewById(R.id.previewView)
            scanResult = findViewById(R.id.scanResult)
            disposalInstructions = findViewById(R.id.disposalInstructions)

            executor = Executors.newSingleThreadExecutor()

            // Request camera permission
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 1)
            }

            cameraProviderFuture = ProcessCameraProvider.getInstance(this)
            cameraProviderFuture.addListener({
                try {
                    val cameraProvider = cameraProviderFuture.get()

                    // Preview use case
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    // Bind use cases to lifecycle
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        this, CameraSelector.DEFAULT_BACK_CAMERA, preview
                    )

                } catch (e: Exception) {
                    Log.e("RecycleScanner", "Error initializing camera", e)
                }

            }, ContextCompat.getMainExecutor(this))
        }

        // Set up scanned items button
        val scannedItemsButton = findViewById<Button>(R.id.scannedItemsButton)
        scannedItemsButton.setOnClickListener {
            // Show scanned items page and hide scanner page
            scannerPageView.visibility = View.GONE
            scannedItemsPageView.visibility = View.VISIBLE
            homePageView.visibility = View.GONE

            // Populate the scanned items list view
            val scannedItemsAdapter = object : android.widget.ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, getUniqueScannedItems()) {}
            scannedItemsList.adapter = scannedItemsAdapter
        }

        // Set up home button
        val homeButton = findViewById<Button>(R.id.homeButton)
        homeButton.setOnClickListener {
            // Show home page and hide scanner page and scanned items page
            homePageView.visibility = View.VISIBLE
            scannerPageView.visibility = View.GONE
            scannedItemsPageView.visibility = View.GONE
        }

        // Set up home button 2
        val homeButton2 = findViewById<Button>(R.id.homeButton2)
        homeButton2.setOnClickListener {
            // Show home page and hide scanner page and scanned items page
            homePageView.visibility = View.VISIBLE
            scannerPageView.visibility = View.GONE
            scannedItemsPageView.visibility = View.GONE
        }

        // Set up scan now button
        val scanNowButton = findViewById<Button>(R.id.scanNowButton)
        scanNowButton.setOnClickListener {
            // Scan the barcode
            val bitmap = previewView.bitmap
            val inputImage = bitmap?.let { it1 -> InputImage.fromBitmap(it1, 0) }

            // Process the barcode
            val scanner = BarcodeScanning.getClient()
            if (inputImage != null) {
                scanner.process(inputImage)
                    .addOnSuccessListener { barcodes ->
                        for (barcode in barcodes) {
                            if (barcode.valueType == Barcode.TYPE_PRODUCT) {
                                val productCode = barcode.rawValue
                                Log.d("RecycleScanner", "Scanned product code: $productCode")
                                scanResult.text = "Scanned product code: $productCode"

                                // Determine disposal instructions
                                val disposalMethod = getDisposalMethod(productCode)
                                disposalInstructions.text = disposalMethod

                                // Add scanned item to the list
                                if (productCode != null && !scannedItems.contains(productCode)) {
                                    scannedItems.add(productCode)
                                    disposalMethods.add(disposalMethod)
                                }
                            }
                        }
                    }
                    .addOnFailureListener {
                        Log.e("RecycleScanner", "Failed to process barcode", it)
                    }
            }
        }
    }

    private fun getDisposalMethod(productCode: String?): String {
        // Randomly return "Recyclable" or "Non-Recyclable"
        return if ((0..1).random() == 0) "Recyclable" else "Non-Recyclable"
    }

    private fun getUniqueScannedItems(): ArrayList<String> {
        val uniqueScannedItems = ArrayList<String>()
        for (i in scannedItems.indices) {
            uniqueScannedItems.add("${scannedItems[i]} - ${disposalMethods[i]}")
        }
        return uniqueScannedItems
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("RecycleScanner", "Camera permission granted")
            } else {
                Log.d("RecycleScanner", "Camera permission denied")
            }
        }
    }
}
