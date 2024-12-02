package com.merge.awadh.activity.scan.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.*
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import com.merge.awadh.R
import com.merge.awadh.ble.BLEManager
import com.merge.awadh.databinding.FragmentRssiFilterBinding
import com.google.android.material.slider.Slider
import com.merge.awadh.activity.scan.DropdownSelectionListener

class RSSIFilterFragment: DialogFragment() {
    private lateinit var binding: FragmentRssiFilterBinding
    private val dropdownListeners = mutableListOf<DropdownSelectionListener>()


    fun registerDropdownListener(listener: DropdownSelectionListener) {
        if (!dropdownListeners.contains(listener)) {
            dropdownListeners.add(listener)
        }
    }

    fun unregisterDropdownListener(listener: DropdownSelectionListener) {
        dropdownListeners.remove(listener)
    }

    private fun notifyDropdownItemSelected(item: String) {
        for (listener in dropdownListeners) {
            listener.onDropdownItemSelected(item)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_rssi_filter,
            container,
            false
        )

        with(binding) {
            // Set Slider Value if previously changed
            if (BLEManager.deviceRSSIFilter.isNotEmpty()) {
                rssiSlider.value = BLEManager.deviceRSSIFilter.toFloat() * -1
            }

            // Change Slider Label (Negative & Int)
            rssiSlider.setLabelFormatter { value: Float ->
                var newValue = value.toInt()

                if (value != 0f) {
                    newValue *= -1
                }

                "$newValue dBm"
            }

            // RSSI Slider Thumb Moved
            rssiSlider.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
                @SuppressLint("RestrictedApi")
                override fun onStartTrackingTouch(slider: Slider) {
                    // Responds to when slider's touch event is being started
                }

                @SuppressLint("RestrictedApi")
                override fun onStopTrackingTouch(slider: Slider) {
                    // Responds to when slider's touch event is being stopped
                    val newValue = slider.value.toInt() * -1

                    if (newValue == -125) {
                        BLEManager.deviceRSSIFilter = ""
                    } else {
                        BLEManager.scanAdapter?.filter(newValue.toString(), "rssi")
                    }
                }
            })
        }
        setupSpinner()
        return binding.root
    }

    private fun setupSpinner() {
        if (!::binding.isInitialized) {
            throw IllegalStateException("Binding is not initialized yet")
        }

        val options = listOf("SHT40", "LIS2DH", "WindSpeed","StepCount", "Speed Distance", "Object Finding")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, options)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.byteSpinner.adapter = adapter

        // Set "Temperature-Humidity" as the default selected item
        val defaultPosition = options.indexOf("SHT40")
        if (defaultPosition != -1) {
            binding.byteSpinner.setSelection(defaultPosition)
        }

        binding.byteSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedOption = options[position]
                Toast.makeText(requireContext(), "Selected: $selectedOption", Toast.LENGTH_SHORT).show()
                lastSelectedOption = selectedOption
                onDropdownItemSelected(selectedOption)
            }


            override fun onNothingSelected(parent: AdapterView<*>) {
                // No action needed here
            }
        }
    }


    private fun onDropdownItemSelected(item: String) {
        // Logic to update the RecyclerView based on the selection
        when (item) {
            "SHT40" -> {
                // Update the list for Temperature-Humidity
            }
            "LIS2DH" -> {
                // Update the list for Accelerometer
            }
            "WindSpeed" -> {
                // Update the list for Accelerometer
            }
            "StepCount" -> {
                // Update the list for Accelerometer
            }
            "Speed Distance" ->{
                // Update the list for Accelerometer
            }
            "Object Finding" ->{
//                filterDevicesForObjectFinding()

            }
        }
        notifyDropdownItemSelected(item)
    }
    fun getCurrentDropdownSelection(): String {
        return binding.byteSpinner.selectedItem.toString()
    }

    override fun onResume() {
        super.onResume()

        // Change Dialog Window Position
        val window: Window? = dialog?.window
        val params: WindowManager.LayoutParams? = window?.attributes
        window?.setGravity(Gravity.TOP)
        params?.y = 100

        // Set Fragment Dimensions
        val width = WindowManager.LayoutParams.MATCH_PARENT
        val height = WindowManager.LayoutParams.WRAP_CONTENT
        window?.setLayout(width, height)
        if (::binding.isInitialized) {
            setupSpinner()
        }
    }

    companion object {
        var lastSelectedOption: String = "SHT40"
    }

}