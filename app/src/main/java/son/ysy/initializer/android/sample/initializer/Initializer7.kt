package son.ysy.initializer.android.sample.initializer

import android.app.Application
import android.util.Log
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import son.ysy.initializer.android.AndroidInitializer
import kotlin.reflect.KClass

class Initializer7 : StringInitializer() {
    override val priority: Int = 100

    override val groupName: String = "1024"

    override val parentClassList: List<KClass<*>>
        get() = listOf(Initializer5::class)

    override fun doSomeThing(context: Application) {
        Thread.sleep(1000)
    }
}