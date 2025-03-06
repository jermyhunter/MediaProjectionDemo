package gurray.demo.mediaprojection

import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

// transmit bitmap data from ScreenShotService to MainActivity
class ScreenshotViewModel: ViewModel() {
    private val _screenshot = MutableLiveData<Bitmap>()
    val screenshot: LiveData<Bitmap> get() = _screenshot

    fun setScreenshot(bitmap: Bitmap) {
        _screenshot.value = bitmap
    }
}