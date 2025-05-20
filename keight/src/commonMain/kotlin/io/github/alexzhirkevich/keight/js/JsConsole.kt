package io.github.alexzhirkevich.keight.js

import io.github.alexzhirkevich.keight.Console
import io.github.alexzhirkevich.keight.ScriptRuntime

internal fun JsConsole(io: () -> Console) = Object("console") {
    "log".js().func("msg".vararg()) { out(it, io()::verbose) }
    "info".js().func("msg".vararg()) { out(it, io()::info) }
    "debug".js().func("msg".vararg()) { out(it, io()::debug) }
    "warn".js().func("msg".vararg()) { out(it, io()::warn) }
    "error".js().func("msg".vararg()) { out(it, io()::error) }
}

private fun ScriptRuntime.out(message : List<Any?>, out : (Any?) -> Unit) : JsAny{
    if (message.isEmpty() || (message[0] as List<*>).isEmpty()) {
        return Undefined
    }
    val args = message[0] as List<JsAny?>
    if (args.size == 1) {
        out(toKotlin(args[0]))
    } else {
        out(args.map(::toKotlin))
    }
    return Undefined
}
