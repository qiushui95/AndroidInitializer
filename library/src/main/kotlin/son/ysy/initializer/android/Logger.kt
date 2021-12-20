package son.ysy.initializer.android

import android.util.Log

internal object Logger {

    fun printLog(msg: String) {
        if (InitializerCache.config.needPrintLog) {
            Log.d("===initializer===", msg)
        }
    }
}