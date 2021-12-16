package son.ysy.initializer.android.execption

internal class InitializerException : RuntimeException {

    constructor(message: String?) : super(message)

    constructor(t: Throwable) : super(t)
}