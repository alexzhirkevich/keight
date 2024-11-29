package io.github.alexzhirkevich.keight.expressions

import io.github.alexzhirkevich.keight.Expression
import io.github.alexzhirkevich.keight.Getter
import io.github.alexzhirkevich.keight.LazyGetter
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.VariableType
import io.github.alexzhirkevich.keight.fastForEach
import io.github.alexzhirkevich.keight.fastForEachIndexed
import io.github.alexzhirkevich.keight.get
import io.github.alexzhirkevich.keight.js.JSStringFunction
import io.github.alexzhirkevich.keight.js.JsAny
import io.github.alexzhirkevich.keight.js.SyntaxError
import io.github.alexzhirkevich.keight.js.interpreter.syntaxCheck
import io.github.alexzhirkevich.keight.js.interpreter.typeCheck

internal fun interface Destruction {

    class Group(private val items: List<Destruction>) : Destruction {
        override suspend fun destruct(
            obj: Any?,
            variableType: VariableType?,
            runtime: ScriptRuntime,
            default : Any?
        ) {
            return items.fastForEach {
                it.destruct(obj, variableType, runtime, default)
            }
        }
    }

    suspend fun destruct(
        obj: Any?,
        variableType: VariableType?,
        runtime: ScriptRuntime,
        default : Any?
    )
}

internal sealed interface DestructionContext {

    suspend fun property(name : String?, obj: Any?, runtime: ScriptRuntime, default: Any?) : Any?

    object Object : DestructionContext {
        override suspend fun property(name : String?, obj: Any?, runtime: ScriptRuntime, default: Any?): Any? {
            runtime.typeCheck(obj is JsAny) {
                "${JSStringFunction.toString(obj, runtime)} is not an object"
            }
            return obj.get(name, runtime)
        }
    }

    class Array(val index : Int) : DestructionContext {
        override suspend fun property(name : String?, obj: Any?, runtime: ScriptRuntime, default: Any?): Any? {
            runtime.typeCheck(obj is List<*>) {
                "${JSStringFunction.toString(obj, runtime)} is not iterable"
            }

            val ret = obj.getOrElse(index) { Unit}

            if (ret != Unit || default !is List<*>){
                return ret
            }

            return default.getOrElse(index) { }
        }
    }
}

internal fun Expression.asDestruction(
    context: DestructionContext? = null
) : Destruction {
    return when (this) {
        is OpGetProperty -> {
            syntaxCheck(receiver == null || context is DestructionContext.Array){
                "Invalid destruction syntax"
            }
            Destruction { obj, variableType, runtime, default ->
                OpAssign.invoke(
                    variableName = name,
                    receiver = receiver,
                    type = variableType,
                    value = if (context != null) context.property(name, obj, runtime,default) else obj,
                    merge = null,
                    runtime = runtime
                )
            }
        }
        is OpIndex -> {
            syntaxCheck(context is DestructionContext.Array){
                "Invalid destruction syntax"
            }
            Destruction { obj, _, runtime, default->
                OpAssignByIndex.invoke(
                    index = index,
                    receiver = receiver,
                    value = context.property(null, obj, runtime,default),
                    merge = null,
                    runtime = runtime
                )
            }
        }
        is OpMakeArray -> Destruction.Group(
            items.mapIndexed { i, v ->
                v.asDestruction(DestructionContext.Array(i))
            }
        )
        is OpBlock -> Destruction.Group(
            expressions.map {
                it.asDestruction(DestructionContext.Object)
            }
        )
        is OpKeyValuePair -> {
            value.asDestruction().run {
                Destruction { obj, variableType, runtime, default ->
                    runtime.typeCheck(obj is JsAny) {
                        "${JSStringFunction.toString(obj, runtime)} is not an object"
                    }
                    destruct(
                        obj = obj.get(key, runtime),
                        variableType = variableType,
                        runtime = runtime,
                        default = default
                    )
                }
            }
        }
        else -> throw SyntaxError("Invalid destruction syntax")
    }
}

internal class OpDestructAssign(
    val destruction: Destruction,
    val variableType: VariableType?,
    val value: Expression,
) : Expression() {

    override suspend fun execute(runtime: ScriptRuntime): Any? {
        destruction.destruct(value(runtime), variableType, runtime) { Unit }
        return Unit
    }
}