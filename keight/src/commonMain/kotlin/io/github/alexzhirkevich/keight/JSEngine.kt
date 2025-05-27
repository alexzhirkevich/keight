package io.github.alexzhirkevich.keight

import io.github.alexzhirkevich.keight.js.JSError
import io.github.alexzhirkevich.keight.js.JsAny
import io.github.alexzhirkevich.keight.js.interpreter.parse
import io.github.alexzhirkevich.keight.js.interpreter.tokenize
import kotlinx.coroutines.withContext
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.cancellation.CancellationException
import kotlin.jvm.JvmInline

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
            .asScript()
    }
}

public fun Expression.asScript(): Script = Script { runtime ->
    withContext(runtime.coroutineContext) {
        try {
            this@asScript.invoke(runtime)
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

