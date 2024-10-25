package io.github.alexzhirkevich.keight.es

import io.github.alexzhirkevich.keight.LangContext
import io.github.alexzhirkevich.keight.Script
import io.github.alexzhirkevich.keight.ScriptInterpreter
import io.github.alexzhirkevich.keight.es.interpreter.ESTokenInterpreter

public class ESInterpreter(
    private val langContext: LangContext,
) : ScriptInterpreter {

    override fun interpret(script: String): Script {
        return ESTokenInterpreter(
            script = script,
            langContext = langContext
        ).interpret()
    }
}
