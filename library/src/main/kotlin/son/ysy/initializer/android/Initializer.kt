package son.ysy.initializer.android

import android.app.Application

internal interface Initializer<T> {

    val id: String

    /**
     * 所依赖的父任务class
     * 当所有父任务完成后才执行该任务
     */
    val parentIdList: List<String>

    /**
     * 是否运行在主线程
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