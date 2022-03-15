package son.ysy.initializer.android.sample.initializer

import android.app.Application

class Initializer1 : StringInitializer() {

    override fun doSomeThing(context: Application) {
        Thread.sleep(100)
    }
}