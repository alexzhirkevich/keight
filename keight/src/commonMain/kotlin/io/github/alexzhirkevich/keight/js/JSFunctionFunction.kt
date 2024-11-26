package io.github.alexzhirkevich.keight.js

import io.github.alexzhirkevich.keight.Callable
import io.github.alexzhirkevich.keight.callableOrNull
import io.github.alexzhirkevich.keight.callableOrThrow
import io.github.alexzhirkevich.keight.thisRef

internal class JSFunctionFunction : JSFunction(
    name = "Function",
    prototype = Object {
        "call".func(
            FunctionParam("thisArg"),
            FunctionParam("argArray", isVararg = true),
        ) {
            thisRef.callableOrThrow(this).call(it[0], it[1] as List<*>, this)
        }

        "bind".func(
            FunctionParam("thisArg"),
            FunctionParam("argArray", isVararg = true),
        ) {
            thisRef.callableOrThrow(this).bind(it[0], it[1] as List<*>, this)
        }

        "apply".func(
            FunctionParam("thisArg"),
            FunctionParam("argArray", default = OpArgOmitted),
        ) {
            thisRef.callableOrThrow(this).call(
                thisArg = it[0],
                args = it.argOrElse(1) { emptyList<Any?>() } as List<*>,
                runtime = this
            )
        }
    }
)
