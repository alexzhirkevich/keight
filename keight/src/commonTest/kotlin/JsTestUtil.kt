import io.github.alexzhirkevich.keight.DefaultScriptIO
import io.github.alexzhirkevich.keight.ScriptIO
import io.github.alexzhirkevich.keight.evaluate
import io.github.alexzhirkevich.keight.JSRuntime
import io.github.alexzhirkevich.keight.JavaScriptEngine
import kotlinx.coroutines.Job
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal fun Any?.assertEqualsTo(other : Any?) = assertEquals(other,this)
internal fun Any?.assertEqualsTo(other : Double, tolerance: Double = 0.0001) {
    assertTrue("$this is not a Double") { this is Double }
    assertEquals(other, this as Double, tolerance)
}

internal suspend fun String.eval(runtime: JSRuntime = JSRuntime(Job())) : Any? {
    return JavaScriptEngine(runtime).evaluate(this)
}

internal suspend fun String.eval(io : ScriptIO = DefaultScriptIO, runtime: JSRuntime = JSRuntime(Job(), io)) : Any? {
    return JavaScriptEngine(runtime).evaluate(this)
}
