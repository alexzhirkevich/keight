package io.github.alexzhirkevich.keight

public interface Wrapper<T> {
    public val value: T
}

internal tailrec fun Wrapper<*>.unwrap() : Any? {
    val v = value
    return if (v is Wrapper<*>) {
        v.unwrap()
    } else {
        v
    }
}