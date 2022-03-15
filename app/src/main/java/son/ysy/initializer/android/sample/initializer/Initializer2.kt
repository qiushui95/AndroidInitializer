package son.ysy.initializer.android.sample.initializer

import android.app.Application
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlin.reflect.KClass

class Initializer2 : StringInitializer() {

    override val needRunOnMain: Boolean = true

    override val needBlockingMain: Boolean = true

    override val parentClassList: List<KClass<*>>
        get() = listOf(Initializer1::class)

    override fun doSomeThing(context: Application) {
        Thread.sleep(100)
    }
}