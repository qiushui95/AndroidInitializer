package son.ysy.initializer.android.sample.initializer.share

import android.app.Application
import kotlinx.coroutines.Dispatchers
import son.ysy.initializer.android.sample.initializer.StringInitializer
import kotlin.coroutines.CoroutineContext

abstract class BaseShareIdInitializer : StringInitializer() {
    private companion object {
        val limitDispatcher = Dispatchers.IO.limitedParallelism(1)
    }

    override val shareId: String = "shareId"

    override val dispatcher: CoroutineContext = limitDispatcher

    override fun doSomeThing(context: Application) {

        Thread.sleep(500)
    }
}