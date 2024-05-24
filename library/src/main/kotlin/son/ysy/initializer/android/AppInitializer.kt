package son.ysy.initializer.android

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import son.ysy.initializer.android.execption.InitializerException
import son.ysy.initializer.android.provider.StartupProvider

private typealias Initializer<T> = AndroidInitializer<T>

private const val TAG_AUTO = "auto"
private const val TAG_MANUAL = "manual"

public object AppInitializer {

    private const val LOG_TAG = "--initializer--"

    private val initializerCoroutine = CoroutineScope(Dispatchers.IO)

    private val manualList = mutableListOf<Initializer<*>>()
    private fun logD(msg: String) {

        Log.d(LOG_TAG, msg)
    }

    internal fun startAutoInit(context: Application) = runBlocking {

        val initializerClassSet = discoverInitializerClass(context)

        val initializerList = doInitialize(initializerClassSet)

        checkSameId(initializerList)

        handleInitializerParentAndChildren(initializerList)

        checkCycle(initializerList)

        handleInitializerAutoStart(initializerList)

        val autoList = initializerList.filter { it.canAutoStart }
        val manualList = initializerList.filter { it.canAutoStart.not() }

        checkManualList(manualList)

        logDepth(TAG_AUTO, autoList)
        logDepth(TAG_MANUAL, manualList)

        AppInitializer.manualList.addAll(manualList)

        startInit(TAG_AUTO, this, context, autoList)
    }

    public fun startManualInit(app: Application): Job = initializerCoroutine.launch {

        startInit(TAG_MANUAL, this, app, manualList)

        manualList.clear()
    }

    private fun discoverInitializerClass(context: Context): Set<Class<*>> {
        val provider = ComponentName(context.packageName, StartupProvider::class.java.name)

        val providerInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getProviderInfo(
                provider,
                PackageManager.ComponentInfoFlags.of(PackageManager.GET_META_DATA.toLong())
            )
        } else {
            context.packageManager.getProviderInfo(provider, PackageManager.GET_META_DATA)
        }

        val classSet = mutableSetOf<Class<*>>()

        val metadata = providerInfo.metaData ?: return classSet

        val initializerValue = context.getString(R.string.initializer_start_up)
        val initializerClass = Initializer::class.java

        for (key in metadata.keySet()) {
            val value = metadata.getString(key) ?: continue

            if (value == initializerValue) {
                try {
                    val clz = Class.forName(key)

                    if (!initializerClass.isAssignableFrom(clz)) continue

                    classSet.add(clz)
                } catch (e: Exception) {

                    throw InitializerException(e)
                }
            }
        }

