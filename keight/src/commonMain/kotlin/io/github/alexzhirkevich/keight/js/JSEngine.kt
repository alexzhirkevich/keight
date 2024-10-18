package io.github.alexzhirkevich.keight.js

import io.github.alexzhirkevich.keight.Module
import io.github.alexzhirkevich.keight.ScriptEngine
import io.github.alexzhirkevich.keight.es.ESInterpreter
import io.github.alexzhirkevich.keight.evaluate

public fun JSEngine(
    runtime: JSRuntime = JSRuntime(),
    interpreter: ESInterpreter = ESInterpreter(JSLangContext),
    vararg modules : Module
) : ScriptEngine = ScriptEngine(runtime, interpreter, *modules)

/**
 * Simplest way to evaluate some JavaScript code.
 *
 * Unlike Kotlin/JS, [script] is not required to be a compile-time constant.
 * To get a persistent runtime or compile a reusable script use [JSEngine].
 * */
public fun js(script : String) : Any? = JSEngine(interpreter = JSInterpreter).evaluate(script)

private val JSInterpreter = ESInterpreter(JSLangContext)
