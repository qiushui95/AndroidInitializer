package son.ysy.initializer.android

import android.app.Application
import kotlinx.coroutines.CoroutineDispatcher

interface Initializer<T> {
    val id: String

    /**
     * 需要依赖的上级任务id
     */
    val parentIdList: List<String>

    /**
     * 运行的协程Dispatcher
     */
    val dispatcher: CoroutineDispatcher

    /**
     * 是否需要阻塞主线程
     */
    val needBlockingMain: Boolean

    /**
     * 初始化任务
     */
    fun doInit(context: Application): T

    /**
     * 父任务完成回调
     */
    fun onParentCompleted(parentId: String, result: Any)

    /**
     * 所有子任务完成回调
     */
    fun onAllChildrenCompleted()
}