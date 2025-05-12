package io.github.alexzhirkevich.keight.expressions

import io.github.alexzhirkevich.keight.Expression
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.js.FunctionParam
import io.github.alexzhirkevich.keight.js.JSError
import io.github.alexzhirkevich.keight.js.JSFunction
import io.github.alexzhirkevich.keight.js.JSObject
import io.github.alexzhirkevich.keight.js.JSObjectImpl
import io.github.alexzhirkevich.keight.js.JsAny
import io.github.alexzhirkevich.keight.js.Object
import io.github.alexzhirkevich.keight.js.ReferenceError
import io.github.alexzhirkevich.keight.js.SyntaxError
import io.github.alexzhirkevich.keight.js.TypeError
import io.github.alexzhirkevich.keight.js.js
import io.github.alexzhirkevich.keight.js.setProto

internal open class JSErrorFunction(
    name : String = "Error",
    prototype : JSObject = Object {
        "message".js() eq "".js()
    },
    private val make : (Any?) -> JSError = { JSError(it) }
) : JSFunction(
    name = name,
    prototype = prototype,
    parameters = listOf(FunctionParam("msg" , default =  OpConstant("Uncaught $name".js()))),
    body = Expression { make(it.get("msg".js())) }
) {
    override suspend fun constructObject(args: List<JsAny?>, runtime: ScriptRuntime): JSObject {
        return make(args.getOrElse(0) { "Uncaught $name" })
    }
}

internal class JSTypeErrorFunction(errorFunction: JSErrorFunction) : JSErrorFunction(
    name = "TypeError",
    prototype = JSObjectImpl("Error")
        .apply { setProto(errorFunction.prototype) },
    make = ::TypeError
) {
    init { setProto(errorFunction) }
}

internal class JSReferenceErrorFunction(errorFunction: JSErrorFunction) : JSErrorFunction(
    name = "ReferenceError",
    prototype = JSObjectImpl("Error")
        .apply { setProto(errorFunction.prototype) },
    make = ::ReferenceError
) {
    init { setProto(errorFunction) }
}

internal class JSSyntaxErrorFunction(errorFunction: JSErrorFunction) : JSErrorFunction(
    name = "SyntaxError",
    prototype = JSObjectImpl("Error")
        .apply { setProto(errorFunction.prototype) },
    make = ::SyntaxError
) {
    init { setProto(errorFunction) }
}