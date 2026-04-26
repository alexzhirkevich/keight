package io.github.alexzhirkevich.keight

import io.github.alexzhirkevich.keight.js.JSError
import io.github.alexzhirkevich.keight.js.JsAny
import io.github.alexzhirkevich.keight.js.interpreter.parse
import io.github.alexzhirkevich.keight.js.interpreter.tokenize
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException

/**
 * Engine that can compile JavaScript code.
 *
 * Scripts compiled with this runtime should not be executed in different runtimes
 *
 * @param runtime [JSRuntime] that will be used as a default invocation runtime for script.
 * */
public open class JSEngine<out R : JSRuntime>(
    override val runtime: R
) : ScriptEngine<R> {

    override fun compile(script: String, name: String?): Script {
        val tokens = "{\n$script\n}".tokenize()
        val expression = tokens.parse(name)

        return if (name == null) {
            JSScript(runtime, expression, name)
        } else {
            JSModule(runtime, expression, name).also { m ->
                runtime.addModule(name, m)
            }
        }
    }
}

private open class JSScript(
    override val runtime: ScriptRuntime,
    private val expression: Expression,
    private val scriptName: String? = null
) : Script {

    protected val mutex = Mutex()

    override suspend fun invoke(runtime: ScriptRuntime): JsAny? {
        return withContext(runtime.coroutineContext) {
            mutex.withLock {
                invokeImpl(runtime)
            }
        }
    }

    protected open suspend fun invokeImpl(runtime: ScriptRuntime): JsAny? {
        return try {
            expression.invoke(runtime)
        } catch (c: CancellationException) {
            throw c
        } catch (c: JSError) {
            // Attach script name if not already set
            if (c.fileName == null && scriptName != null) {
                c.fileName = scriptName
            }
            throw c
        } catch (t: Throwable) {
            val error = JSError("Kotlin exception occurred during JS code evaluation", cause = t)
            if (scriptName != null) {
                error.fileName = scriptName
            }
            throw error
        }
    }
}

private class JSModule(
    parent: JSRuntime,
    expression: Expression,
    name: String
) : JSScript(parent, expression, name), Module {

    override val runtime: ModuleRuntime = ModuleRuntime(parent)


    override suspend fun invokeIfNeeded() {
        mutex.withLock {
            if (!runtime.isEvaluated) {
                try {
                    invokeImpl(runtime)
                } finally {
                    runtime.isEvaluated = true
                }
            }
        }
    }
}

