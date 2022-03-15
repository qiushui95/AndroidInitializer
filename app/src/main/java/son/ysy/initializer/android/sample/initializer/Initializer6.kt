package son.ysy.initializer.android.sample.initializer

import android.app.Application
import kotlinx.coroutines.CoroutineDispatcher
import kotlin.reflect.KClass

class Initializer6 : StringInitializer() {
    override val priority: Int = 10

    override val groupName: String = "1024"

    override val parentClassList: List<KClass<*>>
        get() = listOf(Initializer5::class)

    override fun doSomeThing(context: Application) {
        Thread.sleep(1000)
    }
}