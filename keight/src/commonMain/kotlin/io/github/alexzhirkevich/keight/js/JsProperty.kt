package io.github.alexzhirkevich.keight.js

import io.github.alexzhirkevich.keight.Callable
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.js.interpreter.typeCheck

public interface JsPropertyAccessor : JsAny {

    public suspend fun get(runtime: ScriptRuntime): JsAny?

    public suspend fun set(value: JsAny?, runtime: ScriptRuntime)

    public class Value(internal var field: JsAny?) : JsPropertyAccessor {

        override suspend fun get(runtime: ScriptRuntime): JsAny? = field

        override suspend fun set(value: JsAny?, runtime: ScriptRuntime) {
            field = value
        }
    }

    public class BackedField(
        private val getter: Callable?,
        private val setter: Callable? = null
    ) : JsPropertyAccessor, JsAny  {

        override suspend fun get(runtime: ScriptRuntime): JsAny? {
            return if (getter != null) {
                getter.invoke(emptyList(), runtime)
            } else {
                Undefined
            }
        }

        override suspend fun set(value: JsAny?, runtime: ScriptRuntime) {
            runtime.typeCheck(setter != null || !runtime.isStrict){
                "Cannot set property of which has only a getter".js
            }

            setter?.invoke(value.listOf(), runtime)
        }
    }
}

public interface JsProperty {
    public val value: JsPropertyAccessor
    public val writable: Boolean?
    public val enumerable: Boolean?
    public val configurable: Boolean?
}

public fun JsProperty.descriptor(): JsObject = Object {
    when (val v = value) {
        is JsPropertyAccessor.Value -> "value".js eq v
        is JsPropertyAccessor.BackedField -> {
            "get".func { v.get(this) }
            "set".func("v") { v.set(it[0], this); Undefined }
        }
    }
    "writable".js eq JSBooleanWrapper(writable != false)
    "enumerable".js eq JSBooleanWrapper(enumerable != false)
    "configurable".js eq JSBooleanWrapper(configurable != false)
}

internal class JSPropertyImpl(
    override var value : JsPropertyAccessor,
    override var writable : Boolean? = null,
    override var enumerable : Boolean? = null,
    override var configurable : Boolean? = null,
) : JsProperty {
    fun descriptor(): JsObject = Object {
        when (val v = value) {
            is JsPropertyAccessor.Value -> "value".js eq v
            is JsPropertyAccessor.BackedField -> {
                "get".func { v.get(this) }
                "set".func("v") { v.set(it[0], this); Undefined }
            }
        }
        "writable".js eq JSBooleanWrapper(writable != false)
        "enumerable".js eq JSBooleanWrapper(enumerable != false)
        "configurable".js eq JSBooleanWrapper(configurable != false)
    }
}

internal fun JsProperty(value : suspend ScriptRuntime.() -> JsAny?) : JsPropertyAccessor {
    return JsPropertyAccessor.BackedField(getter = Callable { value(this) })
}

