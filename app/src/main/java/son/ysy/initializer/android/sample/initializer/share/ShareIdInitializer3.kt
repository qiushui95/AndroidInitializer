package son.ysy.initializer.android.sample.initializer.share

import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.CoroutineContext

class ShareIdInitializer3:BaseShareIdInitializer() {

    override val dispatcher: CoroutineContext =Dispatchers.Main
}