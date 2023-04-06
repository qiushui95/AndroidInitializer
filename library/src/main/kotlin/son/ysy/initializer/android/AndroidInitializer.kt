package son.ysy.initializer.android

import android.app.Application
import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.CoroutineContext

abstract class AndroidInitializer<T> {

    open val id: String = javaClass.name

    open val shareId: String? = null

    val idList by lazy {
        listOfNotNull(id, shareId)
    }

    internal val logIdStr by lazy {
        val sb = StringBuilder()

        sb.append(id)

        if (shareId != null) {
            sb.append("($shareId)")
        }

        sb.toString()
    }

    /**
     * 组名
     */
    open val groupName: String = "default"

    /**
     * 组内排序参数,升序排列
     */
    open val groupSort: Int = 0

    /**
     * 运行协程上下文
     */
    open val dispatcher: CoroutineContext = Dispatchers.IO

    /**
     * 是否需要阻塞主线程
     */
    open val needBlockingMain: Boolean = false

    /**
     * 所依赖的父任务Id
     * 当所有父任务完成后才执行该任务
     */
    abstract val parentIdList: List<String>

    /**
     * 初始化任务
     */
    abstract fun doInit(context: Application): T

    /**
     * 父任务完成回调
     */
    abstract fun onParentCompleted(parentIdList: List<String>, result: Any)

    internal val parentInitializerSet = mutableSetOf<AndroidInitializer<*>>()

    internal val childrenInitializerSet = mutableSetOf<AndroidInitializer<*>>()
}