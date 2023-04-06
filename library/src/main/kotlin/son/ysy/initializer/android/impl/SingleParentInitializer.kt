package son.ysy.initializer.android.impl

import son.ysy.initializer.android.AndroidInitializer

abstract class SingleParentInitializer<R, P : Any> : AndroidInitializer<R>() {

    abstract val parentId: String

    protected lateinit var parentResult: P

    final override val parentIdList: List<String> by lazy {
        listOf(parentId)
    }

    @Suppress("UNCHECKED_CAST")
    final override fun onParentCompleted(parentIdList: List<String>, result: Any) {
        if (parentId in parentIdList) {
            parentResult = result as P
        }
    }
}