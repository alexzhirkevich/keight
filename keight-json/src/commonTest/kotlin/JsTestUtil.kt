import io.github.alexzhirkevich.keight.DefaultScriptIO
import io.github.alexzhirkevich.keight.ScriptEngine
import io.github.alexzhirkevich.keight.ScriptIO
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.es.ESInterpreter
import io.github.alexzhirkevich.keight.evaluate
import io.github.alexzhirkevich.keight.js.JSLangContext
import io.github.alexzhirkevich.keight.js.JSRuntime
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal fun Any?.assertEqualsTo(other : Any?) = assertEquals(other,this)
internal fun Any?.assertEqualsTo(other : Double, tolerance: Double = 0.0001) {
    assertTrue("$this is not a Double") { this is Double }
    assertEquals(other, this as Double, tolerance)
}

internal fun String.eval(runtime: ScriptRuntime = JSRuntime()) : Any? {
    return ScriptEngine(runtime, ESInterpreter(JSLangContext)).evaluate(this)
}

internal fun String.eval(io : ScriptIO = DefaultScriptIO, runtime: ScriptRuntime = JSRuntime(io)) : Any? {
    return ScriptEngine(runtime, ESInterpreter(JSLangContext)).evaluate(this)
}
