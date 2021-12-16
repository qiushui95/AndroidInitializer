package son.ysy.initializer.android.sample.initializer

import android.content.Context
import kotlinx.coroutines.CoroutineDispatcher
import son.ysy.initializer.android.AndroidInitializer

class SampleInitializer : AndroidInitializer<String>() {
    override val id: String = super.id
    override val parentIdList: List<String> = super.parentIdList
    override val dispatcher: CoroutineDispatcher = super.dispatcher
    override val needBlockingMain: Boolean = super.needBlockingMain

    override fun onParentCompleted(parentId: String, result: Any) {
        super.onParentCompleted(parentId, result)
    }

    override fun onAllChildrenCompleted() {
        super.onAllChildrenCompleted()
    }

    override fun doInit(context: Context): String {
        Thread.sleep(3000)

        return "SampleInitializer.doInit执行完毕"
    }
}