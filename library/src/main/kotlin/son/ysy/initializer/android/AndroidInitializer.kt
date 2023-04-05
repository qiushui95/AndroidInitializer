package son.ysy.initializer.android

import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.CoroutineContext

abstract class AndroidInitializer<T> : Initializer<T> {

    override val id: String = javaClass.name

    override val dispatcher: CoroutineContext = Dispatchers.IO

    override val needBlockingMain: Boolean = false
}