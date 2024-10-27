import io.github.alexzhirkevich.keight.JSRuntime
import io.github.alexzhirkevich.keight.set
import kotlinx.coroutines.Job
import kotlinx.coroutines.test.runTest
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertTrue

class GlobalThisTest {

    @Test
    @Ignore
    fun recursive() = runTest {
        assertTrue { "globalThis == globalThis.globalThis".eval() as Boolean }
    }

//    @Test
//    fun instance() = runTest {
//        val runtime = JSRuntime(coroutineContext).apply { set("runtime", this) }
//        assertTrue { "runtime == globalThis".eval(runtime) as Boolean }
//    }
}