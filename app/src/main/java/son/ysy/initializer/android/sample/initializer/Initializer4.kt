package son.ysy.initializer.android.sample.initializer

import son.ysy.initializer.android.AndroidInitializer
import kotlin.reflect.KClass

class Initializer4 : StringInitializer() {

    override fun getParentKClassList(): List<KClass<out AndroidInitializer<*>>> {
        return listOf(Initializer2::class, Initializer3::class)
    }
}