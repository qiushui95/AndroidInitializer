package son.ysy.initializer.android.sample.initializer

import android.app.Application
import kotlin.reflect.KClass

class Initializer7 : StringInitializer() {


    override val groupName: String = "group"
    override val groupSort: Int = 3

    override val parentClassList: List<KClass<*>>
        get() = listOf(Initializer5::class)

    override fun doSomeThing(context: Application) {
        Thread.sleep(1000)
    }
}