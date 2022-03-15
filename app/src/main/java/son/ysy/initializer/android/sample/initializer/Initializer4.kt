package son.ysy.initializer.android.sample.initializer

import android.app.Application
import kotlin.reflect.KClass

class Initializer4 : StringInitializer() {
    override val parentClassList: List<KClass<*>>
        get() = listOf(Initializer2::class, Initializer3::class)

    override fun doSomeThing(context: Application) {
        Thread.sleep(100)
    }
}