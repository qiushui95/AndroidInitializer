package son.ysy.initializer.android.sample.initializer

import android.app.Application
import son.ysy.initializer.android.AndroidInitializer

class SampleInitializer : AndroidInitializer<String>() {

    override val needRunOnMain: Boolean = super.needRunOnMain
    override val needBlockingMain: Boolean = super.needBlockingMain

    override fun onParentCompleted(parentId: String, result: Any) {
        super.onParentCompleted(parentId, result)
    }

    override fun onAllChildrenCompleted() {
        super.onAllChildrenCompleted()
    }

    override fun doInit(context: Application): String {
        Thread.sleep(3000)

        return "SampleInitializer.doInit执行完毕"
    }
}