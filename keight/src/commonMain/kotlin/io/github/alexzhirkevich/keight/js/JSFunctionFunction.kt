package io.github.alexzhirkevich.keight.js

import io.github.alexzhirkevich.keight.callableOrThrow

internal class JSFunctionFunction : JSFunction(
    name = "Function",
    prototype = Object {
        "call".js.func(
            FunctionParam("thisArg"),
            "argArray".vararg()
        ) {
            thisRef
                .callableOrThrow(this)
                .call(
                    thisArg = it[0],
                    args = it.argOrElse(1) { emptyList<JsAny?>() } as List<JsAny?>,
                    runtime = this
                )
        }

        "bind".js.func(
            FunctionParam("thisArg"),
            "argArray".vararg()
        ) {
            thisRef
                .callableOrThrow(this)
                .bind(
                    thisArg = it[0],
                    args = it.argOrElse(1) { emptyList<JsAny?>() } as List<JsAny?>,
                    runtime = this
                )
        }

        "apply".js.func(
            FunctionParam("thisArg"),
            "argArray" defaults OpArgOmitted
        ) {
            thisRef
                .callableOrThrow(this)
                .call(
                    thisArg = it[0],
                    args = it.argOrElse(1) { emptyList<JsAny?>() } as List<JsAny?>,
                    runtime = this
                )
        }
    }
)
