package son.ysy.initializer.android

abstract class AndroidInitializer<T> : Initializer<T> {

    override val id: String = javaClass.name

    override val priority: Int = Int.MAX_VALUE

    override val groupName: String? = null

    override val needRunOnMain: Boolean = false

    override val needBlockingMain: Boolean = false
}