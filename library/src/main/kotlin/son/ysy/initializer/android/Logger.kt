package son.ysy.initializer.android

import android.util.Log

internal object Logger {

    fun printLog(tag: String, msg: String) {
        if (InitializerCache.config.needPrintLog) {
            Log.d("===initializer $tag===", msg)
        }
    }
}