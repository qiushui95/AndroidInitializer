package son.ysy.initializer.android.impl

import son.ysy.initializer.android.AndroidInitializer

abstract class NoneParentInitializer<R> : AndroidInitializer<R>() {

    final override val parentIdList: List<String> by lazy {
        emptyList()
    }

    final override fun onParentCompleted(parentId: String, parentShareId: String?, result: Any) {

    }
}