package son.ysy.initializer.android

internal object StatisticsUtil {
    inline fun <reified T> runWithStatistics(tag: String, msg: String, block: () -> T): T {

        Logger.printLog("$tag start", "${msg}-->${Thread.currentThread().name}")

        val start = System.nanoTime()

        return block().apply {
            val cost = (System.nanoTime() - start) / 1_000_000

            Logger.printLog("$tag end", "${msg}-->cost $cost ms")
        }
    }
}