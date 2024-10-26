import io.github.alexzhirkevich.keight.DefaultScriptIO
import io.github.alexzhirkevich.keight.ScriptIO
import io.github.alexzhirkevich.keight.evaluate
import io.github.alexzhirkevich.keight.JSRuntime
import io.github.alexzhirkevich.keight.JavaScriptEngine
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal fun Any?.assertEqualsTo(other : Any?) = assertEquals(other,this)
internal fun Any?.assertEqualsTo(other : Double, tolerance: Double = 0.0001) {
    assertTrue("$this is not a Double") { this is Double }
    assertEquals(other, this as Double, tolerance)
}

internal fun String.eval(runtime: JSRuntime = JSRuntime()) : Any? {
    return JavaScriptEngine(runtime).evaluate(this)
}

internal fun String.eval(io : ScriptIO = DefaultScriptIO, runtime: JSRuntime = JSRuntime(io)) : Any? {
    return JavaScriptEngine(runtime).evaluate(this)
}
