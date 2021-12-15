package son.ysy.initializer.android.sample.initializer

class Initializer4 : StringInitializer() {

    override val parentIdList: List<String> = listOf(
        Initializer2::class.java.name,
        Initializer3::class.java.name,
    )
}