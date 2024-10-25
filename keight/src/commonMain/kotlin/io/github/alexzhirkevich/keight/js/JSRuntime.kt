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

    init { recreate() }

    override fun reset() {
        super.reset()
        recreate()
    }

    private fun recreate() {
        set("console", JsConsole(), VariableType.Global)
    }
}


