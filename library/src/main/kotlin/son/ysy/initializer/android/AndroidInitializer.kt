package son.ysy.initializer.android

import kotlin.reflect.KClass

abstract class AndroidInitializer<T> : Initializer<T> {

    override val id: String = javaClass.name

    override val needRunOnMain: Boolean = false

    override val needBlockingMain: Boolean = false

    final override val parentClassNameList: List<String> by lazy {
        getParentClassName().distinct()
    }

    protected inline fun <reified R> checkParentResult(
        parentId: String,
        result: Any,
        isParentIdCorCorrect: (String) -> Boolean = { true },
        block: (R) -> Unit
    ) {
        if (isParentIdCorCorrect(parentId) && result is R) {
            block(result)
        }
    }

    protected open fun getParentClassName(): List<String> {
        return getParentClassList().map { it.name }
    }

    protected open fun getParentClassList(): List<Class<out AndroidInitializer<*>>> {
        return getParentKClassList().map { it.java }
    }

    protected open fun getParentKClassList(): List<KClass<out AndroidInitializer<*>>> {
        return emptyList()
    }

    override fun onParentCompleted(parentId: String, result: Any) {

    }

    override fun onAllChildrenCompleted() {

    }

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return javaClass == other?.javaClass
    }
}