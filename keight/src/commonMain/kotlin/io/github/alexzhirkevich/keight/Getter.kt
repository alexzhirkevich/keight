package io.github.alexzhirkevich.keight

import io.github.alexzhirkevich.keight.js.JSPropertyAccessor
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

public fun interface Getter<T> {
    public suspend fun get() : T
}

private object UNINITIALIZED
public fun <T> LazyGetter(producer : suspend () -> T): Getter<T> = object : Getter<T> {

    var mutex = Mutex()
    var v: Any? = UNINITIALIZED

    override suspend fun get(): T {
        return mutex.withLock {
            if (v === UNINITIALIZED) {
                v = producer()
            }
            v as T
        }
    }
}

internal suspend fun Any.get(runtime: ScriptRuntime) : Any? = when (this) {
    is Getter<*> -> get()?.get(runtime)
    is JSPropertyAccessor -> get(runtime)?.get(runtime)
    else -> this
}