package io.github.alexzhirkevich.keight

import io.github.alexzhirkevich.keight.js.JSEngine
import io.github.alexzhirkevich.keight.js.JSRuntime
import io.github.alexzhirkevich.keight.ScriptEngine as KeightScriptEngine
import javax.script.ScriptEngine
import javax.script.ScriptEngineFactory

public class KeightScriptEngineFactory : ScriptEngineFactory  {

    internal val globalRuntime : ScriptRuntime = JSRuntime()

    override fun getEngineName(): String {
        return "Keight"
    }

    override fun getEngineVersion(): String {
        return Keight.version
    }

    override fun getExtensions(): MutableList<String> {
        return mutableListOf("js")
    }

    override fun getMimeTypes(): MutableList<String> {
        return mutableListOf("text/javascript")
    }

    override fun getNames(): MutableList<String> {
        return mutableListOf(engineName, "JavaScript")
    }

    override fun getLanguageName(): String {
        return "JavaScript"
    }

    override fun getLanguageVersion(): String {
        return "ES5"
    }

    override fun getParameter(p0: String?): Any? = null

    override fun getMethodCallSyntax(obj: String?, method: String?, vararg p2: String?): String {
        return "$obj.$method(${p2.joinToString(separator = ",")})"
    }

    override fun getOutputStatement(p0: String?): String {
        return "console.log($p0)"
    }

    override fun getProgram(vararg statements: String?): String {
        return statements.joinToString(separator = "\n")
    }

    override fun getScriptEngine(): ScriptEngine {
        return getScriptEngine(JSEngine())
    }

    public fun getScriptEngine(engine: KeightScriptEngine) : ScriptEngine {
        return KeightScriptEngine(this, engine)
    }
}