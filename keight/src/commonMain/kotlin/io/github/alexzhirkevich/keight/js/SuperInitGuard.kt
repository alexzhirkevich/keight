package io.github.alexzhirkevich.keight.js

import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.js.interpreter.referenceError

/**
 * Wraps a derived-class constructor's `this` binding.
 *
 * Per the ECMAScript spec, in a derived constructor the `this` value is not
 * initialized until `super()` returns. Any property access (get/set/delete/
 * has/keys/...) performed before `super()` is called must throw the same
 * ReferenceError a real JS engine throws:
 * "Must call super constructor in derived class before accessing 'this'
 *  or returning from derived constructor".
 *
 * The guard delegates every operation to the real instance [target] once
 * [isInitialized] reports `super()` has been called, so callers see a
 * transparent `this` afterwards.
 */
internal class SuperInitGuard(
    private val target: JsObject,
    private val isInitialized: () -> Boolean,
    private val runtime: ScriptRuntime
) : JsObjectImpl() {

    private suspend fun check() {
        if (!isInitialized()) {
            runtime.referenceError {
                "Must call super constructor in derived class before accessing 'this' or returning from derived constructor".js
            }
        }
    }

    override suspend fun get(property: JsAny?, runtime: ScriptRuntime): JsAny? {
        check()
        return target.get(property, runtime)
    }

    override suspend fun proto(runtime: ScriptRuntime): JsAny? {
        check()
        return target.proto(runtime)
    }

    override suspend fun set(property: JsAny?, value: JsAny?, runtime: ScriptRuntime) {
        check()
        target.set(property, value, runtime)
    }

    override suspend fun setProperty(
        property: JsAny?,
        value: JsPropertyAccessor,
        runtime: ScriptRuntime,
        enumerable: Boolean?,
        configurable: Boolean?,
        writable: Boolean?
    ) {
        check()
        target.setProperty(property, value, runtime, enumerable, configurable, writable)
    }

    override suspend fun delete(property: JsAny?, runtime: ScriptRuntime): Boolean {
        check()
        return target.delete(property, runtime)
    }

    override suspend fun keys(
        runtime: ScriptRuntime,
        excludeSymbols: Boolean,
        excludeNonEnumerables: Boolean
    ): List<JsAny?> {
        check()
        return target.keys(runtime, excludeSymbols, excludeNonEnumerables)
    }

    override suspend fun values(runtime: ScriptRuntime): List<JsAny?> {
        check()
        return target.values(runtime)
    }

    override suspend fun entries(runtime: ScriptRuntime): List<List<JsAny?>> {
        check()
        return target.entries(runtime)
    }

    override suspend fun ownPropertyDescriptor(property: JsAny?): JsProperty? {
        check()
        return target.ownPropertyDescriptor(property)
    }

    override suspend fun ownPropertyDescriptors(): Map<JsAny?, JsProperty> {
        check()
        return target.ownPropertyDescriptors()
    }

    override suspend fun propertyIsEnumerable(name: JsAny?, runtime: ScriptRuntime): Boolean {
        check()
        return target.propertyIsEnumerable(name, runtime)
    }

    override suspend fun hasOwnProperty(name: JsAny?, runtime: ScriptRuntime): Boolean {
        check()
        return target.hasOwnProperty(name, runtime)
    }

    override var isExtensible: Boolean
        get() = target.isExtensible
        set(value) {
            (target as? JsObjectImpl)?.isExtensible = value
        }

    override fun preventExtensions() {
        (target as? JsObjectImpl)?.preventExtensions() ?: target.preventExtensions()
    }

    override fun toKotlin(runtime: ScriptRuntime): Any {
        return target.toKotlin(runtime)
    }
}
