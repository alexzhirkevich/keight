package io.github.alexzhirkevich.keight

import io.github.alexzhirkevich.keight.js.JSKotlinError
import io.github.alexzhirkevich.keight.js.JsPromiseWrapper
import io.github.alexzhirkevich.keight.js.JsAny
import io.github.alexzhirkevich.keight.js.JsArrayWrapper
import io.github.alexzhirkevich.keight.js.JsMapWrapper
import io.github.alexzhirkevich.keight.js.JsNumberWrapper
import io.github.alexzhirkevich.keight.js.JsSetWrapper
import io.github.alexzhirkevich.keight.js.js
import kotlinx.coroutines.Job


internal fun fromKotlin(a: Any?): JsAny? {
    return when (a) {
        null -> null
        is JsAny -> a
        is Boolean -> a.js
        is Number -> a.js
        is UByte -> JsNumberWrapper(a.toLong())
        is UShort -> JsNumberWrapper(a.toLong())
        is UInt -> JsNumberWrapper(a.toLong())
        is ULong -> JsNumberWrapper(a.toLong())
        is Map<*, *> -> JsMapWrapper(
            a.map { fromKotlin(it.key) to fromKotlin(it.value) }.toMap().toMutableMap()
        )

        is Set<*> -> JsSetWrapper(a.map(::fromKotlin).toMutableSet())
        is List<*> -> JsArrayWrapper(a.map(::fromKotlin).toMutableList())
        is CharSequence -> a.js
        is Job -> JsPromiseWrapper(a)
        is Throwable -> JSKotlinError(a)
        else -> error("Failed to convert $a to JS")
    }
}
