package son.ysy.initializer.android.impl

import son.ysy.initializer.android.AndroidInitializer
import kotlin.reflect.KClass

public abstract class ManyParentInitializer<R> : AndroidInitializer<R>() {
    final override val parentIdList: List<String> by lazy {
        val idList = getParentIdList()
        val clzList = getParentClassList()
        val clzListK = getParentKClassList().map { it.java }

        (idList + (clzList + clzListK).map { it.name }).distinct().toList()
    }

    protected open fun getParentIdList(): Sequence<String> {
        return emptySequence()
    }

    protected open fun getParentClassList(): Sequence<Class<*>> {
        return emptySequence()
    }

    protected open fun getParentKClassList(): Sequence<KClass<*>> {
        return emptySequence()
    }
}
