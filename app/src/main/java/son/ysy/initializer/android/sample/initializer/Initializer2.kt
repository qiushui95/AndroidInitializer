package son.ysy.initializer.android.sample.initializer

class Initializer2 : StringInitializer() {

    override val needBlockingMain: Boolean = true

    override val parentIdList: List<String> = listOf(
        Initializer1::class.java.name,
    )
}