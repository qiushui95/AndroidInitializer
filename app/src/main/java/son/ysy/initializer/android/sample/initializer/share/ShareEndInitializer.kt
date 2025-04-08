package son.ysy.initializer.android.sample.initializer.share

import android.app.Application
import android.util.Log
import son.ysy.initializer.android.impl.ManyParentInitializer

class ShareEndInitializer : ManyParentInitializer<Unit>() {
    override val isAutoStart: Boolean = true
    override fun getParentIdList(): Sequence<String> {
        return sequenceOf("shareId")
    }

    override suspend fun doInit(context: Application) {
        Log.e("=====doInit==== ", "--start--")
        Thread.sleep(1000)
        Log.e("=====doInit==== ", "---end---")

    }

    override fun receiveParentResult(parentIdList: List<String>, result: Any) {

    }
}