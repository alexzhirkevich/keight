package io.github.alexzhirkevich.keight.js

import io.github.alexzhirkevich.keight.Callable
import io.github.alexzhirkevich.keight.ScriptRuntime

public interface JsPropertyAccessor : JsAny {

    public suspend fun get(runtime: ScriptRuntime): JsAny?

    public suspend fun set(value: JsAny?, runtime: ScriptRuntime)

    public class Value(field: JsAny?) : JsPropertyAccessor {

        public var field : JsAny? = field
            internal set

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
            if (setter == null && !runtime.isStrict){
                runtime.typeError(
                    runtime.fromKotlin("Cannot set property of which has only a getter")
                )
            }


            setter?.invoke(listOf(value), runtime)
        }
    }
}

public interface JsProperty {
    public val value: JsPropertyAccessor
    public val writable: Boolean?
    public val enumerable: Boolean?
    public val configurable: Boolean?

    public suspend fun copy() : JsProperty
}

public fun JsProperty(value : suspend ScriptRuntime.() -> JsAny?) : JsPropertyAccessor {
    return JsPropertyAccessor.BackedField(getter = Callable { value(this) })
}
