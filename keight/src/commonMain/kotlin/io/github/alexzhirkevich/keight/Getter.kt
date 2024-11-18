package io.github.alexzhirkevich.keight

import io.github.alexzhirkevich.keight.js.JSPropertyAccessor

public fun interface Getter<T> {
    public suspend fun get() : T
}

internal suspend fun Any.get(runtime: ScriptRuntime) : Any? = when (this) {
    is Getter<*> -> get()?.get(runtime)
    is JSPropertyAccessor -> get(runtime)?.get(runtime)
    else -> this
}