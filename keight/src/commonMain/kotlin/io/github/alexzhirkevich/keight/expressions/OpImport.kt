package io.github.alexzhirkevich.keight.expressions

import io.github.alexzhirkevich.keight.Expression
import io.github.alexzhirkevich.keight.ModuleRuntime
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.VariableType
import io.github.alexzhirkevich.keight.findJsRoot
import io.github.alexzhirkevich.keight.js.JsAny
import io.github.alexzhirkevich.keight.js.SyntaxError
import io.github.alexzhirkevich.keight.js.Undefined
import io.github.alexzhirkevich.keight.js.interpreter.syntaxCheck
import io.github.alexzhirkevich.keight.js.js


internal class OpImport(
    private val entries: List<ImportEntry>,
    private val fromModule : Expression
) : Expression() {
    override suspend fun execute(runtime: ScriptRuntime): JsAny? {
        val root = runtime.findJsRoot()
        val name = runtime.toString(fromModule(runtime))

        val module = root.modules[name] ?: throw SyntaxError("Module with name '$name' not found")

        module.invokeIfNeeded()

        entries.forEach {
            it.import(module.runtime, runtime)
        }

        return Undefined
    }

}

internal sealed interface ImportEntry {

    val alias : String?

    suspend fun import(from : ModuleRuntime, into: ScriptRuntime)

    class Default(
        override val alias: String
    ) : ImportEntry {

        override suspend fun import(
            from: ModuleRuntime,
            into: ScriptRuntime
        ) {
            syntaxCheck(from.exports.contains(null, from)) {
                "Module does not provide default export"
            }
            into.set(
                property = alias.js,
                value = from.exports.get(null, from),
                type = VariableType.Const
            )
        }

        override fun toString(): String {
            return "Default(alias='$alias')"
        }
    }

    class Named(
        val import : String,
        override val alias: String?
    ) : ImportEntry {

        override suspend fun import(
            from: ModuleRuntime,
            into: ScriptRuntime
        ) {
            into.set(
                property = (alias ?: import).js,
                value = from.exports.get(import.js, from),
                type = VariableType.Const
            )
        }

        override fun toString(): String {
            return "Named(import='$import', alias=$alias)"
        }

    }


    class Star(
        override val alias: String?
    ) : ImportEntry {

        override suspend fun import(
            from: ModuleRuntime,
            into: ScriptRuntime
        ) {
            if (alias == null) {
                from.exports.entries(from).forEach { (key, value) ->
                    into.set(key, value, VariableType.Const)
                }
            } else {
                into.set(alias.js, from.exports, VariableType.Const)
            }
        }

        override fun toString(): String {
            return "Star(alias=$alias)"
        }


    }
}
