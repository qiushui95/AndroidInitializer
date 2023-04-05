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

        val initializerMap = doInitialize(initializerClassSet)

        checkInitializer(initializerMap)

        val parentMap = dealInitializerParent(initializerMap)

        val childrenMap = dealInitializerChildren(initializerMap)

        checkCycle(initializerMap, parentMap)

        logDepth(initializerMap, parentMap)

        val jobMap = initializerCoroutine
            .prepareJob(context, coroutineContext, initializerMap, parentMap, childrenMap)

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

    private fun doInitialize(initializerClassSet: Set<Class<*>>): Map<String, List<Initializer<*>>> {

        val list = mutableListOf<Initializer<*>>()

        for (initializerClass in initializerClassSet) {
            list.initialize(initializerClass)
        }

        list.sortBy { it.priority }

        val result = mutableMapOf<String, MutableList<Initializer<*>>>()

        list.forEach {
            result.getOrPut(it.id) { mutableListOf() }.add(it)
        }

        return result
    }

    private fun checkInitializer(initializerMap: Map<String, List<Initializer<*>>>) {

        for (initializer in initializerMap.values.flatten()) {

            for (parentId in initializer.parentIdList) {
                if (!initializerMap.containsKey(parentId)) {
                    throw InitializerException("initializer not find which id is '$parentId',${initializer::class.qualifiedName} need it.")
                }
            }
        }

        for (initializer in initializerMap.filterValues { it.size > 1 }.values.flatten()) {

            if (initializer !is ShareIdInitializer) {
                val sameIdInitializerStr = initializerMap[initializer.id]
                    ?.filterNot { it == initializer }
                    ?.joinToString(separator = ",") { it.javaClass.name }

                throw InitializerException("initializer(${initializer.javaClass.name}) have same id with [$sameIdInitializerStr],if you want to use same id,please implements ShareIdInitializer.")
            }
        }

    }

    private fun MutableList<Initializer<*>>.initialize(clz: Class<*>) {

        val initializer = try {
            clz.getDeclaredConstructor().newInstance() as Initializer<*>
        } catch (e: Exception) {
            throw InitializerException(e)
        }

        add(initializer)
    }

    private fun dealInitializerParent(initializerMap: Map<String, List<Initializer<*>>>): Map<Initializer<*>, List<Initializer<*>>> {
        val result = mutableMapOf<Initializer<*>, List<Initializer<*>>>()

        for (initializer in initializerMap.values.flatten()) {
            result[initializer] = initializer.parentIdList
                .flatMap { initializerMap[it] ?: emptyList() }
        }

        return result
    }

    private fun dealInitializerChildren(initializerMap: Map<String, List<Initializer<*>>>): Map<Initializer<*>, Set<Initializer<*>>> {
        val result = mutableMapOf<Initializer<*>, MutableSet<Initializer<*>>>()

        for (initializer in initializerMap.values.flatten()) {
            val parentInitializerList = initializer.parentIdList
                .flatMap { initializerMap[it] ?: emptyList() }

            for (parentInitializer in parentInitializerList) {
                result.getOrPut(parentInitializer) { mutableSetOf() }
                    .add(initializer)
            }
        }

        return result
    }

    private fun checkCycle(
        initializerMap: Map<String, List<Initializer<*>>>,
        parentMap: Map<Initializer<*>, List<Initializer<*>>>,
    ) {
        for (initializer in initializerMap.values.flatten()) {
            checkInitializerCycle(initializer, parentMap, emptyList())
        }
    }

    private fun checkInitializerCycle(
        initializer: Initializer<*>,
        parentMap: Map<Initializer<*>, List<Initializer<*>>>,
        parentList: List<Initializer<*>>
    ) {
        val parentInitializerList = parentMap[initializer] ?: return

        if (parentInitializerList.isEmpty()) return

        for (parentInitializer in parentInitializerList) {
            val curParentList = parentList + parentInitializer

            if (parentInitializer in parentList) {
                throw InitializerException("存在环依赖,依赖路径:${curParentList.joinToString("->") { it.javaClass.name }}")
            }

            checkInitializerCycle(parentInitializer, parentMap, curParentList)
        }
    }

    private fun CoroutineScope.prepareJob(
        context: Application,
        mainContext: CoroutineContext,
        initializerMap: Map<String, List<Initializer<*>>>,
        parentMap: Map<Initializer<*>, List<Initializer<*>>>,
        childrenMap: Map<Initializer<*>, Set<Initializer<*>>>,
    ): Map<Initializer<*>, Job> {

        val result = mutableMapOf<Initializer<*>, Job>()

        val groupJobMap = mutableMapOf<String?, MutableList<Job>>()

        initializerMap.values.flatten().forEach { initializer ->

            val job = launch(Dispatchers.IO, start = CoroutineStart.LAZY) {
                val startTime = System.currentTimeMillis()

                val threadNameStr = "thread:${Thread.currentThread().name}"

                Log.d(LOG_TAG, "start:${initializer.id}-->$threadNameStr")

                parentMap[initializer]
                    ?.mapNotNull { result[it] }
                    ?.forEach {
                        it.join()
                    }

                val coroutineContext = if (initializer.dispatcher == Dispatchers.Main) {
                    mainContext
                } else {
                    initializer.dispatcher
                }

                val initResult = withContext(coroutineContext) { initializer.doInit(context) }

                val costTimeStr = "cost:${System.currentTimeMillis() - startTime}ms"

                Log.d(LOG_TAG, "finish:${initializer.id}-->$costTimeStr-->$threadNameStr")

                childrenMap[initializer]?.forEach {
                    it.onParentCompleted(initializer.id, initResult ?: Unit)
                }
            }

            result[initializer] = job

            groupJobMap.getOrPut(initializer.groupName) { mutableListOf() }.add(job)
        }

        groupJobMap.entries.forEach { entry ->
            launch(Dispatchers.IO) {

                entry.value.forEach {
                    it.start()

                    if (entry.key != null) {
                        it.join()
                    }
                }
            }
        }

        return result
    }

    private fun logDepth(
        initializerMap: Map<String, List<Initializer<*>>>,
        parentMap: Map<Initializer<*>, List<Initializer<*>>>,
    ) {

        val allList = initializerMap.values.flatten().toMutableList()

        val finishList = mutableListOf<Initializer<*>>()

        var depth = 0

        while (allList.size > 0) {

            val curDepthList = mutableListOf<Initializer<*>>()

            for (initializer in allList) {
                val parentInitializer = parentMap[initializer]

                if (parentInitializer == null || parentInitializer.all { it in finishList }) {
                    curDepthList.add(initializer)
                }
            }

            finishList.addAll(curDepthList)

            allList.removeAll(curDepthList)

            val curDepthStr = curDepthList.joinToString(",") { it.javaClass.name }

            Log.d(LOG_TAG, "depth:${depth++}-->[$curDepthStr]")
        }
    }
}