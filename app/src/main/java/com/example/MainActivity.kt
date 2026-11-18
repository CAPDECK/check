package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import com.example.data.local.WriterDatabase
import com.example.data.repository.WriterRepository
import com.example.ui.screens.WriterAppMainLayout
import com.example.ui.viewmodel.WriterViewModel
import com.example.ui.viewmodel.WriterViewModelFactory

class MainActivity : ComponentActivity() {

    private fun applyCustomerPreferredRefreshRate(rate: String) {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                val displayDevice = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    this.display
                } else {
                    @Suppress("DEPRECATION")
                    windowManager.defaultDisplay
                }
                val modes = displayDevice?.supportedModes
                if (!modes.isNullOrEmpty()) {
                    val layoutParams = window.attributes
                    
                    if (rate == "AUTO") {
                        // Max out to get buttery-smooth 120Hz/high rate automatically
                        val highestMode = modes.maxByOrNull { it.refreshRate }
                        if (highestMode != null) {
                            layoutParams.preferredDisplayModeId = highestMode.modeId
                        }
                    } else {
                        // Force a specific target frame rate: 60, 90, or 120 Hz
                        val targetFr = rate.toFloatOrNull() ?: 60f
                        val optimalMode = modes.minByOrNull { kotlin.math.abs(it.refreshRate - targetFr) }
                        if (optimalMode != null) {
                            layoutParams.preferredDisplayModeId = optimalMode.modeId
                        }
                    }
                    window.attributes = layoutParams
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Apply silent hardware, display, and image cache optimizations
        com.example.util.TensorOptimizer.applyOptimizations(applicationContext, window)

        // 1. Initialize local SQLite room database
        val database = WriterDatabase.getDatabase(applicationContext)

        // 2. Initialize domain coordinator repository
        val repository = WriterRepository(database)

        // 3. Prepare factory
        val factory = WriterViewModelFactory(repository)

        // 4. Resolve ViewModel
        val viewModel = ViewModelProvider(this, factory)[WriterViewModel::class.java]

        // Observe the user's preferred refresh rate and apply it inside window attributes dynamically
        lifecycleScope.launch {
            viewModel.preferredRefreshRate.collectLatest { rate ->
                applyCustomerPreferredRefreshRate(rate)
            }
        }

        // Observe preferred language and synchronize Android application locale dynamically
        lifecycleScope.launch {
            viewModel.appLanguage.collectLatest { langCode ->
                try {
                    val targetLocale = com.example.util.LocalizationManager.getLocale(langCode)
                    java.util.Locale.setDefault(targetLocale)
                    val config = resources.configuration
                    config.setLocale(targetLocale)
                    config.setLayoutDirection(targetLocale)
                    @Suppress("DEPRECATION")
                    resources.updateConfiguration(config, resources.displayMetrics)

                    // Also synchronize applicationContext resources
                    val appConfig = applicationContext.resources.configuration
                    appConfig.setLocale(targetLocale)
                    appConfig.setLayoutDirection(targetLocale)
                    @Suppress("DEPRECATION")
                    applicationContext.resources.updateConfiguration(appConfig, applicationContext.resources.displayMetrics)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        // 5. Initialize network sync listener for Google Drive
        viewModel.initNetworkListener(this)

        // 6. Automatically check and restore existing Google session
        viewModel.checkAndRestoreGoogleSession(this)

        setContent {
            // Draw central composition
            WriterAppMainLayout(viewModel)
        }
    }
}
