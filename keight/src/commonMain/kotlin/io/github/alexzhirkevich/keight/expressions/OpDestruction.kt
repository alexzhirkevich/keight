package io.github.alexzhirkevich.keight.expressions

import io.github.alexzhirkevich.keight.Expression
import io.github.alexzhirkevich.keight.Getter
import io.github.alexzhirkevich.keight.LazyGetter
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.VariableType
import io.github.alexzhirkevich.keight.fastForEach
import io.github.alexzhirkevich.keight.js.JSFunction
import io.github.alexzhirkevich.keight.js.JSObject
import io.github.alexzhirkevich.keight.js.JSObjectImpl
import io.github.alexzhirkevich.keight.js.JSStringFunction
import io.github.alexzhirkevich.keight.js.JsAny
import io.github.alexzhirkevich.keight.js.JsArrayWrapper
import io.github.alexzhirkevich.keight.js.OpClassInit
import io.github.alexzhirkevich.keight.js.SyntaxError
import io.github.alexzhirkevich.keight.js.interpreter.syntaxCheck
import io.github.alexzhirkevich.keight.js.interpreter.typeCheck
import io.github.alexzhirkevich.keight.js.interpreter.typeError

internal fun interface Destruction {

    class Group(
        val items: List<Destruction>,
        private val context: DestructionContext?
    ) : Destruction {

        init {
            if (items.isNotEmpty() && items.any { it is SpreadDestruction } && items.last() !is SpreadDestruction) {
                throw SyntaxError("Rest element must be last element")
            }
        }

        override suspend fun destruct(
            obj: Any?,
            variableType: VariableType?,
            runtime: ScriptRuntime,
            default: Getter<*>?
        ) {
            return items.fastForEach {
                val v = if (context != null) {
                    context.property(null, obj, runtime, default)
                } else obj
                it.destruct(v, variableType, runtime, default)
            }
        }
    }

    suspend fun destruct(
        obj: Any?,
        variableType: VariableType?,
        runtime: ScriptRuntime,
        default : Getter<*>?
    )
}

private val EmptyDestruction = Destruction { _, _, _, _ ->  }

internal sealed interface DestructionContext {

    suspend fun property(name : String?, obj: Any?, runtime: ScriptRuntime, default: Getter<*>?) : Any?

    object Object : DestructionContext {
        override suspend fun property(
            name: String?,
            obj: Any?,
            runtime: ScriptRuntime,
            default: Getter<*>?
        ): Any? {
            return when {
                obj is JsAny && obj.contains(name, runtime) -> obj.get(name, runtime)
                default != null -> property(name, default.get(runtime), runtime, null)
                else -> runtime.typeError { "cannot read properties of $obj" }
            }
        }
    }

    class Array(val index : Int) : DestructionContext {
        override suspend fun property(
            name: String?,
            obj: Any?,
            runtime: ScriptRuntime,
            default: Getter<*>?
        ): Any? {
            val ret = if (obj is List<*>) {
                obj.getOrElse(index) { Unit }
            } else {
                if (obj == null) {
                    runtime.typeError { "null is not iterable" }
                } else {
                    Unit
                }
            }

            if (ret != Unit || default == null){
                return ret
            }

            val d = default.get(runtime)

            if (d !is List<*>){
                return Unit
            }
//            runtime.typeCheck (d is List<*>) {
//                "${JSStringFunction.toString(d, runtime)} is not iterable"
//            }

            return d.getOrElse(index) { }
        }
    }
}

