package io.github.alexzhirkevich.keight.es

import io.github.alexzhirkevich.keight.InterpretationContext
import io.github.alexzhirkevich.keight.LangContext
import io.github.alexzhirkevich.keight.Script
import io.github.alexzhirkevich.keight.ScriptInterpreter
import io.github.alexzhirkevich.keight.es.interpreter.ESTokenInterpreter

public class ESInterpreter(
    private val langContext: LangContext,
    private val interpretationContext : InterpretationContext = ESInterpretationContext(false),
) : ScriptInterpreter {

    override fun interpret(script: String): Script {
//        return ESInterpreterImpl(
//            expr = script,
//            langContext = langContext,
//            globalContext = interpretationContext
//        ).interpret()

        return ESTokenInterpreter(
            script = script,
            langContext = langContext
        ).interpret()
    }
}
