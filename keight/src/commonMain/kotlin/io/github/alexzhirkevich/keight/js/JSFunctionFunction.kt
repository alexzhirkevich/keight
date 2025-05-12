package io.github.alexzhirkevich.keight.js

import io.github.alexzhirkevich.keight.callableOrThrow

internal class JSFunctionFunction : JSFunction(
    name = "Function",
    prototype = Object {
        "call".js().func(
            FunctionParam("thisArg"),
            FunctionParam("argArray", isVararg = true),
        ) {
            thisRef.callableOrThrow(this).call(it[0], it[1] as List<JsAny?>, this)
        }

        "bind".js().func(
            FunctionParam("thisArg"),
            FunctionParam("argArray", isVararg = true),
        ) {
            thisRef.callableOrThrow(this).bind(it[0], it[1] as List<JsAny?>, this)
        }

        "apply".js().func(
            FunctionParam("thisArg"),
            FunctionParam("argArray", default = OpArgOmitted),
        ) {
            thisRef.callableOrThrow(this).call(
                thisArg = it[0],
                args = it.argOrElse(1) { emptyList<Any?>() } as List<JsAny?>,
                runtime = this
            )
        }
    }
)
