package io.github.alexzhirkevich.keight.js

import io.github.alexzhirkevich.keight.DefaultScriptIO
import io.github.alexzhirkevich.keight.LangContext
import io.github.alexzhirkevich.keight.ScriptIO
import io.github.alexzhirkevich.keight.VariableType
import io.github.alexzhirkevich.keight.es.ESNumber
import io.github.alexzhirkevich.keight.es.ESObject
import io.github.alexzhirkevich.keight.es.ESRuntime
import io.github.alexzhirkevich.keight.es.init


public open class JSRuntime(
    io: ScriptIO = DefaultScriptIO
) : ESRuntime(io = io), LangContext by JSLangContext {

    init {
        recreate()
    }

    override fun reset() {
        super.reset()
        recreate()
    }

    private fun recreate() {
        set("console", JsConsole(), VariableType.Const)

        init {
            val number = get("Number") as ESNumber
            val globalThis = get("globalThis") as ESObject

            globalThis.set("parseInt", number.parseInt)
            globalThis.set("parseFloat", number.parseFloat)
            globalThis.set("isFinite", number.isFinite)
            globalThis.set("isNan", number.isNan)
            globalThis.set("isInteger", number.isInteger)
            globalThis.set("isSafeInteger", number.isSafeInteger)
        }
    }
}


