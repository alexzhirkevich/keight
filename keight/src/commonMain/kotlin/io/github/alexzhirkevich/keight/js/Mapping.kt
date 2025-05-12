package io.github.alexzhirkevich.keight.js

import io.github.alexzhirkevich.keight.expressions.ThrowableValue
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job

public fun Deferred<JsAny?>.js() : JsAny {
    return if (this is JsAny) this else JSPromiseWrapper(this)
}

public fun Job.js() : JsAny {
    return if (this is JsAny) this else JSPromiseWrapper(this)
}


public fun List<JsAny?>.js() : JsAny = if (this is JsAny) this else JsArrayWrapper(toMutableList())
public fun Set<JsAny?>.js() : JsAny = if (this is JsAny) this else JsSetWrapper(toMutableSet())
public fun Map<JsAny?, JsAny?>.js() : JsAny = if (this is JsAny) this else JsMapWrapper(toMutableMap())

public fun Number.js() : JsAny = if (this is JsAny) this else JsNumberWrapper(
    when(this){
        is Int -> toLong()
        is Short -> toLong()
        is Byte -> toLong()
        is Float -> toDouble()
        else -> this
    }
)

public fun CharSequence.js() : JsAny = if (this is JsAny) this else JsStringWrapper(toString())

public fun Boolean.js() : JsAny = JSBooleanWrapper(this)

public fun Throwable.js() : JsAny?  = when (this) {
    is ThrowableValue -> value
    is JSError -> this
    else -> JSError(message, cause = this)
}