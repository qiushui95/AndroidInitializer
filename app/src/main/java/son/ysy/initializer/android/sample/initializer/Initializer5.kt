package son.ysy.initializer.android.sample.initializer

class Initializer5 : StringInitializer() {

    override val parentIdList: List<String> = listOf(
        Initializer3::class.java.name,
        Initializer4::class.java.name,
    )

    override fun doInit(): String {
        Thread.sleep(5000)
        return super.doInit()
    }
}