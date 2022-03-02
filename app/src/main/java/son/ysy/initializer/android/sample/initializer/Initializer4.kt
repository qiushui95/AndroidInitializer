package son.ysy.initializer.android.sample.initializer

import son.ysy.initializer.android.AndroidInitializer
import kotlin.reflect.KClass

class Initializer4 : StringInitializer() {

    override val parentIdList: List<String> =
        listOf(Initializer2::class, Initializer3::class).map { it.java.name }
}