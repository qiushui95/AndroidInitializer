package son.ysy.initializer.android.sample.initializer

import son.ysy.initializer.android.AndroidInitializer
import kotlin.reflect.KClass

class Initializer2 : StringInitializer() {

    override val needRunOnMain: Boolean = true

    override val needBlockingMain: Boolean = true

    override fun getParentKClassList(): List<KClass<out AndroidInitializer<*>>> {
        return listOf(Initializer1::class)
    }
}