package gurray.demo.mediaprojection.service

import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Bitmap.*
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.app.NotificationCompat
import androidx.lifecycle.ViewModelProvider
import gurray.demo.mediaprojection.R
import gurray.demo.mediaprojection.ScreenshotApplication
import gurray.demo.mediaprojection.ScreenshotViewModel
import java.io.ByteArrayOutputStream


/**
 * WARNING:
 *     in Android 14+, mediaProjection must run in foregroundService, or may encountering Security Exception
 *     permission-requesting and getting MediaProjectionManager MUST BE BEFORE starting foregroundService, or may encountering Security Exception
 *
 *  the whole process:
 *      setting up FOREGROUND_SERVICE_MEDIA_PROJECTION and service in AndroidManifest
 *      getting mediaProjection from MediaProjectionManager
 *      register callbacks(mediaProjectionCallback) to mediaProjection
 *
 *  (permission requesting is already done in MainActivity in this project)
 * */
/**
 * WARNING:
 *     in Android 14+, mediaProjection must run in foregroundService, or may encountering Security Exception
 *     permission-requesting and getting MediaProjectionManager MUST BE BEFORE starting foregroundService, or may encountering Security Exception
 *
 *  the whole process:
 *      setting up FOREGROUND_SERVICE_MEDIA_PROJECTION and service in AndroidManifest
 *      getting mediaProjection from MediaProjectionManager
 *      register callbacks(mediaProjectionCallback) to mediaProjection
 *
 *  (permission requesting is already done in MainActivity in this project)
 * */
class ScreenShotService: Service(){
    private lateinit var screenshotViewModel: ScreenshotViewModel

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private lateinit var mediaProjectionManager: MediaProjectionManager

    private val mediaProjectionCallback = object : MediaProjection.Callback() {
        override fun onStop() {
            super.onStop()
            Log.d("MediaProjection", "MediaProjection stopped, releasing resources.")
            releasingSources()
        }
    }

    override fun onCreate() {
        super.onCreate()
        // must be before starting foregroundService
        mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        // starting foregroundService, instead of service
        startForegroundService()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // get ViewModel
        screenshotViewModel = (application as ScreenshotApplication).screenshotViewModel

        when (intent?.action) {
            Actions.START.name -> startScreenCapture(intent)
        }

        return super.onStartCommand(intent, flags, startId)
    }

    private fun startScreenCapture(intent: Intent?) {
        val resultCode = intent?.getIntExtra("RESULT_CODE", Activity.RESULT_CANCELED)
        val data = intent?.getParcelableExtra<Intent>("DATA")

        if (resultCode == Activity.RESULT_OK && data != null) {
            mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data)
            // warning!!! unregistered callback make result in SecurityException
            mediaProjection?.registerCallback(mediaProjectionCallback, Handler(Looper.getMainLooper()))

            // using VirtualDisplay to get screenshot
            setupVirtualDisplay() //
        }
    }

    // register imageReader to virtualDisplay
    private fun setupVirtualDisplay() {
        val displayMetrics = Resources.getSystem().displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels
        val screenDensity = displayMetrics.densityDpi

        // creating ImageReader
        imageReader = ImageReader.newInstance(screenWidth, screenHeight, PixelFormat.RGBA_8888, 2)

        // postpone 1s to get the right image after alertdialog disappear
        imageReader?.setOnImageAvailableListener({ reader ->
            Handler(Looper.getMainLooper()).postDelayed({
                val image = reader.acquireLatestImage()
                image?.let {
                    val bitmap = imageToBitmap(it)
                    // TODO: image processing here, do as your will
                    screenshotViewModel.setScreenshot(bitmap)

                    image.close()
                    // equals STOP
                    // releasing sources to stop recording to achieve screenshot use
                    releasingSources()
                }
            }, 1000)
        }, Handler(Looper.getMainLooper()))

        // creating VirtualDisplay and attach to mediaProjection
        // for debugging: if virtualDisplay == null, the capturing process may occur errors
        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "ScreenCapture",
            screenWidth, screenHeight, screenDensity,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface, null, null
        )

        Log.d("MediaProjection", "VirtualDisplay created: $screenWidth x $screenHeight")
    }

    private fun imageToBitmap(image: Image): Bitmap {
        val planes = image.planes
        val buffer = planes[0].buffer
        val pixelStride = planes[0].pixelStride
        val rowStride = planes[0].rowStride
        val rowPadding = rowStride - pixelStride * image.width

        // creating a bitmap to restore image
        val bitmap = createBitmap(
            image.width + rowPadding / pixelStride, image.height,
            Config.ARGB_8888
        )
        bitmap.copyPixelsFromBuffer(buffer)

        // transform Bitmap to ImageBitmap
        return bitmap
    }

    private fun sendBitmapToMainActivity(bitmap: Bitmap) {
        val intent = Intent("SEND_SCREENSHOT")
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        intent.putExtra("screenshot", byteArray)

        // 发送广播
        sendBroadcast(intent)
    }


    //----------------------------------------------------------------
    private fun releasingSources() {
        Log.d("MediaProjection", "Stopping MediaProjection")
        virtualDisplay?.release()
        virtualDisplay = null
        mediaProjection?.stop()
        imageReader?.close()
    }

    private fun startForegroundService() {
        val channelId = "ScreenCaptureServiceChannel"
        val channel = NotificationChannel(
            channelId, "Screen Capture Service", NotificationManager.IMPORTANCE_LOW
        )

        // warning!!! not creating a notificationChannel may result in Security Exception
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("ScreenShot")
            .setContentText("On ScreenShoting...")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
        }
        else
            startForeground(1, notification)
    }

    enum class Actions {
        START
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }
}