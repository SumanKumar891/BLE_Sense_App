package com.merge.awadh

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.PopupMenu
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import androidx.cardview.widget.CardView
import com.merge.awadh.activity.scan.ScanActivity
import com.merge.awadh.activity.scan.fragment.DeviceInfoFragment
import com.merge.awadh.activity.scan.fragment.RSSIFilterFragment
import com.merge.awadh.ble.BLEManager

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationIcon(R.drawable.ic_menu) // Use your menu icon resource
        toolbar.setNavigationOnClickListener {
            // Show menu options
            showMenuOptions()
        }

        // Get references to the CardViews
        val cardGames: CardView = findViewById(R.id.cardGames)
        val cardBLEApp: CardView = findViewById(R.id.cardBLEApp)

        val button=findViewById<Button>(R.id.btnGames)
        button.setOnClickListener{
            val Intent= Intent(this, test2::class.java)
            startActivity(Intent)
        }

        val button2=findViewById<Button>(R.id.btnBLE)
        button2.setOnClickListener{
            val Intent= Intent(this, ScanActivity::class.java)
            startActivity(Intent)
        }

        // Start animations after the layout is drawn
        cardGames.post {
            animateCard(cardGames, 0) // Delay for the first card
        }
        cardBLEApp.post {
            animateCard(cardBLEApp, 300) // Slight delay for the second card
        }
    }

    private fun animateCard(card: View, delay: Long) {
        // Initial properties for sliding and scaling
        card.translationY = 1000f // Start off-screen
        card.alpha = 0f
        card.scaleX = 0.5f
        card.scaleY = 0.5f

        // Animators
        val slideUp = ObjectAnimator.ofFloat(card, "translationY", 0f)
        val fadeIn = ObjectAnimator.ofFloat(card, "alpha", 1f)
        val scaleX = ObjectAnimator.ofFloat(card, "scaleX", 1f)
        val scaleY = ObjectAnimator.ofFloat(card, "scaleY", 1f)

        // Combine animations
        val animatorSet = AnimatorSet()
        animatorSet.playTogether(slideUp, fadeIn, scaleX, scaleY)
        animatorSet.startDelay = delay // Add delay for staggered animation
        animatorSet.duration = 700 // Animation duration in milliseconds
        animatorSet.start()
    }

    private fun showMenuOptions() {
        // Create a popup menu anchored to the toolbar
        val popupMenu = PopupMenu(this, findViewById<android.widget.Toolbar>(R.id.toolbar))

        // Add only the two menu items dynamically
        popupMenu.menu.add(0, R.id.allDevicesItem1, 0, "All Devices")
        popupMenu.menu.add(0, R.id.gamingOptionsItem1, 1, "Gaming Options")
        popupMenu.menu.add(0, R.id.switchThemeItem1, 2, "Switch Theme")

        // Handle item selection
        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.allDevicesItem1 -> {
                    val intent = Intent(this, ScanActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.gamingOptionsItem1 -> {
                    // Navigate to Test2 activity
                    val intent = Intent(this, test2::class.java)
                    startActivity(intent)
                    true
                }
                R.id.switchThemeItem1 -> {
                    // Toggle the theme
                    toggleTheme()
                    true
                }
                else -> false
            }
        }

        // Show the menu
        popupMenu.show()
    }
    private fun toggleTheme() {
        // Access shared preferences
        val sharedPreferences = getSharedPreferences("appPreferences", Context.MODE_PRIVATE)
        val isDarkTheme = sharedPreferences.getBoolean("isDarkTheme", false)

        // Toggle the theme
        val newTheme = !isDarkTheme
        if (newTheme) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }

        // Save the new theme preference
        with(sharedPreferences.edit()) {
            putBoolean("isDarkTheme", newTheme)
            apply()
        }

        // Optional: Update the theme switch state
//        findViewById<SwitchCompat>(R.id.themeSwitch).isChecked = newTheme
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu2, menu)

        // Get Current App Version
        menu.findItem(R.id.appVersionItem).apply {
            title = "$title ${BuildConfig.VERSION_NAME}"
        }

        return true
    }

    // Item on Toolbar Selected
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.scanItem -> {
                if (BLEManager.isScanning) {
                    BLEManager.stopScan()
                    item.setIcon(R.drawable.ic_play)
                } else {
                    BLEManager.startScan(this)
                    item.setIcon(R.drawable.ic_pause)
                }
            }
            R.id.rssiFilterItem -> {
                RSSIFilterFragment().show(supportFragmentManager, "rssiFilterFragment")
            }
            R.id.deviceInfoItem -> {
                DeviceInfoFragment().show(supportFragmentManager, "deviceInfoFragment")
            }
        }


        return false
    }
}
