package son.ysy.initializer.android.sample.initializer

import android.app.Application
import kotlin.reflect.KClass

class Initializer2 : StringInitializer() {
    override val isAutoStart: Boolean = true

    override val runOnMainThread: Boolean = true

    override val needBlockingMain: Boolean = true

    override val parentClassList: List<KClass<*>>
        get() = listOf(Initializer1::class)

    override fun doSomeThing(context: Application) {
        Thread.sleep(3000)
    }
}