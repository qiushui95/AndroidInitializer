package son.ysy.initializer.android.execption

import son.ysy.initializer.android.AndroidInitializer

internal class InitializerException : RuntimeException {
    companion object {
        fun parentNoFind(
            parentId: String,
            initializer: AndroidInitializer<*>,
        ): InitializerException {
            val msgSB = StringBuilder()

            msgSB.append("initializer not find which id is '$parentId'")
            msgSB.append(",${initializer::class.qualifiedName} need it.")

            return InitializerException(msgSB.toString())
        }
    }

    constructor(message: String?) : super(message)

    constructor(t: Throwable) : super(t)
}
