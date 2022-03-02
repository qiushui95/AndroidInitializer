package son.ysy.initializer.android.sample.initializer

import android.app.Application
import android.util.Log
import son.ysy.initializer.android.AndroidInitializer

abstract class StringInitializer : AndroidInitializer<String>() {
    private val list = mutableListOf<String>()

    override fun doInit(context: Application): String {
        Log.e("====doInit ${javaClass.simpleName}=====", list.toString())
        return javaClass.simpleName
    }

    override fun onParentCompleted(parentId: String, result: Any) {
        if (result is String) list.add(result)
    }
}