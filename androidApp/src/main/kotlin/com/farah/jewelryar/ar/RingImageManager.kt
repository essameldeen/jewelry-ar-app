package com.farah.jewelryar.ar

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL

/**
 * Manages downloading and caching 3D ring model preview images from Sketchfab
 * Uses free, high-quality 3D ring models with proper licenses
 */
class RingImageManager(private val context: Context) {
    private val cacheDir = File(context.cacheDir, "ring_images")
    private val loadedImages = mutableMapOf<String, Bitmap?>()

    companion object {
        private const val TAG = "RingImageManager"

        // High-quality ring product images
        // Using publicly available ring product images for reliable display
        // These represent different ring styles for AR try-on visualization
        private val RING_IMAGE_URLS = mapOf(
            RingStyle.GOLD to "https://images.unsplash.com/photo-1599643478518-a784e5dc4c8f?w=512&h=512&fit=crop",
            RingStyle.SILVER to "https://images.unsplash.com/photo-1515562141207-6811bcdd56cd?w=512&h=512&fit=crop",
            RingStyle.ROSE_GOLD to "https://images.unsplash.com/photo-1611591437281-460bfbe1220a?w=512&h=512&fit=crop",
            RingStyle.DIAMOND to "https://images.unsplash.com/photo-1599643478518-a784e5dc4c8f?w=512&h=512&fit=crop",
            RingStyle.PLATINUM to "https://images.unsplash.com/photo-1597193945716-2b437a6f2e5c?w=512&h=512&fit=crop"
        )

        // Fallback: Generate local ring images if network fails
        private const val USE_LOCAL_FALLBACK = true
    }

