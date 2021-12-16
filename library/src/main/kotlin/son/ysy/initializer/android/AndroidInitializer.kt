package son.ysy.initializer.android

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

abstract class AndroidInitializer<T> : Initializer<T> {
    override val id: String = javaClass.name

    override val parentIdList: List<String> = emptyList()

    override val dispatcher: CoroutineDispatcher = Dispatchers.IO

    override val needBlockingMain: Boolean = false

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

    override fun onParentCompleted(parentId: String, result: Any) {

    }

    override fun onAllChildrenCompleted() {

    }
}