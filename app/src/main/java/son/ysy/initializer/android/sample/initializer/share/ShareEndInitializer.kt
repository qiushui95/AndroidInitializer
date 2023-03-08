package son.ysy.initializer.android.sample.initializer.share

import android.app.Application
import android.util.Log
import son.ysy.initializer.android.impl.ManyParentInitializer

class ShareEndInitializer : ManyParentInitializer<Unit>() {
    override fun getParentIdList(): Sequence<String> {
        return sequenceOf("shareId")
    }

    override fun doInit(context: Application) {
        Log.e("=====doInit==== ", "--start--")
        Thread.sleep(1000)
        Log.e("=====doInit==== ", "---end---")

    }

    override fun onParentCompleted(parentId: String, result: Any) {

    }
}