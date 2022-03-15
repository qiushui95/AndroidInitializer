package son.ysy.initializer.android.sample.initializer

import android.app.Application
import son.ysy.initializer.android.AndroidInitializer
import kotlin.reflect.KClass

class Initializer5 : StringInitializer() {
    override val parentClassList: List<KClass<*>>
        get() = listOf(Initializer3::class, Initializer4::class)

    override val needBlockingMain: Boolean = true

    override fun doSomeThing(context: Application) {
        Thread.sleep(2000)
    }
}