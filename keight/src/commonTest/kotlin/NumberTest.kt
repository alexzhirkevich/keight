import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

class NumberTest {

    @Test
    fun format()= runTest {
        "123".eval().assertEqualsTo(123L)
        "123.1".eval().assertEqualsTo(123.1)
        ".1".eval().assertEqualsTo(.1)

        "0xfF".eval().assertEqualsTo(0xff.toLong())
        "0b11".eval().assertEqualsTo(0b11.toLong())
        "0o123".eval().assertEqualsTo(83L)

        "1e5".eval().assertEqualsTo(1e5)

        "1_000_000".eval().assertEqualsTo(1_000_000L)
    }

    @Test
    fun eq()= runTest {
        assertTrue { "5 === 5.0".eval() as Boolean }
    }

    @Test
    fun toFixed()= runTest {
        "12.12345.toFixed()".eval().assertEqualsTo("12")
        "12.52345.toFixed()".eval().assertEqualsTo("13")
        "12.12345.toFixed(1)".eval().assertEqualsTo("12.1")
        "12.12345.toFixed(3)".eval().assertEqualsTo("12.123")
        "123.456.toFixed(2)".eval().assertEqualsTo("123.46")
        "123.51.toFixed(1)".eval().assertEqualsTo("123.5")
        "123.51.toFixed(1)".eval().assertEqualsTo("123.5")

        "0xFF.toString(16)".eval().assertEqualsTo("ff")
    }

    @Test
    fun toPrecision() = runTest {
        val num = 5.123456;
        "$num.toPrecision()".eval().assertEqualsTo(5.123456)
        "$num.toPrecision(5)".eval().assertEqualsTo(5.1235)
        "$num.toPrecision(2)".eval().assertEqualsTo(5.1)
        "$num.toPrecision(1)".eval().assertEqualsTo(5.0)
    }

    @Test
    fun type()= runTest {
        "typeof(Number)".eval().assertEqualsTo("function")
        "Number(123)".eval().assertEqualsTo(123L)
        "Number(123.0)".eval().assertEqualsTo(123.0)
        "Number(\"123\")".eval().assertEqualsTo(123L)
        "Number(\"123.0\")".eval().assertEqualsTo(123.0)
        "Number(\"unicorn\")".eval().assertEqualsTo(Double.NaN)
        "Number(undefined)".eval().assertEqualsTo(Double.NaN)
        "Number(true)".eval().assertEqualsTo(1L)
        "Number(false)".eval().assertEqualsTo(0L)
        "1 === Number(1)".eval().assertEqualsTo(true)
        "1 == Number(1)".eval().assertEqualsTo(true)
        "1 == true".eval().assertEqualsTo(true)
        "0 == false".eval().assertEqualsTo(true)
        "new Number(1)".eval().assertEqualsTo(1L)
        "new Number('1')".eval().assertEqualsTo(1L)
        "new Number(1) === 1".eval().assertEqualsTo(false)
        "new Number(1) == 1".eval().assertEqualsTo(true)
        "new Number(1) === Number(1)".eval().assertEqualsTo(false)
        "new Number('1') == Number(true)".eval().assertEqualsTo(true)
        "new Number('2') > Number(true)".eval().assertEqualsTo(true)
    }
    @Test
    fun static_props()= runTest {
        "Number.MAX_SAFE_INTEGER".eval().assertEqualsTo(Long.MAX_VALUE)
        "Number.MIN_SAFE_INTEGER".eval().assertEqualsTo(Long.MIN_VALUE)
        "Number.MAX_VALUE".eval().assertEqualsTo(Double.MAX_VALUE)
        "Number.EPSILON".eval().assertEqualsTo(Double.MIN_VALUE)
        "Number.POSITIVE_INFINITY".eval().assertEqualsTo(Double.POSITIVE_INFINITY)
        "Number.NEGATIVE_INFINITY".eval().assertEqualsTo(Double.NEGATIVE_INFINITY)
        "Number.NaN".eval().assertEqualsTo(Double.NaN)
    }

    @Test
    fun static_methods() = runtimeTest { runtime ->

        listOf("Number.", "globalThis.", "").forEach {
            "${it}isFinite(123)".eval(runtime).assertEqualsTo(true)
            "${it}isInteger(123)".eval(runtime).assertEqualsTo(true)
            "${it}isInteger(123.3)".eval(runtime).assertEqualsTo(false)
            "${it}isNaN(123.3)".eval(runtime).assertEqualsTo(false)
            "${it}isNaN(NaN)".eval(runtime).assertEqualsTo(true)
            "${it}isSafeInteger(123.3)".eval(runtime).assertEqualsTo(false)
            "${it}isSafeInteger(123)".eval(runtime).assertEqualsTo(true)
            "${it}parseFloat('123.3')".eval(runtime).assertEqualsTo(123.3)
            "${it}parseFloat('123.3sdfsdf')".eval(runtime).assertEqualsTo(123.3)
            "${it}parseInt('123')".eval(runtime).assertEqualsTo(123L)
            "${it}parseInt('123.3')".eval(runtime).assertEqualsTo(123L)
            "${it}parseInt('123.3sdfsdf')".eval(runtime).assertEqualsTo(123L)
            "${it}parseInt(' 0xff', 16)".eval(runtime).assertEqualsTo(255L)
            "${it}parseInt(' 0xff', 0)".eval(runtime).assertEqualsTo(255L)
            "${it}parseInt(' 0xffhh', 16)".eval(runtime).assertEqualsTo(255L)
            "${it}parseInt(' 110', 2)".eval(runtime).assertEqualsTo(6L)
            "${it}parseInt('1245', 8)".eval(runtime).assertEqualsTo(677L)
        }
    }

}