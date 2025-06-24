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

    @Test
    fun construct_from_array() = runtimeTest {
        "const map = new Map([['a', 1],['b',2],['c',3]])".eval(it)
        "map.get('a')".eval(it).assertEqualsTo(1L)
        "map.get('b')".eval(it).assertEqualsTo(2L)
        "map.get('c')".eval(it).assertEqualsTo(3L)
    }

    @Test
    fun construct_from_iterator() = runtimeTest {
        "const map = new Map([['a', 1],['b',2],['c',3]].values())".eval(it)
        "map.get('a')".eval(it).assertEqualsTo(1L)
        "map.get('b')".eval(it).assertEqualsTo(2L)
        "map.get('c')".eval(it).assertEqualsTo(3L)
    }

    @Test
    fun construct_from_helper_iterator() = runtimeTest {
        """
            const map = new Map([
                ['a', 1].values().filter(() => true),
                ['b', 2].values().filter(() => true),
                ['c', 3].values().filter(() => true)
            ].values().filter(() => true))
        """.eval(it)

        "map.get('a')".eval(it).assertEqualsTo(1L)
        "map.get('b')".eval(it).assertEqualsTo(2L)
        "map.get('c')".eval(it).assertEqualsTo(3L)
    }

    @Test
    fun construct_from_map() = runtimeTest {
        "const origin = new Map([['a', 1],['b',2],['c',3]])".eval(it)
        "const map = new Map(origin)".eval(it)
        "map.get('a')".eval(it).assertEqualsTo(1L)
        "map.get('b')".eval(it).assertEqualsTo(2L)
        "map.get('c')".eval(it).assertEqualsTo(3L)
    }
}