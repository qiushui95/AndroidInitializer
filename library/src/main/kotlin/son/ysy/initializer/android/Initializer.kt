package son.ysy.initializer.android

import android.app.Application
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

internal interface Initializer<T> {

    val id: String

    /**
     * 优先级,值越小越优先
     */
    val priority: Int

    /**
     * 组名
     * 同一组内优先级值越小越优先
     */
    val groupName: String?

    /**
     * 所依赖的父任务class
     * 当所有父任务完成后才执行该任务
     */
    val parentIdList: List<String>

    /**
     * 是否在主线程运行
     */
    val needRunOnMain: Boolean

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
}