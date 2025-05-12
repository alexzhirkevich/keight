package io.github.alexzhirkevich.keight

import io.github.alexzhirkevich.keight.js.JSPropertyAccessor
import io.github.alexzhirkevich.keight.js.JsAny
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

public fun interface Getter<T : JsAny?> {
    public suspend fun get(runtime: ScriptRuntime) : T
}

private object UNINITIALIZED

public fun <T : JsAny?> LazyGetter(producer : suspend (ScriptRuntime) -> T): Getter<T> = object : Getter<T> {

    var mutex = Mutex()
    var v: Any? = UNINITIALIZED

    override suspend fun get(runtime: ScriptRuntime): T {
        return mutex.withLock {
            if (v === UNINITIALIZED) {
                v = producer(runtime)
            }
            v as T
        }
    }
}

internal suspend fun Any.get(runtime: ScriptRuntime) : JsAny? = when (this) {
    is Getter<*> -> get(runtime)?.get(runtime)
    is JSPropertyAccessor -> get(runtime)?.get(runtime)
    is JsAny -> this
    else -> error("$this is not a JS object")
}