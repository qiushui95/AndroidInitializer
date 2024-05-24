package son.ysy.initializer.android.impl

import son.ysy.initializer.android.AndroidInitializer

public abstract class SingleParentInitializer<R, P : Any> : AndroidInitializer<R>() {

    protected abstract val parentId: String

    protected lateinit var parentResult: P

    final override val parentIdList: List<String> by lazy {
        listOf(parentId)
    }

    @Suppress("UNCHECKED_CAST")
    override fun receiveParentResult(parentIdList: List<String>, result: Any) {
        if (parentId in parentIdList) {
            parentResult = result as P
        }
    }
}