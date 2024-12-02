package com.merge.awadh.activity.scan.fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.merge.awadh.activity.scan.ScanActivity
import com.merge.awadh.databinding.FragmentDeviceSelectionBinding
import com.merge.awadh.activity.scan.ScanAdapter
import com.merge.awadh.ble.BLEManager
import com.merge.awadh.databinding.FragmentAdvertisingDataObjectFindingBinding

class DeviceSelectionFragment : Fragment() {
    private lateinit var binding: FragmentDeviceSelectionBinding

//    override fun onCreateView(
//        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
//    ): View? {
//        binding = FragmentDeviceSelectionBinding.inflate(inflater, container, false)
//
//        val devices = listOf("Device 1", "Device 2", "Device 3")
//        val adapter = BLEManager.scanAdapter(devices) { selectedDevices ->
//            binding.startGameButton.isEnabled = selectedDevices.isNotEmpty()
//        }
//        setupRecyclerView()
//
//        binding.startGameButton.setOnClickListener {
//            val selectedDevices = adapter.getSelectedDevices()
//            val action = DeviceSelectionFragmentDirections
//                .actionDeviceSelectionToGame1Main(selectedDevices.toTypedArray())
//            findNavController().navigate(action)
//        }
//
//        return binding.root
//    }
//
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
