package io.github.alexzhirkevich.keight.js

import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.thisRef

private val STRING_PARAM = "string" defaults OpArgOmitted

internal class JSRegExpFunction : JSFunction(
    name = "RegExp",
    prototype = Object {
        ToString.js.func {
            thisRef.toString().js
        }
        "exec".js.func(STRING_PARAM) {
            thisRef<JsRegexWrapper>()
                .exec(toString(it.argOrElse(0) { Undefined }), this)
        }

        "test".js.func(STRING_PARAM){
            thisRef<JsRegexWrapper>()
                .test(toString(it.argOrElse(0) { Undefined }), this).js
        }

        JsSymbol.match.func(STRING_PARAM) {
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

