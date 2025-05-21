package io.github.alexzhirkevich.keight.expressions

import io.github.alexzhirkevich.keight.Expression
import io.github.alexzhirkevich.keight.Getter
import io.github.alexzhirkevich.keight.LazyGetter
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.VariableType
import io.github.alexzhirkevich.keight.fastForEach
import io.github.alexzhirkevich.keight.js.JSFunction
import io.github.alexzhirkevich.keight.js.JSStringFunction
import io.github.alexzhirkevich.keight.js.JsAny
import io.github.alexzhirkevich.keight.js.JsArrayWrapper
import io.github.alexzhirkevich.keight.js.OpClassInit
import io.github.alexzhirkevich.keight.js.SyntaxError
import io.github.alexzhirkevich.keight.js.Undefined
import io.github.alexzhirkevich.keight.js.interpreter.syntaxCheck
import io.github.alexzhirkevich.keight.js.interpreter.typeError
import io.github.alexzhirkevich.keight.js.js
import kotlin.reflect.KClass

internal fun Destruction(
    variables : List<String>,
    block : suspend (obj: JsAny?, variableType: VariableType?, runtime: ScriptRuntime, default: Getter<*>?) -> Unit
) = object  : Destruction {

    override val variables: List<String> get() = variables

    override suspend fun destruct(
        obj: JsAny?,
        variableType: VariableType?,
        runtime: ScriptRuntime,
        default: Getter<*>?
    ) {
        block(obj, variableType, runtime, default)
    }

}

internal interface Destruction {

    val variables : List<String>

    class Group(
        val items: List<Destruction>,
        val context: DestructionContext?,
        val itemsContext : KClass<out DestructionContext>?
    ) : Destruction {

        override val variables: List<String> = items.flatMap { it.variables }

        init {
            if (items.isNotEmpty() && items.any { it is SpreadDestruction } && items.last() !is SpreadDestruction) {
                throw SyntaxError("Rest element must be last element")
            }
        }

        override suspend fun destruct(
            obj: JsAny?,
            variableType: VariableType?,
            runtime: ScriptRuntime,
            default: Getter<*>?
        ) {

            // const f = ({} = null) => {};
            // assert.throws(TypeError, function() { f(); }
            if (itemsContext == DestructionContext.Object::class && (obj !is JsAny) && default == null){
                runtime.typeError { "cannot read properties of $obj" }
            }

            items.fastForEach {
                val v = if (context != null) {
                    context.property(null, obj, runtime, default)
                } else obj

                it.destruct(v, variableType, runtime, default)
            }
        }
    }

    suspend fun destruct(
        obj: JsAny?,
        variableType: VariableType?,
        runtime: ScriptRuntime,
        default: Getter<*>?
    )
}

private val EmptyDestruction = Destruction(emptyList()) { _, _, _, _ ->  }

internal sealed interface DestructionContext {

    suspend fun property(name: String?, obj: Any?, runtime: ScriptRuntime, default: Getter<*>?): JsAny?

    object Object : DestructionContext {
        override suspend fun property(
            name: String?,
            obj: Any?,
            runtime: ScriptRuntime,
            default: Getter<*>?
        ): JsAny? {
            return when {
                obj is JsAny && name != null -> obj.get(name.js(), runtime)/*.let {
                    if (it is Unit && default != null) {
                        property(name, default.get(runtime), runtime, null)
                    } else {
                        it
                    }
                }*/
                default != null -> property(name, default.get(runtime), runtime, null)
                else -> runtime.typeError { "can't get properties of $obj" }
            }
        }
    }

    class Array(val index : Int) : DestructionContext {
        override suspend fun property(
            name: String?,
            obj: Any?,
            runtime: ScriptRuntime,
            default: Getter<*>?
        ): JsAny? {
            val ret = if (obj is List<*>) {
                obj as List<JsAny>
                obj.getOrElse(index) { Undefined }
            } else {
                if (obj == null) {
                    runtime.typeError { "null is not iterable" }
                } else {
                    Undefined
                }
            }

            if (ret != Undefined || default == null){
                return ret
            }

            val d = default.get(runtime)

            if (d !is List<*>){
                return Undefined
            }
//            runtime.typeCheck (d is List<*>) {
//                "${JSStringFunction.toString(d, runtime)} is not iterable"
//            }
            d as List<JsAny?>
            return d.getOrElse(index) { Undefined }
        }
    }
}

