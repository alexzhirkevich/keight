package io.github.alexzhirkevich.keight.js

import io.github.alexzhirkevich.keight.Console
import io.github.alexzhirkevich.keight.ScriptRuntime

internal fun JsConsole(io: () -> Console) = Object("console") {
    "log".func(FunctionParam("msg", isVararg = true)) { out(it, io()::verbose) }
    "info".func(FunctionParam("msg", isVararg = true)) { out(it, io()::info) }
    "debug".func(FunctionParam("msg", isVararg = true)) { out(it, io()::debug) }
    "warn".func(FunctionParam("msg", isVararg = true)) { out(it, io()::warn) }
    "error".func(FunctionParam("msg", isVararg = true)) { out(it, io()::error) }
}

private fun ScriptRuntime.out(message : List<Any?>, out : (Any?) -> Unit){
    if (message.isEmpty() || (message[0] as List<*>).isEmpty()) {
        return
    }
    val args = message[0] as List<*>
    if (args.size == 1) {
        out(toKotlin(args[0]))
    } else {
        out(args.map(::toKotlin))
    }
}
