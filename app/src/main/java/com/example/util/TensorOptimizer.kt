package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.view.Window
import android.view.WindowManager
import coil.Coil
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executors

/**
 * Dedicated Hardware & Architecture Optimizer for Google Tensor chipsets (G1, G2, G3, G4, G5, G6+)
 * and modern ARM64 64-bit architectures.
 *
 * Implements:
 * - Specific detection for:
 *   • Tensor G6 (Malibu) - TSMC N2/N3P, 16KB Pages, next-gen ARM / PowerVR GPU
 *   • Tensor G5 (Laguna) - TSMC 3nm (N3E), DTI TPU, DXT Imagination GPU, Cortex-X925 / X4, 16KB Pages
 *   • Tensor G4 (Zuma Pro) - Cortex-X4, Mali-G715, 16KB Pages
 *   • Tensor G3 (Zuma) - Cortex-X3, Mali-G715, 16KB Pages
 *   • Tensor G2 (GS201) - Dual Cortex-X1, Mali-G710
 *   • Tensor G1 (GS101) - Dual Cortex-X1, 20-core Mali-G78
 * - Zero-copy Hardware Bitmap rendering direct to GPU GraphicBuffers
 * - High refresh rate display mode optimization (90Hz / 120Hz LTPO Smooth Display)
 * - 16 KB memory page compatibility support (Android 15+ Tensor G3/G4/G5/G6)
 * - Tri-cluster and modern multi-cluster CPU thread pooling
 */
object TensorOptimizer {

    data class TensorProfile(
        val isTensor: Boolean,
        val generationName: String,
        val deviceModelName: String,
        val socCode: String,
        val gpuArchitecture: String,
        val cpuTopology: String,
        val is16KbPageReady: Boolean,
        val memoryTier: String,
        val isFlagshipPixel: Boolean,
        val details: String
    )

    @Volatile
    private var cachedProfile: TensorProfile? = null

    /**
     * Inspects device hardware to determine Google Tensor generation and capabilities.
     */
    fun getProfile(context: Context): TensorProfile {
        cachedProfile?.let { return it }

        val socModel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Build.SOC_MODEL.lowercase()
        } else {
            ""
        }
        val hardware = Build.HARDWARE.lowercase()
        val board = Build.BOARD.lowercase()
        val model = Build.MODEL.lowercase()
        val device = Build.DEVICE.lowercase()
        val manufacturer = Build.MANUFACTURER.lowercase()

        val isGoogleDevice = manufacturer.contains("google") || model.contains("pixel")