internal fun Expression.asDestruction(
    context: DestructionContext? = null
) : Destruction {
    return when (this) {
        is OpDestructAssign -> {

//            if (
//                context is DestructionContext.Array &&
//                destruction is Destruction.Group &&
//                context.index in destruction.items.indices &&
//                destruction.items[context.index] is SpreadDestruction
//            ) {
//                throw SyntaxError("Invalid destructuring assignment target")
//            }

            when (context){
                is DestructionContext.Array -> Destruction { obj, variableType, runtime, _ ->
                    obj as List<*>
                    destruction.destruct(
                        obj = obj.getOrElse(context.index) { Unit },
                        variableType = variableType,
                        runtime = runtime,
                        default = LazyGetter(value::invoke)
                    )
                }
                else -> invalidDestruction()
            }
        }
        is OpAssign -> {
            Destruction { obj, variableType, runtime, _ ->
                OpAssign.invoke(
                    variableName = variableName,
                    receiver = receiver,
                    type = variableType,
                    value = if (context != null) {
                        val v = context.property(
                            name = variableName,
                            obj = obj,
                            runtime = runtime,
                            default = when (context) {
                                is DestructionContext.Array -> Getter { emptyList<Any>() }
                                DestructionContext.Object -> Getter { JSObjectImpl() }
                            }
                        )
                        if (v != Unit) {
                            v
                        } else {

                            val defaultValue = assignableValue.invoke(runtime)

                            // anonymous function/class should be named as variable
                            if ((assignableValue is OpConstant || assignableValue is OpClassInit
                                || (assignableValue is OpTouple && assignableValue.singleRecursiveOrNull() != null))
                                && defaultValue is JSFunction
                                && defaultValue.get("name", runtime) == ""
                            ) {
                                defaultValue.defineName(variableName)
                            }
                            defaultValue
                        }
                    }
                    else {
                        obj
                    },
                    merge = null,
                    runtime = runtime
                )
            }
        }
        is OpGetProperty -> {
            syntaxCheck(receiver == null || context is DestructionContext.Array){
                "Invalid destruction syntax"
            }
            Destruction { obj, variableType, runtime, default ->
                OpAssign.invoke(
                    variableName = name,
                    receiver = receiver,
                    type = variableType,
                    value = if (context != null) {
                        context.property(
                            name = name,
                            obj = obj,
                            runtime = runtime,
                            default = default
                        )
                    } else {
                        obj
                    },
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
                    value = context.property(
                        name = null,
                        obj = obj,
                        runtime = runtime,
                        default = default
                    ),
                    merge = null,
                    runtime = runtime
                )
            }
        }
        is OpMakeArray -> Destruction.Group(
            items.mapIndexed { i, v ->
                v.asDestruction(DestructionContext.Array(i))
            },
            context = context
        )
        is OpMakeObject -> Destruction.Group(
            items.map {
                it.asDestruction(DestructionContext.Object)
            },
            context = context
        )
        is OpBlock -> Destruction.Group(
            expressions.map {
                it.asDestruction(DestructionContext.Object)
            },
            context = context
        )
        is OpKeyValuePair -> {
            value.asDestruction().run {
                Destruction { obj, variableType, runtime, default ->
                    destruct(
                        obj = DestructionContext.Object.property(key, obj, runtime, default),
                        variableType = variableType,
                        runtime = runtime,
                        default = null
                    )
                }
            }
        }
        is OpSpread -> {
            syntaxCheck(context is DestructionContext.Array){
                "Invalid destruction syntax"
            }

            syntaxCheck(value !is OpAssign){
                "Rest parameter may not have a default initialize"
            }

            SpreadDestruction(value.asDestruction(), context)
        }
        is OpConstant -> {
            if (value == Unit){
                EmptyDestruction
            } else {
                invalidDestruction()
            }
        }
        else -> invalidDestruction()
    }
}

private class SpreadDestruction(
    private val value: Destruction,
    private val context: DestructionContext.Array
) : Destruction {
    override suspend fun destruct(
        obj: Any?,
        variableType: VariableType?,
        runtime: ScriptRuntime,
        default: Getter<*>?
    ) {
        val arr = if (obj is List<*>) {
            obj
        } else {
            when (val d = default?.get(runtime)) {
                is List<*> -> d
                is Unit -> runtime.typeError {
                    "${JSStringFunction.toString(obj, runtime)} is not iterable"
                }

                else -> runtime.typeError {
                    "${JSStringFunction.toString(d, runtime)} is not iterable"
                }
            }
        }
        value.destruct(
            obj = JsArrayWrapper(arr.drop(context.index).toMutableList()),
            variableType = variableType,
            runtime = runtime,
            default = default
        )
    }
}

private fun invalidDestruction() : Nothing = throw SyntaxError("Invalid destruction syntax")

internal class OpDestructAssign(
    val destruction: Destruction,
    val variableType: VariableType?,
    val value: Expression,
) : Expression() {

    override suspend fun execute(runtime: ScriptRuntime): Any? {
        destruction.destruct(value(runtime), variableType, runtime, null)
        return Unit
    }
}