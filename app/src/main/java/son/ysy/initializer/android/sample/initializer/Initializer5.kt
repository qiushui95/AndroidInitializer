package son.ysy.initializer.android.sample.initializer

import android.app.Application
import son.ysy.initializer.android.AndroidInitializer
import kotlin.reflect.KClass

class Initializer5 : StringInitializer() {
    override val parentIdList: List<String> =
        listOf(Initializer3::class, Initializer4::class).map { it.java.name }

    override fun doInit(context: Application): String {
        Thread.sleep(5000)
        return super.doInit(context)
    }
}