import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertTrue

class RegexTest {

    @Test
    fun literal() = runtimeTest {
        "/x/".eval(it).assertIsRegex().pattern.assertEqualsTo("x")
        "/x/i".eval(it).assertIsRegex().let {
            it.pattern.assertEqualsTo("x")
            it.options.assertEqualsTo(setOf(RegexOption.IGNORE_CASE))
        }
        "/x/dgi".eval(it).assertIsRegex().pattern.assertEqualsTo("x")
        "let x = /x/mi;x".eval(it).assertIsRegex().let {
            it.pattern.assertEqualsTo("x")
            assertContains(it.options, RegexOption.MULTILINE)
            assertContains(it.options, RegexOption.IGNORE_CASE)
        }
        "[/x/dgi, /x/, /x/dgi]".eval(it).let { it as List<*> }.forEach { it.assertIsRegex() }
        "(/x/dgi)".eval(it).assertIsRegex().pattern.assertEqualsTo("x")
        "(/x/dgi,/x/)".eval(it).assertIsRegex().pattern.assertEqualsTo("x")
        "let x1 = 1; /x/mi".eval(it).assertIsRegex().pattern.assertEqualsTo("x")
    }

    @Test
    fun construct() = runtimeTest {
        "new RegExp('x')".eval(it).assertIsRegex().pattern.assertEqualsTo("x")
        "new RegExp(/x/)".eval(it).assertIsRegex().pattern.assertEqualsTo("x")
        "new RegExp('x','i')".eval(it).assertIsRegex().let {
            it.pattern.assertEqualsTo("x")
            it.options.assertEqualsTo(setOf(RegexOption.IGNORE_CASE))
        }
        "new RegExp(/x/i)".eval(it).assertIsRegex().let {
            it.pattern.assertEqualsTo("x")
            it.options.assertEqualsTo(setOf(RegexOption.IGNORE_CASE))
        }
        "new RegExp(/x/i,'m')".eval(it).assertIsRegex().let {
            it.pattern.assertEqualsTo("x")
            it.options.assertEqualsTo(setOf(RegexOption.MULTILINE))
        }
    }

    @Test
    fun exec() = runtimeTest {

        """
            const regex = /[a-e]/id
            const res = regex.exec("123ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz")
        """.eval(it)

        "regex.ignoreCase".eval(it).assertEqualsTo(true)
        "regex.hasIndices".eval(it).assertEqualsTo(true)
        "res[0]".eval(it).assertEqualsTo("A")
        "res.index".eval(it).assertEqualsTo(3L)
        "res.input".eval(it).assertEqualsTo("123ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz")
        "res.indices[0]".eval(it).assertEqualsTo(listOf(3L,4L))
    }

    @Test
    fun test() = runtimeTest {
        """
            const str = "table football";
            const regex = /fo+/;
            const globalRegex = /fo+/g;
        """.eval(it)

        "regex.lastIndex".eval(it).assertEqualsTo(0L)
        "regex.test(str)".eval(it).assertEqualsTo(true)
        "regex.lastIndex".eval(it).assertEqualsTo(0L)

        "globalRegex.lastIndex".eval(it).assertEqualsTo(0L)
        "globalRegex.test(str)".eval(it).assertEqualsTo(true)
        "globalRegex.lastIndex".eval(it).assertEqualsTo(9L)
        "globalRegex.test(str)".eval(it).assertEqualsTo(false)
        "globalRegex.lastIndex".eval(it).assertEqualsTo(0L)

        "globalRegex.lastIndex = 9".eval(it)
        "globalRegex.lastIndex".eval(it).assertEqualsTo(9L)
        "globalRegex.test(str)".eval(it).assertEqualsTo(false)
        "globalRegex.lastIndex".eval(it).assertEqualsTo(0L)
    }
}

private fun Any?.assertIsRegex() : Regex =
    assertTrue("$this is not a Regex") { this is Regex }.let { this as Regex }