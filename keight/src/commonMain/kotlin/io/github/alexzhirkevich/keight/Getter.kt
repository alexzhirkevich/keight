package io.github.alexzhirkevich.keight

public fun interface Getter<T> {
    public suspend fun get() : T
}

internal suspend fun Any.get() : Any? =
    if (this is Getter<*>) get()?.get() else this