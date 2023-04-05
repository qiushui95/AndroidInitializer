package son.ysy.initializer.android.sample.initializer

import android.app.Application
import kotlinx.coroutines.CoroutineDispatcher
import kotlin.reflect.KClass

class Initializer9 : StringInitializer() {

    override val parentClassList: List<KClass<*>>
        get() = listOf(Initializer5::class)

    override fun doSomeThing(context: Application) {
        Thread.sleep(1000)
    }
}