package io.github.alexzhirkevich.keight.js

import io.github.alexzhirkevich.keight.Callable
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.callableOrNull
import io.github.alexzhirkevich.keight.js.interpreter.typeCheck
import io.github.alexzhirkevich.keight.sameValue

public fun JsProperty.descriptor(): JsObject = Object {
    when (val v = value) {
        is JsPropertyAccessor.Value -> Constants.value.js eq v.field
        is JsPropertyAccessor.BackedField -> {
            Constants.get.func { v.get(this) }
            Constants.set.func("v") { v.set(it[0], this); Undefined }
        }
    }
    Constants.writable.js eq JSBooleanWrapper(writable != false)
    Constants.enumerable.js eq JSBooleanWrapper(enumerable != false)
    Constants.configurable.js eq JSBooleanWrapper(configurable != false)
}

internal class JSPropertyImpl(
    override var value : JsPropertyAccessor,
    override var writable : Boolean? = null,
    override var enumerable : Boolean? = null,
    override var configurable : Boolean? = null,
) : JsProperty {

    fun descriptor(): JsObject = Object {
        when (val v = value) {
            is JsPropertyAccessor.Value -> Constants.value.js eq v
            is JsPropertyAccessor.BackedField -> {
               Constants.get.func { v.get(this) }
               Constants.set.func("v") { v.set(it[0], this); Undefined }
            }
        }
        Constants.writable.js eq JSBooleanWrapper(writable != false)
        Constants.enumerable.js eq JSBooleanWrapper(enumerable != false)
        Constants.configurable.js eq JSBooleanWrapper(configurable != false)
    }

    override suspend fun copy(): JsProperty {
        return JSPropertyImpl(
            value = value,
            writable = writable,
            enumerable = enumerable,
            configurable = configurable
        )
    }
}


internal suspend fun JsAny.defineOwnPropertyOrThrow(
    property: JsAny?,
    desc : JsAny,
    runtime: ScriptRuntime
) {
    runtime.typeCheck(ordinaryDefineOwnProperty(property, desc, runtime)){
        "Can't set property $property".js
    }
}

internal suspend fun JsAny.ordinaryDefineOwnProperty(
    property: JsAny?,
    desc : JsAny,
    runtime: ScriptRuntime
): Boolean {
    val current = if (this is JsObject){
        ownPropertyDescriptor(property)
    } else null

    val extensible = if (this is JsObject){
        isExtensible
    }  else {
        false
    }
    return validateAndApplyPropertyDescriptor(
        name = property,
        extensible = extensible,
        desc = desc,
        current = current,
        runtime = runtime
    )
}

