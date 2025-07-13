package io.github.alexzhirkevich.keight.js

import io.github.alexzhirkevich.keight.ScriptRuntime

public interface JsObject : JsAny {

    public val isExtensible : Boolean  get() = true

    public fun preventExtensions() {}

    public suspend fun values(runtime: ScriptRuntime): List<JsAny?> = emptyList()

    public suspend fun entries(runtime: ScriptRuntime): List<List<JsAny?>> = emptyList()

    public suspend fun set(
        property: JsAny?,
        value: JsAny?,
        runtime: ScriptRuntime,
    )

    public suspend fun setProperty(
        property: JsAny?,
        value: JsPropertyAccessor,
        runtime: ScriptRuntime,
        enumerable: Boolean? = null,
        configurable: Boolean? = null,
        writable: Boolean? = null,
    ) {
        set(property, value.get(runtime), runtime)
    }

    public suspend fun ownPropertyDescriptor(property: JsAny?) : JsProperty? = null

    public suspend fun ownPropertyDescriptors() : Map<JsAny?, JsProperty> = emptyMap()

    public suspend fun propertyIsEnumerable(name : JsAny?, runtime: ScriptRuntime) : Boolean {
        return true
    }

    public suspend fun hasOwnProperty(name : JsAny?, runtime: ScriptRuntime) : Boolean {
        return ownPropertyDescriptor(name) != null
    }
}
