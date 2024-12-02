//package com.merge.awadh.activity.scan.fragment
//
//import android.os.Bundle
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import androidx.fragment.app.Fragment
//import com.merge.awadh.databinding.FragmentGame1MainBinding
//
//class Game1MainFragment : Fragment() {
//    private lateinit var binding: FragmentGame1MainBinding
//    private lateinit var selectedDevices: List<String>
//    private val foundDevices = mutableSetOf<String>()
//
//    override fun onCreateView(
//        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
//    ): View? {
//        binding = FragmentGame1MainBinding.inflate(inflater, container, false)
//
//        arguments?.let {
//            selectedDevices = Game1MainFragmentArgs.fromBundle(it).selectedDevices.toList()
//        }
//
//        setupDeviceList()
//        return binding.root
//    }
//
//    private fun setupDeviceList() {
//        val adapter = GameDeviceAdapter(selectedDevices, foundDevices)
//        binding.deviceRecyclerView.adapter = adapter
//        binding.deviceRecyclerView.layoutManager = LinearLayoutManager(requireContext())
//    }
//}
