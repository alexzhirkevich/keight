package io.github.alexzhirkevich.keight.expressions

import io.github.alexzhirkevich.keight.Expression
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.js.FunctionParam
import io.github.alexzhirkevich.keight.js.JSError
import io.github.alexzhirkevich.keight.js.JSFunction
import io.github.alexzhirkevich.keight.js.JSObject
import io.github.alexzhirkevich.keight.js.JSObjectImpl
import io.github.alexzhirkevich.keight.js.Object
import io.github.alexzhirkevich.keight.js.OpArgOmitted
import io.github.alexzhirkevich.keight.js.TypeError
import io.github.alexzhirkevich.keight.js.defaults

internal open class JSErrorFunction(
    name : String = "Error",
    prototype : JSObject = Object {
        "message" eq ""
        "name" eq "Error"
    },
    private val make : (Any?) -> JSError = { JSError(it) }
) : JSFunction(
    name = name,
    prototype = prototype,
    parameters = listOf(FunctionParam("msg" , default =  OpConstant("Uncaught $name"))),
    body = Expression { make(it.get("msg")) }
) {
    override suspend fun constructObject(args: List<Any?>, runtime: ScriptRuntime): JSObject {
        return make(args.getOrElse(0) { "Uncaught $name" })
    }
}

internal class JSTypeErrorFunction(errorFunction: JSErrorFunction) : JSErrorFunction(
    name = "TypeError",
    prototype = errorFunction,
    make = { TypeError(it) }
)