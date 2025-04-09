package son.ysy.initializer.android

import android.app.Application
import kotlinx.coroutines.Job

public abstract class AndroidInitializer<T> {
    public open val id: String = javaClass.name

    protected open val shareId: String? = null

    public val idList: List<String> by lazy {
        listOfNotNull(id, shareId)
    }

    public abstract val isAutoStart: Boolean

    internal var canAutoStart: Boolean = true

    internal val logIdStr by lazy {
        val sb = StringBuilder()

        sb.append(id)

        if (shareId != null) {
            sb.append("($shareId)")
        }

        sb.toString()
    }

    /**
     * 运行协程上下文
     */
    public open val runOnMainThread: Boolean = false

    /**
     * 是否需要阻塞主线程
     */
    public open val needBlockingMain: Boolean = false

    /**
     * 所依赖的父任务Id
     * 当所有父任务完成后才执行该任务
     */
    public abstract val parentIdList: List<String>

    /**
     * 初始化任务
     */
    public abstract suspend fun doInit(context: Application): T

    /**
     * 父任务完成回调
     */
    public abstract fun receiveParentResult(parentIdList: List<String>, result: Any)

    internal val parentInitializerSet by lazy { mutableSetOf<AndroidInitializer<*>>() }
    internal val childrenInitializerSet by lazy { mutableSetOf<AndroidInitializer<*>>() }
    internal val parentJobSet by lazy { mutableSetOf<Job>() }
}
