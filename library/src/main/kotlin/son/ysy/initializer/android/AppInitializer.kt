package son.ysy.initializer.android

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import kotlinx.coroutines.*
import son.ysy.initializer.android.execption.InitializerException
import son.ysy.initializer.android.provider.StartupProvider
import kotlin.coroutines.CoroutineContext

internal object AppInitializer {

    private const val LOG_TAG = "--initializer--"

    private val initializerCoroutine = CoroutineScope(Dispatchers.Main)

    fun startInit(context: Application) = runBlocking {

        val initializerClassSet = discoverInitializerClass(context)

        val initializerList = doInitialize(initializerClassSet)

        checkSameId(initializerList)

        val initializerIdMap = getInitializerIdMap(initializerList)

        findParent(initializerList, initializerIdMap)

        checkCycle(initializerList)

        logDepth(initializerList)

        val jobMap = initializerCoroutine
            .prepareJob(context, coroutineContext, initializerList)

        jobMap.filterKeys { it.needBlockingMain }
            .values
            .forEach {
                it.join()
            }
    }

    private fun discoverInitializerClass(context: Context): Set<Class<*>> {
        val provider = ComponentName(context.packageName, StartupProvider::class.java.name)

        val providerInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getProviderInfo(
                provider,
                PackageManager.ComponentInfoFlags.of(PackageManager.GET_META_DATA.toLong())
            )
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getProviderInfo(
                provider,
                PackageManager.GET_META_DATA
            )
        }

        val classSet = mutableSetOf<Class<*>>()

        val metadata = providerInfo.metaData ?: return classSet

        val initializerValue = context.getString(R.string.initializer_start_up)
        val initializerClass = AndroidInitializer::class.java

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

    private fun MutableList<AndroidInitializer<*>>.initialize(clz: Class<*>) {

        val initializer = try {
            clz.getDeclaredConstructor().newInstance() as AndroidInitializer<*>
        } catch (e: Exception) {
            throw InitializerException(e)
        }

        add(initializer)
    }

    private fun doInitialize(initializerClassSet: Set<Class<*>>): List<AndroidInitializer<*>> {

        val list = mutableListOf<AndroidInitializer<*>>()

        for (initializerClass in initializerClassSet) {
            list.initialize(initializerClass)
        }

        return list
    }

    private fun getInitializerIdMap(initializerList: List<AndroidInitializer<*>>): Map<String, Set<AndroidInitializer<*>>> {
        val resultMap = mutableMapOf<String, MutableSet<AndroidInitializer<*>>>()

        for (initializer in initializerList) {
            for (id in initializer.idList) {
                resultMap.getOrPut(id) { mutableSetOf() }.add(initializer)
            }
        }

        return resultMap
    }

    private fun checkSameId(initializerList: List<AndroidInitializer<*>>) {
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

    private fun findParent(
        initializerList: List<AndroidInitializer<*>>,
        initializerIdMap: Map<String, Set<AndroidInitializer<*>>>
    ) {

        for (initializer in initializerList) {
            for (parentId in initializer.parentIdList) {

                val parentInitializerSet = initializerIdMap[parentId]

                if (parentInitializerSet == null) {
                    throw InitializerException("initializer not find which id is '$parentId',${initializer::class.qualifiedName} need it.")
                } else {
                    initializer.parentInitializerSet.addAll(parentInitializerSet)
                }
            }
        }

        val groupMap = initializerList.groupBy { it.groupName }

        for (list in groupMap.values) {
            for (initializer in list) {
                for (beforeInitializer in list.filter { it.groupSort < initializer.groupSort }) {
                    initializer.parentInitializerSet.add(beforeInitializer)
                }
            }
        }

        for (initializer in initializerList) {
            for (parentInitializer in initializer.parentInitializerSet) {
                parentInitializer.childrenInitializerSet.add(initializer)
            }
        }
    }

    private fun checkCycle(initializerList: List<AndroidInitializer<*>>) {
        for (initializer in initializerList) {
            checkInitializerCycle(initializer.id, initializer, listOf(initializer))
        }
    }

    private fun checkInitializerCycle(
        initializerId: String,
        initializer: AndroidInitializer<*>,
        dependencyList: List<AndroidInitializer<*>>
    ) {

        for (parentInitializer in initializer.parentInitializerSet) {
            val list = mutableListOf<AndroidInitializer<*>>()

            list.addAll(dependencyList)
            list.add(parentInitializer)

            if (parentInitializer.id == initializerId) {
                throw InitializerException("存在环依赖,依赖路径:${list.joinToString("->") { it.javaClass.name }}")
            }

            checkInitializerCycle(initializerId, parentInitializer, list)
        }
    }

    private fun CoroutineScope.prepareJob(
        context: Application,
        mainContext: CoroutineContext,
        initializerList: List<AndroidInitializer<*>>,
    ): Map<AndroidInitializer<*>, Job> {

        val resultMap = mutableMapOf<AndroidInitializer<*>, Job>()

        initializerList.forEach { initializer ->

            val job = launch(Dispatchers.IO, start = CoroutineStart.LAZY) {

                for (parentInitializer in initializer.parentInitializerSet) {
                    resultMap[parentInitializer]?.join()
                }

                val coroutineContext = if (initializer.dispatcher == Dispatchers.Main) {
                    mainContext
                } else {
                    initializer.dispatcher
                }

                val startTime = System.currentTimeMillis()

                val initializerKeyStr = "${initializer.id}(${initializer.javaClass.name})"

                val initResult = withContext(coroutineContext) {
                    val threadNameStr = "thread:${Thread.currentThread().name}"

                    Log.d(LOG_TAG, "start:${initializerKeyStr}-->$threadNameStr")

                    initializer.doInit(context)
                }

                val costTimeStr = "cost:${System.currentTimeMillis() - startTime}ms"

                Log.d(LOG_TAG, "finish:${initializerKeyStr}-->$costTimeStr")

                for (childInitializer in initializer.childrenInitializerSet) {
                    childInitializer.onParentCompleted(
                        parentId = initializer.id,
                        parentShareId = initializer.shareId,
                        result = initResult ?: Unit
                    )
                }
            }

            resultMap[initializer] = job
        }

        resultMap.values.forEach { it.start() }

        return resultMap
    }

    private fun logDepth(initializerList: List<AndroidInitializer<*>>) {

        val allList = initializerList.toMutableList()

        var depth = 0

        while (allList.size > 0) {

            val curDepthList = mutableListOf<AndroidInitializer<*>>()

            for (initializer in allList) {

                if (initializer.parentInitializerSet.none { it in allList }) {
                    curDepthList.add(initializer)
                }
            }


            allList.removeAll(curDepthList)


            val curDepthIdStr = curDepthList.joinToString(",") { it.id }

            Log.d(LOG_TAG, "depth:${depth}(id)-->[$curDepthIdStr]")

            val curDepthStr = curDepthList.joinToString(",") { it.javaClass.name }

            Log.d(LOG_TAG, "depth:${depth++}(class)-->[$curDepthStr]")
        }
    }
}