package io.github.alexzhirkevich.keight

import io.github.alexzhirkevich.keight.js.JSError
import io.github.alexzhirkevich.keight.js.JsAny
import io.github.alexzhirkevich.keight.js.interpreter.parse
import io.github.alexzhirkevich.keight.js.interpreter.tokenize
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException

public class JavaScriptEngine(
    override val runtime: ScriptRuntime,
    vararg modules : Module
) : ScriptEngine() {

    init {
        modules.forEach {
            it.importInto(runtime.findRoot())
        }
    }

    override fun compile(script: String): Script {
        return "{\n$script\n}"
            .tokenize()
            .parse()
            .asScript(runtime)
    }
}

private fun Expression.asScript(runtime: ScriptRuntime): Script = object : Script {

    override suspend fun invoke(): JsAny? {
        return withContext(runtime.coroutineContext) {
            try {
                invoke(runtime)
            } catch (c: CancellationException) {
                throw c
            } catch (t: Throwable) {
                if (t is JSError) {
                    throw t
                } else {
                    throw JSError(t.message, cause = t)
                }
            }
        }
    }
}