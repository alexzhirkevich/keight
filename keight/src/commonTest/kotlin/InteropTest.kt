import io.github.alexzhirkevich.keight.Callable
import io.github.alexzhirkevich.keight.JSEngine
import io.github.alexzhirkevich.keight.JSRuntime
import io.github.alexzhirkevich.keight.VariableType
import io.github.alexzhirkevich.keight.invokeSync
import io.github.alexzhirkevich.keight.js.Undefined
import io.github.alexzhirkevich.keight.js.js
import io.github.alexzhirkevich.keight.set
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.test.Test
import kotlin.test.assertTrue

class InteropTest {

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

    @Test
    fun undispatched_invoke() {
        val runtime = JSRuntime(EmptyCoroutineContext, isSuspendAllowed = false)
        val engine = JSEngine(runtime)
        engine
            .compile("1+1")
            .invokeSync(runtime)
            ?.toKotlin(runtime)
            .assertEqualsTo(2L)
    }

    @Test
    fun external_job_cancel() = runtimeTest {

        var finished = false

        it.set("handler".js, Callable { finished = true; Undefined }, VariableType.Const)

        val job = "new Promise(resolve => setTimeout(resolve, 1000)).finally(handler)"
            .eval(it) as Deferred<*>

        delay(10)
        assertTrue { job.isActive }
        it.reset()
        assertTrue { job.isCancelled }
        delay(10)
        assertTrue { finished }
    }

    @Test
    fun internal_job_cancel() = runtimeTest {

        var finished = false

        it.set("handler".js, Callable { finished = true; Undefined }, VariableType.Const)

        val job = launch {
            "await new Promise(resolve => setTimeout(resolve, 1000)).finally(handler)".eval(it)
        }

        delay(10)
        assertTrue { job.isActive }
        it.reset()
        delay(10)
        assertTrue { job.isCancelled }
        assertTrue { finished }
    }
}

