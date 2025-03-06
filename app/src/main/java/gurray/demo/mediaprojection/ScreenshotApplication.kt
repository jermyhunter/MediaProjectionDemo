package gurray.demo.mediaprojection

import android.app.Application
import androidx.lifecycle.ViewModelProvider

class ScreenshotApplication: Application() {
    val screenshotViewModel: ScreenshotViewModel by lazy {
        ViewModelProvider.AndroidViewModelFactory.getInstance(this).create(ScreenshotViewModel::class.java)
    }
}