        return classSet
    }

    private fun MutableList<Initializer<*>>.initialize(clz: Class<*>) {

        val initializer = try {
            clz.getDeclaredConstructor().newInstance() as Initializer<*>
        } catch (e: Exception) {
            throw InitializerException(e)
        }

        add(initializer)
    }

    private fun doInitialize(initializerClassSet: Set<Class<*>>): List<Initializer<*>> {

        val list = mutableListOf<Initializer<*>>()

        for (initializerClass in initializerClassSet) {
            list.initialize(initializerClass)
        }

        return list
    }

    private fun checkSameId(initializerList: List<Initializer<*>>) {
        val sameIdMap = initializerList.groupBy { it.id }.filterValues { it.size > 1 }

        if (sameIdMap.isNotEmpty()) {
            val msgList = mutableListOf<String>()

            val sb = StringBuilder()

            for (entry in sameIdMap.entries) {
                sb.clear()

                sb.append("same id(${entry.key}) used in [")
                sb.append(entry.value.joinToString { it.javaClass.name })
                sb.append("]")

                msgList.add(sb.toString())
            }

            throw InitializerException("${msgList.joinToString("\n")}\nplease check!!")
        }
    }

    private fun getInitializerIdMap(initializerList: List<Initializer<*>>): Map<String, MutableSet<Initializer<*>>> {
        val initializerIdMap = mutableMapOf<String, MutableSet<Initializer<*>>>()

        for (initializer in initializerList) {
            for (id in initializer.idList) {
                initializerIdMap.getOrPut(id) { mutableSetOf() }.add(initializer)
            }
        }

        return initializerIdMap
    }

    private fun handleInitializerParentAndChildren(initializerList: List<Initializer<*>>) {
        val initializerIdMap = getInitializerIdMap(initializerList)

        for (initializer in initializerList) {

            for (parentId in initializer.parentIdList) {
                val parentSet = initializerIdMap[parentId]

                if (parentSet.isNullOrEmpty()) {
                    throw InitializerException.parentNoFind(parentId, initializer)
                }

                initializer.parentInitializerSet.addAll(parentSet)

                for (parentInitializer in parentSet) {
                    parentInitializer.childrenInitializerSet.add(initializer)
                }
            }
        }
    }

    private fun handleInitializerAutoStart(initializerList: List<Initializer<*>>) {
        for (initializer in initializerList) {
            initializer.canAutoStart = getInitialAutoStart(initializer)
        }
    }

    private fun getInitialAutoStart(initializer: Initializer<*>): Boolean {
        if (initializer.isAutoStart.not()) return false

        for (parentInitializer in initializer.parentInitializerSet) {
            if (parentInitializer.isAutoStart.not()) return false
        }

        return true
    }

    private fun checkCycle(initializerList: List<Initializer<*>>) {
        for (initializer in initializerList) {
            checkCycle(initializer.idList, initializer, listOf(initializer))
        }
    }

    private fun checkCycle(
        idList: List<String>,
        initializer: Initializer<*>,
        dependencyList: List<Initializer<*>>
    ) {

        for (parentInitializer in initializer.parentInitializerSet) {
            val list = mutableListOf<Initializer<*>>()

            list.addAll(dependencyList)
            list.add(parentInitializer)

            if (parentInitializer.idList.any { it in idList }) {
                throw InitializerException("存在环依赖,依赖路径:${list.joinToString("->") { it.javaClass.name }}")
            }

            checkCycle(idList, parentInitializer, list)
        }
    }

    private suspend fun startInit(
        tag: String,
        startScope: CoroutineScope,
        context: Application,
        initializerList: List<Initializer<*>>,
    ) {

        val isManual = tag == TAG_MANUAL

        initializerList.map { it to createInitJob(isManual, startScope, context, it) }
            .onEach { (initializer, job) ->
                initializer.childrenInitializerSet.forEach { it.parentJobSet.add(job) }
            }.onEach { (_, job) -> job.start() }
            .filter { (initializer, _) -> initializer.needBlockingMain || isManual }
            .forEach { (_, job) -> job.join() }

        logD("all $tag blocking main finish")
    }

    private fun createInitJob(
        isManual: Boolean,
        startScope: CoroutineScope,
        context: Application,
        initializer: Initializer<*>
    ): Job = initializerCoroutine.launch(Dispatchers.Default, start = CoroutineStart.LAZY) {
        for (job in initializer.parentJobSet) {
            job.join()
        }

        val initContext = if (initializer.runOnMainThread && isManual) {
            Dispatchers.Main
        } else if (initializer.runOnMainThread) {
            startScope.coroutineContext
        } else {
            Dispatchers.IO
        }

        val startTime = System.currentTimeMillis()

        val initializerKeyStr = "${initializer.id}(${initializer.javaClass.name})"

        val initResult = withContext(initContext) {
            val threadNameStr = "thread:${Thread.currentThread().name}"
            logD("start:${initializerKeyStr}-->$threadNameStr")
            initializer.doInit(context)
        }

        val costTimeStr = "cost:${System.currentTimeMillis() - startTime}ms"

        logD("finish:${initializerKeyStr}-->$costTimeStr")

        for (childInitializer in initializer.childrenInitializerSet) {
            childInitializer.receiveParentResult(initializer.idList, initResult ?: Unit)
        }
    }

    private fun checkManualList(list: List<Initializer<*>>) {
        for (initializer in list) {
            if (initializer.needBlockingMain) {
                throw InitializerException("manual initializer can not block main thread(${initializer.javaClass.name})")
            }
        }
    }

    private fun logDepth(tag: String, initializerList: List<Initializer<*>>) {

        val allList = initializerList.toMutableList()

        var depth = 0

        while (allList.size > 0) {

            val curDepthList = mutableListOf<Initializer<*>>()

            for (initializer in allList) {

                if (initializer.parentInitializerSet.none { it in allList }) {
                    curDepthList.add(initializer)
                }
            }


            allList.removeAll(curDepthList)


            val curDepthIdStr = curDepthList.joinToString(",") { it.id }

            logD("depth($tag):${depth++}-->[$curDepthIdStr]")
        }
    }
}