    init {
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
            Log.d(TAG, "Created cache directory: ${cacheDir.absolutePath}")
        }
    }

    /**
     * Load ring image for given style (from cache, network, or generate locally)
     */
    suspend fun loadRingImage(ringStyle: RingStyle): Bitmap? = withContext(Dispatchers.IO) {
        try {
            // Check if already loaded in memory
            if (loadedImages.containsKey(ringStyle.name)) {
                return@withContext loadedImages[ringStyle.name]
            }

            // Try to load from cache
            val cachedBitmap = loadFromCache(ringStyle)
            if (cachedBitmap != null) {
                loadedImages[ringStyle.name] = cachedBitmap
                Log.d(TAG, "Loaded ring image from cache: ${ringStyle.name}")
                return@withContext cachedBitmap
            }

            // Try to download from Sketchfab CDN
            val urlString = RING_IMAGE_URLS[ringStyle]
            if (urlString != null) {
                val downloadedBitmap = downloadImage(urlString)
                if (downloadedBitmap != null) {
                    saveToCache(ringStyle, downloadedBitmap)
                    loadedImages[ringStyle.name] = downloadedBitmap
                    Log.d(TAG, "Downloaded and cached ring image from Sketchfab: ${ringStyle.name}")
                    return@withContext downloadedBitmap
                }
            }

            // Fallback: Generate ring image locally if network fails
            if (USE_LOCAL_FALLBACK) {
                val bitmap = generateRingImage(ringStyle)
                if (bitmap != null) {
                    saveToCache(ringStyle, bitmap)
                    loadedImages[ringStyle.name] = bitmap
                    Log.d(TAG, "Generated and cached ring image locally (fallback): ${ringStyle.name}")
                    return@withContext bitmap
                }
            }

            Log.w(TAG, "Failed to load ring image for: ${ringStyle.name}")
            loadedImages[ringStyle.name] = null
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error loading ring image: ${e.message}", e)
            null
        }
    }

    /**
     * Generate a realistic 3D ring image based on style
     */
    private fun generateRingImage(ringStyle: RingStyle): Bitmap? {
        return try {
            val size = 512
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bitmap)
            val colors = ringStyle.getColors()

            // Transparent background - critical for overlay
            bitmap.eraseColor(android.graphics.Color.TRANSPARENT)

            val centerX = size / 2f
            val centerY = size / 2f
            val outerRadius = 150f
            val innerRadius = 90f
            val bandWidth = outerRadius - innerRadius

            val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)

            // Draw multiple shadow layers for depth
            for (i in 0 until 3) {
                paint.color = android.graphics.Color.argb(30 - i * 10, 0, 0, 0)
                canvas.drawCircle(centerX + 2 + i, centerY + 3 + i, outerRadius + 2, paint)
            }

            // Draw main ring body with gradient
            paint.shader = android.graphics.RadialGradient(
                centerX - 30f, centerY - 30f, 50f,
                colors.highlightColor,
                colors.primaryColor,
                android.graphics.Shader.TileMode.CLAMP
            )
            canvas.drawCircle(centerX, centerY, outerRadius, paint)

            // Draw inner white area
            paint.shader = null
            paint.color = android.graphics.Color.WHITE
            canvas.drawCircle(centerX, centerY, innerRadius, paint)

            // Draw shiny highlights for realistic 3D effect
            paint.color = android.graphics.Color.argb(180, 255, 255, 255)
            canvas.drawCircle(centerX - 50, centerY - 50, 35f, paint)

            paint.color = android.graphics.Color.argb(100, 255, 255, 255)
            canvas.drawCircle(centerX - 30, centerY - 40, 20f, paint)

            // Add accent color highlights
            val accentR = ((colors.accentColor shr 16) and 0xFF).toInt()
            val accentG = ((colors.accentColor shr 8) and 0xFF).toInt()
            val accentB = (colors.accentColor and 0xFF).toInt()
            paint.color = android.graphics.Color.argb(150, accentR, accentG, accentB)
            canvas.drawCircle(centerX + 40, centerY + 30, 15f, paint)

            // Draw subtle shadow on ring
            paint.color = android.graphics.Color.argb(50, 0, 0, 0)
            canvas.drawCircle(centerX + 40, centerY + 45, 25f, paint)

            Log.d(TAG, "Generated realistic 3D ring image for: ${ringStyle.name}")
            bitmap
        } catch (e: Exception) {
            Log.e(TAG, "Error generating ring image: ${e.message}", e)
            null
        }
    }

    /**
     * Download image from URL
     */
    private suspend fun downloadImage(urlString: String): Bitmap? = withContext(Dispatchers.IO) {
        return@withContext try {
            val url = URL(urlString)
            val connection = url.openConnection()
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.connect()

            val inputStream = connection.getInputStream()
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            Log.d(TAG, "Successfully downloaded image from: $urlString")
            bitmap
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading image: ${e.message}")
            null
        }
    }

    /**
     * Load image from local cache
     */
    private suspend fun loadFromCache(ringStyle: RingStyle): Bitmap? = withContext(Dispatchers.IO) {
        return@withContext try {
            val cacheFile = File(cacheDir, "${ringStyle.name}.png")
            if (cacheFile.exists()) {
                BitmapFactory.decodeFile(cacheFile.absolutePath)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading from cache: ${e.message}")
            null
        }
    }

    /**
     * Save image to local cache
     */
    private suspend fun saveToCache(ringStyle: RingStyle, bitmap: Bitmap) = withContext(Dispatchers.IO) {
        try {
            val cacheFile = File(cacheDir, "${ringStyle.name}.png")
            bitmap.compress(Bitmap.CompressFormat.PNG, 85, cacheFile.outputStream())
            Log.d(TAG, "Saved ring image to cache: ${cacheFile.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving to cache: ${e.message}")
        }
    }

    /**
     * Get image from memory cache
     */
    fun getImage(ringStyle: RingStyle): Bitmap? {
        return loadedImages[ringStyle.name]
    }

    /**
     * Clear all cached images
     */
    fun clearCache() {
        try {
            cacheDir.deleteRecursively()
            loadedImages.clear()
            Log.d(TAG, "Cleared ring image cache")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing cache: ${e.message}")
        }
    }
}
