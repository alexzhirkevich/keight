import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

class GlobalThisTest {

    @Test
    fun recursive() = runTest {
        assertTrue { "globalThis == globalThis.globalThis".eval() as Boolean }
    }
}