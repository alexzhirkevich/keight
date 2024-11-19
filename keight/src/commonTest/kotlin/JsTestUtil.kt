import io.github.alexzhirkevich.keight.DefaultConsole
import io.github.alexzhirkevich.keight.Console
import io.github.alexzhirkevich.keight.JSRuntime
import io.github.alexzhirkevich.keight.JavaScriptEngine
import io.github.alexzhirkevich.keight.ScriptRuntime
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
    before : suspend TestScope.(JSRuntime) -> Unit = {},
    test : suspend TestScope.(JSRuntime) -> Unit
) = runTest {
    val runtime = runtime(backgroundScope.coroutineContext)
    before(runtime)
    test(runtime)
}

internal suspend fun String.eval(runtime: ScriptRuntime = JSRuntime(Job())) : Any? {
    return JavaScriptEngine(runtime).evaluate(this)
}

internal suspend fun String.eval2() : Any? {
    return JavaScriptEngine(JSRuntime(Job())).evaluate(this)
}

internal suspend fun String.eval(io : Console = DefaultConsole, runtime: ScriptRuntime =
    JSRuntime(context = Job(), console = io)) : Any? {
    return JavaScriptEngine(runtime).evaluate(this)
}
