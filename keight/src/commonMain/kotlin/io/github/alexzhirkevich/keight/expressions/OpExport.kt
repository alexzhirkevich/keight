package io.github.alexzhirkevich.keight.expressions

import io.github.alexzhirkevich.keight.Expression
import io.github.alexzhirkevich.keight.ModuleRuntime
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.VariableType
import io.github.alexzhirkevich.keight.findJsRoot
import io.github.alexzhirkevich.keight.findModule
import io.github.alexzhirkevich.keight.js.JsAny
import io.github.alexzhirkevich.keight.js.SyntaxError
import io.github.alexzhirkevich.keight.js.Undefined
import io.github.alexzhirkevich.keight.js.interpreter.referenceCheck
import io.github.alexzhirkevich.keight.js.js
import kotlin.collections.component1
import kotlin.collections.component2

internal class OpExport(
    val name : String?,
    val property : Expression
) : Expression() {

    override suspend fun execute(runtime: ScriptRuntime): JsAny? {
        val module = runtime.findModule()
            ?: throw SyntaxError("Export is only available from modules")

        module.exports.set(name?.js, property(runtime), runtime)
        return Undefined
    }
}

internal sealed interface AggregatingExportEntry {

    val alias : String?

    val assignPropertyForAlias : Boolean

    suspend fun export(thisModule : ModuleRuntime, fromModule: ModuleRuntime, runtime : ScriptRuntime) : JsAny?

    class Star(
        override val alias: String?,
        override val assignPropertyForAlias: Boolean
    ) : AggregatingExportEntry {

        override suspend fun export(
            thisModule : ModuleRuntime,
            fromModule: ModuleRuntime,
            runtime: ScriptRuntime
        ) : JsAny? {
            return if (alias == null) {
                fromModule.exports.entries(runtime).forEach { (key, value) ->
                    thisModule.exports.set(key, value, runtime)
                }
                Undefined
            } else {
                thisModule.exports.set(alias.js, fromModule.exports, runtime)
                fromModule.exports
            }
        }
    }

    class Single(
        val import : String?,
        override val alias: String?,
        override val assignPropertyForAlias: Boolean
    ) : AggregatingExportEntry {

        override suspend fun export(
            thisModule : ModuleRuntime,
            fromModule: ModuleRuntime,
            runtime: ScriptRuntime
        ) : JsAny? {
            val importValue = fromModule.exports.get(import?.js, runtime)
            thisModule.exports.set((alias ?: import)?.js, importValue, runtime)
            return importValue
        }
    }
}

internal fun OpAggregatingExport(
    exports : List<AggregatingExportEntry>,
    fromModule : Expression,
) = Expression { runtime ->

    val thisModule = runtime.findModule()
        ?: throw SyntaxError("Export is only available from modules")

    val moduleName = runtime.toString(fromModule(runtime))
    val fromModule = runtime.findJsRoot().modules[moduleName]

    runtime.referenceCheck(fromModule != null) {
        "Module with name $moduleName not found".js
    }

    fromModule.invokeIfNeeded()

    exports.forEach {
        val exported = it.export(thisModule, fromModule.runtime, runtime)

        val alias = it.alias

        if (it.assignPropertyForAlias && alias != null) {
            OpAssign.invoke(
                type = VariableType.Const,
                variableName = alias,
                receiver = null,
                value = exported,
                merge = null,
                runtime = runtime
            )
        }
    }

    Undefined
}