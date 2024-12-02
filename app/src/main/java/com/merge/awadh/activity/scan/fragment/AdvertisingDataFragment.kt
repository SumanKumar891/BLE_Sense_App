package com.merge.awadh.activity.scan.fragment

import android.Manifest
import android.bluetooth.le.ScanResult
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.content.Intent
import android.graphics.Color
import java.util.LinkedList

import android.view.*
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import com.merge.awadh.R
import com.merge.awadh.activity.plot.PlotActivityAcc
import com.merge.awadh.activity.plot.PlotActivitySHT
import com.merge.awadh.activity.scan.DropdownSelectionListener
//import com.merge.awadh.activity.scan.ScanActivity
import com.merge.awadh.ble.BLEManager.registerScanResultListener
import com.merge.awadh.ble.BLEManager.unregisterScanResultListener
import com.merge.awadh.ble.ScanResultListener
import com.merge.awadh.databinding.FragmentAdvertismentDataWindspeedBinding
import com.merge.awadh.databinding.FragmentAdvertisingDataAccBinding
import com.merge.awadh.databinding.FragmentAdvertisingDataShtBinding
import com.merge.awadh.databinding.FragmentAdvertisingDataStepcountBinding
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import com.merge.awadh.databinding.FragmentAdvertisingDataSdtBinding

import android.view.View
import androidx.core.content.ContextCompat

import com.google.android.filament.utils.*
import com.merge.awadh.databinding.FragmentAdvertisingDataObjectFindingBinding

import java.io.File
import android.animation.ObjectAnimator
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.util.Log
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import android.widget.TextView
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController

import com.merge.awadh.ble.BLEManager


class AdvertisingDataFragment: DialogFragment(), ScanResultListener, DropdownSelectionListener {

    private var deviceAddress: String? = "not find"
    private lateinit var mediaPlayer: MediaPlayer
    private var lastUpdateTime: Long = 0  // Variable to store the timestamp of the last update
    private lateinit var emojiContainer: FrameLayout
    private lateinit var navController: NavController

