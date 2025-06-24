package io.github.alexzhirkevich.keight.js

import io.github.alexzhirkevich.keight.Callable
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.Wrapper
import io.github.alexzhirkevich.keight.thisRef


internal class JSRegExpFunction : JSFunction(
    name = "RegExp",
    prototype = Object {
        ToString.js.func {
            thisRef.toString().js
        }
        "exec".js.func("string" defaults OpArgOmitted) {
            thisRef<JsRegexWrapper>()
                .exec(toString(it.argOrElse(0) { Undefined }), this)
        }

        JsSymbol.match.func(
            "string" defaults OpArgOmitted
        ) {
            thisRef<JsRegexWrapper>()
                .exec(toString(it.argOrElse(0) { Undefined }), this)
        }
    }
) {

    override suspend fun invoke(args: List<JsAny?>, runtime: ScriptRuntime): JsAny {
        val regex = if (args.isEmpty()) {
            JsRegexWrapper()
        } else {
            val r = args[0]
            val options = if (args.size >= 2) {
                runtime.toString(args[1]).toRegexOptions()
            } else {
                if (r is JsRegexWrapper) r.options else emptySet()
            }
            runtime.toString(r).toJsRegex(options)
        }

        return regex
    }

    override suspend fun construct(args: List<JsAny?>, runtime: ScriptRuntime): JsAny {
        return invoke(args, runtime)
    }
}

