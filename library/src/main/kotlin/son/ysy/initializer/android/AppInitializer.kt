package son.ysy.initializer.android

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import son.ysy.initializer.android.execption.InitializerException
import son.ysy.initializer.android.provider.StartupProvider
import java.util.*

internal object AppInitializer {

    fun startInit(context: Application) = runBlocking {

        val initializerClassSet = discoverInitializerClass(context)

        val initializerMap = doInitialize(initializerClassSet)

        checkInitializer(initializerMap)

        val parentMap = dealInitializerParent(initializerMap)

        val childrenMap = dealInitializerChildren(initializerMap)

        checkCycle(initializerMap, parentMap)

        val depthMap = dealInitializerDepth(initializerMap)

        val jobMap = prepareJob(context, parentMap, childrenMap, depthMap)

        depthMap[0]
            ?.mapNotNull { jobMap[it] }
            ?.forEach { it.start() }

        depthMap.values
            .flatten()
            .filter { it.needBlockingMain }
            .mapNotNull { jobMap[it] }
            .forEach { it.join() }
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
        val result = mutableMapOf<String, Initializer<*>>()

        for (initializerClass in initializerClassSet) {
            result.initialize(initializerClass)
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

    private fun MutableMap<String, Initializer<*>>.initialize(clz: Class<*>) {

        val initializer = try {
            clz.getDeclaredConstructor().newInstance() as Initializer<*>
        } catch (e: Exception) {
            throw InitializerException(e)
        }

        put(initializer.id, initializer)
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

    private fun dealInitializerDepth(allInitializer: Map<String, Initializer<*>>): Map<Int, List<Initializer<*>>> {
        val tempMap = mutableMapOf<String, Int>()

        allInitializer.values.forEach { tempMap.getInitializerDepth(it, allInitializer) }

        val result = TreeMap<Int, MutableList<Initializer<*>>>()

        for (entry in tempMap) {
            val list = result.getOrPut(entry.value) { mutableListOf() }

            allInitializer[entry.key]?.apply(list::add)
        }

        return result
    }

    private fun MutableMap<String, Int>.getInitializerDepth(
        initializer: Initializer<*>,
        allInitializer: Map<String, Initializer<*>>
    ): Int {
        val existedDepth = get(initializer.id)

        if (existedDepth != null) return existedDepth

        val parentClassNameList = initializer.parentIdList

        if (parentClassNameList.isEmpty()) return getOrPut(initializer.id) { 0 }

        val parentDepth = parentClassNameList.mapNotNull { allInitializer[it] }
            .maxOf { getInitializerDepth(it, allInitializer) }

        return getOrPut(initializer.id) { parentDepth + 1 }
    }

    private fun CoroutineScope.prepareJob(
        context: Application,
        parentMap: Map<Initializer<*>, List<Initializer<*>>>,
        childrenMap: Map<Initializer<*>, Set<Initializer<*>>>,
        depthMap: Map<Int, List<Initializer<*>>>
    ): Map<Initializer<*>, Job> {
        val result = mutableMapOf<Initializer<*>, Job>()

        val completedIdList = mutableListOf<String>()

        val mutex = Mutex()

        for (initializer in depthMap.values.flatten()) {
            result[initializer] = launch(start = CoroutineStart.LAZY) {
                val parentInitializerList = parentMap[initializer] ?: emptyList()

                for (parentInitializer in parentInitializerList) {
                    result[parentInitializer]?.join()
                }

                val dispatcher = Dispatchers.Unconfined.takeIf {
                    initializer.needRunOnMain
                } ?: Dispatchers.IO

                val initJob = async(dispatcher) {
                    initializer.doInit(context)
                }

                val initResult = initJob.await()

                mutex.withLock {
                    completedIdList.add(initializer.id)
                }

                for (childInitializer in childrenMap[initializer] ?: emptySet()) {
                    if (initResult != null) childInitializer.onParentCompleted(
                        initializer.id,
                        initResult
                    )

                    val allParentCompleted = mutex.withLock {
                        childInitializer.parentIdList.all { it in completedIdList }
                    }

                    if (allParentCompleted) {
                        result[childInitializer]?.start()
                    }
                }
            }
        }

        return result
    }
}