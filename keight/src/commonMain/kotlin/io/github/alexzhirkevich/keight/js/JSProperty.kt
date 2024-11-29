package io.github.alexzhirkevich.keight.js

import io.github.alexzhirkevich.keight.Callable
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.js.interpreter.referenceCheck
import io.github.alexzhirkevich.keight.js.interpreter.typeCheck

public interface JSPropertyAccessor {

    public suspend fun get(runtime: ScriptRuntime) : Any?

    public suspend fun set(value : Any?, runtime: ScriptRuntime)

    public class Value(internal var field: Any?) : JSPropertyAccessor {

        override suspend fun get(runtime: ScriptRuntime) : Any? = field

        override suspend fun set(value: Any?, runtime: ScriptRuntime) {
            field = value
        }
    }

    public class BackedField(
        private val getter: Callable?,
        private val setter: Callable? = null
    ) : JSPropertyAccessor {

        override suspend fun get(runtime: ScriptRuntime): Any? {
            return if (getter != null) {
                getter.invoke(emptyList(), runtime)
            } else {
                Unit
            }
        }

        override suspend fun set(value: Any?, runtime: ScriptRuntime) {
            runtime.typeCheck(setter != null || !runtime.isStrict){
                "Cannot set property of which has only a getter"
            }

            setter?.invoke(value.listOf(), runtime)
        }
    }
}

public interface JSProperty {
    public val value: JSPropertyAccessor
    public val writable: Boolean?
    public val enumerable: Boolean?
    public val configurable: Boolean?
}

public fun JSProperty.descriptor(): JSObject = Object {
    when (val v = value) {
        is JSPropertyAccessor.Value -> "value" eq v
        is JSPropertyAccessor.BackedField -> {
            "get".func { v.get(this) }
            "set".func("v") { v.set(it[0], this) }
        }
    }
    "writable" eq JSBooleanWrapper(writable != false)
    "enumerable" eq JSBooleanWrapper(enumerable != false)
    "configurable" eq JSBooleanWrapper(configurable != false)
}

internal class JSPropertyImpl(
    override var value : JSPropertyAccessor,
    override var writable : Boolean? = null,
    override var enumerable : Boolean? = null,
    override var configurable : Boolean? = null,
) : JSProperty {
    fun descriptor(): JSObject = Object {
        when (val v = value) {
            is JSPropertyAccessor.Value -> "value" eq v
            is JSPropertyAccessor.BackedField -> {
                "get".func { v.get(this) }
                "set".func("v") { v.set(it[0], this) }
            }
        }
        "writable" eq JSBooleanWrapper(writable != false)
        "enumerable" eq JSBooleanWrapper(enumerable != false)
        "configurable" eq JSBooleanWrapper(configurable != false)
    }
}

