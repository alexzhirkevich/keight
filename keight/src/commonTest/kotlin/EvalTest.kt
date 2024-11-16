import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class EvalTest {

    @Test
    fun eval() = runTest {
        "eval('1+1')".eval().assertEqualsTo(2L)
        "eval('eval(\\'1+1\\')')".eval().assertEqualsTo(2L)
    }
}