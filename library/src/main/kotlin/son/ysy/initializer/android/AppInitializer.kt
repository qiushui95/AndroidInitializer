package son.ysy.initializer.android

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import kotlinx.coroutines.*
import son.ysy.initializer.android.execption.InitializerException
import son.ysy.initializer.android.provider.StartupProvider

internal object AppInitializer {

    private val scope = CoroutineScope(Dispatchers.IO)

    fun startInit(context: Context) = runBlocking {
        val initializerMap = discoverStartUp(context)

        val childrenMap = dealInitializerChildren(initializerMap)

        val depthMap = dealInitializerDepth(initializerMap)

        val jobMap = prepareJob(initializerMap, childrenMap, depthMap)

        depthMap[0]?.mapNotNull { jobMap[it.id] }
            ?.forEach { it.start() }

        initializerMap.values
            .filter { it.needBlockingMain }
            .onEach { jobMap[it.id]?.join() }
            .asSequence()
            .mapNotNull { childrenMap[it.id] }
            .flatten()
            .mapNotNull { jobMap[it.id] }.toList()
            .forEach { it.join() }
    }

    private fun discoverStartUp(context: Context): Map<String, Initializer<*>> {
        val provider = ComponentName(context.packageName, StartupProvider::class.java.name)

        val providerInfo = context.packageManager
            .getProviderInfo(provider, PackageManager.GET_META_DATA)

        val result = mutableMapOf<String, Initializer<*>>()

        val metadata = providerInfo.metaData ?: return result

        val initializerValue = context.getString(R.string.initializer_start_up)

        val initializerClass = Initializer::class.java

        for (key in metadata.keySet()) {
            val value = metadata.getString(key) ?: continue

            if (value != initializerValue) continue

            val initializer = try {
                val clz = Class.forName(key)

                if (!initializerClass.isAssignableFrom(clz)) continue

                clz.getDeclaredConstructor().newInstance() as Initializer<*>

            } catch (e: Exception) {

                throw InitializerException(e)
            }

            val existedInitializer = result[initializer.id]

            if (existedInitializer != null) {
                throw InitializerException("${existedInitializer.javaClass.name}和${initializer.javaClass.name}的id一样,需保证每个任务id不同")
            }

            result[initializer.id] = initializer
        }

        return result
    }

    private fun dealInitializerChildren(initializerMap: Map<String, Initializer<*>>): Map<String, List<Initializer<*>>> {
        val resultMapSet = mutableMapOf<String, MutableSet<Initializer<*>>>()

        for (initializer in initializerMap.values) {
            for (parentInitializer in initializer.parentIdList.mapNotNull { initializerMap[it] }) {
                resultMapSet.getOrPut(parentInitializer.id) { mutableSetOf() }
                    .add(initializer)
            }
        }

        val result = mutableMapOf<String, List<Initializer<*>>>()

        for (entry in resultMapSet) {
            result[entry.key] = entry.value.toList()
        }
        return result
    }

    private fun dealInitializerDepth(allInitializer: Map<String, Initializer<*>>): Map<Int, List<Initializer<*>>> {
        val tempMap = mutableMapOf<String, Int>()

        allInitializer.values.forEach { tempMap.getInitializerDepth(it, allInitializer) }

        val result = mutableMapOf<Int, MutableList<Initializer<*>>>()

        for (entry in tempMap) {
            val list = result.getOrPut(entry.value) { mutableListOf() }

            allInitializer[entry.key]?.apply(list::add)
        }

        return result.toSortedMap()
    }

    private fun MutableMap<String, Int>.getInitializerDepth(
        initializer: Initializer<*>,
        allInitializer: Map<String, Initializer<*>>
    ): Int {
        val existedDepth = get(initializer.id)

        if (existedDepth != null) return existedDepth

        if (initializer.parentIdList.isEmpty()) return getOrPut(initializer.id) { 0 }

        val parentDepth = initializer.parentIdList.mapNotNull { allInitializer[it] }
            .maxOf { getInitializerDepth(it, allInitializer) }

        return getOrPut(initializer.id) { parentDepth + 1 }
    }

    private fun prepareJob(
        allInitializer: Map<String, Initializer<*>>,
        childrenMap: Map<String, List<Initializer<*>>>,
        depthMap: Map<Int, List<Initializer<*>>>
    ): Map<String, Job> {
        val result = mutableMapOf<String, Job>()

        val completedIdList = mutableListOf<String>()

        for (initializer in depthMap.values.flatten()) {
            result[initializer.id] =
                scope.launch(Dispatchers.Default, start = CoroutineStart.LAZY) {
                    for (parentId in initializer.parentIdList) {
                        result[parentId]?.join()
                    }

                    val initJob = async(initializer.dispatcher) {
                        initializer.doInit()
                    }

                    val initResult = initJob.await()

                    completedIdList.add(initializer.id)

                    for (parentInitializer in initializer.parentIdList.mapNotNull { allInitializer[it] }) {
                        val childrenList = childrenMap[parentInitializer.id] ?: continue

                        if (childrenList.all { it.id in completedIdList }) {
                            parentInitializer.onAllChildrenCompleted()
                        }
                    }

                    val childrenList = childrenMap[initializer.id] ?: emptyList()

                    if (childrenList.isEmpty()) {
                        initializer.onAllChildrenCompleted()
                    }

                    for (childInitializer in childrenList) {
                        if (initResult != null) childInitializer.onParentCompleted(
                            initializer.id,
                            initResult
                        )

                        result[childInitializer.id]?.start()
                    }
                }
        }

        return result
    }
}