package son.ysy.initializer.android.sample.initializer

import android.app.Application
import kotlin.reflect.KClass

class Initializer5 : StringInitializer() {
    override val isAutoStart: Boolean = false
    override val parentClassList: List<KClass<*>>
        get() = listOf(Initializer3::class, Initializer4::class)

    override fun doSomeThing(context: Application) {
        Thread.sleep(2000)
    }
}