internal suspend fun JsAny.validateAndApplyPropertyDescriptor(
    name: JsAny?,
    extensible : Boolean,
    desc : JsAny,
    current : JsProperty?,
    runtime: ScriptRuntime
) : Boolean {
    if (current == null) {
        if (!extensible) {
            return false
        }
        if (this is Undefined) {
            return true
        }
        val p = when {
            desc.isAccessorDescriptor(runtime) ->
                JsPropertyAccessor.BackedField(
                    desc.get(Constants.get.js, runtime)?.callableOrNull(),
                    desc.get(Constants.set.js, runtime)?.callableOrNull()
                )

            else ->  JsPropertyAccessor.Value(desc.get(Constants.value.js, runtime))
        }

        runtime.typeCheck(this is JsObject) {
            "$this is not an object".js
        }

        setProperty(
            property = name,
            value = p,
            runtime = runtime,
            enumerable = if (desc.contains(Constants.enumerable.js, runtime)) {
                !runtime.isFalse(desc.get(Constants.enumerable.js, runtime))
            } else false,
            configurable = if (desc.contains(Constants.configurable.js, runtime)) {
                !runtime.isFalse(desc.get(Constants.configurable.js, runtime))
            } else false,
            writable = if (desc.contains(Constants.writable.js, runtime)) {
                !runtime.isFalse(desc.get(Constants.writable.js, runtime))
            } else desc.contains(Constants.set.js, runtime),
        )

        return true
    }

    if (current.configurable == false) {

        if (desc.contains(Constants.configurable.js, runtime)
            && !runtime.isFalse(desc.get(Constants.configurable.js, runtime))
        ) {
            return false
        }

        if (!desc.isGenericDescriptor(runtime)
            && desc.isAccessorDescriptor(runtime) != current.value !is JsPropertyAccessor.BackedField
        ) {
            return true
        }

        if (current.value is JsPropertyAccessor.BackedField) {
            if (desc == current.value) {
                return false
            }
        } else {
            if (current.writable == false) {
                if (desc.contains(Constants.writable.js, runtime) &&
                    !runtime.isFalse(desc.get(Constants.writable.js, runtime))
                ) {
                    return false
                }
                return desc.contains(Constants.value.js, runtime) &&
                        desc.get(Constants.value.js, runtime).sameValue(current.value.get(runtime))
            }
        }
    }
    if (this !is Undefined && this is JsObject){

        val configurable = if (desc.contains(Constants.configurable.js, runtime)){
            !runtime.isFalse(desc.get(Constants.configurable.js, runtime))
        } else {
            current.configurable
        }
        val enumerable = if (desc.contains(Constants.enumerable.js, runtime)){
            !runtime.isFalse(desc.get(Constants.enumerable.js, runtime))
        } else {
            current.enumerable
        }

        val writable = if (desc.contains(Constants.writable.js, runtime))
            !runtime.isFalse(desc.get(Constants.writable.js, runtime))
        else false

        if (desc.isAccessorDescriptor(runtime)){
            setProperty(
                property = name,
                value = JsPropertyAccessor.BackedField(
                    getter = desc.get(Constants.get.js,runtime)?.callableOrNull(),
                    setter = desc.get(Constants.set.js,runtime)?.callableOrNull(),
                ),
                configurable = configurable,
                enumerable = enumerable,
                writable = if (current.value is JsPropertyAccessor.Value && desc.isAccessorDescriptor(runtime)){
                    false
                } else {
                    writable
                },
                runtime = runtime
            )
        } else {
            setProperty(
                property = name,
                value = JsPropertyAccessor.Value(
                    desc.get(Constants.value.js, runtime)
                ),
                configurable = configurable,
                enumerable = enumerable,
                writable = if (desc.contains(Constants.writable.js, runtime))
                    !runtime.isFalse(desc.get(Constants.writable.js, runtime))
                else true,
                runtime = runtime
            )
        }
    }

    return true
}



/**
 * [6.2.6.1 IsAccessorDescriptor ( Desc )](https://tc39.es/ecma262/#sec-isaccessordescriptor)
 *
 * The abstract operation IsAccessorDescriptor takes argument Desc (a Property Descriptor)
 * and returns a Boolean. It performs the following steps when called:
 *
 * 1. If Desc has a [[Get]] field, return true.
 * 2. If Desc has a [[Set]] field, return true.
 * 3. Return false.
 * */
internal suspend fun JsAny.isAccessorDescriptor(runtime: ScriptRuntime) : Boolean {
    return contains(Constants.get.js, runtime) || contains(Constants.set.js, runtime)
}


/**
 * [6.2.6.2 IsDataDescriptor ( Desc )](https://tc39.es/ecma262/#sec-isdatadescriptor)
 * The abstract operation IsDataDescriptor takes argument Desc (a Property Descriptor)
 * and returns a Boolean. It performs the following steps when called:
 *
 * 1. If Desc has a [[Value]] field, return true.
 * 2. If Desc has a [[Writable]] field, return true.
 * 3. Return false.
 * */
internal suspend fun JsAny.isDataDescriptor(runtime: ScriptRuntime) : Boolean {
    return contains(Constants.value.js, runtime) || contains(Constants.writable.js, runtime)
}

/**
 * [6.2.6.3 IsGenericDescriptor ( Desc )](https://tc39.es/ecma262/#sec-isgenericdescriptor)
 *
 * The abstract operation IsGenericDescriptor takes argument Desc (a Property Descriptor)
 * and returns a Boolean. It performs the following steps when called:
 *
 * 1. If IsAccessorDescriptor(Desc) is true, return false.
 * 2. If IsDataDescriptor(Desc) is true, return false.
 * 3. Return true.
 * */
internal suspend fun JsAny.isGenericDescriptor(runtime: ScriptRuntime) : Boolean {
    return contains(Constants.value.js, runtime) || contains(Constants.writable.js, runtime)
}
