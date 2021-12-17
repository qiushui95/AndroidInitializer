package son.ysy.initializer.android

import android.util.Log

internal object Logger {

    fun printLog(msg: String) {
        if (BuildConfig.DEBUG) {
            Log.d("===initializer===", msg)
        }
    }
}