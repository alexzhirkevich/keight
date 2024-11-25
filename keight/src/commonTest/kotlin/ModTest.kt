import kotlinx.coroutines.test.runTest
import kotlin.test.Test


class ModTest {

    @Test
    fun numbers()= runTest  {
        "13 % 7".eval().assertEqualsTo(6.0)
        "13 % -7".eval().assertEqualsTo(6.0)
        "-13 % 7".eval().assertEqualsTo(-6.0)
        "-13 % -7".eval().assertEqualsTo(-6.0)

        "13 % 0".eval().assertEqualsTo(Double.NaN)
        "0 % 0".eval().assertEqualsTo(Double.NaN)
        "0 % 13".eval().assertEqualsTo(0.0)
    }

    @Test
    fun string() = runTest {
        "'13' % '7'".eval().assertEqualsTo(6.0)
        "'13' % 7".eval().assertEqualsTo(6.0)
        "13 % '7'".eval().assertEqualsTo(6.0)
    }

    @Test
    fun null_undefined() = runTest {
        "null % 5".eval().assertEqualsTo(0.0)
        "5 % null".eval().assertEqualsTo(Double.NaN)
        "0 % null".eval().assertEqualsTo(Double.NaN)
        "null % 0".eval().assertEqualsTo(Double.NaN)
        "null % null".eval().assertEqualsTo(Double.NaN)

        "undefined % null".eval().assertEqualsTo(Double.NaN)
        "5 % undefined".eval().assertEqualsTo(Double.NaN)
        "undefined % undefined".eval().assertEqualsTo(Double.NaN)
    }
}