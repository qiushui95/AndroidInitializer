package son.ysy.initializer.android.sample

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import son.ysy.initializer.android.AppInitializer

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        Log.e("=======", "MainActivity show")

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val tvText = findViewById<TextView>(R.id.tvText)

        lifecycleScope.launch {
            repeat(10) {
                tvText.text = "${it} times"
                delay(1000)
            }
        }

        AppInitializer.startManualInit(application)
    }
}