package com.merge.awadh

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button

class test3 : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test3)
        val button=findViewById<Button>(R.id.playButton)
        button.setOnClickListener {
            val Intent = Intent(this, test4::class.java)
            startActivity(Intent)
        }
    }
//    private fun setupRecyclerView() {
//        // Ensure the activity is an instance of ScanActivity and cast it
//        val activityContext = requireActivity() as? ScanActivity
//        activityContext?.let {
//            BLEManager.scanAdapter = ScanAdapter(BLEManager.scanResults, it) // Pass ScanActivity context
//        } ?: run {
//            // Handle the case where the activity is not of type ScanActivity
//            Log.e("AdvertisingDataFragment", "Activity is not of type ScanActivity.")
//        }
//
//        (binding as FragmentAdvertisingDataObjectFindingBinding).scanResultsRecyclerView.apply {
//            layoutManager = LinearLayoutManager(
//                requireContext(),
//                RecyclerView.VERTICAL,
//                false
//            )
//            adapter = BLEManager.scanAdapter
//            isNestedScrollingEnabled = false
//        }
//
//        // Turns Off Update Animation
//        val animator = (binding as FragmentAdvertisingDataObjectFindingBinding).scanResultsRecyclerView.itemAnimator
//        if (animator is SimpleItemAnimator) {
//            animator.supportsChangeAnimations = false
//        }
//    }
}
//package com.merge.awadh.activity.scan
//
//import android.os.Bundle
//import androidx.appcompat.app.AppCompatActivity
//import androidx.recyclerview.widget.LinearLayoutManager
//import com.merge.awadh.R
//import com.merge.awadh.databinding.ActivityTest3Binding
//
//
//class test3 : AppCompatActivity() {
//    private lateinit var binding: ActivityTest3Binding
//    private lateinit var selectedDevices: List<String>
//    private val foundDevices = mutableSetOf<String>()
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_test3)
//
//        // Inflate the layout using ViewBinding
//        binding = ActivityTest3Binding.inflate(layoutInflater)
//        setContentView(binding.root)
//
//        // Retrieve arguments passed to this Activity
//        intent?.let {
//            selectedDevices = it.getStringArrayListExtra("selectedDevices")?.toList() ?: emptyList()
//        }
//
//        // Setup the RecyclerView
//        setupDeviceList()
//    }
//
//    private fun setupDeviceList() {
////        val adapter = GameDeviceAdapter(selectedDevices, foundDevices)
////        binding.deviceRecyclerView.adapter = adapter
////        binding.deviceRecyclerView.layoutManager = LinearLayoutManager(this)
//    }
//}
