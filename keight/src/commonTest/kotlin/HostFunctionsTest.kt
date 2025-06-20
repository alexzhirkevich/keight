import io.github.alexzhirkevich.keight.Callable
import io.github.alexzhirkevich.keight.js.Undefined
import io.github.alexzhirkevich.keight.js.js
import io.github.alexzhirkevich.keight.set
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlin.test.Test
import kotlin.test.assertTrue

class HostFunctionsTest {

    @Test
    fun lambda() = runtimeTest { runtime ->

        var x = 0L

        runtime.set("test".js, Callable { x = 2; Undefined })
        "test()".eval(runtime)
        x.assertEqualsTo(2L)

        runtime.set("test".js, Callable { x = toNumber(it[0]).toLong(); Undefined })
        "test(3)".eval(runtime)
        x.assertEqualsTo(3L)

        runtime.set("test".js, Callable { (toNumber(it[0]).toLong() + 1L).js })
        "test(3)".eval(runtime).assertEqualsTo(4L)
    }

    @Test
    fun promise() = runtimeTest { runtime ->

        runtime.set("testSuspend".js, Callable { async { delay(1); "result".js }.js })

        assertTrue {
            "testSuspend()".eval(runtime) is Deferred<*>
        }
        "await testSuspend()".eval(runtime).assertEqualsTo("result")
    }
}

