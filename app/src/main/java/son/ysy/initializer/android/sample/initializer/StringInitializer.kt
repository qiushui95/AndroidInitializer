package son.ysy.initializer.android.sample.initializer

import android.app.Application
import android.util.Log
import son.ysy.initializer.android.AndroidInitializer
import kotlin.reflect.KClass

abstract class StringInitializer : AndroidInitializer<String>() {
    override val id: String
        get() = javaClass.simpleName

    private val list = mutableListOf<String>()

    protected open val parentClassList = emptyList<KClass<*>>()

    final override val parentIdList: List<String>
        get() = parentClassList.map { it.java.simpleName }

    override fun doInit(context: Application): String {

        Log.e("=====doInit====", "start $id,${javaClass.simpleName},thread:${Thread.currentThread().name}")
        doSomeThing(context)
        Log.e("=====doInit====", "end $id,${javaClass.simpleName},thread:${Thread.currentThread().name}")

        Log.e("=====parent params====", "$id,${javaClass.simpleName},${parentIdList}")

        return id
    }

    abstract fun doSomeThing(context: Application)

    override fun onParentCompleted(parentId: String, result: Any) {
        if (result is String) list.add(result)
    }
}