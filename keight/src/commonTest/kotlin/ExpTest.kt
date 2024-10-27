import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class ExpTest {

    @Test
    fun expOperator() = runTest {
        "4 ** 2".eval().assertEqualsTo(16.0)
    }

    @Test
    fun rightAssociativity() = runTest {
        // Same as 4 ** (3 ** 2); evaluates to 262144
        "4 ** 3 ** 2".eval().assertEqualsTo(262144.0)
    }
}