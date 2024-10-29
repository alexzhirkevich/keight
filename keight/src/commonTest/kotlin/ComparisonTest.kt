import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class ComparisonTest {

    @Test
    fun number_string() = runTest {
        "'1' > 2".eval().assertEqualsTo(false)
        "'1' == 1".eval().assertEqualsTo(true)
        "'1' === 1".eval().assertEqualsTo(false)

        "'test' == 0".eval().assertEqualsTo(false)
        "'test' == NaN".eval().assertEqualsTo(false)

        "'00100' < '1'".eval().assertEqualsTo(true)
        "'00100' < 1".eval().assertEqualsTo(false)

        " +'2' > +'10'".eval().assertEqualsTo(false)
        "'2' > '10'".eval().assertEqualsTo(true)

        "2 < 12".eval().assertEqualsTo(true)
        "2 < '12'".eval().assertEqualsTo(true)
        "2 < 'John'".eval().assertEqualsTo(false)
        "2 > 'John'".eval().assertEqualsTo(false)
        "2 == 'John'".eval().assertEqualsTo(false)
        "'2' < '12'".eval().assertEqualsTo(false)
        "'2' > '12'".eval().assertEqualsTo(true)
        "'2' == '12'".eval().assertEqualsTo(false)
    }
}