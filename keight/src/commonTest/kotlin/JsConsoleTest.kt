import io.github.alexzhirkevich.keight.Console
import io.github.alexzhirkevich.keight.js.Undefined
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class JsConsoleTest {

    @Test
    fun out()= runTest {

        var out : Any? = null
        var err : Any? = null

        val io = object : Console {
            override fun verbose(message: Any?) { out = message }
            override fun error(message: Any?) { err = message }
        }

        "console.log(123)".eval(io).assertEqualsTo(Undefined)

        out.assertEqualsTo(123L)
        err.assertEqualsTo(null)
        out = null

        "console.log('123')".eval(io).assertEqualsTo(Undefined)

        out.assertEqualsTo("123")
        err.assertEqualsTo(null)
        out = null

        "console.error('123')".eval(io).assertEqualsTo(Undefined)

        err.assertEqualsTo("123")
        out.assertEqualsTo(null)
    }

    @Test
    fun empty()= runTest {
        var out : Any? = null
        var err : Any? = null

        val io = object : Console {
            override fun verbose(message: Any?) { out = message }
            override fun error(message: Any?) { err = message }
        }

        "console.log()".eval(io).assertEqualsTo(Undefined)
        out.assertEqualsTo(null)
        err.assertEqualsTo(null)
    }

    @Test
    fun multiple()= runTest {
        var out : Any? = null
        var err : Any? = null

        val io = object : Console {
            override fun verbose(message: Any?) { out = message }
            override fun error(message: Any?) { err = message }
        }

        "console.log(123, 456, '789')".eval(io).assertEqualsTo(Undefined)

        out.assertEqualsTo(listOf(123L, 456L, "789"))
        err.assertEqualsTo(null)
        out = null

        "console.error(123, 456, '789')".eval(io).assertEqualsTo(Undefined)

        err.assertEqualsTo(listOf(123L, 456L, "789"))
        out.assertEqualsTo(null)
    }
}