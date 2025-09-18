package io.github.alexzhirkevich.keight.js

import io.github.alexzhirkevich.keight.expressions.ThrowableValue
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job

public val Job.js : JsAny
    get() = if (this is JsAny) this else JsPromiseWrapper(this)

public val Iterator<JsAny?>.js : JsAny
    get() = if (this is JsAny) this else JSIteratorWrapper(this)


public val List<JsAny?>.js : JsAny
    get() = if (this is JsAny) this else JsArrayWrapper(toMutableList())

public val Set<JsAny?>.js : JsAny
    get() = if (this is JsAny) this else JsSetWrapper(toMutableSet())


public val Map<JsAny?, JsAny?>.js : JsAny
    get() = if (this is JsAny) this else JsMapWrapper(toMutableMap())

public val IntArray.js get() = map { it.js }.js
public val ShortArray.js get() = map { it.js }.js
public val ByteArray.js get() = map { it.js }.js
public val FloatArray.js get() = map { it.js }.js
public val DoubleArray.js get() = map { it.js }.js
public val LongArray.js get() = map { it.js }.js

public val Number.js : JsAny get() = if (this is JsAny) this else JsNumberWrapper(
    when(this){
        is Int -> toLong()
        is Short -> toLong()
        is Byte -> toLong()
        is Float -> toDouble()
        else -> this
    }
)

public val CharSequence.js : JsAny get() = if (this is JsAny) this else JsStringWrapper(toString())

public val Boolean.js : JsAny get() = JSBooleanWrapper(this)

public val Throwable.js : JsAny? get() = when (this) {
    is ThrowableValue -> value
    is JSError -> this
    else -> JSError(message, cause = this)
}

public val Regex.js : JsAny get() = JsRegexWrapper(
    pattern = pattern,
    options = options.mapNotNullTo(mutableSetOf()) {
        RegexOptions.entries.firstOrNull { e -> e.option == it }
    }
)