    companion object {
        private const val ARG_DEVICE_ADDRESS = "device_address"
        private const val MAX_QUEUE_SIZE = 30
        private const val PERMISSION_REQUEST_BLUETOOTH_CONNECT = 1
        private const val BATCH_SIZE = 1000  // For batch processing
        init {
            Utils.init()
        }
        fun newInstance(deviceAddress: String): AdvertisingDataFragment {
            val fragment = AdvertisingDataFragment()
            val args = Bundle()
            args.putString(ARG_DEVICE_ADDRESS, deviceAddress)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        deviceAddress = arguments?.getString(ARG_DEVICE_ADDRESS)
        (requireActivity() as? RSSIFilterFragment)?.registerDropdownListener(this)
        dropdownitem = getCurrentDropdownSelection()
    }

    private lateinit var binding: Any
    private var dropdownitem: String? = "SHT40"

    // For long-term storage
    private var temp_Data = mutableListOf<Float>()
    private var humid_Data = mutableListOf<Float>()
    private var x_Data = mutableListOf<Float>()
    private var y_Data = mutableListOf<Float>()
    private var z_Data = mutableListOf<Float>()
    private var speed_Data = mutableListOf<Float>()
    private var stepCount_reset = 0
    private var lastStep = 0
    private var lastSpeedx= 0.0
    private var lastdisx = 0.0
    private var speedx_reset=0.0
    private var disx_reset =0.0
    private var speedx_Data = mutableListOf<Float>()
    private var disx_Data = mutableListOf<Float>()

    // For temporary storage and quick access
    private var tempData = LinkedList<Float>()
    private var humidData = LinkedList<Float>()
    private var xData = LinkedList<Float>()
    private var yData = LinkedList<Float>()
    private var zData = LinkedList<Float>()
    private var speedData = LinkedList<Float>()
    private var speedxData = LinkedList<Float>()

    private var disxData = LinkedList<Float>()
    private val deviceSequence = listOf("Unnamed", "mickey mouse", "DeviceD") // Define device sequence
    private val foundDevices = mutableSetOf<String>()

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {




        return when (dropdownitem) {
            "SHT40" -> {
                binding = DataBindingUtil.inflate<FragmentAdvertisingDataShtBinding>(
                    inflater,
                    R.layout.fragment_advertising_data_sht,
                    container,
                    false
                )
                (binding as FragmentAdvertisingDataShtBinding).apply {
                    openPlotActivityButton.setOnClickListener { openPlotActivity() }
                    okButton.setOnClickListener { dismiss() }
                    downloadButton.setOnClickListener { downloadDataAsExcel() }
                }
                (binding as FragmentAdvertisingDataShtBinding).root
            }
            "LIS2DH" -> {
                binding = DataBindingUtil.inflate<FragmentAdvertisingDataAccBinding>(
                    inflater,
                    R.layout.fragment_advertising_data_acc,
                    container,
                    false
                )
                (binding as FragmentAdvertisingDataAccBinding).apply {
                    openPlotActivityButton.setOnClickListener { openPlotActivity() }
                    okButton.setOnClickListener { dismiss() }
                    downloadButton.setOnClickListener { downloadDataAsExcel() }
                }
                (binding as FragmentAdvertisingDataAccBinding).root
            }
            "WindSpeed" -> {
                binding = DataBindingUtil.inflate<FragmentAdvertismentDataWindspeedBinding>(
                    inflater,
                    R.layout.fragment_advertisment_data_windspeed,
                    container,
                    false
                )
                (binding as FragmentAdvertismentDataWindspeedBinding).apply {
                    val renderer = ModelRenderer()
                    surfaceView.apply {
                        renderer.onSurfaceAvailable(this, lifecycle)
                    }
                    okButton.setOnClickListener {
                        renderer.cleanup()
                        dismiss()
                    }
                    downloadButton.setOnClickListener { downloadDataAsExcel() }
                }
                (binding as FragmentAdvertismentDataWindspeedBinding).root
            }

            "StepCount" -> {
                setScanActivityBackgroundColor(ContextCompat.getColor(requireContext(), R.color.grey_background))
                binding = DataBindingUtil.inflate<FragmentAdvertisingDataStepcountBinding>(
                    inflater,
                    R.layout.fragment_advertising_data_stepcount,
                    container,
                    false
                )
                (binding as FragmentAdvertisingDataStepcountBinding).apply {

                    okButton.setOnClickListener { dismiss() }
                    resetButton.setOnClickListener { resetStepCount() }
                }
                (binding as FragmentAdvertisingDataStepcountBinding).root
            }
            "Speed Distance" ->{
                binding = DataBindingUtil.inflate<FragmentAdvertisingDataSdtBinding>(
                    inflater,
                    R.layout.fragment_advertising_data_sdt,
                    container,
                    false
                )
                (binding as FragmentAdvertisingDataSdtBinding).apply {
                    okButton.setOnClickListener {
                        dismiss()
                    }
                    resetButton.setOnClickListener{ resetSpeed() }
                    downloadButton.setOnClickListener { downloadDataAsExcel() }
                }
                (binding as FragmentAdvertisingDataSdtBinding).root
            }
            "Object Finding" -> {
                binding = DataBindingUtil.inflate<FragmentAdvertisingDataObjectFindingBinding>(
                    inflater,
                    R.layout.fragment_advertising_data_object_finding,
                    container,
                    false
                )

                (binding as FragmentAdvertisingDataObjectFindingBinding).apply {
                    // Handle "OK" button click
//                    okButton.setOnClickListener {
//                        dismiss()
//                    }
                    game1Button.setOnClickListener {
                        findNavController().navigate(R.id.action_advertisingDataFragment_to_game1OpeningPageFragment)
                    }
                }

                mediaPlayer = MediaPlayer.create(requireContext(), R.raw.found_sound)
                (binding as FragmentAdvertisingDataObjectFindingBinding).root
            }
            else -> throw IllegalArgumentException("Unsupported dropdown item: $dropdownitem")
        }
    }
    private fun filterDevicesForObjectFinding(): List<ScanResult> {
        // List of target device names
        val targetNames = listOf("Unnamed","tom"," jerry", "mickey mouse")

        // Filter scan results
        return BLEManager.scanResults.filter { result ->
            ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED && result.device.name?.let { deviceName ->
                targetNames.any { it.equals(deviceName, ignoreCase = true) }
            } ?: false
        }
    }

    private fun setScanActivityBackgroundColor(color: Int) {
        activity?.findViewById<View>(android.R.id.content)?.setBackgroundColor(color)
    }
    private fun getCurrentDropdownSelection(): String {
        return RSSIFilterFragment.lastSelectedOption // Default to SHT40 if null
    }
    private fun resetStepCount(){
        stepCount_reset = lastStep
    }
    private fun resetSpeed(){
        speedx_reset = lastSpeedx
        disx_reset = lastdisx
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        registerScanResultListener(this)
        (activity as? RSSIFilterFragment)?.registerDropdownListener(this)
    }

    override fun onDropdownItemSelected(item: String) {
        dropdownitem = item
        view?.let {
            (it.parent as? ViewGroup)?.removeView(it)
            onCreateView(LayoutInflater.from(context), it.parent as ViewGroup, null)
            onViewCreated(it, null)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        temp_Data.clear()
        temp_Data = mutableListOf()

        humid_Data.clear()
        humid_Data = mutableListOf()

        x_Data.clear()
        x_Data = mutableListOf()

        y_Data.clear()
        y_Data = mutableListOf()

        z_Data.clear()
        z_Data = mutableListOf()

        speed_Data.clear()
        speed_Data = mutableListOf()

        speedx_Data.clear()
        speedx_Data = mutableListOf()

        disx_Data.clear()
        disx_Data = mutableListOf()

        tempData.clear()
        tempData = LinkedList()

        humidData.clear()
        humidData = LinkedList()

        xData.clear()
        xData = LinkedList()

        yData.clear()
        yData = LinkedList()

        zData.clear()
        zData = LinkedList()

        speedData.clear()
        speedData = LinkedList()

        speedxData.clear()
        speedxData = LinkedList()

        disxData.clear()
        disxData = LinkedList()

        setScanActivityBackgroundColor(ContextCompat.getColor(requireContext(), R.color.transparent))

        unregisterScanResultListener(this)
        (activity as? RSSIFilterFragment)?.unregisterDropdownListener(this)
    }

    private fun openPlotActivity() {
        if (dropdownitem == "SHT40") {
            val intent = Intent(requireContext(), PlotActivitySHT::class.java)
            intent.putExtra("TEMP_DATA", tempData.toFloatArray())
            intent.putExtra("HUMID_DATA", humidData.toFloatArray())
            intent.putExtra("DEVICE_ADDRESS", deviceAddress)
            startActivity(intent)
        } else if (dropdownitem == "LIS2DH") {
            val intent = Intent(requireContext(), PlotActivityAcc::class.java)
            intent.putExtra("X_DATA", xData.toFloatArray())
            intent.putExtra("Z_DATA", zData.toFloatArray())
            intent.putExtra("Y_DATA", yData.toFloatArray())
            intent.putExtra("DEVICE_ADDRESS", deviceAddress)
            startActivity(intent)
        }
        else if (dropdownitem == "WindSpeed") {
            val intent = Intent(requireContext(), PlotActivityAcc::class.java)
            intent.putExtra("X_DATA", xData.toFloatArray())
            intent.putExtra("Z_DATA", zData.toFloatArray())
            intent.putExtra("Y_DATA", yData.toFloatArray())
            intent.putExtra("DEVICE_ADDRESS", deviceAddress)
            startActivity(intent)
        }
    }

    override fun onScanResultUpdated(result: ScanResult) {
            val currentTime = System.currentTimeMillis()
            val timeDifference = if (lastUpdateTime == 0L) 0 else currentTime - lastUpdateTime
            lastUpdateTime = currentTime
            when (dropdownitem) {
                "SHT40" -> updateUISHT40(result, timeDifference)
                "LIS2DH" -> updateUILIS2DH(result, timeDifference)
                "WindSpeed" -> updateUISpeed(result, timeDifference)
                "StepCount" -> updateUIStepCount(result, timeDifference)
                "Speed Distance" -> updateSDT(result, timeDifference)
                "Object Finding" -> updateUIFInd(result)
            }
    }
    private fun updateSDT(result: ScanResult, timeDifference: Long){
        if (result.device.address == deviceAddress) {
            val bytes = result.scanRecord?.bytes ?: byteArrayOf()
            val signedBytes = bytes.map { it.toInt() }

            val deviceID = signedBytes.getOrNull(4)?.toUByte()?.toInt()

            val sxd = signedBytes.getOrNull(11)
            val sxf = signedBytes.getOrNull(12)?.toUByte()?.toInt()
            val dxd = signedBytes.getOrNull(13)
            val dxf = signedBytes.getOrNull(14)?.toUByte()?.toInt()

            var sx = 0.0
            var dx = 0.0
            if (sxd != null && sxf != null && dxd != null && dxf != null ) {
                sx= (sxd.toDouble()) + ((sxf.toDouble()) / 100.0) - speedx_reset
                dx= (dxd.toDouble()) + ((dxf.toDouble()) / 100.0) - disx_reset
            }

                if (sxd != null && sxf != null && dxd != null && dxf != null ) {

                val speedx = (sxd.toDouble()) + ((sxf.toDouble()) / 100.0)
                val dxValue = (dxd.toDouble()) + ((dxf.toDouble()) / 100.0)

                if(dxValue == 0.0){
                    lastdisx = 0.0
                    speedx_reset = 0.0
                    disx_reset =0.0
                    lastdisx =0.0
                }else{
                    lastdisx = dxValue
                    lastSpeedx = speedx
                }
                if((dxValue - disx_reset) < 0.0 ){
                    lastdisx = 0.0
                    speedx_reset = 0.0
                    disx_reset =0.0
                    lastdisx =0.0
                }

                // Batch process data
                if (speedx_Data.size < BATCH_SIZE) {
                    speedx_Data.add((speedx - speedx_reset ).toFloat())
                    disx_Data.add((dxValue- disx_reset).toFloat())
                } else {
                    speedxData.addAll(speedx_Data)
                    disxData.addAll(disx_Data)

                    speedx_Data.clear()
                    disx_Data.clear()
                }

                if (speedxData.size > MAX_QUEUE_SIZE) speedxData.removeFirst()
                disxData.add(speedx.toFloat())
                if (disxData.size > MAX_QUEUE_SIZE) disxData.removeFirst()
            }

            requireActivity().runOnUiThread {
                (binding as FragmentAdvertisingDataSdtBinding).apply {
                    Byte0Text.text = deviceAddress
                    Byte2Text.text = "$sx m/s"
                    Byte4Text.text = "$dx m"
                    Byte1Text.text = deviceID?.toString() ?: ""
                    Byte5Text.text = "$timeDifference ms"
                }
            }
        }
    }
    private  fun updateUISpeed(result: ScanResult, timeDifference: Long) {
            if (result.device.address == deviceAddress) {
                val bytes = result.scanRecord?.bytes ?: byteArrayOf()
                val unsignedBytes = bytes.map { it.toUByte().toInt() }

                val deviceID = unsignedBytes.getOrNull(4)
                val speed = unsignedBytes.getOrNull(5)

                if (speed != null) {


                    // Batch process data
                    if (speed_Data.size < BATCH_SIZE) {
                        speed_Data.add(speed.toFloat())
                    } else {
                        // Flush batch to the main list
                        speedData.addAll(speed_Data)
                        // Clear batch for new data
                        speed_Data.clear()
                    }

                    // Maintain temporary storage with a max size
                    speedData.add(speed.toFloat())
                    if (tempData.size > MAX_QUEUE_SIZE) speedData.removeFirst()
                }

                requireActivity().runOnUiThread {
                    (binding as FragmentAdvertismentDataWindspeedBinding).apply {
                        Byte0Text.text = deviceAddress
                        Byte1Text.text = deviceID?.toString() ?: ""
                        Byte2Text.text = speed.toString() ?: ""
                        Byte5Text.text = "$timeDifference ms"
                    }
                }
            }
    }

    private fun updateUIStepCount(result: ScanResult, timeDifference: Long) {
        if (result.device.address == deviceAddress) {
            val bytes = result.scanRecord?.bytes ?: byteArrayOf()
            val unsignedBytes = bytes.map { it.toUByte().toInt() }

            val deviceID = unsignedBytes.getOrNull(4)
            val highByte = unsignedBytes.getOrNull(5) ?: 0 // Retrieve high byte, default to 0 if not found
            val lowByte = unsignedBytes.getOrNull(6) ?: 0 // Retrieve low byte, default to 0 if not found

            val stepcount = (highByte shl 8) or lowByte

            if(stepcount == 0){
                lastStep = 0
                stepCount_reset = 0
            }else{
                lastStep = stepcount
            }
            if((stepcount - stepCount_reset) < 0 ){
                lastStep = 0
                stepCount_reset = 0
            }


            requireActivity().runOnUiThread {
                (binding as FragmentAdvertisingDataStepcountBinding).apply {

                    Byte2Text.text = (stepcount - stepCount_reset).toString()

                }
            }
        }
    }
    private  fun updateUISHT40(result: ScanResult, timeDifference: Long) {
            if (result.device.address == deviceAddress) {
                val bytes = result.scanRecord?.bytes ?: byteArrayOf()
                val unsignedBytes = bytes.map { it.toUByte().toInt() }

                val deviceID = unsignedBytes.getOrNull(4)
                val temp1 = unsignedBytes.getOrNull(5)
                val temp2 = unsignedBytes.getOrNull(6)
                val humid1 = unsignedBytes.getOrNull(7)
                val humid2 = unsignedBytes.getOrNull(8)

                val temp = "$temp1.$temp2\u00B0C"
                val humid = "$humid1.$humid2\u0025"

                if (temp1 != null && temp2 != null && humid1 != null && humid2 != null) {
                    val temperature = temp1.toDouble() + (temp2.toDouble() / 100.0)
                    val humidity = humid1.toDouble() + (humid2.toDouble() / 100.0)

                    // Batch process data
                    if (temp_Data.size < BATCH_SIZE) {
                        temp_Data.add(temperature.toFloat())
                        humid_Data.add(humidity.toFloat())
                    } else {
                        // Flush batch to the main list
                        tempData.addAll(temp_Data)
                        humidData.addAll(humid_Data)
                        // Clear batch for new data
                        temp_Data.clear()
                        humid_Data.clear()
                    }

                    // Maintain temporary storage with a max size
                    tempData.add(temperature.toFloat())
                    if (tempData.size > MAX_QUEUE_SIZE) tempData.removeFirst()

                    humidData.add(humidity.toFloat())
                    if (humidData.size > MAX_QUEUE_SIZE) humidData.removeFirst()
                }

                requireActivity().runOnUiThread {
                    (binding as FragmentAdvertisingDataShtBinding).apply {
                        Byte0Text.text = deviceAddress
                        Byte2Text.text = temp
                        Byte3Text.text = humid
                        Byte1Text.text = deviceID?.toString() ?: ""
                        Byte5Text.text = "$timeDifference ms"
                    }
                }
            }
    }
    private fun updateUIFInd(result: ScanResult) {
//        if (result.device.address == deviceAddress) {
//            // Check for BLUETOOTH_CONNECT permission
//            if (ContextCompat.checkSelfPermission(
//                    requireContext(),
//                    Manifest.permission.BLUETOOTH_CONNECT
//                ) == PackageManager.PERMISSION_GRANTED
//            ) {
//                val bytes = result.scanRecord?.bytes ?: byteArrayOf()
//                val signedBytes = bytes.map { it.toInt() }
//                val rssi = result.rssi
//                val deviceName = result.device.name ?: "Unnamed"
//                val currentIndex = deviceSequence.indexOf(deviceName)
//
//                val objectStatus = when {
//                    currentIndex > 0 && !foundDevices.contains(deviceSequence[currentIndex - 1]) -> {
//                        "\n\n\n\n\n\nPlease find ${deviceSequence[currentIndex - 1]} first\n\n\n\n\n\n\n"
//                    }
//                    rssi > -40 ->{
//                        foundDevices.add(deviceName)
//                        "\uD83C\uDF89      ✨\n\uD83C\uDF8A       ✨     \uD83E\uDD73\n\uD83E\uDD73           \uD83C\uDF89\n\n\uD83C\uDF88Object is found\uD83C\uDF88\n✨               \uD83E\uDD73\n\uD83E\uDD73         \uD83C\uDF89        \uD83C\uDF8A\n \uD83E\uDD73     ✨   \n\uD83C\uDF89"
//                }
//                    rssi in -70..-40 -> "\n\n\n\n\n\nObject is near\n\n\n\n\n\n\n"
//                    else -> "\n\n\n\n\n\nObject is far\n\n\n\n\n\n\n"
//                }
//
//                requireActivity().runOnUiThread {
//                    (binding as FragmentAdvertisingDataObjectFindingBinding).apply {
//                        Byte0Text.text = result.device.name ?: "unnamed"
////                        Byte4Text.text = result.device.name ?: "unnamed"
//                        Byte2Text.text = objectStatus
//                        Byte2Text.setTextColor(
//                            when (objectStatus) {
//                                    "\uD83C\uDF89      ✨\n\uD83C\uDF8A       ✨     \uD83E\uDD73\n\uD83E\uDD73           \uD83C\uDF89\n\n\uD83C\uDF88Object is found\uD83C\uDF88\n✨               \uD83E\uDD73\n\uD83E\uDD73         \uD83C\uDF89        \uD83C\uDF8A\n \uD83E\uDD73     ✨   \n\uD83C\uDF89" -> Color.GREEN
//                                "\n\n\n\n\n\nObject is near\n\n\n\n\n\n\n" -> Color.BLUE
//                                else -> Color.RED
//                            }
//                        )
//                        val backgroundResource = when (objectStatus) {
//                            "\uD83C\uDF89      ✨\n\uD83C\uDF8A       ✨     \uD83E\uDD73\n\uD83E\uDD73           \uD83C\uDF89\n\n\uD83C\uDF88Object is found\uD83C\uDF88\n✨               \uD83E\uDD73\n\uD83E\uDD73         \uD83C\uDF89        \uD83C\uDF8A\n \uD83E\uDD73     ✨   \n\uD83C\uDF89" -> R.drawable.boy
//                            "\n\n\n\n\n\nObject is near\n\n\n\n\n\n\n" -> R.drawable.boy2
//                            else -> R.drawable.boy3
//                        }
//                        mainLayout.background = ContextCompat.getDrawable(requireContext(), backgroundResource)
//
//
//                        if (objectStatus == "\uD83C\uDF89      ✨\n\uD83C\uDF8A       ✨     \uD83E\uDD73\n\uD83E\uDD73           \uD83C\uDF89\n\n\uD83C\uDF88Object is found\uD83C\uDF88\n✨               \uD83E\uDD73\n\uD83E\uDD73         \uD83C\uDF89        \uD83C\uDF8A\n \uD83E\uDD73     ✨   \n\uD83C\uDF89") {
//                            if (!mediaPlayer.isPlaying) { // Prevent multiple sound plays
//                                mediaPlayer.start()
//                            }
//                        }
//                        startBlinkingAnimation(Byte2Text)
//                    }
//                }
//            } else {
//                // Request permission if not granted
//                ActivityCompat.requestPermissions(
//                    requireActivity(),
//                    arrayOf(Manifest.permission.BLUETOOTH_CONNECT),
//                    PERMISSION_REQUEST_BLUETOOTH_CONNECT
//                )
//            }
//        }
    }

    // Function to trigger emoji celebration effect
//    private fun showCelebrationEmojis() {
//        val emojiContainer = binding.emojiContainer // Ensure emojiContainer is visible
//        emojiContainer.removeAllViews()
//
//        repeat(10) { // Show 10 random emojis for the effect
//            val emojiView = TextView(requireContext()).apply {
//                text = getRandomCelebrationEmoji()
//                textSize = 24f // Starting size
//                gravity = Gravity.CENTER
//                setTextColor(Color.parseColor("#FFCC00")) // Golden color for celebration
//            }
//
//            // Random position within the container
//            val layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
//            emojiView.layoutParams = layoutParams
//
//            // Start Scale Animation
//            val scaleAnimation = ScaleAnimation(
//                0.1f, 2.0f, // From 0.1 to 2.0 size
//                0.1f, 2.0f,
//                Animation.RELATIVE_TO_SELF, 0.5f,
//                Animation.RELATIVE_TO_SELF, 0.5f
//            ).apply {
//                duration = 500
//                fillAfter = true
//            }
//
//            // Alpha Animation to fade out
//            val alphaAnimation = AlphaAnimation(1.0f, 0.0f).apply {
//                duration = 500
//                startOffset = 250 // Start fading out halfway
//                fillAfter = true
//            }
//
//            // Combine Scale and Alpha Animation
//            val animationSet = Animation.AnimationSet(true).apply {
//                addAnimation(scaleAnimation)
//                addAnimation(alphaAnimation)
//            }
//
//            // Set a random position for the emoji within the container
//            emojiView.translationX = Random.nextInt(emojiContainer.width).toFloat()
//            emojiView.translationY = Random.nextInt(emojiContainer.height).toFloat()
//
//            // Start the animation
//            emojiView.startAnimation(animationSet)
//
//            // Remove the emoji after animation ends
//            animationSet.setAnimationListener(object : Animation.AnimationListener {
//                override fun onAnimationStart(animation: Animation) {}
//                override fun onAnimationEnd(animation: Animation) {
//                    emojiContainer.removeView(emojiView)
//                }
//                override fun onAnimationRepeat(animation: Animation) {}
//            })
//
//            // Add the emoji to the container and animate
//            emojiContainer.addView(emojiView)
//        }
//
//        // Make the emoji container visible if hidden
//        emojiContainer.visibility = View.VISIBLE
//    }
//
//
//
//    private fun getRandomCelebrationEmoji(): String {
//        val emojis = listOf("🎉", "🎈", "🎊", "✨", "🥳")
//        return emojis.random()
//    }
    private fun startBlinkingAnimation(view: TextView) {
        val animator = ObjectAnimator.ofFloat(view, "alpha", 1f, 0f)
        animator.duration = 500  // Duration for fade in and fade out
        animator.interpolator = LinearInterpolator()
        animator.repeatCount = ObjectAnimator.INFINITE
        animator.repeatMode = ObjectAnimator.REVERSE
        animator.start()
    }

    private  fun updateUILIS2DH(result: ScanResult, timeDifference: Long) {
            if (result.device.address == deviceAddress) {
                val bytes = result.scanRecord?.bytes ?: byteArrayOf()
                val signedBytes = bytes.map { it.toInt() }

                val deviceID = signedBytes.getOrNull(4)?.toUByte()?.toInt()
                val xd = signedBytes.getOrNull(5)
                val xf = signedBytes.getOrNull(6)?.toUByte()?.toInt()
                val yd = signedBytes.getOrNull(7)
                val yf = signedBytes.getOrNull(8)?.toUByte()?.toInt()
                val zd = signedBytes.getOrNull(9)
                val zf = signedBytes.getOrNull(10)?.toUByte()?.toInt()


                val X = "$xd.$xf"
                val Y = "$yd.$yf"
                val Z = "$zd.$zf"

                if (xd != null && xf != null && yd != null && yf != null && zd != null && zf != null) {
                    val xValue = xd.toDouble() + (xf.toDouble() / 100.0)
                    val yValue = yd.toDouble() + (yf.toDouble() / 100.0)
                    val zValue = zd.toDouble() + (zf.toDouble() / 100.0)


                    // Batch process data
                    if (x_Data.size < BATCH_SIZE) {
                        x_Data.add(xValue.toFloat())
                        y_Data.add(yValue.toFloat())
                        z_Data.add(zValue.toFloat())

                    } else {
                        // Flush batch to the main list
                        xData.addAll(x_Data)
                        yData.addAll(y_Data)
                        zData.addAll(z_Data)

                        // Clear batch for new data
                        x_Data.clear()
                        y_Data.clear()
                        z_Data.clear()
                    }

                    // Maintain temporary storage with a max size
                    xData.add(xValue.toFloat())
                    if (xData.size > MAX_QUEUE_SIZE) xData.removeFirst()
                    yData.add(yValue.toFloat())
                    if (yData.size > MAX_QUEUE_SIZE) yData.removeFirst()
                    zData.add(zValue.toFloat())
                    if (zData.size > MAX_QUEUE_SIZE) zData.removeFirst()
                }

                requireActivity().runOnUiThread {
                    (binding as FragmentAdvertisingDataAccBinding).apply {
                        Byte0Text.text = deviceAddress
                        Byte2Text.text = X
                        Byte3Text.text = Y
                        Byte4Text.text = Z
                        Byte1Text.text = deviceID?.toString() ?: ""
                        Byte5Text.text = "$timeDifference ms"
                    }
                }
        }
    }

    private fun downloadDataAsExcel() {
        val workbook = XSSFWorkbook()
        val sheet = when (dropdownitem) {
            "SHT40" -> workbook.createSheet("Temp Humid Data")
            "LIS2DH" -> workbook.createSheet("Acc Data")
            "WindSpeed" -> workbook.createSheet("Speed Data")
            "StepCount" -> workbook.createSheet("Steps Data")
            "Speed Distance" -> workbook.createSheet("Speed Data")
            else -> return
        }

        // Create the header row
        val headerRow = sheet.createRow(0)
        val headerCell1 = headerRow.createCell(0)
        val headerCell2 = headerRow.createCell(1)

        when (dropdownitem) {
            "SHT40" -> {
                headerCell1.setCellValue("Temperature (°C)")
                headerCell2.setCellValue("Humidity (%)")

                for (i in temp_Data.indices) {
                    val row = sheet.createRow(i + 1)
                    val cell1 = row.createCell(0)
                    cell1.setCellValue(temp_Data[i].toDouble())
                    val cell2 = row.createCell(1)
                    cell2.setCellValue(humid_Data[i].toDouble())
                }
            }
            "LIS2DH" -> {
                headerCell1.setCellValue("X (g)")
                headerCell2.setCellValue("Y (g)")
                val headerCell3 = headerRow.createCell(2)
                headerCell3.setCellValue("Z (g)")


                for (i in x_Data.indices) {
                    val row = sheet.createRow(i + 1)
                    val cell1 = row.createCell(0)
                    cell1.setCellValue(x_Data[i].toDouble())
                    val cell2 = row.createCell(1)
                    cell2.setCellValue(y_Data[i].toDouble())
                    val cell3 = row.createCell(2)
                    cell3.setCellValue(z_Data[i].toDouble())
                }
            }
            "WindSpeed" -> {
                headerCell1.setCellValue("Speed")


                for (i in speed_Data.indices) {
                    val row = sheet.createRow(i + 1)
                    val cell1 = row.createCell(0)
                    cell1.setCellValue(speed_Data[i].toDouble())

                }
            }
            "Speed Distance" -> {
                headerCell1.setCellValue("Speed (m/s)")
                headerCell2.setCellValue("Distance (m)")


                for (i in speedx_Data.indices) {
                    val row = sheet.createRow(i + 1)
                    val cell1 = row.createCell(0)
                    cell1.setCellValue(speedx_Data[i].toDouble())
                    val cell2 = row.createCell(1)
                    cell2.setCellValue(disx_Data[i].toDouble())
                }
            }

        }

        try {
            val fileName = "${dropdownitem}_Data.xlsx"
            val fileOutputStream = requireContext().openFileOutput(fileName, Context.MODE_PRIVATE)
            workbook.write(fileOutputStream)
            fileOutputStream.close()
            workbook.close()

            val file = File(requireContext().filesDir, fileName)
            val uri = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.provider", file)
            val intent = Intent(Intent.ACTION_SEND)
            intent.type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            intent.putExtra(Intent.EXTRA_STREAM, uri)
            startActivity(Intent.createChooser(intent, "Share via"))

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "Failed to create Excel file", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        val width = WindowManager.LayoutParams.MATCH_PARENT
        val height = WindowManager.LayoutParams.WRAP_CONTENT
        dialog?.window?.setLayout(width, height)

    }
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == PERMISSION_REQUEST_BLUETOOTH_CONNECT) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, you can re-call updateUIFInd or continue
            } else {
                // Permission denied, handle the case accordingly (e.g., show a message)
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        // Release MediaPlayer resources when the Fragment is destroyed
        if (::mediaPlayer.isInitialized) {
            mediaPlayer.release()
        }
    }

}