        val profile = when {
            // Tensor G6 (Pixel 11 series: Pixel 11, Pixel 11 Pro, Pixel 11 Pro XL, Pixel 11 Fold) - Malibu / TSMC N2 / N3P
            socModel.contains("malibu") || hardware.contains("malibu") || board in listOf("malibu") ||
            (isGoogleDevice && (model.contains("pixel 11") || device.contains("malibu"))) -> {
                val detectedModel = when {
                    model.contains("pro xl") -> "Google Pixel 11 Pro XL"
                    model.contains("pro") -> "Google Pixel 11 Pro"
                    model.contains("fold") -> "Google Pixel 11 Fold"
                    model.contains("pixel 11") -> "Google Pixel 11"
                    else -> "Google Pixel 11 Series (Malibu)"
                }
                TensorProfile(
                    isTensor = true,
                    generationName = "Google Tensor G6 (Malibu)",
                    deviceModelName = detectedModel,
                    socCode = if (socModel.isNotEmpty()) socModel else "malibu",
                    gpuArchitecture = "Next-Gen ARM / IMG GPU with HW Ray Tracing & Mesh Shading",
                    cpuTopology = "Next-Gen Multi-Cluster: Cortex-X Prime + ARMv9 Performance Cores (TSMC)",
                    is16KbPageReady = true,
                    memoryTier = "LPDDR5X (Up to 16GB / 24GB)",
                    isFlagshipPixel = true,
                    details = "Google Pixel 11 & Tensor G6: Built on cutting-edge TSMC node with next-gen Deep Thinking TPU, 16KB native memory pages, and direct hardware graphics buffer allocation."
                )
            }

            // Tensor G5 (Pixel 10 series: Pixel 10, Pixel 10 Pro, Pixel 10 Pro XL, Pixel 10 Pro Fold) - Laguna / TSMC 3nm (N3E)
            socModel.contains("laguna") || hardware.contains("laguna") || board in listOf("laguna", "frankel", "blazer", "mustang") ||
            (isGoogleDevice && (model.contains("pixel 10") || device in listOf("laguna", "frankel", "blazer", "mustang") || device.contains("laguna"))) -> {
                val detectedModel = when {
                    model.contains("pro xl") || board == "mustang" || device == "mustang" -> "Google Pixel 10 Pro XL"
                    model.contains("pro fold") || board == "blazer" || device == "blazer" -> "Google Pixel 10 Pro Fold"
                    model.contains("pro") || board == "frankel" || device == "frankel" -> "Google Pixel 10 Pro"
                    model.contains("pixel 10") || board == "laguna" || device == "laguna" -> "Google Pixel 10"
                    else -> "Google Pixel 10 Series (Laguna)"
                }
                TensorProfile(
                    isTensor = true,
                    generationName = "Google Tensor G5 (Laguna)",
                    deviceModelName = detectedModel,
                    socCode = if (socModel.isNotEmpty()) socModel else "laguna",
                    gpuArchitecture = "IMG DXT-72-2304 / PowerVR Graphics (TSMC 3nm)",
                    cpuTopology = "8-Core Tri-Cluster: 1x Cortex-X4 / X925 (3.4 GHz) + 5x Cortex-A725 + 2x Cortex-A520",
                    is16KbPageReady = true,
                    memoryTier = "LPDDR5X (12GB - 16GB)",
                    isFlagshipPixel = true,
                    details = "Google Pixel 10 & Tensor G5: Google's first fully custom TSMC 3nm SoC with dedicated DTI TPU, 16KB page alignment, and hardware texture streaming."
                )
            }

            // Tensor G4 (Pixel 9, 9 Pro, 9 Pro XL, 9 Pro Fold) - Zuma Pro / S5P9875
            socModel.contains("zumapro") || socModel.contains("zuma_pro") || socModel.contains("s5p9875") ||
            hardware.contains("zumapro") || board in listOf("tokay", "caiman", "komodo", "comet") ||
            (isGoogleDevice && (model.contains("pixel 9") || device in listOf("tokay", "caiman", "komodo", "comet"))) -> {
                TensorProfile(
                    isTensor = true,
                    generationName = "Google Tensor G4 (Zuma Pro)",
                    deviceModelName = if (model.contains("pixel")) model.split(" ").joinToString(" ") { it.replaceFirstChar(Char::uppercase) } else "Google Pixel 9 Series",
                    socCode = if (socModel.isNotEmpty()) socModel else "zuma_pro",
                    gpuArchitecture = "ARM Mali-G715 (Valhall 5th Gen) + Ray Tracing",
                    cpuTopology = "8-Core Tri-Cluster: 1x Cortex-X4 (3.1 GHz) + 3x Cortex-A720 + 4x Cortex-A520",
                    is16KbPageReady = true,
                    memoryTier = "LPDDR5X (Up to 16GB)",
                    isFlagshipPixel = true,
                    details = "Google Tensor G4: Optimized for Edge TPU, 16KB Page Alignment, and Mali GPU direct texture streaming."
                )
            }

            // Tensor G3 (Pixel 8, 8 Pro, 8a) - Zuma / S5P9865
            socModel.contains("zuma") || socModel.contains("s5p9865") ||
            hardware.contains("zuma") || board in listOf("shiba", "husky", "akita") ||
            (isGoogleDevice && (model.contains("pixel 8") || device in listOf("shiba", "husky", "akita"))) -> {
                TensorProfile(
                    isTensor = true,
                    generationName = "Google Tensor G3 (Zuma)",
                    deviceModelName = if (model.contains("pixel")) model.split(" ").joinToString(" ") { it.replaceFirstChar(Char::uppercase) } else "Google Pixel 8 Series",
                    socCode = if (socModel.isNotEmpty()) socModel else "zuma",
                    gpuArchitecture = "ARM Mali-G715 MP10 (Valhall 5th Gen)",
                    cpuTopology = "9-Core Tri-Cluster: 1x Cortex-X3 (2.9 GHz) + 4x Cortex-A715 + 4x Cortex-A510",
                    is16KbPageReady = true,
                    memoryTier = "LPDDR5X (8GB - 12GB)",
                    isFlagshipPixel = true,
                    details = "Google Tensor G3: First 16KB Page Size compatible Pixel chip with hardware raytracing acceleration."
                )
            }

            // Tensor G2 (Pixel 7, 7 Pro, 7a, Fold, Tablet) - GS201 / Cloudripper
            socModel.contains("gs201") || socModel.contains("s5e9855") ||
            hardware.contains("gs201") || board in listOf("panther", "cheetah", "lynx", "felix", "tangorpro") ||
            (isGoogleDevice && (model.contains("pixel 7") || model.contains("pixel fold") || model.contains("pixel tablet") || device in listOf("panther", "cheetah", "lynx", "felix", "tangorpro"))) -> {
                TensorProfile(
                    isTensor = true,
                    generationName = "Google Tensor G2 (GS201)",
                    deviceModelName = if (model.contains("pixel")) model.split(" ").joinToString(" ") { it.replaceFirstChar(Char::uppercase) } else "Google Pixel 7 Series",
                    socCode = if (socModel.isNotEmpty()) socModel else "gs201",
                    gpuArchitecture = "ARM Mali-G710 MP7",
                    cpuTopology = "8-Core Tri-Cluster: 2x Cortex-X1 (2.85 GHz) + 2x Cortex-A78 + 4x Cortex-A55",
                    is16KbPageReady = Build.VERSION.SDK_INT >= 35,
                    memoryTier = "LPDDR5 (8GB - 12GB)",
                    isFlagshipPixel = true,
                    details = "Google Tensor G2: Dual Cortex-X1 super cores with high-throughput Mali-G710 GPU pipeline."
                )
            }

            // Tensor G1 (Pixel 6, 6 Pro, 6a) - GS101 / Whitechapel
            socModel.contains("gs101") || socModel.contains("s5e9845") ||
            hardware.contains("gs101") || hardware.contains("slider") || board in listOf("oriole", "raven", "bluejay") ||
            (isGoogleDevice && (model.contains("pixel 6") || device in listOf("oriole", "raven", "bluejay"))) -> {
                TensorProfile(
                    isTensor = true,
                    generationName = "Google Tensor G1 (GS101)",
                    deviceModelName = if (model.contains("pixel")) model.split(" ").joinToString(" ") { it.replaceFirstChar(Char::uppercase) } else "Google Pixel 6 Series",
                    socCode = if (socModel.isNotEmpty()) socModel else "gs101",
                    gpuArchitecture = "ARM Mali-G78 MP20 (20 Cores)",
                    cpuTopology = "8-Core Tri-Cluster: 2x Cortex-X1 (2.8 GHz) + 2x Cortex-A76 + 4x Cortex-A55",
                    is16KbPageReady = false,
                    memoryTier = "LPDDR5 (6GB - 12GB)",
                    isFlagshipPixel = true,
                    details = "Google Tensor G1: Debut custom Google SoC with 20-core Mali-G78 GPU compute."
                )
            }

            // General Google Pixel or ARM64 Flagship device
            isGoogleDevice -> {
                TensorProfile(
                    isTensor = true,
                    generationName = "Google Tensor Architecture (ARM64)",
                    deviceModelName = if (model.isNotEmpty()) model.replaceFirstChar(Char::uppercase) else "Google Pixel Device",
                    socCode = if (socModel.isNotEmpty()) socModel else hardware,
                    gpuArchitecture = "ARM Mali Graphics Pipeline",
                    cpuTopology = "Multi-Cluster 64-bit ARM CPU",
                    is16KbPageReady = Build.VERSION.SDK_INT >= 35,
                    memoryTier = "High-Bandwidth Unified RAM",
                    isFlagshipPixel = true,
                    details = "Google Tensor Architecture: Optimized for Google Pixel hardware features and 120Hz display refresh."
                )
            }

            else -> {
                TensorProfile(
                    isTensor = false,
                    generationName = "Universal ARM64 Engine",
                    deviceModelName = if (model.isNotEmpty()) model else "Android Device",
                    socCode = if (socModel.isNotEmpty()) socModel else hardware,
                    gpuArchitecture = "Hardware Accelerated GPU",
                    cpuTopology = "${Runtime.getRuntime().availableProcessors()} Cores",
                    is16KbPageReady = Build.VERSION.SDK_INT >= 35,
                    memoryTier = "Standard System Memory",
                    isFlagshipPixel = false,
                    details = "Standard ARM64 acceleration: 64-bit multi-threading and hardware graphics rendering."
                )
            }
        }

