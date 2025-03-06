package gurray.demo.mediaprojection

import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import gurray.demo.mediaprojection.service.ScreenShotService
import gurray.demo.mediaprojection.ui.theme.MediaProjectionDemoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val mediaProjectionManager = getSystemService(MediaProjectionManager::class.java)
        // requesting permissions from user
        val startMediaProjection = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                // starting screen capturing services
                // everytime running, there wil be a user-permission requesting
                startScreenCaptureService(result.resultCode, result.data!!)
            }
        }

        setContent {
            MediaProjectionDemoTheme{
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(
                        modifier = Modifier.padding(innerPadding)
                            .fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Button(
                            onClick = {
                                val screenCaptureIntent =
                                    mediaProjectionManager.createScreenCaptureIntent()
                                startMediaProjection.launch(screenCaptureIntent)
                            }
                        ) { Text("ScreenShot\n开始截屏") }
                    }
                }
            }
        }
    }

    private fun startScreenCaptureService(resultCode: Int, data: Intent) {
        val serviceIntent = Intent(this, ScreenShotService::class.java).apply {
            action = ScreenShotService.Actions.START.name
            putExtra("RESULT_CODE", resultCode)
            putExtra("DATA", data)
        }
        startForegroundService(serviceIntent)
    }
}