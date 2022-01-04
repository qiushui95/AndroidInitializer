package son.ysy.initializer.android.sample.initializer

import android.app.Application
import son.ysy.initializer.android.AndroidInitializer
import kotlin.reflect.KClass

class Initializer5 : StringInitializer() {

    override fun doInit(context: Application): String {
        Thread.sleep(5000)
        return super.doInit(context)
    }

    override fun getParentKClassList(): List<KClass<out AndroidInitializer<*>>> {
        return listOf(Initializer3::class, Initializer4::class)
    }
}