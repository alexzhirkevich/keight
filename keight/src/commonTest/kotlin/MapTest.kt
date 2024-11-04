import kotlin.test.Test

class MapTest {

    @Test
    fun creation() = runtimeTest {

        ("new Map()".eval() as Map<*,*>).size.assertEqualsTo(0)

        """
            new Map([
                ["apples", 500],
                ["bananas", 300],
                ["oranges", 200]
            ])
        """.trimIndent().eval().assertEqualsTo(
            mapOf(
                "apples" to 500L,
                "bananas" to 300L,
                "oranges" to 200L,
            )
        )
    }

    @Test
    fun set() = runtimeTest {
        """
            const map = new Map()
            map.set("test",1)
            map.get("test")
        """.eval().assertEqualsTo(1L)
    }
}