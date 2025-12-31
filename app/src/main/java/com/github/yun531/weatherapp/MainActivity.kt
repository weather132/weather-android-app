package com.github.yun531.weatherapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.core.content.ContextCompat
import com.github.yun531.weatherapp.ui.AppNav

class MainActivity : ComponentActivity() {

    private val requestNotiPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            // 필요하면 여기서 로그 추가 가능
            // android.util.Log.d("NOTI", "POST_NOTIFICATIONS granted=$granted")
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Android 13+(API 33)부터 알림 권한 런타임 요청 필요
        if (Build.VERSION.SDK_INT >= 33) {
            val granted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!granted) {
                requestNotiPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            MaterialTheme {
                Surface {
                    AppNav()
                }
            }
        }
    }
}