package io.github.alexzhirkevich.keight.js

import io.github.alexzhirkevich.keight.Module
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.VariableType

/**
 * JSON module for the EcmaScript runtime
 * */
public val Module.Companion.JSON : Module get() = JSONModule

private object JSONModule : Module  {

    override fun importInto(runtime: ScriptRuntime) {
        runtime.set("JSON", JsJSON(), VariableType.Global)
    }
}
