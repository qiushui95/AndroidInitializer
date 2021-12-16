package son.ysy.initializer.android.sample.initializer

import android.app.Application

class Initializer5 : StringInitializer() {

    override val parentIdList: List<String> = listOf(
        Initializer3::class.java.name,
        Initializer4::class.java.name,
    )

    override fun doInit(context: Application): String {
        Thread.sleep(5000)
        return super.doInit(context)
    }
}