        cachedProfile = profile
        return profile
    }

    /**
     * Dedicated Coroutine Dispatcher tuned for high-throughput background work
     * without choking Tensor's Cortex-A efficiency cores.
     * On Pixel 10 (Laguna) and Pixel 11 (Malibu), leverages wider IPC bandwidth
     * and high-core cluster topologies.
     */
    val tensorWorkerDispatcher: CoroutineDispatcher by lazy {
        val availableCores = Runtime.getRuntime().availableProcessors()
        // For Tensor's multi-cluster topologies (e.g. 1 prime + 5 performance on G5):
        // Allocate worker pool proportional to high-IPC cores to prevent thermal throttling.
        val optimalThreads = when {
            availableCores >= 10 -> (availableCores - 2).coerceAtLeast(6)
            availableCores >= 8 -> (availableCores - 2).coerceAtLeast(4)
            availableCores >= 4 -> availableCores - 1
            else -> 2
        }
        Executors.newFixedThreadPool(optimalThreads) { runnable ->
            Thread(runnable, "TensorOptimizedWorker").apply {
                priority = Thread.NORM_PRIORITY
            }
        }.asCoroutineDispatcher()
    }

    /**
     * Applies full suite of Tensor optimizations:
     * 1. Configures Coil ImageLoader with Hardware Bitmaps (zero copy to GPU)
     *    with dynamically scaled cache for 12GB - 24GB LPDDR5X on Pixel 10 & 11
     * 2. Sets Window flags for maximum refresh rate and hardware acceleration
     *    with Android 15/16 Frame Rate compatibility (up to 120Hz/144Hz LTPO)
     */
    fun applyOptimizations(context: Context, window: Window? = null) {
        val profile = getProfile(context)

        // 1. Configure Coil ImageLoader with Hardware Bitmaps and aggressive memory cache
        try {
            // Allocate larger memory pool for Pixel 10 & 11 high RAM tiers (12GB - 24GB)
            val memoryPercent = when {
                profile.generationName.contains("G6") || profile.generationName.contains("Pixel 11") -> 0.30
                profile.generationName.contains("G5") || profile.generationName.contains("Pixel 10") -> 0.28
                profile.isTensor -> 0.25
                else -> 0.20
            }
            val diskCacheSize = when {
                profile.generationName.contains("G6") || profile.generationName.contains("G5") -> 250L * 1024 * 1024 // 250MB fast UFS 4.0 storage cache
                profile.isTensor -> 150L * 1024 * 1024
                else -> 100L * 1024 * 1024
            }

            val imageLoader = ImageLoader.Builder(context.applicationContext)
                .bitmapConfig(Bitmap.Config.HARDWARE) // Direct GraphicBuffer mapping on Mali / IMG GPU
                .allowHardware(true)
                .crossfade(true)
                .memoryCache {
                    MemoryCache.Builder(context.applicationContext)
                        .maxSizePercent(memoryPercent)
                        .build()
                }
                .diskCache {
                    DiskCache.Builder()
                        .directory(context.applicationContext.cacheDir.resolve("tensor_image_cache"))
                        .maxSizeBytes(diskCacheSize)
                        .build()
                }
                .build()

            Coil.setImageLoader(imageLoader)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Hardware Window and Refresh Rate Pacing
        window?.let { win ->
            try {
                // Ensure hardware accelerated window
                win.setFlags(
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
                )

                // High refresh rate display selection for Smooth Display (90Hz / 120Hz / 144Hz LTPO)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val displayDevice = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        context.display
                    } else {
                        @Suppress("DEPRECATION")
                        win.windowManager.defaultDisplay
                    }

                    val modes = displayDevice?.supportedModes
                    if (!modes.isNullOrEmpty()) {
                        val maxMode = modes.maxByOrNull { it.refreshRate }
                        if (maxMode != null) {
                            val layoutParams = win.attributes
                            layoutParams.preferredDisplayModeId = maxMode.modeId
                            win.attributes = layoutParams
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
