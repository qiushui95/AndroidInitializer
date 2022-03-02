package son.ysy.initializer.android

import kotlin.reflect.KClass

abstract class AndroidInitializer<T> : Initializer<T> {

    override val id: String = javaClass.name

    override val needRunOnMain: Boolean = false

    override val needBlockingMain: Boolean = false
}