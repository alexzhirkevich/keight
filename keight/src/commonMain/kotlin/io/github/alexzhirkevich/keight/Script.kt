package io.github.alexzhirkevich.keight

import io.github.alexzhirkevich.keight.js.JSError
import io.github.alexzhirkevich.keight.js.JsAny
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException

public interface Script {
    public suspend operator fun invoke(): Any?
}

internal fun Expression.asScript(runtime: ScriptRuntime): Script = object : Script {

    override suspend fun invoke(): Any? {
        return withContext(runtime.coroutineContext) {
            try {
                runtime.toKotlin(invoke(runtime))
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
