package io.github.alexzhirkevich.keight

import io.github.alexzhirkevich.keight.js.JSError
import io.github.alexzhirkevich.keight.js.JsAny
import io.github.alexzhirkevich.keight.js.interpreter.parse
import io.github.alexzhirkevich.keight.js.interpreter.tokenize
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.cancellation.CancellationException
import kotlin.jvm.JvmInline

public open class JavaScriptEngine<out R : JSRuntime>(
    override val runtime: R,
    vararg modules : Module
) : ScriptEngine<R> {

    init {
        modules.forEach {
            it.importInto(runtime.findRoot())
        }
    }

    final override val mutex: Mutex = Mutex()

    override fun compile(script: String): Script {
        return "{\n$script\n}"
            .tokenize()
            .parse()
            .asScript(mutex)
    }
}

public fun Expression.asScript(mutex: Mutex): Script = Script { runtime ->
    withContext(runtime.coroutineContext) {
        try {
            mutex.withLock {
                this@asScript.invoke(runtime)
            }
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