internal fun Expression.asDestruction(
    context: DestructionContext? = null
) : Destruction {
    return when (this) {
        is OpDestructAssign -> {
            DestructionWithDefault(destruction, context, value)
//            if (context is DestructionContext.Array) {
//                Destruction { obj, variableType, runtime, default ->
//                    obj as List<*>
//                    destruction.destruct(
//                        obj = obj.getOrElse(context.index) { Unit },
//                        variableType = variableType,
//                        runtime = runtime,
//                        default = default ?: LazyGetter(value::invoke)
//                    )
//                }
//            } else {
//                Destruction { obj, variableType, runtime, default ->
//                    destruction.destruct(
//                        obj = obj,
//                        variableType = variableType,
//                        runtime = runtime,
//                        default = default ?: LazyGetter(value::invoke)
//                    )
//                }
//            }
        }
        is OpAssign -> {
            Destruction(listOf(this.variableName)) { obj, variableType, runtime, default ->
                OpAssign.invoke(
                    variableName = variableName,
                    receiver = receiver,
                    type = variableType,
                    value = if (context != null) {
                        val v = context.property(
                            name = variableName,
                            obj = obj,
                            runtime = runtime,
                            default = default
                        )
                        if (v != Undefined) {
                            v
                        } else {

                            val defaultValue = assignableValue.invoke(runtime)

                            // anonymous function/class should be named as variable
                            if ((assignableValue is OpConstant || assignableValue is OpClassInit
                                || (assignableValue is OpTouple && assignableValue.singleRecursiveOrNull() != null))
                                && defaultValue is JSFunction
                                && JSStringFunction.toString(defaultValue.get("name".js(), runtime),runtime).isEmpty()
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
            Destruction(listOf(this.name)) { obj, variableType, runtime, default ->
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
            Destruction(emptyList()) { obj, _, runtime, default ->
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
            context = context,
            itemsContext = DestructionContext.Array::class
        )
        is OpMakeObject -> Destruction.Group(
            items.map {
                it.asDestruction(DestructionContext.Object)
            },
            context = context,
            itemsContext = DestructionContext.Object::class
        )
        is OpBlock -> Destruction.Group(
            expressions.map {
                it.asDestruction(DestructionContext.Object)
            },
            context = context,
            itemsContext = DestructionContext.Object::class
        )
        is OpKeyValuePair -> {
            value.asDestruction().let {
                Destruction(it.variables + key) { obj, variableType, runtime, default ->
                    it.destruct(
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
            syntaxCheck(value !is OpAssign && value !is OpDestructAssign){
                "Rest parameter may not have a default initializer"
            }
            SpreadDestruction(value.asDestruction(), context)
        }
        is OpConstant -> {
            if (value == Undefined){
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

    override val variables: List<String>
        get() = value.variables

    override suspend fun destruct(
        obj: JsAny?,
        variableType: VariableType?,
        runtime: ScriptRuntime,
        default: Getter<*>?
    ) {
        when {
            obj is Iterable<*> -> {
                obj as Iterable<JsAny?>
                value.destruct(
                    obj = JsArrayWrapper(obj.drop(context.index).toMutableList()),
                    variableType = variableType,
                    runtime = runtime,
                    default = default
                )
            }
            default != null -> destruct(default.get(runtime), variableType, runtime, null)
            else -> runtime.typeError {
                "${JSStringFunction.toString(obj, runtime)} is not iterable"
            }
        }
    }
}

private class DestructionWithDefault(
    private val destruction: Destruction,
    private val context: DestructionContext?,
    var defaultValue : Expression
) : Destruction {

    override val variables: List<String>
        get() = destruction.variables

    override suspend fun destruct(
        obj: JsAny?,
        variableType: VariableType?,
        runtime: ScriptRuntime,
        default: Getter<*>?
    ) {
        if (context is DestructionContext.Array) {
            obj as List<JsAny?>
            destruction.destruct(
                obj = obj.getOrElse(context.index) { Undefined },
                variableType = variableType,
                runtime = runtime,
                default = default ?: LazyGetter(defaultValue::invoke)
            )
        } else {
            destruction.destruct(
                obj = obj,
                variableType = variableType,
                runtime = runtime,
                default = default ?: LazyGetter(defaultValue::invoke)
            )
        }
    }
}

private fun invalidDestruction() : Nothing = throw SyntaxError("Invalid destruction syntax")

internal class OpDestructAssign(
    val destruction: Destruction,
    val variableType: VariableType?,
    val value: Expression,
) : Expression() {

    override suspend fun execute(runtime: ScriptRuntime): JsAny? {
        destruction.destruct(value(runtime), variableType, runtime, null)
        return Undefined
    }
}