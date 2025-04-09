package son.ysy.initializer.android.impl

import son.ysy.initializer.android.AndroidInitializer

public abstract class NoneParentInitializer<R> : AndroidInitializer<R>() {
    final override val parentIdList: List<String> by lazy {
        emptyList()
    }

    final override fun receiveParentResult(parentIdList: List<String>, result: Any) {
    }
}
