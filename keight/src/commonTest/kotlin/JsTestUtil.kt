import io.github.alexzhirkevich.keight.DefaultScriptIO
import io.github.alexzhirkevich.keight.ScriptIO
import io.github.alexzhirkevich.keight.evaluate
import io.github.alexzhirkevich.keight.JSRuntime
import io.github.alexzhirkevich.keight.JavaScriptEngine
import kotlinx.coroutines.Job
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.coroutines.CoroutineContext
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal fun Any?.assertEqualsTo(other : Any?) = assertEquals(other,this)
internal fun Any?.assertEqualsTo(other : Double, tolerance: Double = 0.0001) {
    assertTrue("$this is not a Double") { this is Double }
    assertEquals(other, this as Double, tolerance)
}

internal fun runtimeTest(
    runtime : (CoroutineContext) -> JSRuntime = {JSRuntime(it)},
    test : suspend TestScope.(JSRuntime) -> Unit
) = runTest {
    test(runtime(backgroundScope.coroutineContext))
}

internal suspend fun String.eval(runtime: JSRuntime = JSRuntime(Job())) : Any? {
    return JavaScriptEngine(runtime).evaluate(this)
}

internal suspend fun String.eval(io : ScriptIO = DefaultScriptIO, runtime: JSRuntime =
    JSRuntime(context = Job(), io = io)) : Any? {
    return JavaScriptEngine(runtime).evaluate(this)
}
