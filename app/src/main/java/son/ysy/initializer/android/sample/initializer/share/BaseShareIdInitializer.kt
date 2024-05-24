package son.ysy.initializer.android.sample.initializer.share

import android.app.Application
import son.ysy.initializer.android.sample.initializer.StringInitializer

abstract class BaseShareIdInitializer : StringInitializer() {

    override val shareId: String = "shareId"

    override val runOnMainThread: Boolean = true

    override fun doSomeThing(context: Application) {

        Thread.sleep(500)
    }
}