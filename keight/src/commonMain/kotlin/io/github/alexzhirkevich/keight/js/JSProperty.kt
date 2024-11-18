package io.github.alexzhirkevich.keight.js

import io.github.alexzhirkevich.keight.Callable
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.js.interpreter.referenceCheck
import io.github.alexzhirkevich.keight.js.interpreter.typeCheck

public interface JSPropertyAccessor {

    public suspend fun get(runtime: ScriptRuntime) : Any?

    public suspend fun set(value : Any?, runtime: ScriptRuntime)

    public class Value(private var field: Any?) : JSPropertyAccessor {

        override suspend fun get(runtime: ScriptRuntime) : Any? = field

        override suspend fun set(value: Any?, runtime: ScriptRuntime) {
            field = value
        }
    }

    public class BackedField(
        private val getter: Callable?,
        private val setter: Callable?
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

public interface JSPropertyDescriptor {
    public val value : Any?
    public val writable : Boolean?
    public val enumerable : Boolean?
    public val configurable : Boolean?
}

internal class JSProperty(
    override var value : JSPropertyAccessor,
    override var writable : Boolean? = null,
    override var enumerable : Boolean? = null,
    override var configurable : Boolean? = null,
) : JSPropertyDescriptor

internal fun JSPropertyDescriptor.asObject() : JSObject {
    return Object {
        "value" eq value
        "writable" eq (writable ?: true)
        "configurable" eq (configurable ?: true)
        "enumerable" eq (enumerable ?: true)
    }
}