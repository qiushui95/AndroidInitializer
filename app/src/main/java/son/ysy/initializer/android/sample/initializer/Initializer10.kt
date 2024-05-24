package son.ysy.initializer.android.sample.initializer

import android.app.Application
import kotlin.reflect.KClass

class Initializer10 : StringInitializer() {
    override val isAutoStart: Boolean = true

    override val parentClassList: List<KClass<*>>
        get() = listOf(Initializer5::class)

    override val parentIdSequence: Sequence<String>
        get() = sequenceOf("shareId")

    override fun doSomeThing(context: Application) {
        Thread.sleep(1000)
    }
}