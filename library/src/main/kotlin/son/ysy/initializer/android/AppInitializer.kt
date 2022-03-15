package son.ysy.initializer.android

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import kotlinx.coroutines.*
import son.ysy.initializer.android.execption.InitializerException
import son.ysy.initializer.android.provider.StartupProvider
import kotlin.coroutines.CoroutineContext

internal object AppInitializer {

    private val initializerCoroutine = CoroutineScope(Dispatchers.Main)

    fun startInit(context: Application) = runBlocking() {

        val initializerClassSet = discoverInitializerClass(context)

        val initializerMap = doInitialize(initializerClassSet)

        checkInitializer(initializerMap)

        val parentMap = dealInitializerParent(initializerMap)

        val childrenMap = dealInitializerChildren(initializerMap)

        checkCycle(initializerMap, parentMap)

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

        val providerInfo = context.packageManager
            .getProviderInfo(provider, PackageManager.GET_META_DATA)

        val classSet = mutableSetOf<Class<*>>()

        val metadata = providerInfo.metaData ?: return classSet

        val initializerValue = context.getString(R.string.initializer_start_up)
        val initializerClass = Initializer::class.java

        val configValue = context.getString(R.string.initializer_start_up_config)
        val configClass = InitializerConfig::class.java

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


            } else if (value == configValue) {
                InitializerCache.config = try {

                    val clz = Class.forName(key)

                    if (!configClass.isAssignableFrom(clz)) continue

                    clz.getDeclaredConstructor().newInstance() as InitializerConfig

                } catch (e: Exception) {

                    throw InitializerException(e)
                }
            }
        }

        return classSet
    }

    private fun doInitialize(initializerClassSet: Set<Class<*>>): Map<String, Initializer<*>> {

        val list = mutableListOf<Initializer<*>>()

        for (initializerClass in initializerClassSet) {
            list.initialize(initializerClass)
        }

        list.sortBy { it.priority }

        val result = mutableMapOf<String, Initializer<*>>()

        list.forEach {
            result[it.id] = it
        }

        return result
    }

    private fun checkInitializer(initializerMap: Map<String, Initializer<*>>) {
        for (initializer in initializerMap.values) {

            for (parentId in initializer.parentIdList) {
                if (!initializerMap.containsKey(parentId)) {
                    throw InitializerException("initializer not find which id is '$parentId',${initializer::class.qualifiedName} need it.")
                }
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

    private fun dealInitializerParent(initializerMap: Map<String, Initializer<*>>): Map<Initializer<*>, List<Initializer<*>>> {
        val result = mutableMapOf<Initializer<*>, List<Initializer<*>>>()

        for (initializer in initializerMap.values) {
            result[initializer] = initializer.parentIdList.mapNotNull { initializerMap[it] }
        }

        return result
    }

    private fun dealInitializerChildren(initializerMap: Map<String, Initializer<*>>): Map<Initializer<*>, Set<Initializer<*>>> {
        val result = mutableMapOf<Initializer<*>, MutableSet<Initializer<*>>>()

        for (initializer in initializerMap.values) {
            val parentInitializerList = initializer.parentIdList
                .mapNotNull { initializerMap[it] }

            for (parentInitializer in parentInitializerList) {
                result.getOrPut(parentInitializer) { mutableSetOf() }
                    .add(initializer)
            }
        }

        return result
    }

    private fun checkCycle(
        initializerMap: Map<String, Initializer<*>>,
        parentMap: Map<Initializer<*>, List<Initializer<*>>>,
    ) {
        for (initializer in initializerMap.values) {
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
        initializerMap: Map<String, Initializer<*>>,
        parentMap: Map<Initializer<*>, List<Initializer<*>>>,
        childrenMap: Map<Initializer<*>, Set<Initializer<*>>>,
    ): Map<Initializer<*>, Job> {

        val result = mutableMapOf<Initializer<*>, Job>()

        val groupJobMap = mutableMapOf<String?, MutableList<Job>>()

        initializerMap.values.forEach { initializer ->

            val job = launch(Dispatchers.IO, start = CoroutineStart.LAZY) {
                parentMap[initializer]
                    ?.mapNotNull { result[it] }
                    ?.forEach {
                        it.join()
                    }

                val coroutineContext = if (initializer.needRunOnMain) {
                    mainContext
                } else {
                    Dispatchers.IO
                }

                val initResult = withContext(coroutineContext) { initializer.doInit(context) }

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
}