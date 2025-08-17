package io.github.alexzhirkevich.keight

import io.github.alexzhirkevich.keight.js.JSError
import io.github.alexzhirkevich.keight.js.JsAny
import io.github.alexzhirkevich.keight.js.interpreter.parse
import io.github.alexzhirkevich.keight.js.interpreter.tokenize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException

public open class JSEngine<out R : JSRuntime>(
    override val runtime: R
) : ScriptEngine<R> {

    override fun compile(script: String, name: String?): Script {
        return "{\n$script\n}"
            .tokenize()
            .parse()
            .let {
                if (name == null) {
                    JSScript(runtime, it)
                } else {
                    JSModule(runtime, it).also { m ->
                        runtime.addModule(name, m)
                    }
                }
            }
    }
}

private open class JSScript(
    override val runtime: ScriptRuntime,
    private val expression: Expression
) : Script {

    protected val mutex = Mutex()

    override suspend fun invoke(runtime: ScriptRuntime): JsAny? {
        require(runtime.findRoot() === this.runtime.findRoot()) {
            "Script can only be invoked in the runtime or a sub-runtime of the engine it compiled with"
        }
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
            throw c
        } catch (t: Throwable) {
            throw JSError("Kotlin exception occurred during JS code evaluation", cause = t)
        }
    }
}

private class JSModule(
    parent: JSRuntime,
    expression: Expression
) : JSScript(parent, expression), Module {

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

